package com.example.nghenhac.service;

import androidx.annotation.NonNull;
import androidx.media3.common.Player;
import androidx.media3.session.MediaSession;

/**
 * Quản lý MediaSession — cầu nối giữa ExoPlayer và các controller bên ngoài (notification, Bluetooth, lock screen).
 *
 * Nguyên lý:
 * - MediaSession cho phép các MediaController (VD: system UI, Bluetooth, Android Auto) điều khiển playback.
 * - Callback xử lý các lệnh: play, pause, stop, seekTo, skipToNext, skipToPrevious.
 * - Kết nối trực tiếp với MusicPlayer (singleton) để thực thi các lệnh.
 * - onConnect() xác định các lệnh mà controller được phép gửi.
 *
 * Luồng xử lý:
 * 1. MusicService.onCreate() → tạo MediaSession với MusicPlayer.getExoPlayer() + callback này.
 * 2. System/Bluetooth gửi lệnh PLAY → onPlay() → MusicPlayer.play().
 * 3. User bấm next trên notification → onSkipToNext() → MusicPlayer.next().
 * 4. User seek trên lock screen → onSeekTo() → MusicPlayer.seekTo().
 *
 * Input:
 * - MusicPlayer instance (singleton) để thao tác playback.
 *
 * Output:
 * - Các lệnh playback được chuyển đến ExoPlayer.
 *
 * Lưu ý:
 * - Tất cả callback đều chạy trên main thread.
 * - Cần kiểm tra MusicPlayer.getExoPlayer() != null trước khi thao tác.
 */
public class MediaSessionManager {

    private MediaSession mediaSession;
    private MusicService service;

    /**
     * Khởi tạo MediaSession từ MusicService.
     *
     * Nguyên lý:
     * - Tạo MediaSession với Player từ MusicPlayer.
     * - Set callback để xử lý các lệnh điều khiển.
     * - Lưu reference đến service để start/stop foreground.
     *
     * Input:
     * @param service        MusicService instance (để gọi startForeground/stopForeground).
     * @param player         ExoPlayer instance từ MusicPlayer.
     * @param callback       Custom callback xử lý các lệnh.
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
     * Lấy MediaSession instance.
     *
     * Output: MediaSession để trả về từ onGetSession() của MusicService.
     */
    public MediaSession getSession() {
        return mediaSession;
    }

    /**
     * Giải phóng MediaSession. Gọi trong onDestroy() của MusicService.
     */
    public void release() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
    }

}
