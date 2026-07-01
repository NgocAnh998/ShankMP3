package com.example.nghenhac.ui.library;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.LiveData;

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.repository.PlaylistRepository;
import com.example.nghenhac.data.repository.SongRepository;
import com.example.nghenhac.player.MusicPlayer;
import com.example.nghenhac.ui.adapter.SongAdapter;
import com.example.nghenhac.ui.player.PlayerActivity;
import com.example.nghenhac.util.FilePickerUtil;
import com.example.nghenhac.util.ImageLoader;
import com.example.nghenhac.util.PlaylistExporter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PlaylistDetailActivity — màn hình chi tiết playlist.
 * <p>
 * Nguyên lý:
 * - Nhận playlistId từ Intent, observe PlaylistRepository để lấy thông tin playlist + danh sách bài hát.
 * - Hiển thị tên, mô tả, số lượng bài hát.
 * - RecyclerView + SongAdapter hiển thị danh sách bài hát trong playlist.
 * - Click bài hát → phát từ playlist này (MusicPlayer.play() với danh sách).
 * - Menu button → export playlist ra M3U hoặc XML.
 */
public class PlaylistDetailActivity extends AppCompatActivity {

    private static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private long playlistId;

    // Views
    private TextView playlistNameText, playlistDescription, playlistSongCount;
    private View emptyState;
    private ShapeableImageView playlistCover;
    private FloatingActionButton addSongFab;

    // Adapter
    private SongAdapter songAdapter;

    // Repository
    private PlaylistRepository repository;

    // Data
    private List<SongEntity> currentSongs;

    // Export
    private ExecutorService executor;
    private ActivityResultLauncher<String[]> exportLauncher;

    // ── Intent helper ──

    public static Intent createIntent(@NonNull android.content.Context context, long playlistId) {
        Intent intent = new Intent(context, PlaylistDetailActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        return intent;
    }

    // ════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getLongExtra(EXTRA_PLAYLIST_ID, -1);
        if (playlistId == -1) {
            finish();
            return;
        }

        repository = PlaylistRepository.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        // Setup ActivityResultLauncher for export file picker
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::doExport);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        observePlaylist();
        observeSongs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    // ════════════════════════════════════════════
    //  Init
    // ════════════════════════════════════════════

    private void initViews() {
        playlistNameText = findViewById(R.id.playlist_detail_name);
        playlistDescription = findViewById(R.id.playlist_detail_description);
        playlistSongCount = findViewById(R.id.playlist_detail_song_count);
        playlistCover = findViewById(R.id.playlist_detail_cover);
        emptyState = findViewById(R.id.playlist_detail_empty);
        addSongFab = findViewById(R.id.playlist_detail_add_song);

        findViewById(R.id.playlist_detail_back).setOnClickListener(v -> finish());
        findViewById(R.id.playlist_detail_menu).setOnClickListener(v -> showExportMenu(v));
    }

