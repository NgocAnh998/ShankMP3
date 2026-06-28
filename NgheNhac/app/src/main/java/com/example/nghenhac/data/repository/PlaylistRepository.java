package com.example.nghenhac.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.nghenhac.data.local.AppDatabase;
import com.example.nghenhac.data.local.dao.PlaylistDao;
import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.local.entity.PlaylistSongCrossRef;
import com.example.nghenhac.data.local.entity.SongEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository — single source of truth cho dữ liệu playlist.
 *
 * Nguyên lý:
 * - Quản lý CRUD playlist và quan hệ N-N với SongEntity qua PlaylistSongCrossRef.
 * - Tự động đồng bộ songCount sau mỗi lần thêm/xoá bài hát.
 * - Tất cả mutations chạy trên ExecutorService (background thread).
 * - Singleton pattern với double-checked locking.
 *
 * Luồng xử lý:
 * 1. User tạo playlist → createPlaylist() → insert → callback trả về ID.
 * 2. User thêm bài hát → addSongToPlaylist() → insert CrossRef → updateSongCount().
 * 3. User xoá bài hát khỏi playlist → removeSongFromPlaylist() → updateSongCount().
 * 4. UI observe getAllPlaylists() / getSongsInPlaylist() → LiveData tự động cập nhật.
 *
 * Lưu ý:
 * - createPlaylist() callback-driven, an toàn gọi từ main thread.
 * - isSongInPlaylist() chạy đồng bộ (phù hợp gọi từ background thread).
 * - CASCADE xoá tự động khi delete playlist.
 */
public class PlaylistRepository {

    private static final String TAG = "PlaylistRepository";
    private static volatile PlaylistRepository instance;

    private final PlaylistDao playlistDao;
    private final ExecutorService executor;

    // ── Singleton ──

