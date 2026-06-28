package com.example.nghenhac.data.remote;

import com.example.nghenhac.data.model.SongDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * REST API interface cho streaming nhạc.
 *
 * Nguyên lý:
 * - Định nghĩa các endpoint API dưới dạng interface Retrofit.
 * - Retrofit tự động sinh implementation (Call adapter) cho các method.
 * - Các endpoint sẽ được cấu hình sau khi server sẵn sàng.
 *
 * Endpoints:
 * - GET /songs: lấy danh sách tất cả bài hát.
 * - GET /songs/search?q=: tìm kiếm bài hát.
 * - GET /songs/{id}: lấy chi tiết bài hát.
 * - GET /songs/{id}/stream: lấy URL stream của bài hát.
 *
 * Lưu ý:
 * - Đây là phiên bản cơ bản. Sẽ mở rộng thêm các endpoint sau:
 *   + GET /albums, GET /artists
 *   + GET /songs?genre=... (lọc theo thể loại)
 *   + POST /sync (đồng bộ playlist lên server)
 */
public interface MusicApiService {

    /** Lấy danh sách tất cả bài hát từ server. */
    @GET("songs")
    Call<List<SongDto>> getAllSongs();

    /** Tìm kiếm bài hát theo từ khoá. */
    @GET("songs/search")
    Call<List<SongDto>> searchSongs(@Query("q") String query);

    /** Lấy chi tiết một bài hát theo ID. */
    @GET("songs/{id}")
    Call<SongDto> getSongDetail(@Path("id") String id);

    /** Lấy URL stream của bài hát để phát qua ExoPlayer. */
    @GET("songs/{id}/stream")
    Call<SongDto> getStreamUrl(@Path("id") String id);
}
