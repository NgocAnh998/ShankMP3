package com.example.nghenhac.ui.library;

import android.content.Intent;
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

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.repository.SongRepository;
import com.example.nghenhac.player.MusicPlayer;
import com.example.nghenhac.ui.adapter.SongAdapter;
import com.example.nghenhac.ui.player.PlayerActivity;

import java.util.List;

/**
 * Fragment hiển thị danh sách tất cả bài hát (tab "Bài hát" trong Library).
 *
 * Nguyên lý:
 * - Kết nối với SongRepository (singleton) để observe LiveData danh sách bài hát.
 * - Sử dụng SongAdapter (ListAdapter với SongDiffUtil) cho RecyclerView.
 * - Click bài hát → mở PlayerActivity và phát nhạc từ bài được chọn.
 * - Hỗ trợ: favorite toggle, now playing indicator, loading indicator.
 * - Tự động cập nhật UI khi dữ liệu trong Room thay đổi.
 *
 * Luồng xử lý:
 * 1. Fragment tạo → onViewCreated() → khởi tạo RecyclerView + SongAdapter.
 * 2. Observe SongRepository.getAllSongs() → khi dữ liệu thay đổi → adapter.submitList().
 * 3. User click bài hát → onItemClick → MusicPlayer.play(songs, startIndex) → PlayerActivity.
 * 4. User click favorite → gọi SongRepository.updateFavorite().
 * 5. User click "more" → mở SongBottomSheetDialog.
 *
 * Lưu ý:
 * - LiveData tự động quản lý lifecycle — không cần huỷ subscription.
 * - submitList() dùng DiffUtil — chỉ cập nhật item thay đổi, không refresh toàn bộ.
 */
public class SongListFragment extends Fragment {

    private SongAdapter adapter;
    private SongRepository repository;
    private RecyclerView recyclerView;
    private View emptyState;
    private View loadingIndicator;
    private MusicPlayer musicPlayer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo Repository
        repository = SongRepository.getInstance(requireContext());
        musicPlayer = MusicPlayer.getInstance(requireContext());

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.song_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        emptyState = view.findViewById(R.id.empty_state);
        loadingIndicator = view.findViewById(R.id.loading_indicator);

        // Tạo adapter + click listener
        adapter = new SongAdapter();
        adapter.setOnSongClickListener(new SongAdapter.OnSongClickListener() {
            @Override
            public void onItemClick(@NonNull SongEntity song, int position, @NonNull View itemView) {
                playSong(song, position, itemView);
            }

            @Override
            public void onMoreClick(@NonNull SongEntity song, @NonNull View anchor) {
                SongBottomSheetDialog.show(requireContext(), song);
            }

            @Override
            public void onFavoriteClick(@NonNull SongEntity song, boolean isFavorite, int position) {
                repository.updateFavorite(song.getId(), isFavorite);
            }
        });

        recyclerView.setAdapter(adapter);

        // Observe dữ liệu từ Repository
        observeSongs();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật now playing indicator khi quay lại fragment
        if (musicPlayer != null && adapter != null) {
            long currentSongId = -1;
            if (musicPlayer.getCurrentMediaItem() != null) {
                try {
                    currentSongId = Long.parseLong(
                            musicPlayer.getCurrentMediaItem().mediaId);
                } catch (NumberFormatException ignored) {}
            }
            adapter.setNowPlayingId(currentSongId);
        }
    }

    /**
     * Observe LiveData từ SongRepository — tự động cập nhật adapter khi dữ liệu thay đổi.
     *
     * Nguyên lý:
     * - getAllSongs() trả về LiveData từ Room DAO.
     * - Room tự động notify LiveData khi có insert/update/delete.
     * - Fragment tự động unsubscribe khi view bị destroy (LifecycleOwner).
     * - Loading indicator hiển thị cho đến khi dữ liệu được load lần đầu.
     */
    private void observeSongs() {
        LiveData<List<SongEntity>> songsLiveData = repository.getAllSongs();

        if (songsLiveData != null) {
            songsLiveData.observe(getViewLifecycleOwner(), songs -> {
                if (songs != null) {
                    // Ẩn loading indicator, hiện RecyclerView khi có dữ liệu
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    adapter.submitList(songs);
                    emptyState.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);

                    // Cập nhật now playing indicator
                    if (musicPlayer != null) {
                        long currentSongId = -1;
                        if (musicPlayer.getCurrentMediaItem() != null) {
                            try {
                                currentSongId = Long.parseLong(
                                        musicPlayer.getCurrentMediaItem().mediaId);
                            } catch (NumberFormatException ignored) {}
                        }
                        adapter.setNowPlayingId(currentSongId);
                    }
                }
            });
        }
    }

    /**
     * Phát nhạc khi user click bài hát.
     *
     * Nguyên lý:
     * - Lấy toàn bộ danh sách bài hát hiện tại từ adapter.
     * - Gọi MusicPlayer.play(songs, startIndex) để phát từ bài được chọn.
     * - Cập nhật now playing indicator.
     * - Mở PlayerActivity với shared element transition (album art bay từ danh sách lên player).
     *
     * Input:
     * @param song     Bài hát được click.
     * @param position Vị trí của bài hát trong danh sách.
     * @param itemView View của item (chứa album art) — dùng cho shared element.
     */
    private void playSong(@NonNull SongEntity song, int position, @NonNull View itemView) {
        // Lấy danh sách bài hát hiện tại từ adapter
        List<SongEntity> currentList = adapter.getCurrentList();

        if (currentList.isEmpty()) {
            // Fallback: chỉ phát một bài
            MusicPlayer.getInstance(requireContext()).play(song);
        } else {
            // Phát từ bài được chọn trong toàn bộ danh sách
            MusicPlayer.getInstance(requireContext()).play(currentList, position);
        }

        // Cập nhật now playing indicator
        adapter.setNowPlayingId(song.getId());

        // Mở PlayerActivity với shared element transition
        Intent intent = new Intent(requireContext(), PlayerActivity.class);
        View albumArt = itemView.findViewById(R.id.song_art);
        if (albumArt != null && albumArt.getTransitionName() != null) {
            android.app.ActivityOptions options = android.app.ActivityOptions
                    .makeSceneTransitionAnimation(requireActivity(), albumArt, albumArt.getTransitionName());
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }
}
