package com.example.nghenhac.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.nghenhac.data.local.entity.SongEntity;

import java.util.List;

/**
 * DAO cho thao tác với bảng songs.
 *
 * Nguyên lý:
 * - Cung cấp các phương thức truy vấn dữ liệu bài hát từ Room database.
 * - Các phương thức trả về LiveData sẽ tự động cập nhật UI khi dữ liệu thay đổi (reactive).
 * - Dùng OnConflictStrategy.REPLACE để tránh duplicate khi đồng bộ từ MediaStore.
 *
 * Luồng xử lý:
 * 1. UI gọi Repository → Repository gọi DAO → Room thực thi SQL → trả về LiveData.
 * 2. Khi có insert/update/delete → Room tự động notify các LiveData liên quan.
 *
 * Lưu ý:
 * - Các query LIKE dùng toán tử || (SQLite concat) thay vì hàm CONCAT().
 * - getByMediaStoreIdSync và getSongByIdSync trả về trực tiếp (không LiveData) dùng cho đồng bộ nội bộ.
 * - search() tìm kiếm trên title, artist, album cùng lúc.
 */
@Dao
public interface SongDao {

    // ── Queries ──

    /**
     * Lấy tất cả bài hát, sắp xếp theo title A-Z.
     *
     * Output: LiveData — tự động cập nhật UI khi danh sách thay đổi.
     */
    @Query("SELECT * FROM songs ORDER BY title ASC")
    LiveData<List<SongEntity>> getAllSongs();

    /**
     * Lấy tất cả bài hát (phiên bản đồng bộ, không LiveData).
     *
     * Dùng khi cần dữ liệu ngay lập tức mà không observe (VD: dialog thêm bài vào playlist).
     * Gọi từ background thread để tránh block UI.
     */
    @Query("SELECT * FROM songs ORDER BY title ASC")
    List<SongEntity> getAllSongsSync();

    /**
     * Lấy bài hát theo ID.
     *
     * Input: id — khoá chính của bài hát.
     * Output: LiveData — null nếu không tìm thấy.
     */
    @Query("SELECT * FROM songs WHERE id = :id")
    LiveData<SongEntity> getSongById(long id);

