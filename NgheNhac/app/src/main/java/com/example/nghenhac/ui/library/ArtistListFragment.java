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
import com.example.nghenhac.ui.adapter.ArtistAdapter;
import com.example.nghenhac.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment hiển thị danh sách nghệ sĩ (tab "Nghệ sĩ" trong Library).
 * <p>
 * Nguyên lý:
 * - Observe getAllSongs() từ SongRepository → nhóm các bài hát theo nghệ sĩ.
 * - Mỗi nghệ sĩ hiển thị tên + số lượng bài hát.
 * - Click nghệ sĩ → lọc bài hát từ danh sách đã load → phát tất cả.
 * <p>
 * Luồng xử lý:
 * 1. Fragment tạo → onViewCreated() → khởi tạo RecyclerView + ArtistAdapter.
 * 2. Observe SongRepository.getAllSongs() → lưu allSongs + groupSongsByArtist() → adapter.submitList().
 * 3. User click nghệ sĩ → filter allSongs → MusicPlayer.play(filteredList, 0) → mở PlayerActivity.
 * 4. Song count tự động cập nhật khi dữ liệu thay đổi.
 */
public class ArtistListFragment extends Fragment {

    private static final String UNKNOWN_ARTIST = "Unknown Artist";

    private ArtistAdapter adapter;
    private SongRepository repository;
    private View emptyState;
    /** Lưu toàn bộ danh sách bài hát để filter khi user click nghệ sĩ. */
    private List<SongEntity> allSongs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artist_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = SongRepository.getInstance(requireContext());

        emptyState = view.findViewById(R.id.empty_state);

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.artist_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        // Setup adapter
        adapter = new ArtistAdapter();
        adapter.setOnArtistClickListener((artist, position) -> {
            // Phát tất cả bài hát của nghệ sĩ này
            playArtistSongs(artist.getName());
        });

        recyclerView.setAdapter(adapter);

        // Observe data
        observeSongs();
    }

    private void observeSongs() {
        LiveData<List<SongEntity>> songsLiveData = repository.getAllSongs();
        if (songsLiveData != null) {
            songsLiveData.observe(getViewLifecycleOwner(), songs -> {
                if (songs != null) {
                    allSongs = songs;
                    List<ArtistAdapter.ArtistItem> artists = groupSongsByArtist(songs);
                    adapter.submitList(artists);
                    emptyState.setVisibility(artists.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    /**
     * Nhóm danh sách bài hát thành danh sách nghệ sĩ.
     * <p>
     * Nguyên lý:
     * - Duyệt qua tất cả bài hát, nhóm theo tên nghệ sĩ.
     * - Dùng LinkedHashMap để giữ thứ tự xuất hiện.
     *
     * @param songs Danh sách tất cả bài hát.
     * @return Danh sách ArtistItem đã nhóm.
     */
    @NonNull
    private static List<ArtistAdapter.ArtistItem> groupSongsByArtist(@NonNull List<SongEntity> songs) {
        Map<String, Integer> artistMap = new LinkedHashMap<>();

        for (SongEntity song : songs) {
            String artistName = song.getArtist() != null && !song.getArtist().isEmpty()
                    ? song.getArtist() : UNKNOWN_ARTIST;
            Integer count = artistMap.get(artistName);
            artistMap.put(artistName, (count != null ? count : 0) + 1);
        }

        List<ArtistAdapter.ArtistItem> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : artistMap.entrySet()) {
            result.add(new ArtistAdapter.ArtistItem(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * Phát tất cả bài hát của một nghệ sĩ.
     * <p>
     * Nguyên lý:
     * - Lọc từ allSongs đã load sẵn (tránh tạo LiveData observation mới).
     * - Phát từ vị trí 0, mở PlayerActivity.
     *
     * @param artistName Tên nghệ sĩ.
     */
    private void playArtistSongs(@NonNull String artistName) {
        if (allSongs == null || allSongs.isEmpty()) {
            return;
        }

        List<SongEntity> artistSongs = new ArrayList<>();
        for (SongEntity song : allSongs) {
            // Dùng cùng fallback UNKNOWN_ARTIST như groupSongsByArtist()
            String songArtist = song.getArtist() != null && !song.getArtist().isEmpty()
                    ? song.getArtist() : UNKNOWN_ARTIST;
            if (songArtist.equals(artistName)) {
                artistSongs.add(song);
            }
        }

        if (!artistSongs.isEmpty()) {
            MusicPlayer.getInstance(requireContext()).play(artistSongs, 0);
            startActivity(new Intent(requireContext(), PlayerActivity.class));
        }
    }
}
