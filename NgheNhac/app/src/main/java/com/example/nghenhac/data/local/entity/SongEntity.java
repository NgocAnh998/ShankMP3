package com.example.nghenhac.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity đại diện cho một bài hát trong database local.
 *
 * Nguyên lý:
 * - Lưu trữ metadata của bài hát từ hai nguồn: MediaStore (nhạc local) hoặc API server (streaming).
 * - Dùng làm đối tượng trung gian giữa tầng UI và tầng dữ liệu (Room Database).
 * - Entity này là trung tâm của ứng dụng: tất cả playlist, cache, yêu thích đều tham chiếu đến nó.
 *
 * Luồng xử lý:
 * 1. MediaStoreScanner quét thiết bị → tạo SongEntity → insert vào Room.
 * 2. Repository trả về LiveData<List<SongEntity>> cho UI observe.
 * 3. User tương tác (yêu thích, thêm vào playlist) → cập nhật entity → Room tự động notify UI.
 *
 * Input:
 * - Dữ liệu từ MediaStore (ContentResolver cursor) hoặc API (SongDto → mapper).
 *
 * Output:
 * - ID tự sinh (autoincrement) làm khoá chính.
 * - mediaStoreId là Long (nullable) — null với nhạc streaming, không null với nhạc local.
 *
 * Lưu ý:
 * - mediaStoreId có unique index: nhiều bài streaming sẽ có mediaStoreId = null (SQLite coi null là khác nhau).
 * - Các trường title, artist, album được đánh index để tăng tốc tìm kiếm.
 */
@Entity(
        tableName = "songs",
        indices = {
                @Index(value = "media_store_id", unique = true),
                @Index(value = "title"),
                @Index(value = "artist"),
                @Index(value = "album"),
                @Index(value = "is_favorite")
        }
)
public class SongEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "title")
    private String title;

    @NonNull
    @ColumnInfo(name = "artist")
    private String artist;

    @NonNull
    @ColumnInfo(name = "album")
    private String album;

    @ColumnInfo(name = "duration")
    private long duration;

    @ColumnInfo(name = "file_path")
    private String filePath;

    @ColumnInfo(name = "album_art_uri")
    private String albumArtUri;

    @ColumnInfo(name = "media_store_id")
    private Long mediaStoreId;

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    private boolean isFavorite;

    @ColumnInfo(name = "date_added")
    private long dateAdded;

    @ColumnInfo(name = "track_number")
    private int trackNumber;

    @ColumnInfo(name = "mime_type")
    private String mimeType;

    @ColumnInfo(name = "file_size")
    private long fileSize;

    // ── Constructors ──

    /**
     * No-arg constructor bắt buộc cho Room.
     * Khởi tạo title, artist, album với giá trị mặc định để tránh null.
     */
    public SongEntity() {
        this.title = "";
        this.artist = "";
        this.album = "";
    }

    /**
     * Constructor với các trường bắt buộc.
     *
     * Nguyên lý:
     * - Tạo entity từ dữ liệu quét được từ MediaStore hoặc API.
     * - Các trường còn lại (trackNumber, mimeType, fileSize) set sau qua setter.
     *
     * Input:
     * @param title        Tên bài hát
     * @param artist       Tên nghệ sĩ
     * @param album        Tên album
     * @param duration     Thời lượng (milliseconds)
     * @param filePath     URI content:// hoặc đường dẫn file
     * @param albumArtUri  URI ảnh bìa album
     * @param mediaStoreId ID từ MediaStore (null với nhạc streaming)
     * @param dateAdded    Thời điểm thêm vào (epoch millis)
     */
    @Ignore
    public SongEntity(@NonNull String title, @NonNull String artist, @NonNull String album,
                      long duration, String filePath, String albumArtUri,
                      Long mediaStoreId, long dateAdded) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.filePath = filePath;
        this.albumArtUri = albumArtUri;
        this.mediaStoreId = mediaStoreId;
        this.dateAdded = dateAdded;
    }

    // ── Getters & Setters ──

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    @NonNull
    public String getArtist() { return artist; }
    public void setArtist(@NonNull String artist) { this.artist = artist; }

    @NonNull
    public String getAlbum() { return album; }
    public void setAlbum(@NonNull String album) { this.album = album; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getAlbumArtUri() { return albumArtUri; }
    public void setAlbumArtUri(String albumArtUri) { this.albumArtUri = albumArtUri; }

    public Long getMediaStoreId() { return mediaStoreId; }
    public void setMediaStoreId(Long mediaStoreId) { this.mediaStoreId = mediaStoreId; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

    public int getTrackNumber() { return trackNumber; }
    public void setTrackNumber(int trackNumber) { this.trackNumber = trackNumber; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}
