package com.example.musicapi.model;

/**
 * Data Transfer Object cho bài hát — khớp với SongDto bên Android.
 *
 * SerializedName dùng snake_case để API trả về JSON đúng format
 * mà Gson bên Android mong đợi (streamUrl, albumArtUrl).
 */
public class SongDto {

    private String id;
    private String title;
    private String artist;
    private String album;
    private long duration;
    private String streamUrl;
    private String albumArtUrl;

    public SongDto() {}

    public SongDto(String id, String title, String artist, String album,
                   long duration, String streamUrl, String albumArtUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.streamUrl = streamUrl;
        this.albumArtUrl = albumArtUrl;
    }

    // ── Getters ──
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public long getDuration() { return duration; }
    public String getStreamUrl() { return streamUrl; }
    public String getAlbumArtUrl() { return albumArtUrl; }

    // ── Setters ──
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setAlbum(String album) { this.album = album; }
    public void setDuration(long duration) { this.duration = duration; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public void setAlbumArtUrl(String albumArtUrl) { this.albumArtUrl = albumArtUrl; }
}