    private PlaylistRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.playlistDao = db.playlistDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static PlaylistRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (PlaylistRepository.class) {
                if (instance == null) {
                    instance = new PlaylistRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ════════════════════════════════════════════
    //  Queries (LiveData)
    // ════════════════════════════════════════════

    /** Lấy tất cả playlist, sắp xếp theo tên. */
    public LiveData<List<PlaylistEntity>> getAllPlaylists() {
        return playlistDao.getAllPlaylists();
    }

    /** Lấy chi tiết playlist theo ID. */
    public LiveData<PlaylistEntity> getPlaylistById(long id) {
        return playlistDao.getPlaylistById(id);
    }

    /** Lấy danh sách bài hát trong playlist (JOIN query). */
    public LiveData<List<SongEntity>> getSongsInPlaylist(long playlistId) {
        return playlistDao.getSongsInPlaylist(playlistId);
    }

    // ════════════════════════════════════════════
    //  Mutations (background thread)
    // ════════════════════════════════════════════

    // ── Playlist CRUD ──

    /**
     * Tạo playlist mới (async).
     *
     * Nguyên lý:
     * - Tạo entity PlaylistEntity với name và description.
     * - Insert vào Room → nhận ID.
     * - Gọi callback.onCreated(id) khi hoàn thành.
     *
     * Input:
     * @param name        Tên playlist.
     * @param description Mô tả (có thể null).
     * @param callback    Callback trả về ID playlist vừa tạo.
     */
    public void createPlaylist(@NonNull String name, String description,
                               @NonNull OnPlaylistCreated callback) {
        executor.execute(() -> {
            try {
                PlaylistEntity playlist = new PlaylistEntity(name, description);
                long id = playlistDao.insert(playlist);
                callback.onCreated(id);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Tạo playlist mới (phiên bản đồng bộ).
     *
     * Chỉ gọi từ background thread. Trả về ID ngay lập tức.
     *
     * Input:
     * @param name        Tên playlist.
     * @param description Mô tả.
     *
     * Output: ID của playlist vừa tạo.
     */
    public long createPlaylistSync(@NonNull String name, String description) {
        PlaylistEntity playlist = new PlaylistEntity(name, description);
        return playlistDao.insert(playlist);
    }

    /** Cập nhật thông tin playlist (tên, mô tả). */
    public void updatePlaylist(@NonNull PlaylistEntity playlist) {
        executor.execute(() -> playlistDao.update(playlist));
    }

    /**
     * Xoá playlist.
     * FK CASCADE tự động xoá tất cả bản ghi playlist_songs liên quan.
     */
    public void deletePlaylist(@NonNull PlaylistEntity playlist) {
        executor.execute(() -> playlistDao.delete(playlist));
    }

    /** Xoá playlist theo ID. */
    public void deletePlaylistById(long id) {
        executor.execute(() -> playlistDao.deleteById(id));
    }

    // ── Quản lý bài hát trong playlist ──

    /**
     * Thêm bài hát vào playlist.
     *
     * Nguyên lý:
     * - Tạo PlaylistSongCrossRef với playlistId, songId, orderIndex.
     * - IGNORE strategy: nếu bài hát đã có trong playlist, bỏ qua (không báo lỗi).
     * - Tự động cập nhật songCount sau khi thêm.
     *
     * Input:
     * @param playlistId ID playlist.
     * @param songId     ID bài hát.
     * @param orderIndex Thứ tự (mặc định 0 = thêm vào cuối).
     */
    public void addSongToPlaylist(long playlistId, long songId, int orderIndex) {
        executor.execute(() -> {
            try {
                PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef(playlistId, songId, orderIndex);
                playlistDao.addSongToPlaylist(crossRef);
                playlistDao.updateSongCount(playlistId);
            } catch (Exception e) {
                // IGNORE conflict — bài hát đã có trong playlist rồi
            }
        });
    }

    /** Thêm bài hát vào playlist (thêm vào cuối cùng). */
    public void addSongToPlaylist(long playlistId, long songId) {
        addSongToPlaylist(playlistId, songId, 0);
    }

    /**
     * Thêm nhiều bài hát vào playlist cùng lúc.
     *
     * Nguyên lý:
     * - Duyệt danh sách songIds, tạo CrossRef với orderIndex tăng dần.
     * - Mỗi bài insert riêng lẻ với IGNORE strategy.
     * - Cập nhật songCount một lần sau khi hoàn thành.
     *
     * Input:
     * @param playlistId ID playlist.
     * @param songIds    Danh sách ID bài hát cần thêm.
     */
    public void addSongsToPlaylist(long playlistId, @NonNull List<Long> songIds) {
        executor.execute(() -> {
            try {
                for (int i = 0; i < songIds.size(); i++) {
                    PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef(
                            playlistId, songIds.get(i), i);
                    playlistDao.addSongToPlaylist(crossRef);
                }
                playlistDao.updateSongCount(playlistId);
            } catch (Exception e) {
                // Bỏ qua lỗi (IGNORE strategy đã xử lý conflict)
            }
        });
    }

    /**
     * Xoá bài hát khỏi playlist.
     * Tự động cập nhật songCount sau khi xoá.
     */
    public void removeSongFromPlaylist(long playlistId, long songId) {
        executor.execute(() -> {
            playlistDao.removeSongFromPlaylist(playlistId, songId);
            playlistDao.updateSongCount(playlistId);
        });
    }

    /**
     * Xoá tất cả bài hát khỏi playlist (giữ lại playlist trống).
     * Tự động cập nhật songCount về 0.
     */
    public void clearPlaylist(long playlistId) {
        executor.execute(() -> {
            playlistDao.clearPlaylist(playlistId);
            playlistDao.updateSongCount(playlistId);
        });
    }

    /**
     * Kiểm tra bài hát đã có trong playlist chưa.
     * Chạy đồng bộ — phù hợp gọi từ background thread.
     */
    public boolean isSongInPlaylist(long playlistId, long songId) {
        return playlistDao.isSongInPlaylist(playlistId, songId) > 0;
    }

    /**
     * Lấy URI ảnh bìa của bài hát đầu tiên trong playlist.
     *
     * Dùng để load ảnh bìa playlist trong PlaylistAdapter và PlaylistDetailActivity.
     * Chạy đồng bộ — gọi từ background thread hoặc main thread (query nhanh).
     *
     * Input:
     * @param playlistId ID playlist.
     *
     * Output:
     * @return URI ảnh bìa (có thể null nếu playlist rỗng).
     */
    @Nullable
    public String getFirstSongAlbumArtUri(long playlistId) {
        return playlistDao.getFirstSongAlbumArtUri(playlistId);
    }

    /**
     * Lấy tên bài hát đầu tiên trong playlist.
     *
     * Dùng để hiển thị preview trong danh sách playlist.
     *
     * Input:
     * @param playlistId ID playlist.
     *
     * Output:
     * @return Tên bài hát đầu tiên (có thể null).
     */
    @Nullable
    public String getFirstSongTitle(long playlistId) {
        return playlistDao.getFirstSongTitle(playlistId);
    }

    // ── Callback ──

    /** Callback cho hoạt động tạo playlist. */
    public interface OnPlaylistCreated {
        void onCreated(long playlistId);
        void onError(Exception e);
    }
}
