package com.example.nghenhac.ui.library;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;

import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.repository.PlaylistRepository;
import com.example.nghenhac.data.repository.SongRepository;

import java.util.List;

/**
 * Utility class cho thao tác với bài hát: thêm vào playlist, yêu thích.
 *
 * Nguyên lý:
 * - Tái sử dụng cho SongListFragment, AlbumDetailActivity, SearchFragment.
 * - Hiển thị các tuỳ chọn: Thêm vào playlist, Yêu thích/Bỏ thích.
 * - Thêm vào playlist: hiển thị danh sách playlist, click để thêm.
 *
 * Input:
 * - context: Context để tạo dialog và truy cập Repository.
 * - song: SongEntity cần thao tác.
 */
public class SongBottomSheetDialog {

    /**
     * Hiển thị dialog cho bài hát.
     *
     * Nguyên lý:
     * - Dialog có 2 mục: Thêm vào playlist, Yêu thích/Bỏ thích.
     * - Thêm vào playlist: mở AlertDialog danh sách playlist → chọn → thêm.
     * - Yêu thích: toggle trạng thái isFavorite.
     *
     * Input:
     * @param context Context để tạo dialog.
     * @param song    Bài hát cần thao tác.
     */
    public static void show(@NonNull Context context, @NonNull SongEntity song) {
        final boolean[] isFavorite = {song.isFavorite()};
        String favoriteText = isFavorite[0] ? "Bỏ yêu thích" : "Yêu thích";

        new AlertDialog.Builder(context)
                .setTitle(song.getTitle())
                .setItems(new String[]{
                        "Thêm vào playlist",
                        favoriteText,
                        "Huỷ"
                }, (dialog, which) -> {
                    if (which == 0) {
                        showPlaylistPicker(context, song);
                    } else if (which == 1) {
                        boolean newState = !isFavorite[0];
                        SongRepository.getInstance(context)
                                .updateFavorite(song.getId(), newState);
                        String msg = newState ? "Đã thêm vào yêu thích" : "Đã bỏ yêu thích";
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /**
     * Hiển thị dialog danh sách playlist để thêm bài hát.
     *
     * Nguyên lý:
     * - Lấy danh sách playlist từ PlaylistRepository.
     * - Hiển thị AlertDialog với danh sách tên playlist.
     * - Click → gọi PlaylistRepository.addSongToPlaylist().
     *
     * Input:
     * @param context Context.
     * @param song    Bài hát cần thêm.
     */
    private static void showPlaylistPicker(@NonNull Context context, @NonNull SongEntity song) {
        PlaylistRepository repository = PlaylistRepository.getInstance(context);
        LiveData<List<PlaylistEntity>> playlistsLiveData = repository.getAllPlaylists();
        List<PlaylistEntity> playlists = playlistsLiveData.getValue();

        if (playlists == null || playlists.isEmpty()) {
            Toast.makeText(context,
                    "Chưa có playlist nào. Tạo playlist trước.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playlistNames = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            playlistNames[i] = playlists.get(i).getName();
        }

        new AlertDialog.Builder(context)
                .setTitle("Chọn playlist")
                .setItems(playlistNames, (dialog, which) -> {
                    PlaylistEntity selected = playlists.get(which);
                    repository.addSongToPlaylist(selected.getId(), song.getId());
                    Toast.makeText(context,
                            "Đã thêm \"" + song.getTitle() + "\" vào \"" + selected.getName() + "\"",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
