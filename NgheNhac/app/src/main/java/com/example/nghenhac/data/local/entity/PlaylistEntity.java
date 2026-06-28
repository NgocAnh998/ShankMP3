package com.example.nghenhac.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity đại diện cho một playlist (danh sách phát) do người dùng tạo.
 *
 * Nguyên lý:
 * - Playlist là tập hợp có thứ tự các bài hát, người dùng tự tạo và quản lý.
 * - Quan hệ N-N với SongEntity thông qua bảng trung gian PlaylistSongCrossRef.
 * - songCount được cache trong entity để hiển thị nhanh trên UI mà không cần JOIN.
 *
 * Luồng xử lý:
 * 1. User tạo playlist → PlaylistRepository.createPlaylist() → insert vào Room.
 * 2. User thêm bài hát → thêm bản ghi PlaylistSongCrossRef → updateSongCount().
 * 3. User xoá playlist → cascade tự động xoá các bản ghi trong playlist_songs.
 *
 * Input:
 * - name: tên playlist (unique, có index)
 * - description: mô tả ngắn
 *
 * Output:
 * - ID tự sinh làm khoá chính.
 * - createdAt: timestamp lúc tạo.
 *
 * Lưu ý:
 * - name có unique index: không cho phép hai playlist trùng tên.
 * - songCount được cập nhật qua subquery trong PlaylistDao.
 */
@Entity(
        tableName = "playlists",
        indices = @Index(value = "name", unique = true)
)
public class PlaylistEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "song_count", defaultValue = "0")
    private int songCount;

    // ── Constructors ──

    /**
     * No-arg constructor bắt buộc cho Room.
     * Gán createdAt với thời điểm hiện tại.
     */
    public PlaylistEntity() {
        this.name = "";
        this.description = "";
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Constructor tạo playlist mới.
     *
     * Nguyên lý:
     * - Tạo playlist với tên và mô tả, tự động gán createdAt.
     * - songCount khởi tạo = 0, sẽ được cập nhật khi thêm bài hát.
     *
     * Input:
     * @param name        Tên playlist
     * @param description Mô tả playlist
     */
    @Ignore
    public PlaylistEntity(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.songCount = 0;
    }

    // ── Getters & Setters ──

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }
}