    /**
     * Tìm kiếm bài hát theo title, artist hoặc album.
     *
     * Nguyên lý:
     * - LIKE với wildcard % ở cả hai đầu để match substring.
     * - Tìm trên nhiều trường cùng lúc, sắp xếp theo title.
     *
     * Input: query — từ khoá tìm kiếm.
     * Output: Danh sách bài hát phù hợp.
     *
     * Lưu ý: Không phân biệt hoa/thường (LIKE mặc định của SQLite không phân biệt với ASCII).
     */
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' " +
            "OR artist LIKE '%' || :query || '%' " +
            "OR album LIKE '%' || :query || '%' " +
            "ORDER BY title ASC")
    LiveData<List<SongEntity>> search(String query);

    /**
     * Lấy danh sách bài hát yêu thích.
     *
     * Điều kiện: is_favorite = 1, sắp xếp theo title.
     */
    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY title ASC")
    LiveData<List<SongEntity>> getFavorites();

    /**
     * Lấy bài hát theo album.
     *
     * Sắp xếp theo track_number để giữ đúng thứ tự bài trong album.
     */
    @Query("SELECT * FROM songs WHERE album = :album ORDER BY track_number ASC")
    LiveData<List<SongEntity>> getByAlbum(String album);

    /**
     * Lấy bài hát theo nghệ sĩ.
     *
     * Sắp xếp theo album, sau đó track_number.
     */
    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, track_number ASC")
    LiveData<List<SongEntity>> getByArtist(String artist);

    /**
     * Tìm kiếm bài hát (phiên bản đồng bộ, không LiveData).
     *
     * Dùng trong PlaylistImporter để match bài hát khi import playlist.
     */
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' " +
            "OR artist LIKE '%' || :query || '%' " +
            "OR album LIKE '%' || :query || '%' " +
            "ORDER BY title ASC")
    List<SongEntity> searchSync(String query);

    /**
     * Lấy bài hát theo MediaStore ID (phiên bản đồng bộ, không LiveData).
     *
     * Nguyên lý:
     * - Dùng trong quá trình đồng bộ MediaStore → Room để kiểm tra bài hát đã tồn tại chưa.
     * - Trả về trực tiếp SongEntity (không LiveData) vì gọi từ background thread.
     *
     * Input: mediaStoreId — ID từ MediaStore.Audio.Media.
     * Output: SongEntity hoặc null nếu chưa có trong database.
     */
    @Query("SELECT * FROM songs WHERE media_store_id = :mediaStoreId LIMIT 1")
    SongEntity getByMediaStoreIdSync(long mediaStoreId);

    /**
     * Lấy toàn bộ cặp (id, media_store_id) hiện có trong DB — dùng để build map tra cứu nhanh.
     *
     * Nguyên lý:
     * - Dùng trong quá trình đồng bộ MediaStore → Room để giữ nguyên id cũ cho bài hát đã tồn tại,
     *   tránh việc REPLACE (do trùng unique index media_store_id) xoá + tạo lại bản ghi với id MỚI,
     *   điều này sẽ làm mất liên kết playlist_songs / favorites vì FK CASCADE sẽ xoá theo.
     * - Trả về 1 lần duy nhất (bulk) thay vì query từng bài (N+1) để tối ưu hiệu năng khi thư viện lớn.
     *
     * Output: Danh sách IdMapping(id, mediaStoreId) cho các bài hát có media_store_id khác null.
     */
    @Query("SELECT id, media_store_id AS mediaStoreId, is_favorite AS isFavorite FROM songs WHERE media_store_id IS NOT NULL")
    List<SongIdMapping> getMediaStoreIdMappings();

    /**
     * POJO nhẹ dùng cho getMediaStoreIdMappings() — chỉ chứa các cột cần thiết,
     * tránh phải load toàn bộ SongEntity (nhiều cột) chỉ để lấy id.
     *
     * isFavorite được thêm vào để giữ nguyên trạng thái yêu thích khi đồng bộ lại
     * (updateAll() ghi đè toàn bộ cột, nếu không mang theo isFavorite cũ thì bài hát
     * quét mới sẽ có is_favorite = false mặc định, làm mất trạng thái yêu thích).
     */
    class SongIdMapping {
        public long id;
        public long mediaStoreId;
        public boolean isFavorite;
    }

    /**
     * Lấy bài hát theo ID (phiên bản đồng bộ).
     *
     * Dùng nội bộ khi cần thao tác với entity mà không cần LiveData.
     */
    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    SongEntity getSongByIdSync(long id);

    /**
     * Đếm tổng số bài hát trong database.
     */
    @Query("SELECT COUNT(*) FROM songs")
    LiveData<Integer> getSongCount();

    /**
     * Lấy danh sách album duy nhất (DISTINCT), sắp xếp theo tên.
     *
     * Dùng cho màn hình Library tab "Album".
     */
    @Query("SELECT DISTINCT album FROM songs ORDER BY album ASC")
    LiveData<List<String>> getAllAlbums();

    /**
     * Lấy danh sách nghệ sĩ duy nhất (DISTINCT), sắp xếp theo tên.
     *
     * Dùng cho màn hình Library tab "Nghệ sĩ".
     */
    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    LiveData<List<String>> getAllArtists();

    // ── Inserts ──

    /**
     * Thêm một bài hát mới.
     *
     * Nguyên lý:
     * - OnConflictStrategy.REPLACE: nếu đã tồn tại (conflict trên unique index), ghi đè.
     * - Trả về ID của bản ghi mới hoặc bản ghi bị ghi đè.
     *
     * Input: song — entity cần thêm.
     * Output: Row ID của bản ghi (long).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SongEntity song);

    /**
     * Thêm nhiều bài hát cùng lúc trong một transaction.
     *
     * Nguyên lý:
     * - Bulk insert hiệu quả hơn insert từng cái một.
     * - REPLACE strategy xử lý trùng lặp tự động.
     *
     * Input: songs — danh sách entity cần thêm.
     * Output: Danh sách row IDs.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<SongEntity> songs);

    // ── Updates ──

    /**
     * Cập nhật thông tin bài hát.
     *
     * Room tự động match theo primary key (id) để update.
     * Trả về số dòng bị ảnh hưởng (1 nếu thành công, 0 nếu không tìm thấy).
     */
    @Update
    int update(SongEntity song);

    /**
     * Cập nhật nhiều bài hát cùng lúc (UPDATE tại chỗ theo primary key).
     *
     * Nguyên lý — quan trọng để tránh mất playlist khi đồng bộ MediaStore:
     * - Room @Update sinh ra câu lệnh UPDATE thuần (UPDATE songs SET ... WHERE id = ?),
     *   KHÔNG phải DELETE + INSERT như OnConflictStrategy.REPLACE.
     * - Vì không có DELETE nào xảy ra trên bảng songs, ràng buộc FK ON DELETE CASCADE
     *   của playlist_songs (và cached_songs) sẽ KHÔNG bị kích hoạt.
     * - Dùng method này cho các bài hát đã tồn tại (match theo media_store_id) khi
     *   đồng bộ lại từ MediaStore, thay vì insertAll() với REPLACE — để giữ nguyên
     *   liên kết playlist và trạng thái yêu thích.
     *
     * Input: songs — danh sách entity cần cập nhật (đã có id trùng bản ghi cũ).
     */
    @Update
    void updateAll(List<SongEntity> songs);

    /**
     * Cập nhật trạng thái yêu thích của bài hát.
     *
     * Nguyên lý:
     * - Query update trực tiếp thay vì load entity rồi update để tránh overhead.
     * - isFavorite: 1 = yêu thích, 0 = không yêu thích.
     *
     * Input:
     * @param id         ID bài hát
     * @param isFavorite true = thích, false = bỏ thích
     *
     * Output: Số dòng bị ảnh hưởng.
     */
    @Query("UPDATE songs SET is_favorite = :isFavorite WHERE id = :id")
    int updateFavorite(long id, boolean isFavorite);

    // ── Deletes ──

    /**
     * Xoá bài hát khỏi database.
     *
     * Room tự động match theo primary key. Các bản ghi liên quan (playlist_songs, cached_songs) được CASCADE xoá.
     */
    @Delete
    int delete(SongEntity song);

    /**
     * Xoá bài hát theo ID.
     *
     * Query delete trực tiếp, không cần load entity.
     */
    @Query("DELETE FROM songs WHERE id = :id")
    int deleteById(long id);

    /**
     * Xoá tất cả bài hát trong database.
     *
     * Dùng khi reset app hoặc đồng bộ lại từ đầu.
     * Lưu ý: CASCADE sẽ xoá luôn dữ liệu liên quan (playlist_songs, cached_songs).
     */
    @Query("DELETE FROM songs")
    int deleteAll();
}