package com.example.nghenhac.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.example.nghenhac.data.local.AppDatabase;
import com.example.nghenhac.data.local.MediaStoreScanner;
import com.example.nghenhac.data.local.dao.SongDao;
import com.example.nghenhac.data.local.entity.SongEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository — single source of truth cho dữ liệu bài hát.
 *
 * Nguyên lý:
 * - Kết hợp hai nguồn dữ liệu: Room Database (cache local) và MediaStore (quét thiết bị).
 * - UI observe LiveData từ Room (nhanh, offline-ready, reactive).
 * - Khi cần refresh: scan MediaStore → insertAll vào Room → LiveData tự động cập nhật UI.
 * - Singleton pattern với double-checked locking.
 *
 * Luồng xử lý:
 * 1. UI gọi getAllSongs() → nhận LiveData từ Room (dữ liệu có sẵn, không cần đợi).
 * 2. App khởi động / user kéo refresh → refreshFromMediaStore() → scan → insert → UI tự cập nhật.
 * 3. User thích/bỏ thích bài hát → updateFavorite() → Room notify LiveData → UI cập nhật.
 *
 * Lưu ý:
 * - Tất cả mutations (insert, update, delete) đều chạy trên ExecutorService (background thread).
 * - refreshFromMediaStore() callback-driven, an toàn gọi từ main thread.
 * - refreshFromMediaStoreSync() blocking, chỉ gọi từ background thread.
 */
public class SongRepository {

    private static final String TAG = "SongRepository";
    private static volatile SongRepository instance;

    private final SongDao songDao;
    private final ExecutorService executor;

    // ── Singleton ──