    private void setupRecyclerView() {
        androidx.recyclerview.widget.RecyclerView recyclerView = findViewById(R.id.playlist_detail_song_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        songAdapter = new SongAdapter();
        songAdapter.setOnSongClickListener(new SongAdapter.OnSongClickListener() {
            @Override
            public void onItemClick(@NonNull SongEntity song, int position, @NonNull View itemView) {
                if (currentSongs != null && !currentSongs.isEmpty()) {
                    MusicPlayer.getInstance(PlaylistDetailActivity.this)
                            .play(currentSongs, position);
                    Intent intent = new Intent(PlaylistDetailActivity.this, PlayerActivity.class);
                    startActivityWithSharedElement(intent, itemView);
                }
            }

            @Override
            public void onMoreClick(@NonNull SongEntity song, @NonNull View anchor) {
                showRemoveSongDialog(song);
            }

            @Override
            public void onFavoriteClick(@NonNull SongEntity song, boolean isFavorite, int position) {
                com.example.nghenhac.data.repository.SongRepository.getInstance(PlaylistDetailActivity.this)
                        .updateFavorite(song.getId(), isFavorite);
            }
        });

        recyclerView.setAdapter(songAdapter);
    }

    private void setupClickListeners() {
        addSongFab.setOnClickListener(v -> showAddSongsDialog());
    }

    /**
     * Start activity với shared element transition (album art).
     *
     * Nguyên lý:
     * - Tìm album art View trong itemView.
     * - Tạo ActivityOptions.makeSceneTransitionAnimation với shared element.
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
    //  Export
    // ════════════════════════════════════════════

    private boolean exportModeIsM3u = true;

    private void showExportMenu(@NonNull View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, R.string.action_export_m3u);
        popup.getMenu().add(0, 2, 0, R.string.action_export_xml);
        popup.setOnMenuItemClickListener(item -> {
            String name = playlistNameText.getText().toString();
            switch (item.getItemId()) {
                case 1:
                    exportModeIsM3u = true;
                    exportLauncher.launch(new String[]{FilePickerUtil.MIME_TYPE_M3U});
                    return true;
                case 2:
                    exportModeIsM3u = false;
                    exportLauncher.launch(new String[]{FilePickerUtil.MIME_TYPE_XML});
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void doExport(@Nullable Uri uri) {
        if (uri == null) return; // User cancelled

        if (currentSongs == null || currentSongs.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content),
                    R.string.playlist_no_songs, Snackbar.LENGTH_SHORT).show();
            return;
        }

        String name = playlistNameText.getText().toString();
        executor.execute(() -> {
            boolean success;
            if (exportModeIsM3u) {
                success = PlaylistExporter.exportToM3U(PlaylistDetailActivity.this,
                        uri, new PlaylistEntity(name, ""), currentSongs);
            } else {
                success = PlaylistExporter.exportToXML(PlaylistDetailActivity.this,
                        uri, new PlaylistEntity(name, ""), currentSongs);
            }

            boolean finalSuccess = success;
            runOnUiThread(() -> {
                Snackbar.make(findViewById(android.R.id.content),
                        finalSuccess ? R.string.export_success : R.string.export_error,
                        Snackbar.LENGTH_SHORT).show();
            });
        });
    }

    // ════════════════════════════════════════════
    //  Data observation
    // ════════════════════════════════════════════

    private void observePlaylist() {
        LiveData<PlaylistEntity> playlistLiveData = repository.getPlaylistById(playlistId);
        playlistLiveData.observe(this, playlist -> {
            if (playlist != null) {
                playlistNameText.setText(playlist.getName());
                String desc = playlist.getDescription();
                if (desc != null && !desc.isEmpty()) {
                    playlistDescription.setText(desc);
                    playlistDescription.setVisibility(View.VISIBLE);
                } else {
                    playlistDescription.setVisibility(View.GONE);
                }
                updateSongCount(playlist.getSongCount());
                // Load ảnh bìa từ bài hát đầu tiên trong playlist
                loadPlaylistCover();
            } else {
                finish();
            }
        });
    }

    /**
     * Load ảnh bìa playlist từ bài hát đầu tiên.
     *
     * Nguyên lý:
     * - Gọi repository.getFirstSongAlbumArtUri() để lấy URI ảnh bìa.
     * - Dùng ImageLoader để load ảnh vào ShapeableImageView.
     * - Nếu không có ảnh (playlist rỗng), giữ icon mặc định.
     */
    private void loadPlaylistCover() {
        executor.execute(() -> {
            String albumArtUri = repository.getFirstSongAlbumArtUri(playlistId);
            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                String finalUri = albumArtUri;
                runOnUiThread(() ->
                        ImageLoader.load(PlaylistDetailActivity.this, finalUri, playlistCover));
            }
        });
    }

