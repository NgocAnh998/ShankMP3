package com.example.musicapi.controller;

import com.example.musicapi.model.SongDto;
import com.example.musicapi.service.SongService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho music streaming API.
 *
 * Các endpoint khớp với MusicApiService bên Android:
 *   GET  /songs              → danh sách tất cả bài hát
 *   GET  /songs/search?q=    → tìm kiếm bài hát
 *   GET  /songs/{id}         → chi tiết bài hát
 *   GET  /songs/{id}/stream  → URL stream (trả về toàn bộ song, Android dùng streamUrl)
 */
@RestController
@RequestMapping("/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    /**
     * GET /songs — danh sách tất cả bài hát.
     */
    @GetMapping
    public ResponseEntity<List<SongDto>> getAllSongs() {
        List<SongDto> songs = songService.getAllSongs();
        return ResponseEntity.ok(songs);
    }

    /**
     * GET /songs/search?q=query — tìm kiếm bài hát.
     */
    @GetMapping("/search")
    public ResponseEntity<List<SongDto>> searchSongs(@RequestParam("q") String query) {
        List<SongDto> results = songService.searchSongs(query);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /songs/{id} — chi tiết bài hát.
     *
     * Trả về 404 nếu không tìm thấy.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SongDto> getSongDetail(@PathVariable String id) {
        return songService.getSongById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /songs/{id}/stream — lấy URL stream.
     *
     * Android app dùng streamUrl từ response này để phát qua ExoPlayer.
     * Trả về 404 nếu không tìm thấy.
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<SongDto> getStreamUrl(@PathVariable String id) {
        return songService.getStreamUrl(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
