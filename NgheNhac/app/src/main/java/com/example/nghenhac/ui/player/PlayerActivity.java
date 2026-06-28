package com.example.nghenhac.ui.player;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nghenhac.R;
import com.example.nghenhac.player.MusicPlayer;
import com.example.nghenhac.player.PlayerEventListener;
import com.example.nghenhac.util.ImageLoader;
import com.example.nghenhac.util.PaletteHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;

import java.util.Locale;

import android.graphics.drawable.GradientDrawable;

/**
 * PlayerActivity — màn hình phát nhạc toàn màn hình.
 *
 * Nguyên lý:
 * - Kết nối với MusicPlayer (singleton) để điều khiển và theo dõi playback.
 * - Cập nhật UI realtime qua PlayerEventListener.OnEventListener.
 * - Handler postDelayed để cập nhật seekbar position trong khi phát.
 * - Hỗ trợ đầy đủ controls: play/pause, next, previous, seek, shuffle, repeat.
 *
 * Luồng xử lý:
 * 1. Activity mở → onCreate() → kết nối listener, restore trạng thái.
 * 2. onStart() → đăng ký listener + bắt đầu cập nhật seekbar.
 * 3. User tương tác → gọi MusicPlayer methods (play, pause, seek, next...).
 * 4. Playback state thay đổi → listener cập nhật UI (title, artist, play icon).
 * 5. onStop() → huỷ listener, ngừng cập nhật seekbar.
 *
 * Lưu ý:
 * - Không release MusicPlayer khi activity destroy — player chạy xuyên suốt.
 * - Dùng handler để cập nhật seekbar (không dùng Choreographer vì đơn giản).
 */
public class PlayerActivity extends AppCompatActivity {

    // Views
    private ShapeableImageView albumArt;
    private TextView songTitle, songArtist;
    private TextView currentTime, totalTime;
    private Slider seekbar;
    private FloatingActionButton playPauseFab;
    private ImageButton shuffleButton, repeatButton, menuButton;

    // Player
    private MusicPlayer musicPlayer;
    private PlayerEventListener.OnEventListener eventListener;

