package com.example.nghenhac.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.local.entity.PlaylistSongCrossRef;
import com.example.nghenhac.data.local.entity.SongEntity;

import java.util.List;

/**
 * DAO cho thao tác với bảng playlists và bảng trung gian playlist_songs.
 *
 * Nguyên lý:
 * - Quản lý danh sách phát và quan hệ N-N với bài hát thông qua bảng trung gian.
 * - Khi playlist bị xoá, các bản ghi trong playlist_songs tự động bị xoá (FK CASCADE).
 * - updateSongCount dùng subquery để đồng bộ số lượng bài hát.
 *
 * Luồng xử lý:
 * 1. User tạo playlist → insert() → Room tạo bản ghi.
 * 2. User thêm bài hát → addSongToPlaylist() → updateSongCount().
 * 3. User xoá playlist → deleteById() → CASCADE xoá playlist_songs.
 *
 * Lưu ý:
 * - Các phương thức JOIN (getSongsInPlaylist) dùng SELECT s.* để lấy toàn bộ trường của SongEntity.
 * - isSongInPlaylist dùng COUNT để kiểm tra tồn tại, không load dữ liệu.
 */
@Dao
public interface PlaylistDao {

    // ════════════════════════════════════════════
    //  Playlist CRUD
    // ════════════════════════════════════════════

    /**
     * Lấy tất cả playlist, sắp xếp theo tên A-Z.
     *
     * Output: LiveData — tự động cập nhật UI khi có thay đổi.
     */
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    LiveData<List<PlaylistEntity>> getAllPlaylists();

    /**
     * Lấy tất cả playlist (phiên bản đồng bộ, không LiveData).
     *
     * Dùng khi cần dữ liệu ngay lập tức mà không observe (VD: picker dialog thêm bài vào playlist).
     * Gọi từ background thread để tránh block UI.
     */
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    List<PlaylistEntity> getAllPlaylistsSync();

    /** Lấy tất cả bài hát trong playlist (sync, không LiveData). Dùng cho background sync. */
    @Query("SELECT s.* FROM songs s " +
            "INNER JOIN playlist_songs ps ON s.id = ps.song_id " +
            "WHERE ps.playlist_id = :playlistId " +
            "ORDER BY ps.order_index ASC")
    List<SongEntity> getSongsInPlaylistSync(long playlistId);

    /** Tìm playlist theo tên (sync). Dùng để tránh tạo trùng khi download sync. */
    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    PlaylistEntity findByNameSync(String name);

    /**
     * Lấy playlist theo ID.
     *
     * Input: id — khoá chính của playlist.
     * Output: LiveData — null nếu không tìm thấy.
     */
    @Query("SELECT * FROM playlists WHERE id = :id")
    LiveData<PlaylistEntity> getPlaylistById(long id);

    /**
     * Lấy playlist theo ID (phiên bản đồng bộ, không LiveData).
     *
     * Dùng nội bộ khi cần thao tác với entity mà không cần observe.
     * Trả về trực tiếp PlaylistEntity hoặc null.
     */
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    PlaylistEntity getPlaylistByIdSync(long id);

    /**
     * Thêm playlist mới.
     *
     * OnConflictStrategy.REPLACE: ghi đè nếu trùng tên.
     * Trả về ID của playlist vừa tạo.
     *
     * Input: playlist — entity playlist cần thêm.
     * Output: Row ID (long).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PlaylistEntity playlist);

    /**
     * Cập nhật thông tin playlist (tên, mô tả, ...).
     *
     * Room tự động match theo primary key.
     * Trả về số dòng bị ảnh hưởng.
     */
    @Update
    int update(PlaylistEntity playlist);

    /**
     * Cập nhật song_count của playlist dựa trên số bản ghi trong playlist_songs.
     *
     * Nguyên lý:
     * - Dùng subquery COUNT(*) để tính số bài hát hiện tại.
     * - Tránh mất đồng bộ giữa song_count và số bản ghi thực tế.
     *
     * Input: playlistId — ID playlist cần cập nhật.
     * Output: Số dòng bị ảnh hưởng.
     */
    @Query("UPDATE playlists SET song_count = (SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId) WHERE id = :playlistId")
    int updateSongCount(long playlistId);

    /**
     * Xoá playlist.
     * FK CASCADE tự động xoá tất cả bản ghi trong playlist_songs của playlist này.
     */
    @Delete
    int delete(PlaylistEntity playlist);

    /**
     * Xoá playlist theo ID.
     *
     * Query trực tiếp, không cần load entity. CASCADE xoá bản ghi liên quan.
     */
    @Query("DELETE FROM playlists WHERE id = :id")
    int deleteById(long id);

