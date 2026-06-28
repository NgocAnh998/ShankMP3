package com.example.nghenhac.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object cho bài hát từ REST API.
 *
 * Nguyên lý:
 * - Dùng để parse JSON response từ server thành Java object.
 * - @SerializedName map tên trường JSON với tên biến Java (hỗ trợ rename).
 * - Không phụ thuộc vào Room entity (songs có thể khác format với API response).
 *
 * Luồng xử lý:
 * 1. Retrofit nhận JSON response → GsonConverter → SongDto.
 * 2. Repository map SongDto → SongEntity để lưu vào Room.
 * 3. UI chỉ làm việc với SongEntity (không dùng SongDto trực tiếp).
 *
 * Lưu ý:
 * - Dùng String (không long) cho id vì server ID có thể không phải số.
 * - Các field optional (streamUrl, albumArtUrl) có thể null.
 */
public class SongDto {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private String artist;

    @SerializedName("album")
    private String album;

    @SerializedName("duration")
    private long duration;

    @SerializedName("streamUrl")
    private String streamUrl;

    @SerializedName("albumArtUrl")
    private String albumArtUrl;

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
