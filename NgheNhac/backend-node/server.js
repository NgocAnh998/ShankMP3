/**
 * NgheNhac Music API — Node.js/Express
 * 
 * Cài đặt: npm install express cors
 * Chạy:    node server.js
 *
 * API:
 *   GET /songs              → danh sách tất cả bài hát
 *   GET /songs/search?q=... → tìm kiếm bài hát
 *   GET /songs/:id          → chi tiết bài hát
 *   GET /songs/:id/stream   → URL stream
 */

const express = require('express');
const cors = require('cors');

const app = express();
const PORT = 8080;

app.use(cors());
app.use(express.json());

// ── Danh sách 30 bài hát mẫu ──
const songs = [
    // 🎸 Pop / Dance
    { id: '1',  title: 'Happy Morning',   artist: 'Luna Star',      album: 'Sunrise',             duration: 180000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',  albumArtUrl: 'https://picsum.photos/seed/pop1/300/300' },
    { id: '2',  title: 'Summer Vibes',    artist: 'Luna Star',      album: 'Sunrise',             duration: 210000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',  albumArtUrl: 'https://picsum.photos/seed/pop2/300/300' },
    { id: '3',  title: 'Candy Cloud',     artist: 'Luna Star',      album: 'Sunrise',             duration: 195000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',  albumArtUrl: 'https://picsum.photos/seed/pop3/300/300' },
    { id: '4',  title: 'Starlight',       artist: 'Justin Beats',   album: 'Pop Hits 2024',       duration: 225000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',  albumArtUrl: 'https://picsum.photos/seed/pop4/300/300' },
    { id: '5',  title: 'Dancing Alone',   artist: 'Justin Beats',   album: 'Pop Hits 2024',       duration: 198000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',  albumArtUrl: 'https://picsum.photos/seed/pop5/300/300' },
    // 🤘 Rock / Alternative
    { id: '6',  title: 'Electric Storm',  artist: 'Black Thunder',  album: 'Storm Rising',        duration: 265000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',  albumArtUrl: 'https://picsum.photos/seed/rock1/300/300' },
    { id: '7',  title: 'Neon Lights',     artist: 'Black Thunder',  album: 'Storm Rising',        duration: 242000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',  albumArtUrl: 'https://picsum.photos/seed/rock2/300/300' },
    { id: '8',  title: 'Highway Run',     artist: 'The Wild Wolves',album: 'Road Trip',           duration: 280000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',  albumArtUrl: 'https://picsum.photos/seed/rock3/300/300' },
    { id: '9',  title: 'Fire and Ice',    artist: 'The Wild Wolves',album: 'Road Trip',           duration: 255000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',  albumArtUrl: 'https://picsum.photos/seed/rock4/300/300' },
    { id: '10', title: 'Rebel Heart',     artist: 'The Wild Wolves',album: 'Road Trip',           duration: 310000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', albumArtUrl: 'https://picsum.photos/seed/rock5/300/300' },
    // 🎷 Jazz / Lofi
    { id: '11', title: 'Jazz Cafe',       artist: 'Mellow Tones',   album: 'Late Night Jazz',     duration: 285000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',  albumArtUrl: 'https://picsum.photos/seed/jazz1/300/300' },
    { id: '12', title: 'Blue Midnight',   artist: 'Mellow Tones',   album: 'Late Night Jazz',     duration: 320000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',  albumArtUrl: 'https://picsum.photos/seed/jazz2/300/300' },
    { id: '13', title: 'Rainy Day',       artist: 'Mellow Tones',   album: 'Late Night Jazz',     duration: 270000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',  albumArtUrl: 'https://picsum.photos/seed/jazz3/300/300' },
    { id: '14', title: 'Smooth Coffee',   artist: 'Lofi Girl',      album: 'Chill Study Beats',   duration: 175000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',  albumArtUrl: 'https://picsum.photos/seed/lofi1/300/300' },
    { id: '15', title: 'Cloud Nine',      artist: 'Lofi Girl',      album: 'Chill Study Beats',   duration: 198000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',  albumArtUrl: 'https://picsum.photos/seed/lofi2/300/300' },
    { id: '16', title: 'Moonlit Walk',    artist: 'Lofi Girl',      album: 'Chill Study Beats',   duration: 220000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',  albumArtUrl: 'https://picsum.photos/seed/lofi3/300/300' },
    // 🌿 Ambient / Classical
    { id: '17', title: 'Ocean Waves',     artist: 'Nature Sounds',  album: 'Peaceful Relaxation', duration: 300000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',  albumArtUrl: 'https://picsum.photos/seed/ambient1/300/300' },
    { id: '18', title: 'Forest Walk',     artist: 'Nature Sounds',  album: 'Peaceful Relaxation', duration: 280000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',  albumArtUrl: 'https://picsum.photos/seed/ambient2/300/300' },
    { id: '19', title: 'Starry Night',    artist: 'Nature Sounds',  album: 'Peaceful Relaxation', duration: 340000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',  albumArtUrl: 'https://picsum.photos/seed/ambient3/300/300' },
    { id: '20', title: 'Mountain High',   artist: 'Echo Valley',    album: 'Nature',              duration: 250000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', albumArtUrl: 'https://picsum.photos/seed/ambient4/300/300' },
    { id: '21', title: 'Gentle River',    artist: 'Echo Valley',    album: 'Nature',              duration: 215000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',  albumArtUrl: 'https://picsum.photos/seed/ambient5/300/300' },
    // 🎹 Electronic / EDM
    { id: '22', title: 'Electric Dreams', artist: 'DJ Pixel',       album: 'Cyber Night',         duration: 256000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',  albumArtUrl: 'https://picsum.photos/seed/edm1/300/300' },
    { id: '23', title: 'Neon City',       artist: 'DJ Pixel',       album: 'Cyber Night',         duration: 235000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',  albumArtUrl: 'https://picsum.photos/seed/edm2/300/300' },
    { id: '24', title: 'Pulse',           artist: 'DJ Pixel',       album: 'Cyber Night',         duration: 290000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',  albumArtUrl: 'https://picsum.photos/seed/edm3/300/300' },
    { id: '25', title: 'Midnight Run',    artist: 'Electro Storm',  album: 'Club Hits Vol.1',     duration: 215000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',  albumArtUrl: 'https://picsum.photos/seed/edm4/300/300' },
    { id: '26', title: 'Bass Drop',       artist: 'Electro Storm',  album: 'Club Hits Vol.1',     duration: 270000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',  albumArtUrl: 'https://picsum.photos/seed/edm5/300/300' },
    // 🎤 R&B / Soul
    { id: '27', title: 'Sweet Melody',    artist: 'Velvet Soul',    album: 'Smooth R&B',          duration: 240000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',  albumArtUrl: 'https://picsum.photos/seed/rnb1/300/300' },
    { id: '28', title: 'Golden Hour',     artist: 'Velvet Soul',    album: 'Smooth R&B',          duration: 265000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',  albumArtUrl: 'https://picsum.photos/seed/rnb2/300/300' },
    { id: '29', title: 'Deep Inside',     artist: 'Velvet Soul',    album: 'Smooth R&B',          duration: 195000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',  albumArtUrl: 'https://picsum.photos/seed/rnb3/300/300' },
    { id: '30', title: 'Night Rain',      artist: 'Riley',          album: 'Midnight Stories',    duration: 230000, streamUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', albumArtUrl: 'https://picsum.photos/seed/rnb4/300/300' },
];

// ── Routes ──

// GET /songs — danh sách tất cả bài hát
app.get('/songs', (req, res) => {
    res.json(songs);
});

// GET /songs/search?q=query — tìm kiếm
app.get('/songs/search', (req, res) => {
    const query = (req.query.q || '').toLowerCase().trim();
    if (!query) {
        return res.json(songs);
    }
    const results = songs.filter(s =>
        s.title.toLowerCase().includes(query) ||
        s.artist.toLowerCase().includes(query) ||
        s.album.toLowerCase().includes(query)
    );
    res.json(results);
});

// GET /songs/:id — chi tiết bài hát
app.get('/songs/:id', (req, res) => {
    const song = songs.find(s => s.id === req.params.id);
    if (!song) return res.status(404).json({ error: 'Song not found' });
    res.json(song);
});

// GET /songs/:id/stream — URL stream
app.get('/songs/:id/stream', (req, res) => {
    const song = songs.find(s => s.id === req.params.id);
    if (!song) return res.status(404).json({ error: 'Song not found' });
    res.json(song);
});

// ── Khởi động server ──
app.listen(PORT, '0.0.0.0', () => {
    console.log(`🎵 NgheNhac Music API running at http://0.0.0.0:${PORT}`);
    console.log(`   Danh sách: http://localhost:${PORT}/songs`);
    console.log(`   Tìm kiếm:  http://localhost:${PORT}/songs/search?q=jazz`);
    console.log(`   Chi tiết:  http://localhost:${PORT}/songs/1`);
    console.log(`   Stream:    http://localhost:${PORT}/songs/1/stream`);
});
