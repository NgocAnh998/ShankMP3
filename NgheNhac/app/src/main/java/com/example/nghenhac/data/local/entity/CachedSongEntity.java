package com.example.nghenhac.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity lưu thông tin bài hát đã được cache để phát offline.
 *
 * Nguyên lý:
 * - Hỗ trợ hai chế độ cache:
 *   + Cache chunk (isFullDownload = false): lưu từng phần dữ liệu streaming.
 *   + Tải toàn bộ file (isFullDownload = true): cho phép nghe offline hoàn toàn.
 * - Mỗi bài hát chỉ có một bản ghi cache (songId unique).
 * - Khi bài hát gốc bị xoá (VD: xoá khỏi thư viện), cache cũng tự động bị xoá (CASCADE).
 *
 * Luồng xử lý:
 * 1. OfflineDownloadManager tải nhạc → tạo CachedSongEntity → insert.
 * 2. Khi phát, MusicPlayer kiểm tra cache → nếu có thì dùng file local thay vì streaming.
 * 3. User xoá cache → deleteBySongId().
 * 4. Hệ thống dọn dẹp cache cũ → kiểm tra cachedAt để xoá cache lâu ngày.
 *
 * Input:
 * - songId: FK → SongEntity.id (CASCADE khi xoá bài hát gốc).
 * - localFilePath: đường dẫn file trong external cache dir.
 * - fileSize: dung lượng file cache (bytes).
 *
 * Output:
 * - ID tự sinh làm khoá chính.
 * - isFullDownload: phân loại chế độ cache.
 *
 * Lưu ý:
 * - songId có unique index: mỗi bài hát chỉ có một bản ghi cache.
 * - localFilePath cần được cập nhật nếu file bị move.
 * - cachedAt dùng cho LRU eviction khi cache đầy.
 */
@Entity(
        tableName = "cached_songs",
        foreignKeys = @ForeignKey(
                entity = SongEntity.class,
                parentColumns = "id",
                childColumns = "song_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = "song_id", unique = true)
)
public class CachedSongEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "song_id")
    private long songId;

    @ColumnInfo(name = "local_file_path")
    private String localFilePath;

    @ColumnInfo(name = "file_size")
    private long fileSize;

    @ColumnInfo(name = "cached_at")
    private long cachedAt;

    @ColumnInfo(name = "is_full_download", defaultValue = "0")
    private boolean isFullDownload;

    // ── Constructors ──

    /**
     * No-arg constructor bắt buộc cho Room.
     */
    public CachedSongEntity() {}

    /**
     * Constructor tạo bản ghi cache cho một bài hát.
     *
     * Nguyên lý:
     * - Tự động gán cachedAt = thời điểm hiện tại.
     * - isFullDownload phân biệt cache chunk (streaming) với full file (offline).
     *
     * Input:
     * @param songId         ID bài hát gốc
     * @param localFilePath  Đường dẫn file cache
     * @param fileSize       Dung lượng file (bytes)
     * @param isFullDownload True nếu là full file, false nếu là chunk
     */
    @Ignore
    public CachedSongEntity(long songId, String localFilePath, long fileSize,
                            boolean isFullDownload) {
        this.songId = songId;
        this.localFilePath = localFilePath;
        this.fileSize = fileSize;
        this.cachedAt = System.currentTimeMillis();
        this.isFullDownload = isFullDownload;
    }

    // ── Getters & Setters ──

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSongId() { return songId; }
    public void setSongId(long songId) { this.songId = songId; }

    public String getLocalFilePath() { return localFilePath; }
    public void setLocalFilePath(String localFilePath) { this.localFilePath = localFilePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getCachedAt() { return cachedAt; }
    public void setCachedAt(long cachedAt) { this.cachedAt = cachedAt; }

    public boolean isFullDownload() { return isFullDownload; }
    public void setFullDownload(boolean fullDownload) { isFullDownload = fullDownload; }
}
