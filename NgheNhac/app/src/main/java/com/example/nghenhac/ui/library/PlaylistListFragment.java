package com.example.nghenhac.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.repository.PlaylistRepository;
import com.example.nghenhac.data.repository.SongRepository;
import com.example.nghenhac.ui.adapter.PlaylistAdapter;
import com.example.nghenhac.util.FilePickerUtil;
import com.example.nghenhac.util.PlaylistImporter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.List;

/**
 * Fragment hiển thị danh sách playlist (tab "Playlist" trong Library).
 *
 * Nguyên lý:
 * - Kết nối với PlaylistRepository (singleton) để observe LiveData danh sách playlist.
 * - Sử dụng PlaylistAdapter (ListAdapter với PlaylistDiffUtil) cho RecyclerView.
 * - Click playlist → mở PlaylistDetailActivity.
 * - FAB ở góc dưới → mở CreatePlaylistDialog để tạo playlist mới.
 *
 * Luồng xử lý:
 * 1. Fragment tạo → onViewCreated() → khởi tạo RecyclerView + PlaylistAdapter + FAB.
 * 2. Observe PlaylistRepository.getAllPlaylists() → adapter.submitList().
 * 3. User click playlist → Intent mở PlaylistDetailActivity.
 * 4. User click FAB → CreatePlaylistDialog.show() → tạo playlist qua Repository.
 */
public class PlaylistListFragment extends Fragment {

    private PlaylistAdapter adapter;
    private PlaylistRepository repository;
    private ExecutorService executor;
    private ActivityResultLauncher<String[]> importLauncher;
    private RecyclerView recyclerView;
    private SongRepository songRepository;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = PlaylistRepository.getInstance(requireContext());
        songRepository = SongRepository.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();

        // Setup import launcher
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::doImport);

        emptyState = view.findViewById(R.id.empty_state);

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.playlist_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        adapter = new PlaylistAdapter();
        adapter.setOnPlaylistClickListener(new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(@NonNull PlaylistEntity playlist, int position) {
                startActivity(PlaylistDetailActivity.createIntent(requireContext(), playlist.getId()));
            }

            @Override
            public void onPlaylistMoreClick(@NonNull PlaylistEntity playlist, @NonNull View anchor) {
                showPlaylistMoreMenu(playlist, anchor);
            }
        });

        recyclerView.setAdapter(adapter);

        // Setup FAB — click tạo playlist, long-click import
        FloatingActionButton fab = view.findViewById(R.id.playlist_create_fab);
        fab.setOnClickListener(v ->
                CreatePlaylistDialog.show(requireContext(), recyclerView)
        );
        fab.setOnLongClickListener(v -> {
            importLauncher.launch(new String[]{
                    FilePickerUtil.MIME_TYPE_M3U,
                    FilePickerUtil.MIME_TYPE_XML
            });
            return true;
        });

        // Observe playlists
        observePlaylists();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Hiển thị popup menu cho một playlist (edit tên, xoá).
     */
    private void showPlaylistMoreMenu(@NonNull PlaylistEntity playlist, @NonNull View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, R.string.action_rename);
        popup.getMenu().add(0, 2, 0, R.string.action_delete);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    showRenameDialog(playlist);
                    return true;
                case 2:
                    repository.deletePlaylistById(playlist.getId());
                    Snackbar.make(recyclerView, R.string.playlist_deleted, Snackbar.LENGTH_SHORT).show();
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    /**
     * Hiển thị dialog đổi tên playlist.
     *
     * Nguyên lý:
     * - AlertDialog với EditText pre-filled tên hiện tại.
     * - Xác nhận → repository.updatePlaylist() với tên mới.
     * - Snackbar thông báo thành công.
     *
     * Input:
     * @param playlist PlaylistEntity cần đổi tên.
     */
    private void showRenameDialog(@NonNull PlaylistEntity playlist) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Đổi tên playlist");

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setText(playlist.getName());
        input.setSelection(input.getText().length());
        input.setSingleLine();
        // Wrap EditText trong FrameLayout với padding
        android.widget.FrameLayout frame = new android.widget.FrameLayout(requireContext());
        frame.setPadding(48, 16, 48, 16);
        frame.addView(input);
        builder.setView(frame);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(playlist.getName())) {
                playlist.setName(newName);
                repository.updatePlaylist(playlist);
                Snackbar.make(recyclerView, "Đã đổi tên thành \"" + newName + "\"", Snackbar.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Huỷ", null);
        builder.show();
    }

    /**
     * Import playlist từ file.
     */
    private void doImport(@Nullable android.net.Uri uri) {
        if (uri == null) return;

        executor.execute(() -> {
            try {
                PlaylistImporter.ImportResult result = PlaylistImporter.readFile(requireContext(), uri);

                requireActivity().runOnUiThread(() -> {
                    // Tạo playlist mới với tên từ file
                    repository.createPlaylist(result.playlistName, "Imported: " + result.totalCount + " songs",
                            new PlaylistRepository.OnPlaylistCreated() {
                                @Override
                                public void onCreated(long playlistId) {
                                    // Thêm các bài hát match được
                                    if (!result.matchedSongIds.isEmpty()) {
                                        repository.addSongsToPlaylist(playlistId, result.matchedSongIds);
                                    }
                                    String msg = getString(R.string.import_match_format,
                                            result.matchedCount, result.totalCount);
                                    Snackbar.make(recyclerView, msg, Snackbar.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Snackbar.make(recyclerView, R.string.import_error, Snackbar.LENGTH_LONG).show();
                                }
                            });
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Snackbar.make(recyclerView, R.string.import_error, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void observePlaylists() {
        LiveData<List<PlaylistEntity>> playlistsLiveData = repository.getAllPlaylists();
        if (playlistsLiveData != null) {
            playlistsLiveData.observe(getViewLifecycleOwner(), playlists -> {
                if (playlists != null) {
                    adapter.submitList(playlists);
                    emptyState.setVisibility(playlists.isEmpty() ? View.VISIBLE : View.GONE);
                    // Load ảnh bìa cho từng playlist
                    loadPlaylistArts(playlists);
                }
            });
        }
    }

    /**
     * Load ảnh bìa cho tất cả playlist trong danh sách.
     *
     * Nguyên lý:
     * - Với mỗi playlist, gọi repository.getFirstSongAlbumArtUri() trên background thread.
     * - Khi có kết quả, cập nhật adapter qua setPlaylistArt().
     * - RecyclerView tự động rebind ViewHolder với ảnh mới.
     *
     * Input:
     * @param playlists Danh sách playlist cần load ảnh.
     */
    private void loadPlaylistArts(@NonNull List<PlaylistEntity> playlists) {
        executor.execute(() -> {
            for (PlaylistEntity playlist : playlists) {
                String albumArtUri = repository.getFirstSongAlbumArtUri(playlist.getId());
                if (albumArtUri != null && !albumArtUri.isEmpty()) {
                    String finalUri = albumArtUri;
                    requireActivity().runOnUiThread(() ->
                            adapter.setPlaylistArt(playlist.getId(), finalUri));
                }
            }
        });
    }
}
