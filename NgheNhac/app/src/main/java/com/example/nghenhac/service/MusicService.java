package com.example.nghenhac.service;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.nghenhac.player.MusicPlayer;
import com.example.nghenhac.player.PlayerEventListener;

/**
 * MediaSessionService — quản lý phát nhạc nền với Media3 Session.
 *
 * Nguyên lý:
 * - Kế thừa MediaSessionService (thay vì Service) để Media3 tự quản lý MediaSession lifecycle.
 * - Kết nối với MusicPlayer (singleton) để điều khiển ExoPlayer.
 * - Media3 DefaultMediaNotificationProvider tự động tạo notification với controls.
 * - onGetSession() trả về MediaSession để các controller (notification, Bluetooth, lock screen) kết nối.
 *
 * Luồng xử lý:
 * 1. App gửi intent play → onCreate() → tạo MediaSession → MusicPlayer.initialize().
 * 2. onGetSession() → trả về MediaSession → controller kết nối.
 * 3. User bấm play/pause/next/prev trên notification → MediaSession.Callback → MusicPlayer.
 * 4. Bài hát kết thúc → tự động chuyển bài (MusicPlayer.handlePlaybackEnded).
 * 5. User dừng → stopForeground(STOP_FOREGROUND_REMOVE).
 * 6. App destroy → onDestroy() → release MediaSession.
 *
 * Lưu ý:
 * - foregroundServiceType = "mediaPlayback" trong AndroidManifest.
 * - Không dùng START_NOT_STICKY — MediaSessionService tự quản lý lifecycle.
 * - Intent filter "androidx.media3.session.MediaSessionService" bắt buộc trong manifest.
 */
public class MusicService extends MediaSessionService {

    private MediaSession mediaSession;
    private MusicPlayer musicPlayer;

    @Override
    public void onCreate() {
        super.onCreate();

        // Lấy MusicPlayer instance (singleton)
        musicPlayer = MusicPlayer.getInstance(this); //Lấy trình phát nhạc
        musicPlayer.initialize(); //Khởi tạo ExoPlayer

        // Kiểm tra ExoPlayer không null trước khi tạo MediaSession
        androidx.media3.exoplayer.ExoPlayer exoPlayer = musicPlayer.getExoPlayer();
        if (exoPlayer == null) {
            // Nếu ExoPlayer chưa được khởi tạo, tạo lại
            musicPlayer.initialize();
            exoPlayer = musicPlayer.getExoPlayer();
        }
        if (exoPlayer == null) {
            // Vẫn null — không thể tạo MediaSession
            return;
        }

        // Tạo MediaSession từ ExoPlayer — Media3 tự xử lý play/pause/next/prev qua Player
        mediaSession = new MediaSession.Builder(this, exoPlayer)
                .build();

        // Lắng nghe sự kiện playback để cập nhật notification
        musicPlayer.getEventListener().setListener(new PlayerEventListener.OnEventListener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                // Media3 tự động cập nhật notification
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                // Media3 tự động cập nhật notification metadata
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    // Tự động dừng service nếu hết queue và không repeat
                    if (musicPlayer.getPlaybackQueue().isEmpty()) {
                        stopSelf();
                    }
                }
            }
        });
    }

    @Override
    @Nullable
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        // Trả về MediaSession để controller kết nối
        return mediaSession;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        // MediaSessionService tự động start foreground khi có playback
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Dừng service khi app bị xoá khỏi recent tasks và không đang phát
        if (!musicPlayer.isPlaying()) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        // Huỷ listener để tránh memory leak
        if (musicPlayer != null) {
            musicPlayer.getEventListener().setListener(null);
        }
        // Giải phóng MediaSession trước
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        // Giải phóng ExoPlayer và audio focus
        if (musicPlayer != null) {
            musicPlayer.release();
        }
        super.onDestroy();
    }

    /**
     * Lấy MusicPlayer instance (cho MediaSessionManager.PlaybackCallback).
     */
    @NonNull
    public MusicPlayer getMusicPlayer() {
        return musicPlayer;
    }
}
