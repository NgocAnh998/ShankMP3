package com.example.nghenhac.service;

import androidx.annotation.NonNull;
import androidx.media3.common.Player;
import androidx.media3.session.MediaSession;

/**
 * 🎮 MediaSessionManager — Cầu nối giữa ExoPlayer và các controller bên ngoài.
 *
 * ============================================================
 *  GIẢI THÍCH CHI TIẾT — DÀNH CHO BÁO CÁO ĐỒ ÁN
 * ============================================================
 *
 * ─── 1. VAI TRÒ ───
 * Class này là CẦU NỐI giữa:
 *   - ExoPlayer (engine phát nhạc thật sự)
 *   - Các thiết bị điều khiển bên ngoài (notification, Bluetooth, màn hình khoá)
 *
 * ─── 2. VÍ DỤ THỰC TẾ ───
 * Giống như REMOTE ĐIỀU KHIỂN TV:
 *   - TV   = ExoPlayer (phát nhạc)
 *   - Remote = MediaSessionManager (nhận lệnh từ người dùng)
 *   - Nút trên remote = Các controller (notification, Bluetooth)
 *
 * Khi user bấm nút "Next" trên tai nghe Bluetooth:
 *   Bluetooth → Android → MediaSession → onSkipToNext() → MusicPlayer.next()
 *
 * ─── 3. LUỒNG CHI TIẾT ───
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │                    MediaSessionManager                      │
 *   │                                                             │
 *   │  MusicService (Service)                                     │
 *   │       │                                                     │
 *   │       ├── Tạo MediaSessionManager(service, player, callback)│
 *   │       │                                                     │
 *   │       ▼                                                     │
 *   │  MediaSession.Builder(service, player)                      │
 *   │       │  .setCallback(callback)                             │
 *   │       │  .build()                                           │
 *   │       │                                                     │
 *   │       ▼                                                     │
 *   │  Các Controller kết nối:                                    │
 *   │  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
 *   │  │Notification│  │Bluetooth │  │LockScreen│                  │
 *   │  └─────┬────┘  └─────┬────┘  └─────┬────┘                  │
 *   │        │             │             │                        │
 *   │        └─────────────┼─────────────┘                        │
 *   │                      ▼                                      │
 *   │            MediaSession.Callback                            │
 *   │  ┌─────────────────────────────────────────────────┐       │
 *   │  │  onPlay()       → MusicPlayer.play()           │       │
 *   │  │  onPause()      → MusicPlayer.pause()          │       │
 *   │  │  onSkipToNext() → MusicPlayer.next()           │       │
 *   │  │  onSkipToPrev() → MusicPlayer.previous()       │       │
 *   │  │  onSeekTo(pos)  → MusicPlayer.seekTo(pos)      │       │
 *   │  │  onStop()       → stopForeground() + stopSelf()│       │
 *   │  └─────────────────────────────────────────────────┘       │
 *   │                      │                                      │
 *   │                      ▼                                      │
 *   │                  ExoPlayer (phát nhạc)                      │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * ─── 4. LƯU Ý ───
 * - Tất cả callback chạy trên MAIN thread
 * - Cần kiểm tra null trước khi gọi MusicPlayer
 * - onConnect() có thể giới hạn quyền của từng controller
 *   (VD: chỉ cho phép Bluetooth điều khiển, không cho app khác)
 */
public class MediaSessionManager {

    private MediaSession mediaSession;
    private MusicService service;

    /**
     * ─── KHỞI TẠO MEDIASESSION ───
     *
     * LUỒNG:
     *  1. Nhận service context + ExoPlayer + callback
     *  2. Tạo MediaSession.Builder với service và player
     *  3. Gắn callback (xử lý play/pause/next/prev/seek/stop)
     *  4. Build → MediaSession sẵn sàng cho controller kết nối
     *
     * @param service  MusicService instance (để start/stop foreground)
     * @param player   ExoPlayer instance từ MusicPlayer
     * @param callback Custom callback xử lý các lệnh điều khiển
     */
    public MediaSessionManager(@NonNull MusicService service,
                               @NonNull Player player,
                               @NonNull MediaSession.Callback callback) {
        this.service = service;
        this.mediaSession = new MediaSession.Builder(service, player)
                .setCallback(callback)
                .build();
    }

    /**
     * Lấy MediaSession instance để MusicService trả về từ onGetSession().
     *
     * @return MediaSession cho controller bên ngoài kết nối
     */
    public MediaSession getSession() {
        return mediaSession;
    }

    /**
     * ─── GIẢI PHÓNG MEDIASESSION ───
     *
     * Gọi trong onDestroy() của MusicService.
     * Ngắt kết nối tất cả controller (notification, Bluetooth, lock screen).
     */
    public void release() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
    }

}
