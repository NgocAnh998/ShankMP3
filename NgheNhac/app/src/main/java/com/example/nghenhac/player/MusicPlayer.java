package com.example.nghenhac.player;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import com.example.nghenhac.data.local.PreferencesManager;
import com.example.nghenhac.data.local.entity.SongEntity;

import java.util.List;

/**
 * Core music player — singleton wrapper quanh ExoPlayer.
 *
 * Nguyên lý:
 * - Singleton: một instance ExoPlayer duy nhất cho toàn app, tránh xung đột âm thanh.
 * - Cấu hình chuyên biệt cho nhạc: buffer nhỏ, audio focus, media source factory.
 * - Tích hợp PlaybackQueue để quản lý danh sách phát (next/previous/repeat/shuffle).
 * - Tích hợp PlayerEventListener để thông báo sự kiện cho UI/Service.
 * - Audio Focus: yêu cầu/gỡ bỏ focus, xử lý transient loss (giảm volume) và gain (tăng lại).
 *
 * Luồng xử lý:
 * 1. initialize() → tạo ExoPlayer với LoadControl (15s-50s buffer) + AudioAttributes + MediaSourceFactory.
 * 2. play(List<SongEntity>, startIndex) → set queue → ExoPlayer phát.
 * 3. User next/previous → ExoPlayer.seekToNext() / seekToPrevious().
 * 4. User pause/resume → ExoPlayer.pause() / play().
 * 5. User seek → ExoPlayer.seekTo(positionMs).
 * 6. Audio focus changes → handleAudioFocusChange() → volume duck / resume.
 * 7. App destroy → release() → giải phóng ExoPlayer, hủy audio focus.
 *
 * Input:
 * - SongEntity hoặc MediaItem: bài hát cần phát.
 * - List<SongEntity>: danh sách phát.
 *
 * Output:
 * - Phát nhạc qua loa/tai nghe.
 * - Sự kiện playback → PlayerEventListener.OnEventListener.
 *
 * Lưu ý:
 * - Tất cả phương thức public đều thread-safe (gọi từ main thread là đủ).
 * - Audio focus request cần AUDIO_SERVICE — dùng context.getSystemService().
 * - getExoPlayer() nullable — kiểm tra null trước khi dùng trực tiếp.
 */
public class MusicPlayer {

    private static final String TAG = "MusicPlayer";

    private static MusicPlayer instance;
    private ExoPlayer exoPlayer;
    private final Context context;
    private final PlaybackQueue playbackQueue;
    private final PlayerEventListener eventListener;

    private MusicPlayer(Context context) {
        this.context = context.getApplicationContext();
        this.playbackQueue = new PlaybackQueue(context);
        this.eventListener = new PlayerEventListener();
    }

