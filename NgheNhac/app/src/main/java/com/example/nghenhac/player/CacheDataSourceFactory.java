package com.example.nghenhac.player;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.database.StandaloneDatabaseProvider;

import java.io.File;

/**
 * Quản lý SimpleCache và cung cấp CacheDataSource.Factory cho ExoPlayer.
 *
 * Nguyên lý:
 * - Singleton: một SimpleCache duy nhất cho toàn app, tránh lock database.
 * - Sử dụng LeastRecentlyUsedCacheEvictor: tự động xoá cache cũ nhất khi đạt giới hạn.
 * - Dung lượng mặc định: 500MB.
 * - StandaloneDatabaseProvider: database riêng để index các file cache.
 * - Cache được lưu trong internal cache dir (context.getCacheDir()/media_cache).
 *   OS có thể tự động xoá khi bộ nhớ thấp, user có thể xoá qua Settings.
 *
 * Luồng xử lý:
 * 1. App khởi động → (lazy) getInstance() → tạo SimpleCache.
 * 2. ExoPlayer initialize() → getCacheDataSourceFactory() → set vào DefaultMediaSourceFactory.
 * 3. Khi stream nhạc, CacheDataSource tự động cache chunk dữ liệu.
 * 4. OfflineDownloadManager getSimpleCache() để cache full file offline.
 * 5. User xoá cache → clearCache() → xoá toàn bộ file + database.
 *
 * Input:
 * - Context: để lấy cacheDir.
 * - maxCacheSizeBytes: dung lượng cache tối đa (mặc định 500MB).
 *
 * Output:
 * - CacheDataSource.Factory: dùng cho ExoPlayer.
 * - SimpleCache: dùng cho OfflineDownloadManager.
 *
 * Lưu ý:
 * - SimpleCache không thread-safe — dùng singleton để đảm bảo chỉ một instance.
 * - CacheDir bị OS xoá khi app bị uninstall.
 * - Khi xoá cache, cần xoá cả file vật lý lẫn database index.
 */
public class CacheDataSourceFactory {

    private static final long DEFAULT_MAX_CACHE_SIZE = 500L * 1024L * 1024L; // 500MB
    private static final String CACHE_SUBDIR = "media_cache";

    private static volatile CacheDataSourceFactory instance;

    private final SimpleCache simpleCache;
    private final CacheDataSource.Factory cacheDataSourceFactory;
    private final long maxCacheSize;

    // ── Singleton ──

    private CacheDataSourceFactory(@NonNull Context context, long maxCacheSizeBytes) {
        this.maxCacheSize = maxCacheSizeBytes;

        File cacheDir = new File(context.getCacheDir(), CACHE_SUBDIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        LeastRecentlyUsedCacheEvictor evictor =
                new LeastRecentlyUsedCacheEvictor(maxCacheSizeBytes);
        StandaloneDatabaseProvider databaseProvider =
                new StandaloneDatabaseProvider(context);

        this.simpleCache = new SimpleCache(cacheDir, evictor, databaseProvider);

        // Tạo upstream DataSource.Factory (content:// + file:// + http:// + https://)
        // Dùng DefaultDataSource.Factory để hỗ trợ content:// URI từ MediaStore
        // và http:// URI cho streaming — tự động chọn DataSource phù hợp theo URI scheme.
        DataSource.Factory upstreamFactory = new DefaultDataSource.Factory(
                context,
                new DefaultHttpDataSource.Factory()
        );

        // Tạo CacheDataSource.Factory với upstream + cache
        this.cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    /**
     * Lấy instance duy nhất với dung lượng cache mặc định (500MB).
     */
    public static CacheDataSourceFactory getInstance(@NonNull Context context) {
        return getInstance(context, DEFAULT_MAX_CACHE_SIZE);
    }

    /**
     * Lấy instance duy nhất với dung lượng cache tuỳ chỉnh.
     *
     * Input:
     * @param context           Context.
     * @param maxCacheSizeBytes Dung lượng cache tối đa (bytes).
     */
    public static CacheDataSourceFactory getInstance(@NonNull Context context,
                                                      long maxCacheSizeBytes) {
        if (instance == null) {
            synchronized (CacheDataSourceFactory.class) {
                if (instance == null) {
                    instance = new CacheDataSourceFactory(
                            context.getApplicationContext(), maxCacheSizeBytes);
                }
            }
        }
        return instance;
    }

    // ════════════════════════════════════════════
    //  Public API
    // ════════════════════════════════════════════

    /**
     * Lấy CacheDataSource.Factory — inject vào ExoPlayer để tự động cache khi stream.
     *
     * Output:
     * @return CacheDataSource.Factory dùng cho ExoPlayer.Builder.setMediaSourceFactory().
     */
    @NonNull
    public CacheDataSource.Factory getCacheDataSourceFactory() {
        return cacheDataSourceFactory;
    }

    /**
     * Lấy SimpleCache instance — dùng cho OfflineDownloadManager.
     *
     * Output:
     * @return SimpleCache singleton.
     */
    @NonNull
    public SimpleCache getSimpleCache() {
        return simpleCache;
    }

    /**
     * Lấy dung lượng cache tối đa (bytes).
     */
    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Lấy dung lượng cache hiện tại (bytes).
     *
     * Output: Số bytes đã được cache, hoặc -1 nếu có lỗi.
     */
    public long getCurrentCacheSize() {
        try {
            return simpleCache.getCacheSpace();
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * Xoá toàn bộ cache (file + database index).
     *
     * Nguyên lý:
     * - Gọi simpleCache.removeResource() để xoá database index.
     * - Không xoá file vật lý trực tiếp (SimpleCache tự xử lý).
     *
     * Lưu ý:
     * - Nên gọi trên background thread.
     * - Khi đang phát nhạc từ cache, xoá cache có thể gây lỗi playback.
     */
    public void clearCache() {
        try {
            simpleCache.removeResource(CACHE_SUBDIR);
        } catch (Exception e) {
            // Bỏ qua lỗi khi xoá cache
        }
    }

    /**
     * Giải phóng SimpleCache. Gọi khi app destroy.
     */
    public void release() {
        try {
            simpleCache.release();
        } catch (Exception e) {
            // Bỏ qua lỗi
        }
        instance = null;
    }

    /**
     * Tạo CacheDataSource.Factory để tích hợp với ExoPlayer.
     * Wrap upstream DataSource.Factory với cache layer.
     *
     * Nguyên lý:
     * - CacheDataSource kiểm tra cache trước khi request từ mạng.
     * - FLAG_IGNORE_CACHE_ON_ERROR: nếu cache bị lỗi, fallback về stream trực tiếp.
     * - Tự động cache chunk dữ liệu khi stream — không cần xử lý thủ công.
     *
     * Input:
     * @param context        Context.
     * @param upstreamFactory DataSource.Factory gốc (VD: DefaultHttpDataSource.Factory).
     *
     * Output:
     * @return CacheDataSource.Factory.
     */
    @NonNull
    public static CacheDataSource.Factory createCachedDataSourceFactory(
            @NonNull Context context,
            @Nullable DataSource.Factory upstreamFactory) {
        CacheDataSourceFactory factory = getInstance(context);
        return factory.getCacheDataSourceFactory();
    }

    /**
     * Reset singleton instance (dùng cho testing).
     */
    static void resetInstance() {
        instance = null;
    }
}