    private SongRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.songDao = db.songDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static SongRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (SongRepository.class) {
                if (instance == null) {
                    instance = new SongRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ════════════════════════════════════════════
    //  Queries (LiveData — observe từ UI)
    // ════════════════════════════════════════════

    /** Lấy tất cả bài hát (LiveData, tự động cập nhật khi dữ liệu thay đổi). */
    public LiveData<List<SongEntity>> getAllSongs() {
        return songDao.getAllSongs();
    }

    /**
     * Lấy tất cả bài hát (phiên bản đồng bộ).
     *
     * Dùng khi cần dữ liệu ngay lập tức (VD: dialog thêm bài vào playlist).
     * Gọi từ background thread để tránh block UI.
     */
    public List<SongEntity> getAllSongsSync() {
        return songDao.getAllSongsSync();
    }

    /** Lấy chi tiết một bài hát theo ID. */
    public LiveData<SongEntity> getSongById(long id) {
        return songDao.getSongById(id);
    }

    /** Tìm kiếm bài hát theo title, artist, album. */
    public LiveData<List<SongEntity>> search(String query) {
        return songDao.search(query);
    }

    /** Lấy danh sách bài hát yêu thích. */
    public LiveData<List<SongEntity>> getFavorites() {
        return songDao.getFavorites();
    }

    /** Lấy bài hát theo album. */
    public LiveData<List<SongEntity>> getByAlbum(String album) {
        return songDao.getByAlbum(album);
    }

    /** Lấy bài hát theo nghệ sĩ. */
    public LiveData<List<SongEntity>> getByArtist(String artist) {
        return songDao.getByArtist(artist);
    }

    /** Lấy danh sách album duy nhất. */
    public LiveData<List<String>> getAllAlbums() {
        return songDao.getAllAlbums();
    }

    /** Lấy danh sách nghệ sĩ duy nhất. */
    public LiveData<List<String>> getAllArtists() {
        return songDao.getAllArtists();
    }

    /** Đếm tổng số bài hát trong database. */
    public LiveData<Integer> getSongCount() {
        return songDao.getSongCount();
    }

    /**
     * Tìm kiếm bài hát (phiên bản đồng bộ).
     *
     * Dùng trong PlaylistImporter để match bài hát khi import playlist.
     * Chạy trực tiếp trên thread hiện tại (gọi từ background thread).
     */
    public List<SongEntity> searchSync(String query) {
        return songDao.searchSync(query);
    }

    // ════════════════════════════════════════════
    //  Sync queries (không LiveData, dùng nội bộ)
    // ════════════════════════════════════════════

    /**
     * Lấy bài hát theo MediaStore ID (phiên bản đồng bộ).
     * Dùng trong quá trình đồng bộ để kiểm tra bài hát đã tồn tại chưa.
     */
    public SongEntity getByMediaStoreIdSync(long mediaStoreId) {
        return songDao.getByMediaStoreIdSync(mediaStoreId);
    }

    // ════════════════════════════════════════════
    //  Mutations (chạy trên background thread)
    // ════════════════════════════════════════════

    /** Thêm hoặc cập nhật một bài hát (REPLACE strategy). */
    public void insertSong(SongEntity song) {
        executor.execute(() -> songDao.insert(song));
    }

    /** Thêm nhiều bài hát cùng lúc (bulk insert). */
    public void insertSongs(List<SongEntity> songs) {
        executor.execute(() -> songDao.insertAll(songs));
    }

    /** Cập nhật thông tin bài hát. */
    public void updateSong(SongEntity song) {
        executor.execute(() -> songDao.update(song));
    }

    /** Cập nhật trạng thái yêu thích của bài hát. */
    public void updateFavorite(long id, boolean isFavorite) {
        executor.execute(() -> songDao.updateFavorite(id, isFavorite));
    }

    /** Xoá bài hát khỏi database. */
    public void deleteSong(SongEntity song) {
        executor.execute(() -> songDao.delete(song));
    }

    // ════════════════════════════════════════════
    //  MediaStore sync (quét & đồng bộ)
    // ════════════════════════════════════════════

    /**
     * Quét MediaStore và đồng bộ vào Room database (phiên bản async).
     *
     * Nguyên lý:
     * - Gửi tác vụ xuống ExecutorService (background thread).
     * - Scan MediaStore → insertAll với REPLACE strategy → gọi callback khi hoàn thành.
     * - LiveData từ Room sẽ tự động cập nhật UI sau khi insert.
     * - An toàn gọi từ main thread (không block).
     *
     * Luồng xử lý:
     * 1. executor.execute() → chạy background.
     * 2. MediaStoreScanner.scanAllSongs() → List<SongEntity>.
     * 3. Nếu rỗng → callback.onComplete(0).
     * 4. Nếu có dữ liệu → songDao.insertAll() → callback.onComplete(count).
     * 5. Exception → callback.onError(e).
     *
     * Input:
     * @param context  Context.
     * @param callback Callback khi hoàn thành (chạy trên thread của executor).
     */
    public void refreshFromMediaStore(@NonNull Context context,
                                      @NonNull OnRefreshComplete callback) {
        executor.execute(() -> {
            try {
                List<SongEntity> scannedSongs = MediaStoreScanner.scanAllSongs(context);

                if (scannedSongs.isEmpty()) {
                    callback.onComplete(0);
                    return;
                }

                List<Long> insertedIds = songDao.insertAll(scannedSongs);
                callback.onComplete(insertedIds.size());
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Quét MediaStore và đồng bộ (phiên bản đồng bộ, blocking).
     *
     * Nguyên lý:
     * - Chạy trực tiếp trên thread hiện tại (không gửi xuống executor).
     * - Chỉ gọi từ background thread (WorkerThread).
     * - Dùng khi cần đảm bảo đồng bộ hoàn thành trước khi tiếp tục.
     *
     * Input:
     * @param context Context.
     *
     * Output:
     * @return Số bài hát đã đồng bộ.
     */
    @WorkerThread
    public int refreshFromMediaStoreSync(@NonNull Context context) {
        List<SongEntity> scannedSongs = MediaStoreScanner.scanAllSongs(context);
        if (scannedSongs.isEmpty()) return 0;
        List<Long> ids = songDao.insertAll(scannedSongs);
        return ids != null ? ids.size() : 0;
    }

    /** Xoá tất cả bài hát (dùng khi reset app). */
    public void deleteAll() {
        executor.execute(() -> songDao.deleteAll());
    }

    // ── Callback ──

    /** Callback cho hoạt động refresh MediaStore. */
    public interface OnRefreshComplete {
        void onComplete(int songCount);
        void onError(Exception e);
    }
}