    // Seekbar update
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private final Runnable seekRunnable = new Runnable() {
        @Override
        public void run() {
            updateSeekbarPosition();
            seekHandler.postDelayed(this, 500); // update mỗi 500ms
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Init views
        initViews();

        // Get MusicPlayer singleton
        musicPlayer = MusicPlayer.getInstance(this);
        musicPlayer.initialize();

        // Setup click listeners
        setupClickListeners();

        // Tạo listener cập nhật UI
        eventListener = new PlayerEventListener.OnEventListener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updatePlayPauseIcon();
                updateDurationInfo();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();
                if (isPlaying) {
                    startSeekbarUpdates();
                } else {
                    stopSeekbarUpdates();
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable androidx.media3.common.MediaItem mediaItem, int reason) {
                updateSongInfo();
                updateDurationInfo();
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                // ExoPlayer gọi callback này trên main thread, an toàn để cập nhật UI
                String errorMsg = error.getMessage();
                if (errorMsg != null && errorMsg.length() > 100) {
                    errorMsg = errorMsg.substring(0, 100);
                }
                com.google.android.material.snackbar.Snackbar.make(
                        findViewById(android.R.id.content),
                        "Lỗi phát nhạc: " + (errorMsg != null ? errorMsg : "không rõ"),
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("Thử lại", v -> {
                    if (!musicPlayer.isPlaying()) {
                        musicPlayer.play();
                    }
                }).show();
            }
        };

        // Update UI lần đầu
        updateSongInfo();
        updateDurationInfo();
        updatePlayPauseIcon();
        updateShuffleRepeatIcons();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Đăng ký listener
        musicPlayer.getEventListener().setListener(eventListener);

        // Bắt đầu cập nhật seekbar nếu đang phát
        if (musicPlayer.isPlaying()) {
            startSeekbarUpdates();
        }
        updateSongInfo();
        updatePlayPauseIcon();
        updateShuffleRepeatIcons();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopSeekbarUpdates();
        // Lưu vị trí phát hiện tại để khôi phục sau
        if (musicPlayer != null) {
            com.example.nghenhac.data.local.PreferencesManager.getInstance(this)
                    .setLastPosition(musicPlayer.getCurrentPosition());
        }
        // Huỷ listener khi không cần
        musicPlayer.getEventListener().setListener(null);
    }

    // ════════════════════════════════════════════
    //  Init
    // ════════════════════════════════════════════

    // Player background được cập nhật bằng palette colors
    private View playerBackground;

    private void initViews() {
        albumArt = findViewById(R.id.player_album_art);
        songTitle = findViewById(R.id.player_song_title);
        songArtist = findViewById(R.id.player_song_artist);
        currentTime = findViewById(R.id.player_current_time);
        totalTime = findViewById(R.id.player_total_time);
        seekbar = findViewById(R.id.player_seekbar);
        playPauseFab = findViewById(R.id.player_play_pause);
        shuffleButton = findViewById(R.id.player_shuffle);
        repeatButton = findViewById(R.id.player_repeat);
        playerBackground = findViewById(R.id.player_root);

        // Back button
        findViewById(R.id.player_back).setOnClickListener(v -> finish());

        // Menu button — sleep timer + queue
        menuButton = findViewById(R.id.player_menu);
        menuButton.setOnClickListener(v -> showPlayerMenu());
    }

    private void setupClickListeners() {
        // Play / Pause
        playPauseFab.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
            } else {
                musicPlayer.play();
            }
        });

        // Next
        findViewById(R.id.player_next).setOnClickListener(v -> musicPlayer.next());

        // Previous
        findViewById(R.id.player_prev).setOnClickListener(v -> musicPlayer.previous());

        // Shuffle
        shuffleButton.setOnClickListener(v -> {
            musicPlayer.setShuffleEnabled(!musicPlayer.isShuffleEnabled());
            updateShuffleRepeatIcons();
        });

        // Repeat
        repeatButton.setOnClickListener(v -> {
            musicPlayer.cycleRepeatMode();
            updateShuffleRepeatIcons();
        });

        // Seekbar
        seekbar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                musicPlayer.seekTo((long) value);
            }
        });
    }

    // ════════════════════════════════════════════
    //  UI Updates
    // ════════════════════════════════════════════

    private void updateSongInfo() {
        androidx.media3.common.MediaItem currentItem = musicPlayer.getCurrentMediaItem();
        if (currentItem != null && currentItem.mediaMetadata != null) {
            String title = currentItem.mediaMetadata.title != null
                    ? currentItem.mediaMetadata.title.toString() : getString(R.string.app_name);
            String artist = currentItem.mediaMetadata.artist != null
                    ? currentItem.mediaMetadata.artist.toString() : getString(R.string.unknown_artist);
            songTitle.setText(title);
            songArtist.setText(artist);
        } else {
            songTitle.setText(R.string.app_name);
            songArtist.setText(R.string.unknown_artist);
        }
        // Load album art qua ImageLoader
        if (currentItem != null && currentItem.mediaMetadata != null) {
            String artUri = currentItem.mediaMetadata.artworkUri != null
                    ? currentItem.mediaMetadata.artworkUri.toString() : null;
            ImageLoader.load(this, artUri, albumArt);

            // Phân tích màu sắc từ ảnh bìa và áp dụng làm background
            if (artUri != null) {
                PaletteHelper.loadPaletteAsync(this, artUri,
                        getColor(com.google.android.material.R.color.design_default_color_surface),
                        new PaletteHelper.PaletteCallback() {
                            @Override
                            public void onColorsExtracted(int vibrantColor,
                                                          int mutedColor,
                                                          int defaultColor) {
                                applyPaletteBackground(vibrantColor, mutedColor);
                            }

                            @Override
                            public void onError(Exception e) {
                                // Không cần xử lý — giữ màu mặc định
                            }
                        });
            }
        } else {
            albumArt.setImageResource(R.drawable.ic_library);
        }
    }

    private void updateSeekbarPosition() {
        long position = musicPlayer.getCurrentPosition();
        long duration = musicPlayer.getDuration();
        if (duration > 0) {
            seekbar.setValueTo(duration);
            seekbar.setValue(position);
        }
        currentTime.setText(formatDuration(position));
    }

    private void updateDurationInfo() {
        long duration = musicPlayer.getDuration();
        if (duration > 0) {
            seekbar.setValueTo(duration);
            totalTime.setText(formatDuration(duration));
        } else {
            totalTime.setText("--:--");
        }
    }

    private void updatePlayPauseIcon() {
        if (musicPlayer.isPlaying()) {
            playPauseFab.setImageResource(R.drawable.ic_pause);
            playPauseFab.setContentDescription(getString(R.string.action_pause));
        } else {
            playPauseFab.setImageResource(R.drawable.ic_play_arrow);
            playPauseFab.setContentDescription(getString(R.string.action_play));
        }
    }

    /**
     * Hiển thị menu ngữ cảnh của PlayerActivity.
     *
     * Nguyên lý:
     * - PopupMenu với các tuỳ chọn: Sleep timer, Queue.
     * - Gọi SleepTimerDialog.show() hoặc QueueDialog.show() tương ứng.
     */
    private void showPlayerMenu() {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, menuButton);
        popup.getMenu().add(0, 1, 0, "Hẹn giờ tắt nhạc");
        popup.getMenu().add(0, 2, 0, "Danh sách phát");
        popup.getMenu().add(0, 3, 0, "Cân bằng âm thanh");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    SleepTimerDialog.show(this, () -> {
                        if (musicPlayer.isPlaying()) {
                            musicPlayer.pause();
                            com.google.android.material.snackbar.Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "Đã tắt nhạc theo hẹn giờ",
                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    });
                    return true;
                case 2:
                    QueueDialog.show(this);
                    return true;
                case 3:
                    EqualizerDialog.show(this);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void updateShuffleRepeatIcons() {
        // Shuffle
        shuffleButton.setColorFilter(getColor(musicPlayer.isShuffleEnabled()
                ? R.color.primary : android.R.color.darker_gray));

        // Repeat
        int repeatMode = musicPlayer.getRepeatMode();
        if (repeatMode == 0) { // NONE
            repeatButton.setColorFilter(getColor(android.R.color.darker_gray));
        } else {
            repeatButton.setColorFilter(getColor(R.color.primary));
        }
    }

    // ════════════════════════════════════════════
    //  Seekbar Updates
    // ════════════════════════════════════════════

    private void startSeekbarUpdates() {
        seekHandler.removeCallbacks(seekRunnable);
        seekHandler.post(seekRunnable);
    }

    private void stopSeekbarUpdates() {
        seekHandler.removeCallbacks(seekRunnable);
    }

    /**
     * Áp dụng màu sắc từ Palette vào background của PlayerActivity.
     *
     * Nguyên lý:
     * - Tạo GradientDrawable dọc từ vibrant (trên) xuống muted (dưới).
     * - Set làm background của CoordinatorLayout (player_root).
     * - Màu sắc thay đổi theo từng bài hát dựa trên ảnh bìa.
     *
     * Input:
     * @param vibrantColor Màu nổi bật (phía trên).
     * @param mutedColor   Màu dịu (phía dưới).
     */
    private void applyPaletteBackground(int vibrantColor, int mutedColor) {
        if (playerBackground == null) return;

        try {
            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{vibrantColor, mutedColor});
            gradient.setAlpha(50); // Trong suốt nhẹ để chữ vẫn đọc được
            playerBackground.setBackground(gradient);
        } catch (Exception e) {
            // Fallback: giữ màu mặc định
        }
    }

    // ════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════

    private static String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}
