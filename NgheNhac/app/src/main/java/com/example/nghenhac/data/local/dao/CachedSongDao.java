package com.example.nghenhac.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.nghenhac.data.local.entity.CachedSongEntity;

import java.util.List;

/**
 * DAO cho thao tác với bảng cached_songs.
 *
 * Nguyên lý:
 * - Quản lý các bài hát đã được cache để phát offline.
 * - Hỗ trợ hai loại cache: chunk (streaming) và full download (offline hoàn toàn).
 * - Thống kê dung lượng cache dùng COALESCE để tránh null khi SUM trên bảng rỗng.
 *
 * Luồng xử lý:
 * 1. OfflineDownloadManager tải nhạc → insert().
 * 2. MusicPlayer phát → kiểm tra getBySongId() → nếu có cache thì dùng file local.
 * 3. User xoá cache → deleteBySongId() hoặc deleteAll().
 * 4. UI hiển thị dung lượng cache → getTotalCacheSize().
 */
@Dao
public interface CachedSongDao {

    // ── Queries ──

    /**
     * Lấy tất cả bài hát đã cache, sắp xếp theo thời gian cache (mới nhất trước).
     *
     * Output: LiveData — tự động cập nhật UI.
     */
    @Query("SELECT * FROM cached_songs ORDER BY cached_at DESC")
    LiveData<List<CachedSongEntity>> getAllCachedSongs();

    /**
     * Lấy thông tin cache của một bài hát theo songId.
     *
     * Nguyên lý:
     * - Mỗi bài hát chỉ có một bản ghi cache (songId unique).
     * - Trả về trực tiếp (không LiveData) vì gọi từ background thread (MusicPlayer).
     *
     * Input: songId — ID bài hát.
     * Output: CachedSongEntity hoặc null nếu chưa cache.
     */
    @Query("SELECT * FROM cached_songs WHERE song_id = :songId LIMIT 1")
    CachedSongEntity getBySongId(long songId);

    /**
     * Lấy danh sách bài hát đã tải full (offline hoàn toàn).
     *
     * Điều kiện: is_full_download = 1.
     * Sắp xếp theo thời gian cache.
     */
    @Query("SELECT * FROM cached_songs WHERE is_full_download = 1 ORDER BY cached_at DESC")
    LiveData<List<CachedSongEntity>> getFullDownloads();

    /**
     * Đếm tổng số bài hát đã cache.
     *
     * Output: LiveData<Integer> — 0 nếu chưa có cache.
     */
    @Query("SELECT COUNT(*) FROM cached_songs")
    LiveData<Integer> getCacheCount();

    /**
     * Tính tổng dung lượng cache (bytes).
     *
     * Nguyên lý:
     * - Dùng COALESCE để trả về 0 thay vì null khi bảng rỗng.
     * - SUM(file_size) tính tổng dung lượng của tất cả file cache.
     *
     * Output: LiveData<Long> — tổng dung lượng bytes, 0 nếu chưa có cache.
     */
    @Query("SELECT COALESCE(SUM(file_size), 0) FROM cached_songs")
    LiveData<Long> getTotalCacheSize();

    // ── Inserts ──

    /**
     * Thêm bản ghi cache mới.
     *
     * Nguyên lý:
     * - OnConflictStrategy.REPLACE: nếu đã cache bài này rồi, cập nhật thông tin mới.
     * - Tránh duplicate cache cho cùng một bài hát.
     *
     * Input: cachedSong — entity cache cần thêm.
     * Output: Row ID của bản ghi.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CachedSongEntity cachedSong);

    // ── Updates ──

    /**
     * Cập nhật thông tin cache (VD: cập nhật fileSize, localFilePath).
     *
     * Trả về số dòng bị ảnh hưởng.
     */
    @Update
    int update(CachedSongEntity cachedSong);

    // ── Deletes ──

    /**
     * Xoá một bản ghi cache.
     *
     * Room tự động match theo primary key.
     */
    @Delete
    int delete(CachedSongEntity cachedSong);

    /**
     * Xoá cache của một bài hát theo songId.
     *
     * Dùng khi user xoá cache của một bài cụ thể.
     *
     * Input: songId — ID bài hát cần xoá cache.
     * Output: Số dòng bị xoá.
     */
    @Query("DELETE FROM cached_songs WHERE song_id = :songId")
    int deleteBySongId(long songId);

    /**
     * Xoá tất cả cache.
     *
     * Dùng khi user chọn "Xoá toàn bộ cache" trong Settings.
     * Không ảnh hưởng đến dữ liệu songs hay playlists.
     * File vật lý trên disk cần được xoá riêng (OfflineDownloadManager).
     */
    @Query("DELETE FROM cached_songs")
    int deleteAll();
}
