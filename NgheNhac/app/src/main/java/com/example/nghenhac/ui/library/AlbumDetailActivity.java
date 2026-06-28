package com.example.nghenhac.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.repository.SongRepository;
import com.example.nghenhac.player.MusicPlayer;
import com.example.nghenhac.ui.adapter.SongAdapter;
import com.example.nghenhac.ui.player.PlayerActivity;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * AlbumDetailActivity — màn hình chi tiết album.
 * <p>
 * Nguyên lý:
 * - Nhận tên album và nghệ sĩ từ Intent, observe SongRepository.getByAlbum() LiveData.
 * - Hiển thị ảnh bìa, tên album, nghệ sĩ, số lượng bài hát.
 * - RecyclerView + SongAdapter hiển thị danh sách bài hát trong album.
 * - Click bài hát → phát từ album này (MusicPlayer.play() với danh sách).
 * <p>
 * Luồng xử lý:
 * 1. Activity mở → onCreate() → lấy album name từ Intent → observe dữ liệu.
 * 2. Danh sách bài hát thay đổi → adapter.submitList() + cập nhật header.
 * 3. User click bài hát → MusicPlayer.play(songs, startIndex) + mở PlayerActivity.
 * 4. User click back → finish().
 */
public class AlbumDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ALBUM_NAME = "album_name";
    private static final String EXTRA_ARTIST = "artist";

    private String albumName;
    private String artist;

    // Views
    private ShapeableImageView albumArt;
    private TextView albumNameView, albumArtistView, albumSongCount;

    // Adapter
    private SongAdapter songAdapter;

    // Repository
    private SongRepository repository;

    // Data
    private List<SongEntity> currentSongs;

    // ── Intent helper ──

    /**
     * Tạo Intent để mở AlbumDetailActivity.
     *
     * @param context   Context.
     * @param albumName Tên album.
     * @param artist    Tên nghệ sĩ.
     * @return Intent đã có extras.
     */
    public static Intent createIntent(@NonNull android.content.Context context,
                                      @NonNull String albumName,
                                      @NonNull String artist) {
        Intent intent = new Intent(context, AlbumDetailActivity.class);
        intent.putExtra(EXTRA_ALBUM_NAME, albumName);
        intent.putExtra(EXTRA_ARTIST, artist);
        return intent;
    }

    // ════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        // Get extras from intent
        albumName = getIntent().getStringExtra(EXTRA_ALBUM_NAME);
        artist = getIntent().getStringExtra(EXTRA_ARTIST);

        if (albumName == null || albumName.isEmpty()) {
            finish();
            return;
        }

        repository = SongRepository.getInstance(this);

        // Init views
        initViews();

        // Setup RecyclerView + adapter
        setupRecyclerView();

        // Setup click listeners
        setupClickListeners();

        // Observe data
        observeSongs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // LiveData tự động dọn dẹp qua LifecycleOwner, không cần unregister thủ công
    }

    // ════════════════════════════════════════════
    //  Init
    // ════════════════════════════════════════════

    private void initViews() {
        albumArt = findViewById(R.id.album_detail_art);
        albumNameView = findViewById(R.id.album_detail_name);
        albumArtistView = findViewById(R.id.album_detail_artist);
        albumSongCount = findViewById(R.id.album_detail_song_count);

        // Set album info from extras
        albumNameView.setText(albumName);

        if (artist != null && !artist.isEmpty()) {
            albumArtistView.setText(artist);
        } else {
            albumArtistView.setText(R.string.unknown_artist);
        }

        // Back button
        findViewById(R.id.album_detail_back).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        androidx.recyclerview.widget.RecyclerView recyclerView = findViewById(R.id.album_detail_song_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        songAdapter = new SongAdapter();
        songAdapter.setOnSongClickListener(new SongAdapter.OnSongClickListener() {
            @Override
            public void onItemClick(@NonNull SongEntity song, int position, @NonNull View itemView) {
                // Phát nhạc từ album này
                if (currentSongs != null && !currentSongs.isEmpty()) {
                    MusicPlayer.getInstance(AlbumDetailActivity.this)
                            .play(currentSongs, position);
                    Intent intent = new Intent(AlbumDetailActivity.this, PlayerActivity.class);
                    startActivityWithSharedElement(intent, itemView);
                }
            }

            @Override
            public void onMoreClick(@NonNull SongEntity song, @NonNull View anchor) {
                SongBottomSheetDialog.show(AlbumDetailActivity.this, song);
            }

            @Override
            public void onFavoriteClick(@NonNull SongEntity song, boolean isFavorite, int position) {
                com.example.nghenhac.data.repository.SongRepository.getInstance(AlbumDetailActivity.this)
                        .updateFavorite(song.getId(), isFavorite);
            }
        });

        recyclerView.setAdapter(songAdapter);
    }

    private void setupClickListeners() {
        // No special click listeners beyond back button and song items
    }

    /**
     * Start activity với shared element transition (album art).
     *
     * Nguyên lý:
     * - Tìm album art View trong itemView.
     * - Tạo ActivityOptions.makeSceneTransitionAnimation với shared element.
     * - Nếu không tìm thấy (item bị recycle), fallback về startActivity thường.
     *
     * Input:
     * @param intent   Intent đến PlayerActivity.
     * @param itemView View của item được click.
     */
    private void startActivityWithSharedElement(@NonNull Intent intent, @NonNull View itemView) {
        View albumArt = itemView.findViewById(R.id.song_art);
        if (albumArt != null && albumArt.getTransitionName() != null) {
            android.app.ActivityOptions options = android.app.ActivityOptions
                    .makeSceneTransitionAnimation(this, albumArt, albumArt.getTransitionName());
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }

    // ════════════════════════════════════════════
    //  Data observation
    // ════════════════════════════════════════════

    private void observeSongs() {
        LiveData<List<SongEntity>> songsLiveData = repository.getByAlbum(albumName);
        songsLiveData.observe(this, songs -> {
            currentSongs = songs;
            songAdapter.submitList(songs);

            // Cập nhật song count
            if (songs != null) {
                updateSongCount(songs.size());
            } else {
                updateSongCount(0);
            }
        });
    }

    // ════════════════════════════════════════════
    //  UI Updates
    // ════════════════════════════════════════════

    private void updateSongCount(int count) {
        if (count > 0) {
            albumSongCount.setText(getString(R.string.playlist_song_count_format, count));
        } else {
            albumSongCount.setText(R.string.playlist_no_songs);
        }
    }
}
