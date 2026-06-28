package com.example.nghenhac.ui.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;

import com.example.nghenhac.R;
import com.example.nghenhac.player.MusicPlayer;
import com.example.nghenhac.player.PlayerEventListener;
import com.example.nghenhac.util.ImageLoader;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * Mini player Fragment — thanh điều khiển thu nhỏ ở đáy màn hình chính.
 *
 * Nguyên lý:
 * - Inflate layout_mini_player.xml để hiển thị album art, title, artist, controls.
 * - Kết nối với MusicPlayer (singleton) qua PlayerEventListener để cập nhật realtime.
 * - Ẩn/hiện mini player dựa trên trạng thái playback queue.
 * - Click vào mini player → mở PlayerActivity toàn màn hình.
 *
 * Luồng xử lý:
 * 1. Fragment attach → onResume() → đăng ký listener + cập nhật UI.
 * 2. MusicPlayer phát nhạc → onMediaItemTransition → cập nhật title/artist.
 * 3. User bấm play/pause/next/prev → gọi MusicPlayer tương ứng.
 * 4. User click vào mini player → mở PlayerActivity.
 * 5. Queue rỗng → mini player ẩn; có bài hát → mini player hiện.
 * 6. Fragment detach → onPause() → huỷ listener.
 */
public class MiniPlayerFragment extends Fragment {

    // Views
    private View rootView;
    private ShapeableImageView albumArt;
    private TextView songTitle, songArtist;
    private ImageButton playPauseButton;

    // Player
    private MusicPlayer musicPlayer;
    private PlayerEventListener.OnEventListener eventListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.layout_mini_player, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init views
        albumArt = view.findViewById(R.id.mini_player_art);
        songTitle = view.findViewById(R.id.mini_player_title);
        songArtist = view.findViewById(R.id.mini_player_artist);
        playPauseButton = view.findViewById(R.id.mini_player_play_pause);

        // MusicPlayer
        musicPlayer = MusicPlayer.getInstance(requireContext());

        // Setup click listeners
        setupClickListeners();

        // Tạo listener cập nhật UI
        eventListener = new PlayerEventListener.OnEventListener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                updateSongInfo();
                updateVisibility();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();
                updateVisibility();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updatePlayPauseIcon();
                updateVisibility();
            }
        };

        // Update UI lần đầu
        updateSongInfo();
        updatePlayPauseIcon();
        updateVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Đăng ký listener
        musicPlayer.getEventListener().setListener(eventListener);
        // Update UI
        updateSongInfo();
        updatePlayPauseIcon();
        updateVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Huỷ listener để tránh memory leak
        musicPlayer.getEventListener().setListener(null);
    }

    // ════════════════════════════════════════════
    //  Click Listeners
    // ════════════════════════════════════════════

    private void setupClickListeners() {
        // Click toàn bộ mini player → mở PlayerActivity
        rootView.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PlayerActivity.class);
            startActivity(intent);
        });

        // Play / Pause
        playPauseButton.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
            } else {
                musicPlayer.play();
            }
        });

        // Previous
        rootView.findViewById(R.id.mini_player_prev).setOnClickListener(v ->
                musicPlayer.previous());

        // Next
        rootView.findViewById(R.id.mini_player_next).setOnClickListener(v ->
                musicPlayer.next());
    }

    // ════════════════════════════════════════════
    //  UI Updates
    // ════════════════════════════════════════════

    /**
     * Cập nhật title + artist từ MediaItem hiện tại.
     * Nếu không có bài hát, hiển thị tên app.
     */
    private void updateSongInfo() {
        MediaItem currentItem = musicPlayer.getCurrentMediaItem();
        if (currentItem != null && currentItem.mediaMetadata != null) {
            String title = currentItem.mediaMetadata.title != null
                    ? currentItem.mediaMetadata.title.toString()
                    : getString(R.string.app_name);
            String artist = currentItem.mediaMetadata.artist != null
                    ? currentItem.mediaMetadata.artist.toString()
                    : getString(R.string.unknown_artist);
            songTitle.setText(title);
            songArtist.setText(artist);
        } else {
            songTitle.setText(R.string.app_name);
            songArtist.setText(R.string.unknown_artist);
        }
        // Load album art thumbnail qua ImageLoader
        if (currentItem != null && currentItem.mediaMetadata != null
                && currentItem.mediaMetadata.artworkUri != null) {
            ImageLoader.loadThumb(requireContext(),
                    currentItem.mediaMetadata.artworkUri.toString(), albumArt);
        } else {
            albumArt.setImageResource(R.drawable.ic_library);
        }
    }

    /**
     * Cập nhật icon play/pause.
     * Icon play khi đang phát, pause khi dừng.
     */
    private void updatePlayPauseIcon() {
        if (musicPlayer.isPlaying()) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            playPauseButton.setContentDescription(getString(R.string.action_pause));
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            playPauseButton.setContentDescription(getString(R.string.action_play));
        }
    }

    /**
     * Ẩn mini player khi queue rỗng (không có bài hát), hiện khi có bài.
     */
    private void updateVisibility() {
        boolean hasSong = musicPlayer.getCurrentMediaItem() != null
                || !musicPlayer.getPlaybackQueue().isEmpty();
        rootView.setVisibility(hasSong ? View.VISIBLE : View.GONE);
    }
}
