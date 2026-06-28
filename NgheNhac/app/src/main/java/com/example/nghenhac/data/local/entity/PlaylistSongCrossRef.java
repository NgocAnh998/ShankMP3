package com.example.nghenhac.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

/**
 * Junction table (bảng trung gian) cho quan hệ N-N giữa PlaylistEntity và SongEntity.
 *
 * Nguyên lý:
 * - Một playlist có nhiều bài hát, một bài hát thuộc nhiều playlist.
 * - SQL thuần không hỗ trợ N-N, cần bảng trung gian để lưu cặp (playlistId, songId).
 * - orderIndex lưu thứ tự bài hát trong playlist, cho phép user sắp xếp lại.
 *
 * Luồng xử lý:
 * 1. User thêm bài hát vào playlist → tạo PlaylistSongCrossRef → insert.
 * 2. User xoá playlist → FK CASCADE tự động xoá tất cả bản ghi liên quan.
 * 3. User xoá bài hát → FK CASCADE tự động xoá khỏi mọi playlist chứa nó.
 *
 * Input:
 * - playlistId: FK → PlaylistEntity.id (CASCADE khi xoá)
 * - songId: FK → SongEntity.id (CASCADE khi xoá)
 *
 * Output:
 * - Composite primary key (playlistId, songId): đảm bảo không trùng lặp.
 *
 * Lưu ý:
 * - orderIndex mặc định 0, cần cập nhật nếu user sắp xếp lại.
 * - Cả playlistId và songId đều có index riêng để tối ưu JOIN query.
 * - Khi xoá playlist hoặc bài hát, các bản ghi liên quan tự động bị xoá (CASCADE).
 */
@Entity(
        tableName = "playlist_songs",
        primaryKeys = {"playlist_id", "song_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = PlaylistEntity.class,
                        parentColumns = "id",
                        childColumns = "playlist_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = SongEntity.class,
                        parentColumns = "id",
                        childColumns = "song_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "playlist_id"),
                @Index(value = "song_id")
        }
)
public class PlaylistSongCrossRef {

    @ColumnInfo(name = "playlist_id")
    private long playlistId;

    @ColumnInfo(name = "song_id")
    private long songId;

    @ColumnInfo(name = "order_index", defaultValue = "0")
    private int orderIndex;

    // ── Constructors ──

    /**
     * No-arg constructor bắt buộc cho Room.
     */
    public PlaylistSongCrossRef() {}

    /**
     * Constructor tạo liên kết giữa playlist và bài hát.
     *
     * @param playlistId ID playlist
     * @param songId     ID bài hát
     * @param orderIndex Thứ tự bài hát trong playlist
     */
    @Ignore
    public PlaylistSongCrossRef(long playlistId, long songId, int orderIndex) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.orderIndex = orderIndex;
    }

    // ── Getters & Setters ──

    public long getPlaylistId() { return playlistId; }
    public void setPlaylistId(long playlistId) { this.playlistId = playlistId; }

    public long getSongId() { return songId; }
    public void setSongId(long songId) { this.songId = songId; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
