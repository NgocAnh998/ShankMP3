package com.example.musicapi.service;

import com.example.musicapi.model.SongDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý danh sách bài hát mẫu.
 *
 * Dùng dữ liệu public domain / free music để demo streaming.
 * Có thể thay thế bằng database sau này.
 *
 * Nguồn nhạc free (public domain / CC0):
 * - https://freepd.com/
 * - https://pixabay.com/music/
 */
@Service
public class SongService {

    private final List<SongDto> songs = new ArrayList<>();

    @PostConstruct
    public void init() {
        // ════════════════════════════════════════════════
        //  Danh sách bài hát mẫu — đa dạng thể loại, nghệ sĩ, album
        // ════════════════════════════════════════════════
        //
        //  Nguồn nhạc: SoundHelix (https://www.soundhelix.com)
        //  Ảnh bìa: picsum.photos (random images theo seed)
        //
        //  GỒM 30 BÀI — 10 nghệ sĩ, 10 albums, 5 thể loại
        // ════════════════════════════════════════════════

        // ── 🎸 Pop / Dance ──
        songs.add(new SongDto(
                "1", "Happy Morning", "Luna Star",
                "Sunrise", 180000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                "https://picsum.photos/seed/pop1/300/300"
        ));
        songs.add(new SongDto(
                "2", "Summer Vibes", "Luna Star",
                "Sunrise", 210000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                "https://picsum.photos/seed/pop2/300/300"
        ));
        songs.add(new SongDto(
                "3", "Candy Cloud", "Luna Star",
                "Sunrise", 195000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                "https://picsum.photos/seed/pop3/300/300"
        ));
        songs.add(new SongDto(
                "4", "Starlight", "Justin Beats",
                "Pop Hits 2024", 225000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                "https://picsum.photos/seed/pop4/300/300"
        ));
        songs.add(new SongDto(
                "5", "Dancing Alone", "Justin Beats",
                "Pop Hits 2024", 198000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                "https://picsum.photos/seed/pop5/300/300"
        ));

        // ── 🤘 Rock / Alternative ──
        songs.add(new SongDto(
                "6", "Electric Storm", "Black Thunder",
                "Storm Rising", 265000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                "https://picsum.photos/seed/rock1/300/300"
        ));
        songs.add(new SongDto(
                "7", "Neon Lights", "Black Thunder",
                "Storm Rising", 242000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                "https://picsum.photos/seed/rock2/300/300"
        ));
        songs.add(new SongDto(
                "8", "Highway Run", "The Wild Wolves",
                "Road Trip", 280000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                "https://picsum.photos/seed/rock3/300/300"
        ));
        songs.add(new SongDto(
                "9", "Fire and Ice", "The Wild Wolves",
                "Road Trip", 255000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
                "https://picsum.photos/seed/rock4/300/300"
        ));
        songs.add(new SongDto(
                "10", "Rebel Heart", "The Wild Wolves",
                "Road Trip", 310000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
                "https://picsum.photos/seed/rock5/300/300"
        ));

        // ── 🎷 Jazz / Lofi ──
        songs.add(new SongDto(
                "11", "Jazz Cafe", "Mellow Tones",
                "Late Night Jazz", 285000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                "https://picsum.photos/seed/jazz1/300/300"
        ));
        songs.add(new SongDto(
                "12", "Blue Midnight", "Mellow Tones",
                "Late Night Jazz", 320000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                "https://picsum.photos/seed/jazz2/300/300"
        ));
        songs.add(new SongDto(
                "13", "Rainy Day", "Mellow Tones",
                "Late Night Jazz", 270000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                "https://picsum.photos/seed/jazz3/300/300"
        ));
        songs.add(new SongDto(
                "14", "Smooth Coffee", "Lofi Girl",
                "Chill Study Beats", 175000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                "https://picsum.photos/seed/lofi1/300/300"
        ));
        songs.add(new SongDto(
                "15", "Cloud Nine", "Lofi Girl",
                "Chill Study Beats", 198000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                "https://picsum.photos/seed/lofi2/300/300"
        ));
        songs.add(new SongDto(
                "16", "Moonlit Walk", "Lofi Girl",
                "Chill Study Beats", 220000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                "https://picsum.photos/seed/lofi3/300/300"
        ));

        // ── 🌿 Ambient / Classical ──
        songs.add(new SongDto(
                "17", "Ocean Waves", "Nature Sounds",
                "Peaceful Relaxation", 300000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                "https://picsum.photos/seed/ambient1/300/300"
        ));
        songs.add(new SongDto(
                "18", "Forest Walk", "Nature Sounds",
                "Peaceful Relaxation", 280000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                "https://picsum.photos/seed/ambient2/300/300"
        ));
        songs.add(new SongDto(
                "19", "Starry Night", "Nature Sounds",
                "Peaceful Relaxation", 340000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
                "https://picsum.photos/seed/ambient3/300/300"
        ));
        songs.add(new SongDto(
                "20", "Mountain High", "Echo Valley",
                "Nature", 250000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
                "https://picsum.photos/seed/ambient4/300/300"
        ));
        songs.add(new SongDto(
                "21", "Gentle River", "Echo Valley",
                "Nature", 215000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                "https://picsum.photos/seed/ambient5/300/300"
        ));

        // ── 🎹 Electronic / EDM ──
        songs.add(new SongDto(
                "22", "Electric Dreams", "DJ Pixel",
                "Cyber Night", 256000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                "https://picsum.photos/seed/edm1/300/300"
        ));
        songs.add(new SongDto(
                "23", "Neon City", "DJ Pixel",
                "Cyber Night", 235000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                "https://picsum.photos/seed/edm2/300/300"
        ));
        songs.add(new SongDto(
                "24", "Pulse", "DJ Pixel",
                "Cyber Night", 290000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                "https://picsum.photos/seed/edm3/300/300"
        ));
        songs.add(new SongDto(
                "25", "Midnight Run", "Electro Storm",
                "Club Hits Vol.1", 215000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                "https://picsum.photos/seed/edm4/300/300"
        ));
        songs.add(new SongDto(
                "26", "Bass Drop", "Electro Storm",
                "Club Hits Vol.1", 270000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                "https://picsum.photos/seed/edm5/300/300"
        ));

        // ── 🎤 R&B / Soul ──
        songs.add(new SongDto(
                "27", "Sweet Melody", "Velvet Soul",
                "Smooth R&B", 240000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
                "https://picsum.photos/seed/rnb1/300/300"
        ));
        songs.add(new SongDto(
                "28", "Golden Hour", "Velvet Soul",
                "Smooth R&B", 265000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                "https://picsum.photos/seed/rnb2/300/300"
        ));
        songs.add(new SongDto(
                "29", "Deep Inside", "Velvet Soul",
                "Smooth R&B", 195000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
                "https://picsum.photos/seed/rnb3/300/300"
        ));
        songs.add(new SongDto(
                "30", "Night Rain", "Riley",
                "Midnight Stories", 230000,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
                "https://picsum.photos/seed/rnb4/300/300"
        ));
    }

    /**
     * Lấy tất cả bài hát.
     */
    public List<SongDto> getAllSongs() {
        return Collections.unmodifiableList(songs);
    }

    /**
     * Tìm kiếm bài hát theo title, artist, album (không phân biệt hoa/thường).
     */
    public List<SongDto> searchSongs(String query) {
        if (query == null || query.isBlank()) {
            return songs;
        }
        String q = query.toLowerCase().trim();
        return songs.stream()
                .filter(s -> s.getTitle().toLowerCase().contains(q)
                        || s.getArtist().toLowerCase().contains(q)
                        || s.getAlbum().toLowerCase().contains(q))
                .toList();
    }

    /**
     * Lấy chi tiết bài hát theo ID.
     */
    public Optional<SongDto> getSongById(String id) {
        return songs.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    /**
     * Lấy URL stream của bài hát (trả về toàn bộ song).
     * Android app dùng streamUrl trong response để phát qua ExoPlayer.
     */
    public Optional<SongDto> getStreamUrl(String id) {
        return getSongById(id);
    }
}