    public static synchronized MusicPlayer getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayer(context);
        }
        return instance;
    }

    // ════════════════════════════════════════════
    //  Initialization
    // ════════════════════════════════════════════

    /**
     * Khởi tạo ExoPlayer với cấu hình tối ưu cho nhạc.
     *
     * Nguyên lý:
     * - DefaultLoadControl: buffer 15s (min) – 50s (max) — phù hợp nhạc, tiết kiệm RAM hơn video.
     * - AudioAttributes: USAGE_MEDIA + CONTENT_TYPE_MUSIC — Android xử lý âm thanh nền đúng.
     * - DefaultMediaSourceFactory: tự động tạo MediaSource từ URI.
     * - setAudioAttributes(true): ExoPlayer tự động request audio focus.
     * - Gắn PlayerEventListener để forward sự kiện ra ngoài.
     *
     * Lưu ý:
     * - Gọi nhiều lần an toàn — luôn tạo mới nếu player cũ đã được release.
     * - Nếu đã có player, release và tạo lại (tránh trạng thái cũ).
     */
    public void initialize() {
        if (exoPlayer != null) {
            // Player đã tồn tại, không cần tạo lại
            return;
        }

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        15_000,  // min buffer: 15s
                        50_000,  // max buffer: 50s
                        2_500,   // buffer để bắt đầu phát: 2.5s
                        5_000    // buffer để phát lại sau rebuffer: 5s
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        androidx.media3.common.AudioAttributes audioAttributes =
                new androidx.media3.common.AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.CONTENT_TYPE_MUSIC)
                        .build();

        // Sử dụng CacheDataSourceFactory để tự động caching khi stream
        androidx.media3.datasource.cache.CacheDataSource.Factory cacheFactory =
                CacheDataSourceFactory.getInstance(context).getCacheDataSourceFactory();
        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(cacheFactory);

        exoPlayer = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(audioAttributes, true)
                .build();

        // Gắn listener chính cho UI updates
        exoPlayer.addListener(eventListener);

        // Gắn listener riêng cho xử lý queue advancement và error retry
        // Tách biệt với eventListener (UI) tránh xử lý STATE_ENDED 2 lần
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    handlePlaybackEnded();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                handlePlayerError(error);
            }
        });

        // Khôi phục âm lượng từ preferences
        float savedVolume = PreferencesManager.getInstance(context).getVolume();
        exoPlayer.setVolume(savedVolume);
    }

    /**
     * Xử lý khi bài hát kết thúc — tự động chuyển bài tiếp theo.
     *
     * Nguyên lý:
     * - Nếu repeat ONE → seekTo(0) phát lại.
     * - Nếu còn bài tiếp → next() → seekToNextMediaItem().
     * - Nếu hết queue + repeat NONE → gọi onPlaybackEnded.
     */
    private void handlePlaybackEnded() {
        if (playbackQueue.isEmpty()) return;

        int nextIndex = playbackQueue.next();

        if (nextIndex >= 0 && exoPlayer != null) {
            exoPlayer.seekTo(nextIndex, C.TIME_UNSET);
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }

    // ════════════════════════════════════════════
    //  Playback Controls
    // ════════════════════════════════════════════

    /**
     * Phát danh sách bài hát từ đầu.
     *
     * Input:
     * @param songs      Danh sách bài hát cần phát.
     * @param startIndex Index bắt đầu phát (0 = bài đầu tiên).
     */
    public void play(@Nullable List<SongEntity> songs, int startIndex) {
        initialize();
        resetRetryCount();

        if (songs != null && !songs.isEmpty()) {
            playbackQueue.setQueue(songs, startIndex);
            List<MediaItem> items = playbackQueue.getMediaItems();
            int index = Math.min(startIndex, items.size() - 1);

            exoPlayer.setMediaItems(items, index, C.TIME_UNSET);
            exoPlayer.prepare();
            exoPlayer.play();
        } else if (!playbackQueue.isEmpty()) {
            // Queue đã có sẵn, chỉ việc play
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }

    /**
     * Phát một bài hát đơn lẻ.
     *
     * Input:
     * @param song Bài hát cần phát.
     */
    public void play(@Nullable SongEntity song) {
        if (song == null) return;

        MediaItem mediaItem = buildMediaItem(song);
        initialize();
        resetRetryCount();

        playbackQueue.clear();

        List<MediaItem> singleItem = java.util.Collections.singletonList(mediaItem);
        playbackQueue.setMediaItems(singleItem, 0);

        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    /** Tiếp tục phát (resume) bài hát hiện tại. */
    public void play() {
        if (exoPlayer != null) {
            resetRetryCount();
            // Nếu player đang ở STATE_IDLE (sau lỗi hoặc sau stop), cần prepare() trước
            // ExoPlayer sẽ tự động chuyển tiếp khi sẵn sàng nếu playWhenReady = true
            if (exoPlayer.getPlaybackState() == Player.STATE_IDLE) {
                exoPlayer.prepare();
            }
            exoPlayer.play();
        }
    }

    /** Tạm dừng phát. */
    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    /** Dừng phát hoàn toàn. */
    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
    }

    /** Chuyển đến bài hát tiếp theo trong queue. */
    public void next() {
        if (exoPlayer == null || playbackQueue.isEmpty()) return;

        int nextIndex = playbackQueue.next();
        if (nextIndex >= 0) {
            exoPlayer.seekTo(nextIndex, C.TIME_UNSET);
            exoPlayer.prepare();
            exoPlayer.play();
        } else {
            // Hết queue
            pause();
        }
    }

    /** Chuyển đến bài hát trước đó trong queue. */
    public void previous() {
        if (exoPlayer == null || playbackQueue.isEmpty()) return;

        // Nếu đã phát > 3s, quay lại đầu bài hiện tại
        if (getCurrentPosition() > 3000) {
            seekTo(0);
            return;
        }

        int prevIndex = playbackQueue.previous();
        if (prevIndex >= 0) {
            exoPlayer.seekTo(prevIndex, C.TIME_UNSET);
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }

    /**
     * Tua đến vị trí chỉ định.
     *
     * Input:
     * @param positionMs Vị trí tính bằng milliseconds.
     */
    public void seekTo(long positionMs) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(positionMs);
        }
    }

    // ════════════════════════════════════════════
    //  Repeat & Shuffle
    // ════════════════════════════════════════════

    /** Chuyển đổi repeat mode (NONE → ONE → ALL → NONE). */
    public void cycleRepeatMode() {
        playbackQueue.cycleRepeatMode();
        if (exoPlayer != null) {
            exoPlayer.setRepeatMode(playbackQueue.getRepeatMode());
        }
    }

    /** Lấy repeat mode hiện tại (0=NONE, 1=ONE, 2=ALL). */
    public int getRepeatMode() {
        return playbackQueue.getRepeatMode();
    }

    /** Bật/tắt shuffle mode. */
    public void setShuffleEnabled(boolean enabled) {
        playbackQueue.setShuffleEnabled(enabled);
        if (exoPlayer != null) {
            exoPlayer.setShuffleModeEnabled(enabled);
        }
    }

    /** Kiểm tra shuffle có đang bật không. */
    public boolean isShuffleEnabled() {
        return playbackQueue.isShuffleEnabled();
    }

    // ════════════════════════════════════════════
    //  Volume
    // ════════════════════════════════════════════

    /**
     * Set âm lượng (0.0 - 1.0) và lưu vào preferences.
     *
     * Input:
     * @param volume Giá trị âm lượng (0.0 = câm, 1.0 = max).
     */
    public void setVolume(float volume) {
        if (exoPlayer != null) {
            exoPlayer.setVolume(volume);
        }
        PreferencesManager.getInstance(context).setVolume(volume);
    }

    /** Lấy âm lượng hiện tại (0.0 - 1.0). */
    public float getVolume() {
        return exoPlayer != null ? exoPlayer.getVolume() : 0.8f;
    }

    // ════════════════════════════════════════════
    //  Getters
    // ════════════════════════════════════════════

    /** Lấy vị trí phát hiện tại (milliseconds). */
    public long getCurrentPosition() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    /** Lấy tổng thời lượng bài hát hiện tại (milliseconds). */
    public long getDuration() {
        if (exoPlayer != null && exoPlayer.getDuration() != Long.MIN_VALUE) {
            return exoPlayer.getDuration();
        }
        return 0;
    }

    /** Kiểm tra player đang phát. */
    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    /** Lấy MediaItem hiện tại từ queue. */
    @Nullable
    public MediaItem getCurrentMediaItem() {
        return playbackQueue.getCurrentMediaItem();
    }

    /** Lấy PlaybackQueue để quản lý danh sách phát. */
    @NonNull
    public PlaybackQueue getPlaybackQueue() {
        return playbackQueue;
    }

    /** Lấy PlayerEventListener để set listener UI/Service. */
    @NonNull
    public PlayerEventListener getEventListener() {
        return eventListener;
    }

    @Nullable
    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    // ════════════════════════════════════════════
    //  Error Handling & Retry
    // ════════════════════════════════════════════

    /** Số lần retry tối đa khi player bị lỗi. */
    private static final int MAX_RETRY_COUNT = 3;
    /** Khoảng thời gian chờ giữa các lần retry (ms). */
    private static final long RETRY_DELAY_MS = 1000L;

    private int retryCount = 0;

    /**
     * Xử lý lỗi player — thử phát lại nếu còn retry.
     *
     * Nguyên lý:
     * - Nếu còn retry (tối đa MAX_RETRY_COUNT), chờ RETRY_DELAY_MS rồi prepare() + play() lại.
     * - Nếu hết retry, dừng player và thông báo lỗi qua listener.
     * - Retry count được reset khi bài hát mới được phát (play() mới).
     *
     * Input:
     * @param error PlaybackException từ ExoPlayer.
     */
    public void handlePlayerError(@NonNull PlaybackException error) {
        retryCount++;
        Log.w(TAG, "Player error (attempt " + retryCount + "/" + MAX_RETRY_COUNT + "): "
                + error.getMessage());

        if (retryCount <= MAX_RETRY_COUNT && exoPlayer != null) {
            // Thử phát lại sau delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (exoPlayer != null) {
                    exoPlayer.prepare();
                    exoPlayer.play();
                }
            }, RETRY_DELAY_MS);
        } else {
            // Hết retry — KHÔNG stop() (vì stop() xoá media items)
            // Chỉ pause lại và reset retry, giữ nguyên queue
            Log.e(TAG, "Max retry count reached. Pausing player — user can retry manually.");
            if (exoPlayer != null) {
                exoPlayer.pause();
            }
            retryCount = 0;
        }
    }

    /** Reset retry count khi phát bài mới. */
    private void resetRetryCount() {
        retryCount = 0;
    }

    // ════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════

    /**
     * Giải phóng toàn bộ tài nguyên player.
     *
     * Nguyên lý:
     * - release() ExoPlayer → giải phóng decoder, audio track, surface.
     * - abandonAudioFocus() → giải phóng audio focus cho app khác.
     * - set instance = null → cho phép tạo lại sau.
     *
     * Gọi trong onDestroy() của Service.
     * KHÔNG gọi trong onDestroy() của Activity — player chạy xuyên suốt.
     */
    public void release() {
        if (exoPlayer != null) {
            // ExoPlayer.release() tự động abandon audio focus + giải phóng decoder
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        playbackQueue.clear();
        instance = null;
    }

    // ════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════

    /**
     * Chuyển SongEntity thành MediaItem với metadata.
     *
     * Input:
     * @param song Entity bài hát cần map.
     *
     * Output: MediaItem với mediaId, URI, và metadata (title, artist, album, albumArt).
     */
    @NonNull
    private static MediaItem buildMediaItem(@NonNull SongEntity song) {
        return new MediaItem.Builder()
                .setMediaId(String.valueOf(song.getId()))
                .setUri(song.getFilePath())
                .setMediaMetadata(
                        new androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(song.getTitle())
                                .setArtist(song.getArtist())
                                .setAlbumTitle(song.getAlbum())
                                .setArtworkUri(
                                        song.getAlbumArtUri() != null
                                                ? android.net.Uri.parse(song.getAlbumArtUri())
                                                : null)
                                .build()
                )
                .build();
    }
}