    private void observeSongs() {
        LiveData<List<SongEntity>> songsLiveData = repository.getSongsInPlaylist(playlistId);
        songsLiveData.observe(this, songs -> {
            currentSongs = songs;
            songAdapter.submitList(songs);
            if (songs == null || songs.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.GONE);
            }
            updateSongCount(songs != null ? songs.size() : 0);
        });
    }

    // ════════════════════════════════════════════
    //  UI Updates
    // ════════════════════════════════════════════

    private void updateSongCount(int count) {
        if (count > 0) {
            playlistSongCount.setText(getString(R.string.playlist_song_count_format, count));
        } else {
            playlistSongCount.setText(R.string.playlist_no_songs);
        }
    }

    // ════════════════════════════════════════════
    //  Actions
    // ════════════════════════════════════════════

    /**
     * Hiển thị dialog chọn bài hát từ thư viện để thêm vào playlist.
     *
     * Nguyên lý:
     * - Dùng Room LiveData observe() để load danh sách bài hát — đảm bảo dữ liệu được query đúng.
     * - observeOnce pattern: sau khi nhận được dữ liệu lần đầu, tự removeObserver để chỉ chạy một lần.
     * - Hiển thị AlertDialog multi-choice với danh sách bài hát.
     * - User chọn → thêm các bài đã chọn vào playlist.
     *
     * Luồng xử lý:
     * 1. Gọi songRepo.getAllSongs() → observe với Observer dùng 1 lần.
     * 2. Observer chạy khi Room load xong (luôn có dữ liệu, dù empty).
     * 3. Nếu rỗng → Snackbar "Thư viện trống".
     * 4. Nếu có dữ liệu → hiển thị AlertDialog multi-choice.
     * 5. User chọn → click "Thêm" → executor.execute → repository.addSongsToPlaylist().
     * 6. Snackbar thông báo số lượng bài đã thêm.
     *
     * Lưu ý:
     * - Dùng `this` làm LifecycleOwner (Activity), an toàn vì dialog chỉ hiển thị khi activity active.
     * - Observer tự remove sau lần callback đầu tiên để tránh gọi lại khi dữ liệu thay đổi.
     */
    private void showAddSongsDialog() {
        final SongRepository songRepo = SongRepository.getInstance(this);
        
        // Dùng observeOnce: Room tự load data trên background thread rồi callback trên UI thread
        final androidx.lifecycle.LiveData<List<SongEntity>> songsLiveData = songRepo.getAllSongs();
        songsLiveData.observe(this, new androidx.lifecycle.Observer<List<SongEntity>>() {
            @Override
            public void onChanged(List<SongEntity> allSongs) {
                // Chỉ chạy một lần — remove ngay sau khi nhận data
                songsLiveData.removeObserver(this);

                if (allSongs == null || allSongs.isEmpty()) {
                    Snackbar.make(findViewById(android.R.id.content),
                            "Thư viện trống",
                            Snackbar.LENGTH_SHORT).show();
                    return;
                }

                int songCount = allSongs.size();
                String[] songNames = new String[songCount];
                boolean[] selected = new boolean[songCount];
                for (int i = 0; i < songCount; i++) {
                    SongEntity s = allSongs.get(i);
                    songNames[i] = s.getTitle() + " - " + s.getArtist();
                }

                new androidx.appcompat.app.AlertDialog.Builder(PlaylistDetailActivity.this)
                        .setTitle("Thêm bài hát")
                        .setMultiChoiceItems(songNames, selected, (dialog, which, isChecked) -> {
                            selected[which] = isChecked;
                        })
                        .setPositiveButton("Thêm", (dialog, which) -> {
                            int selectedCount = countSelected(selected);
                            if (selectedCount == 0) {
                                Snackbar.make(findViewById(android.R.id.content),
                                        "Chưa chọn bài hát nào", Snackbar.LENGTH_SHORT).show();
                                return;
                            }

                            // Snackbar trước (optimistic), DB insert sau
                            Snackbar.make(findViewById(android.R.id.content),
                                    "Đã thêm " + selectedCount + " bài hát vào playlist",
                                    Snackbar.LENGTH_SHORT).show();

                            // Thêm các bài đã chọn vào playlist
                            executor.execute(() -> {
                                List<Long> songIdsToAdd = new ArrayList<>();
                                for (int i = 0; i < songCount; i++) {
                                    if (selected[i]) {
                                        songIdsToAdd.add(allSongs.get(i).getId());
                                    }
                                }
                                repository.addSongsToPlaylist(playlistId, songIdsToAdd);
                            });
                        })
                        .setNegativeButton("Huỷ", null)
                        .show();
            }
        });
    }

    /**
     * Đếm số lượng item đã chọn.
     *
     * Input:
     * @param selected Mảng boolean selected state.
     *
     * Output:
     * @return Số lượng true trong mảng.
     */
    private int countSelected(boolean[] selected) {
        int count = 0;
        for (boolean b : selected) {
            if (b) count++;
        }
        return count;
    }

    private void showRemoveSongDialog(@NonNull SongEntity song) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(song.getTitle())
                .setMessage(R.string.action_remove_from_playlist)
                .setPositiveButton(R.string.action_remove, (dialog, which) ->
                        repository.removeSongFromPlaylist(playlistId, song.getId()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
