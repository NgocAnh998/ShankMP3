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
 * 📋 QueueDialog — Xem danh sách bài hát sắp phát.
 *
 * ============================================================
 *  GIẢI THÍCH CHI TIẾT — DÀNH CHO BÁO CÁO ĐỒ ÁN
 * ============================================================
 *
 * ─── 1. VAI TRÒ ───
 * Hiển thị danh sách các bài hát ĐANG CHỜ PHÁT (queue).
 * Cho phép user:
 *   - XEM toàn bộ queue (bài nào sắp phát?)
 *   - CHUYỂN đến bài hát bất kỳ trong queue
 *   - BIẾT bài nào đang phát (đánh dấu ▶)
 *
 * ─── 2. QUEUE KHÁC GÌ PLAYLIST? ───
 *
 *   |            | QUEUE                      | PLAYLIST              |
 *   |────────────┼────────────────────────────┼───────────────────────|
 *   | Tính chất  | TẠM THỜI                  | CỐ ĐỊNH              |
 *   | Lưu trữ    | Trong RAM (PlaybackQueue)  | Database (Room)      |
 *   | Thay đổi   | Khi chọn bài mới → queue   | Chỉ thay đổi khi user |
 *   |            | mới được tạo               | sửa playlist         |
 *   | Mục đích   | "Sắp phát gì tiếp theo?"  | "Bộ sưu tập nhạc"  |
 *
 * ─── 3. VÍ DỤ THỰC TẾ ───
 *   Queue = danh sách chờ ở quán coffee:
 *     - Bạn order 1 ly → vào queue (đang phát)
 *     - Có người order sau → xếp sau bạn (chờ)
 *     - Bạn có thể đổi chỗ với người khác (chọn bài khác)
 *
 *   Playlist = menu đồ uống yêu thích:
 *     - Bạn lưu sẵn danh sách (không thay đổi trừ khi bạn sửa)
 *
 * ─── 4. LUỒNG CHI TIẾT ───
 *
 *   Bước 1: User bấm "Queue" trong PlayerActivity
 *           → QueueDialog.show(context)
 *
 *   Bước 2: Lấy MusicPlayer instance
 *           → musicPlayer.getPlaybackQueue().getMediaItems()
 *           → List<MediaItem> (danh sách các bài hát)
 *
 *   Bước 3: Lấy currentIndex
 *           → musicPlayer.getPlaybackQueue().getCurrentIndex()
 *           → Biết bài nào đang phát (index)
 *
 *   Bước 4: Kiểm tra queue RỖNG?
 *           → Rỗng: "Danh sách phát trống" + OK button
 *           → Có bài: Tạo mảng tên bài hát
 *
 *   Bước 5: Xây dựng mảng String[] songNames:
 *           → "▶ Hãy trao cho anh - Sơn Tùng M-TP" (đang phát)
 *           → "  Nơi này có anh - Sơn Tùng M-TP" (chờ)
 *           → "  Lạc trôi - Sơn Tùng M-TP" (chờ)
 *
 *   Bước 6: Hiển thị AlertDialog:
 *           - Title: "Danh sách phát (3 bài)"
 *           - Items: mảng songNames
 *           - Click → musicPlayer.playbackQueue.goTo(which)
 *           -     → musicPlayer.seekTo(0)
 *           -     → musicPlayer.play()
 *           - OK button → đóng dialog
 *
 *   Bước 7: Gửi broadcast → PlayerActivity cập nhật UI
 *           → Intent("com.example.nghenhac.QUEUE_ITEM_SELECTED")
 *
 * ─── 5. GIAO DIỆN ───
 *
 *   ┌─────────────────────────────────────┐
 *   │  Danh sách phát (5 bài)             │
 *   │                                     │
 *   │  ▶ Hãy trao cho anh - Sơn Tùng     │ ← Đang phát
 *   │    Nơi này có anh - Sơn Tùng       │
 *   │    Lạc trôi - Sơn Tùng             │
 *   │    Chạy ngay đi - Sơn Tùng         │
 *   │    Có chàng trai viết lên cây      │
 *   │                                     │
 *   │            [Đóng]                   │
 *   └─────────────────────────────────────┘
 */
public class QueueDialog {

    /**
     * ─── HIỂN THỊ DANH SÁCH PHÁT ───
     *
     * LUỒNG CHI TIẾT:
     *
     *   1. Lấy MusicPlayer.getInstance(context)
     *      → Singleton: đảm bảo cùng 1 player với app
     *
     *   2. musicPlayer.getPlaybackQueue().getMediaItems()
     *      → Lấy danh sách MediaItem từ PlaybackQueue
     *      → MediaItem chứa: mediaId (ID), URI (file path),
     *        MediaMetadata (title, artist, albumArt)
     *
     *   3. musicPlayer.getPlaybackQueue().getCurrentIndex()
     *      → Index của bài đang phát trong queue
     *      → -1 nếu queue rỗng
     *
     *   4. Nếu queue rỗng → AlertDialog thông báo + OK
     *
     *   5. Nếu có bài hát:
     *      → Duyệt từng MediaItem
     *      → Lấy title + artist từ MediaMetadata
     *      → Nếu null → fallback "Không rõ"
     *      → Thêm prefix: "▶ " (đang phát) hoặc "  " (chờ)
     *      → Tạo mảng String[] songNames
     *
     *   6. AlertDialog.Builder.setItems(songNames, clickListener)
     *      → User click bài → goTo(index) → seekTo(0) → play()
     *
     *   7. Gửi broadcast QUEUE_ITEM_SELECTED
     *      → PlayerActivity lắng nghe → cập nhật UI
     *
     * @param context Context để tạo AlertDialog
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
