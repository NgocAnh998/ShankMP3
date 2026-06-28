package com.example.nghenhac.player;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;

/**
 * Listener chuyên biệt cho các sự kiện playback của ExoPlayer.
 *
 * Nguyên lý:
 * - Tách listener ra khỏi MusicPlayer để dễ quản lý và mở rộng.
 * - Cung cấp các callback chi tiết hơn Player.Listener mặc định.
 * - Kết nối MusicPlayer với UI/Service qua OnEventListener.
 *
 * Luồng xử lý:
 * 1. MusicPlayer khởi tạo ExoPlayer → attach PlayerEventListener.
 * 2. ExoPlayer gọi các phương thức của Player.Listener.
 * 3. PlayerEventListener forward các sự kiện đến OnEventListener.
 * 4. UI/Service nhận sự kiện và cập nhật giao diện tương ứng.
 *
 * Lưu ý:
 * - Tất cả callback đều chạy trên main thread.
 * - Có thể null-check listener trước khi gọi (an toàn).
 */
public class PlayerEventListener implements Player.Listener {

    /** Interface cho các callback playback chi tiết. */
    public interface OnEventListener {
        /** Trạng thái phát thay đổi (READY, BUFFERING, ENDED, IDLE). */
        default void onPlaybackStateChanged(int playbackState) {}
        /** Trạng thái play/pause thay đổi. */
        default void onIsPlayingChanged(boolean isPlaying) {}
        /** Chuyển bài hát (auto/seek/repeat). */
        default void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {}
        /** Player bị lỗi. */
        default void onPlayerError(PlaybackException error) {}
        /** Lỗi được recover (phát lại từ cache). */
        default void onPlayerErrorRecovered() {}
        /** Kết thúc một bài hát (chuẩn bị chuyển bài tiếp theo). */
        default void onPlaybackEnded() {}
        /** Volume thay đổi. */
        default void onVolumeChanged(float volume) {}
    }

    @Nullable
    private OnEventListener listener;

    public PlayerEventListener() {}

    /**
     * Set listener nhận sự kiện playback.
     *
     * @param listener Listener (có thể null để huỷ đăng ký).
     */
    public void setListener(@Nullable OnEventListener listener) {
        this.listener = listener;
    }

    @Nullable
    public OnEventListener getListener() {
        return listener;
    }

    // ════════════════════════════════════════════
    //  Player.Listener Implementation
    // ════════════════════════════════════════════

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (listener != null) {
            listener.onPlaybackStateChanged(playbackState);

            if (playbackState == Player.STATE_ENDED) {
                listener.onPlaybackEnded();
            }
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (listener != null) {
            listener.onIsPlayingChanged(isPlaying);
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        if (listener != null) {
            listener.onMediaItemTransition(mediaItem, reason);
        }
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        if (listener != null) {
            listener.onPlayerError(error);
        }
    }

    @Override
    public void onPlayerErrorChanged(@Nullable PlaybackException error) {
        if (error == null && listener != null) {
            listener.onPlayerErrorRecovered();
        }
    }

    @Override
    public void onVolumeChanged(float volume) {
        if (listener != null) {
            listener.onVolumeChanged(volume);
        }
    }
}
