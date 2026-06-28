package com.example.nghenhac.player;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;


import com.example.nghenhac.data.local.AppDatabase;
import com.example.nghenhac.data.local.dao.CachedSongDao;
import com.example.nghenhac.data.local.entity.CachedSongEntity;
import com.example.nghenhac.data.local.entity.SongEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quản lý tải nhạc offline — tải toàn bộ file nhạc về thiết bị để nghe khi không có mạng.
 *
 * Nguyên lý:
 * - Dùng ExecutorService với single thread để quản lý hàng đợi tải (tránh xung đột I/O).
 * - Hỗ trợ hai cơ chế cache:
 *   + Cache chunk (qua CacheDataSource): tự động, phát lại được offline sau khi đã stream.
 *   + Full download (qua OfflineDownloadManager): tải toàn bộ file cho offline hoàn toàn.
 * - Lưu file vào external cache dir: không chiếm bộ nhớ trong, user dễ dàng clear.
 * - Cập nhật CachedSongDao để lưu metadata cache cho UI hiển thị.
 *
 * Luồng xử lý:
 * 1. User chọn bài hát để tải offline → downloadSong(song) → thêm vào queue.
 * 2. ExecutorService xử lý từng tác vụ → tải file → ghi vào cache → update database.
 * 3. Progress được báo qua OnDownloadListener (nếu có).
 * 4. User có thể cancel → cancelDownload(songId).
 * 5. User xoá cache → deleteCacheForSong() / clearAllCache().
 * 6. Dọn dẹp cache cũ (LRU) được xử lý bởi CacheDataSourceFactory.
 *
 * Input:
 * - SongEntity: bài hát cần tải offline.
 *
 * Output:
 * - File cache được lưu trong external cache dir.
 * - Bản ghi CachedSongEntity được thêm vào Room database.
 *
 * Lưu ý:
 * - Tất cả tác vụ tải đều chạy trên background thread.
 * - Callback (OnDownloadListener) chạy trên thread của executor — không cập nhật UI trực tiếp.
 * - Context dùng getApplicationContext() để tránh memory leak.
 */
public class OfflineDownloadManager {

    private static final String TAG = "OfflineDownloadManager";
    private static final String OFFLINE_DIR = "offline_songs";

    private static volatile OfflineDownloadManager instance;

    private final Context context;
    private final CachedSongDao cachedSongDao;
    private final ExecutorService executor;
    private final Map<Long, Future<?>> activeDownloads;
    private final AtomicBoolean isRunning;
    private final File offlineDir;

    @Nullable
    private OnDownloadListener downloadListener;

    /** Callback cho trạng thái tải. */
    public interface OnDownloadListener {
        /** Bắt đầu tải. */
        default void onDownloadStarted(long songId) {}
        /** Tiến trình tải (0.0 - 1.0). */
        default void onDownloadProgress(long songId, float progress) {}
        /** Tải hoàn thành. */
        default void onDownloadCompleted(long songId, String localPath) {}
        /** Tải thất bại. */
        default void onDownloadFailed(long songId, String error) {}
        /** Tải bị cancel. */
        default void onDownloadCancelled(long songId) {}
    }

    // ── Singleton ──

    private OfflineDownloadManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.cachedSongDao = AppDatabase.getInstance(context).cachedSongDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.activeDownloads = new HashMap<>();
        this.isRunning = new AtomicBoolean(true);

