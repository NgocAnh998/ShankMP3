package com.example.nghenhac.ui.player;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.MediaItem;

import com.example.nghenhac.R;
import com.example.nghenhac.player.MusicPlayer;

import java.util.List;

/**
 * Dialog hiển thị danh sách phát hiện tại (queue).
 *
 * Nguyên lý:
 * - Lấy danh sách MediaItem từ MusicPlayer.PlaybackQueue.
 * - Hiển thị dưới dạng AlertDialog với danh sách các bài hát.
 * - Highlight bài hát đang phát.
 * - Click bài hát → chuyển đến bài đó (seekTo).
 *
 * Luồng xử lý:
 * 1. User click "Queue" button trong PlayerActivity → show().
 * 2. Lấy danh sách queue từ MusicPlayer.getPlaybackQueue().
 * 3. Tạo mảng tên bài hát từ MediaItem metadata.
 * 4. Hiển thị AlertDialog với danh sách → click → MusicPlayer.playbackQueue.goTo().
 * 5. Đóng dialog, cập nhật PlayerActivity.
 */
public class QueueDialog {

    /**
     * Hiển thị dialog danh sách phát.
     *
     * Nguyên lý:
     * - Lấy queue từ MusicPlayer.
     * - Xây dựng mảng tên bài hát.
     * - Highlight bài đang phát.
     * - Click item → chuyển đến bài đó.
     *
     * Input:
     * @param context Context để tạo dialog.
     */
    public static void show(Context context) {
        MusicPlayer musicPlayer = MusicPlayer.getInstance(context);
        List<MediaItem> items = musicPlayer.getPlaybackQueue().getMediaItems();
        int currentIndex = musicPlayer.getPlaybackQueue().getCurrentIndex();

        if (items == null || items.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Danh sách phát")
                    .setMessage("Danh sách phát trống")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] songNames = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            MediaItem item = items.get(i);
            String title = item.mediaMetadata != null && item.mediaMetadata.title != null
                    ? item.mediaMetadata.title.toString() : "Không rõ";
            String artist = item.mediaMetadata != null && item.mediaMetadata.artist != null
                    ? item.mediaMetadata.artist.toString() : "";
            String prefix = (i == currentIndex) ? "\u25B6 " : "  ";
            songNames[i] = prefix + title + (artist.isEmpty() ? "" : " - " + artist);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Danh sách phát (" + items.size() + " bài)")
                .setItems(songNames, (dialogInterface, which) -> {
                    // Chuyển đến bài hát được chọn
                    if (which >= 0 && which < items.size()) {
                        musicPlayer.getPlaybackQueue().goTo(which);
                        musicPlayer.seekTo(0);
                        if (!musicPlayer.isPlaying()) {
                            musicPlayer.play();
                        }
                        // Gửi broadcast để PlayerActivity cập nhật UI
                        Intent intent = new Intent("com.example.nghenhac.QUEUE_ITEM_SELECTED");
                        context.sendBroadcast(intent);
                    }
                })
                .setPositiveButton("Đóng", null)
                .create();

        dialog.show();
    }
}
