package com.example.nghenhac.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.repository.SongRepository;
import com.example.nghenhac.ui.adapter.AlbumAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment hiển thị danh sách album dạng Grid (tab "Album" trong Library).
 *
 * Nguyên lý:
 * - Observe getAllSongs() từ SongRepository → nhóm các bài hát theo album.
 * - Chuyển đổi thành List<AlbumItem> để hiển thị qua AlbumAdapter.
 * - GridLayoutManager với spanCount = 2 (hiển thị 2 cột).
 * - Số lượng bài hát và ảnh bìa được lấy từ bài hát đầu tiên trong album.
 *
 * Luồng xử lý:
 * 1. Fragment tạo → onViewCreated() → khởi tạo RecyclerView + AlbumAdapter.
 * 2. Observe SongRepository.getAllSongs() → groupSongsByAlbum() → adapter.submitList().
 * 3. User click album → TODO: mở AlbumDetailActivity.
 */
public class AlbumGridFragment extends Fragment {

    private AlbumAdapter adapter;
    private SongRepository repository;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_album_grid, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = SongRepository.getInstance(requireContext());

        emptyState = view.findViewById(R.id.empty_state);

        RecyclerView recyclerView = view.findViewById(R.id.album_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        adapter = new AlbumAdapter();
        adapter.setOnAlbumClickListener((album, position) -> {
            startActivity(AlbumDetailActivity.createIntent(
                    requireContext(),
                    album.getAlbumName(),
                    album.getArtist()
            ));
        });

        recyclerView.setAdapter(adapter);

        // Observe songs và nhóm thành albums
        LiveData<List<SongEntity>> songsLiveData = repository.getAllSongs();
        if (songsLiveData != null) {
            songsLiveData.observe(getViewLifecycleOwner(), songs -> {
                if (songs != null) {
                    List<AlbumAdapter.AlbumItem> albums = groupSongsByAlbum(songs);
                    adapter.submitList(albums);
                    emptyState.setVisibility(albums.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    /**
     * Nhóm danh sách bài hát thành danh sách album.
     *
     * Nguyên lý:
     * - Duyệt qua tất cả bài hát, nhóm theo tên album.
     * - Mỗi album lấy thông tin: tên album, nghệ sĩ (từ bài đầu tiên),
     *   ảnh bìa (từ bài đầu tiên), số lượng bài hát.
     * - Dùng LinkedHashMap để giữ thứ tự xuất hiện.
     *
     * Input:
     * @param songs Danh sách tất cả bài hát.
     *
     * Output:
     * @return Danh sách AlbumItem đã nhóm.
     */
    @NonNull
    private static List<AlbumAdapter.AlbumItem> groupSongsByAlbum(@NonNull List<SongEntity> songs) {
        // LinkedHashMap giữ thứ tự insert
        Map<String, AlbumAccumulator> albumMap = new LinkedHashMap<>();

        for (SongEntity song : songs) {
            String albumName = song.getAlbum() != null && !song.getAlbum().isEmpty()
                    ? song.getAlbum() : "Unknown Album";

            AlbumAccumulator acc = albumMap.get(albumName);
            if (acc == null) {
                acc = new AlbumAccumulator(
                        song.getArtist() != null ? song.getArtist() : "",
                        song.getAlbumArtUri()
                );
                albumMap.put(albumName, acc);
            }
            acc.songCount++;
        }

        List<AlbumAdapter.AlbumItem> result = new ArrayList<>();
        for (Map.Entry<String, AlbumAccumulator> entry : albumMap.entrySet()) {
            result.add(new AlbumAdapter.AlbumItem(
                    entry.getKey(),
                    entry.getValue().artist,
                    entry.getValue().albumArtUri,
                    entry.getValue().songCount
            ));
        }
        return result;
    }

    /** Accumulator tạm để đếm số bài hát khi nhóm album. */
    private static class AlbumAccumulator {
        @NonNull
        final String artist;
        @Nullable
        final String albumArtUri;
        int songCount;

        AlbumAccumulator(@NonNull String artist, @Nullable String albumArtUri) {
            this.artist = artist;
            this.albumArtUri = albumArtUri;
            this.songCount = 0;
        }
    }
}