        // Tạo thư mục offline
        this.offlineDir = new File(this.context.getExternalCacheDir(), OFFLINE_DIR);
        if (!offlineDir.exists()) {
            offlineDir.mkdirs();
        }
    }

    public static OfflineDownloadManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (OfflineDownloadManager.class) {
                if (instance == null) {
                    instance = new OfflineDownloadManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ════════════════════════════════════════════
    //  Download Operations
    // ════════════════════════════════════════════

    /**
     * Tải một bài hát về offline (full file).
     *
     * Nguyên lý:
     * - Kiểm tra bài hát đã được tải chưa (tránh tải trùng).
     * - Nếu song là local (MediaStore URI) → copy từ thiết bị vào cache.
     * - Nếu song là streaming (HTTP URL) → tải từ mạng về cache.
     * - Sau khi tải xong → lưu thông tin vào CachedSongDao.
     *
     * Input:
     * @param song Bài hát cần tải offline.
     */
    public void downloadSong(@NonNull SongEntity song) {
        if (!isRunning.get()) return;

        long songId = song.getId();

        // Kiểm tra đã tải hay đang tải chưa
        if (activeDownloads.containsKey(songId)) {
            Log.d(TAG, "Song " + songId + " is already being downloaded");
            return;
        }

        CachedSongEntity existing = cachedSongDao.getBySongId(songId);
        if (existing != null && existing.isFullDownload()) {
            Log.d(TAG, "Song " + songId + " already fully downloaded");
            return;
        }

        Future<?> future = executor.submit(() -> {
            downloadFile(song);
        });

        activeDownloads.put(songId, future);
        notifyStarted(songId);
    }

    /**
     * Tải nhiều bài hát cùng lúc (thêm vào hàng đợi).
     *
     * Input:
     * @param songs Danh sách bài hát cần tải.
     */
    public void downloadSongs(@NonNull List<SongEntity> songs) {
        for (SongEntity song : songs) {
            downloadSong(song);
        }
    }

    /**
     * Thực hiện tải file (chạy trên background thread).
     */
    @WorkerThread
    private void downloadFile(@NonNull SongEntity song) {
        long songId = song.getId();
        String filePath = song.getFilePath();

        if (filePath == null || filePath.isEmpty()) {
            notifyFailed(songId, "Invalid file path");
            return;
        }

        try {
            String ext = getFileExtension(song.getMimeType());
            String baseName = songId + "_" + sanitizeFileName(song.getTitle());
            File tmpFile = new File(offlineDir, baseName + ".tmp");
            File outputFile = new File(offlineDir, baseName + ext);

            if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
                copyLocalFile(filePath, tmpFile, song);
            } else {
                downloadFromHttp(filePath, tmpFile, song);
            }

            // Đổi tên từ .tmp → tên thật (thành công mới giữ lại)
            if (outputFile.exists()) {
                outputFile.delete();
            }
            tmpFile.renameTo(outputFile);

            // Cập nhật database
            CachedSongEntity cacheEntity = new CachedSongEntity(
                    songId, outputFile.getAbsolutePath(),
                    outputFile.length(), true);
            cachedSongDao.insert(cacheEntity);

            activeDownloads.remove(songId);
            notifyCompleted(songId, outputFile.getAbsolutePath());

        } catch (InterruptedException e) {
            // Bị cancel
            activeDownloads.remove(songId);
            notifyCancelled(songId);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.e(TAG, "Download failed for song " + songId, e);
            activeDownloads.remove(songId);
            notifyFailed(songId, e.getMessage());
        }
    }

    /**
     * Copy file nhạc từ MediaStore URI vào cache (cho nhạc local).
     */
    @WorkerThread
    private void copyLocalFile(@NonNull String sourceUri, @NonNull File outputFile,
                                @NonNull SongEntity song) throws Exception {
        try (InputStream input = context.getContentResolver()
                .openInputStream(Uri.parse(sourceUri));
             FileOutputStream output = new FileOutputStream(outputFile)) {

            if (input == null) throw new Exception("Cannot open input stream");

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            long fileSize = song.getFileSize();
            while ((bytesRead = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    outputFile.delete();
                    throw new InterruptedException();
                }
                output.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (fileSize > 0) {
                    notifyProgress(song.getId(), (float) totalRead / fileSize);
                }
            }
        }
    }

    /**
     * Tải file nhạc từ HTTP URL (cho nhạc streaming).
     */
    @WorkerThread
    private void downloadFromHttp(@NonNull String url, @NonNull File outputFile,
                                   @NonNull SongEntity song) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP " + responseCode);
            }

            long fileSize = connection.getContentLengthLong();
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalRead = 0;
                while ((bytesRead = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) {
                        outputFile.delete();
                        throw new InterruptedException();
                    }
                    output.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    if (fileSize > 0) {
                        notifyProgress(song.getId(), (float) totalRead / fileSize);
                    }
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ════════════════════════════════════════════
    //  Cache Management
    // ════════════════════════════════════════════

    /**
     * Xoá cache của một bài hát cụ thể.
     *
     * Nguyên lý:
     * - Xoá file vật lý trong offline cache dir.
     * - Xoá bản ghi trong CachedSongDao.
     *
     * Input:
     * @param songId ID bài hát cần xoá cache.
     */
    public void deleteCacheForSong(long songId) {
        CachedSongEntity cache = cachedSongDao.getBySongId(songId);
        if (cache == null) return;

        // Xoá file vật lý
        File file = new File(cache.getLocalFilePath());
        if (file.exists()) {
            file.delete();
        }

        // Xoá database record
        cachedSongDao.deleteBySongId(songId);
    }

    /**
     * Xoá toàn bộ cache offline.
     *
     * Nguyên lý:
     * - Huỷ tất cả tác vụ đang chạy.
     * - Xoá tất cả file trong offline dir.
     * - Xoá tất cả bản ghi trong CachedSongDao.
     *
     * Input:
     * @param clearDatabase Có xoá database record không (true = xoá cả).
     */
    public void clearAllCache(boolean clearDatabase) {
        // Huỷ tất cả tác vụ đang chạy
        cancelAll();

        // Xoá file vật lý
        File[] files = offlineDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }

        // Xoá database record trên background thread
        if (clearDatabase) {
            executor.execute(() -> cachedSongDao.deleteAll());
        }
    }

    /**
     * Tính tổng dung lượng cache offline (bytes).
     *
     * Output:
     * @return Tổng dung lượng các file trong offline dir.
     */
    public long getCacheSize() {
        long totalSize = 0;
        File[] files = offlineDir.listFiles();
        if (files != null) {
            for (File file : files) {
                totalSize += file.length();
            }
        }
        return totalSize;
    }

    // ════════════════════════════════════════════
    //  Cancel / Pause
    // ════════════════════════════════════════════

    /**
     * Huỷ tải một bài hát cụ thể.
     *
     * Input:
     * @param songId ID bài hát cần huỷ.
     */
    public void cancelDownload(long songId) {
        Future<?> future = activeDownloads.remove(songId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    /**
     * Huỷ tất cả tác vụ đang tải.
     */
    public void cancelAll() {
        for (Future<?> future : activeDownloads.values()) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        activeDownloads.clear();
    }

    /** Kiểm tra bài hát có đang được tải không. */
    public boolean isDownloading(long songId) {
        return activeDownloads.containsKey(songId);
    }

    /** Kiểm tra bài hát đã được tải offline chưa. */
    public boolean isDownloaded(long songId) {
        CachedSongEntity cache = cachedSongDao.getBySongId(songId);
        return cache != null && cache.isFullDownload();
    }

    /** Lấy danh sách songId đang tải. */
    @NonNull
    public List<Long> getActiveDownloadIds() {
        return new ArrayList<>(activeDownloads.keySet());
    }

    // ════════════════════════════════════════════
    //  Listener
    // ════════════════════════════════════════════

    public void setDownloadListener(@Nullable OnDownloadListener listener) {
        this.downloadListener = listener;
    }

    private void notifyStarted(long songId) {
        if (downloadListener != null) {
            downloadListener.onDownloadStarted(songId);
        }
    }

    private void notifyProgress(long songId, float progress) {
        if (downloadListener != null) {
            downloadListener.onDownloadProgress(songId, progress);
        }
    }

    private void notifyCompleted(long songId, String localPath) {
        if (downloadListener != null) {
            downloadListener.onDownloadCompleted(songId, localPath);
        }
    }

    private void notifyFailed(long songId, String error) {
        if (downloadListener != null) {
            downloadListener.onDownloadFailed(songId, error);
        }
    }

    private void notifyCancelled(long songId) {
        if (downloadListener != null) {
            downloadListener.onDownloadCancelled(songId);
        }
    }

    // ════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════

    /**
     * Giải phóng tài nguyên. Gọi khi app destroy.
     * Huỷ tất cả tác vụ đang chạy và shutdown executor.
     */
    public void destroy() {
        isRunning.set(false);
        cancelAll();
        executor.shutdownNow();
        instance = null;
    }

    // ════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════

    /**
     * Làm sạch tên file — loại bỏ ký tự đặc biệt không hợp lệ cho file system.
     */
    @NonNull
    private static String sanitizeFileName(@NonNull String name) {
        return name.replaceAll("[^a-zA-Z0-9\\-_.]", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("(^_|_$)", "");
    }

    /**
     * Map MIME type thành extension file.
     *
     * Nguyên lý:
     * - Dùng HashMap tra cứu nhanh các MIME type audio phổ biến.
     * - Fallback về .mp3 nếu không xác định được.
     *
     * Input:
     * @param mimeType MIME type (VD: audio/mpeg, audio/flac, audio/ogg).
     *
     * Output:
     * @return Extension file (VD: .mp3, .flac, .ogg).
     */
    @NonNull
    private static String getFileExtension(@Nullable String mimeType) {
        if (mimeType == null) return ".mp3";
        switch (mimeType.toLowerCase()) {
            case "audio/mpeg":
            case "audio/mp3":
                return ".mp3";
            case "audio/flac":
                return ".flac";
            case "audio/ogg":
            case "application/ogg":
                return ".ogg";
            case "audio/m4a":
            case "audio/mp4":
            case "audio/aac":
                return ".m4a";
            case "audio/wav":
            case "audio/wave":
                return ".wav";
            case "audio/x-ms-wma":
                return ".wma";
            case "audio/opus":
                return ".opus";
            default:
                return ".mp3";
        }
    }
}