    // ════════════════════════════════════════════
    //  Playlist ↔ Song (quan hệ N-N)
    // ════════════════════════════════════════════

    /**
     * Thêm bài hát vào playlist.
     *
     * Nguyên lý:
     * - Thêm một bản ghi vào bảng trung gian playlist_songs.
     * - OnConflictStrategy.IGNORE: nếu bài hát đã có trong playlist, bỏ qua.
     *
     * Input: crossRef — đối tượng chứa playlistId, songId, orderIndex.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addSongToPlaylist(PlaylistSongCrossRef crossRef);

    /**
     * Thêm nhiều bài hát vào playlist cùng lúc.
     *
     * Bulk insert trong một transaction, hiệu quả hơn insert từng cái.
     * IGNORE strategy: bỏ qua các bài đã có trong playlist.
     *
     * Input: crossRefs — danh sách PlaylistSongCrossRef.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addSongsToPlaylist(List<PlaylistSongCrossRef> crossRefs);

    /**
     * Xoá bài hát khỏi playlist.
     *
     * Query DELETE với composite key (playlistId, songId).
     * Trả về số dòng bị xoá (1 nếu thành công, 0 nếu không tìm thấy).
     *
     * Input:
     * @param playlistId ID playlist
     * @param songId     ID bài hát
     */
    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    int removeSongFromPlaylist(long playlistId, long songId);

    /**
     * Xoá tất cả bài hát khỏi playlist (giữ lại playlist).
     *
     * Dùng khi user muốn xoá toàn bộ bài hát nhưng giữ lại playlist trống.
     * Sau đó cần gọi updateSongCount() để đồng bộ song_count về 0.
     *
     * Input: playlistId — ID playlist cần clear.
     */
    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId")
    int clearPlaylist(long playlistId);

    /**
     * Lấy tất cả bài hát trong playlist (JOIN query).
     *
     * Nguyên lý:
     * - INNER JOIN songs với playlist_songs qua song_id.
     * - Sắp xếp theo order_index để giữ thứ tự user đã sắp xếp.
     * - SELECT s.* để lấy toàn bộ trường của SongEntity.
     *
     * Input: playlistId — ID playlist.
     * Output: LiveData — danh sách bài hát trong playlist.
     */
    @Query("SELECT s.* FROM songs s " +
            "INNER JOIN playlist_songs ps ON s.id = ps.song_id " +
            "WHERE ps.playlist_id = :playlistId " +
            "ORDER BY ps.order_index ASC")
    LiveData<List<SongEntity>> getSongsInPlaylist(long playlistId);

    /**
     * Kiểm tra bài hát đã có trong playlist chưa.
     *
     * Nguyên lý:
     * - Dùng COUNT thay vì SELECT * để tối ưu hiệu năng.
     * - Trả về số bản ghi tìm thấy (0 = chưa có, > 0 = đã có).
     *
     * Input:
     * @param playlistId ID playlist
     * @param songId     ID bài hát
     *
     * Output: int — số bản ghi tìm thấy.
     */
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    int isSongInPlaylist(long playlistId, long songId);

    /**
     * Lấy URI ảnh bìa của bài hát đầu tiên trong playlist.
     *
     * Nguyên lý:
     * - JOIN với songs, sắp xếp theo order_index, lấy LIMIT 1.
     * - Dùng để hiển thị ảnh bìa playlist trong danh sách.
     *
     * Input:
     * @param playlistId ID playlist.
     *
     * Output:
     * @return String URI ảnh bìa (có thể null nếu playlist rỗng).
     */
    @Query("SELECT s.album_art_uri FROM songs s " +
            "INNER JOIN playlist_songs ps ON s.id = ps.song_id " +
            "WHERE ps.playlist_id = :playlistId " +
            "ORDER BY ps.order_index ASC LIMIT 1")
    String getFirstSongAlbumArtUri(long playlistId);

    /**
     * Lấy tên bài hát đầu tiên trong playlist.
     *
     * Dùng để hiển thị subtitle cho playlist trong danh sách.
     *
     * Input:
     * @param playlistId ID playlist.
     *
     * Output:
     * @return String tên bài hát đầu tiên (có thể null).
     */
    @Query("SELECT s.title FROM songs s " +
            "INNER JOIN playlist_songs ps ON s.id = ps.song_id " +
            "WHERE ps.playlist_id = :playlistId " +
            "ORDER BY ps.order_index ASC LIMIT 1")
    String getFirstSongTitle(long playlistId);
}