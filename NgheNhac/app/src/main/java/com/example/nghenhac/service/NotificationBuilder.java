package com.example.nghenhac.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.example.nghenhac.MainActivity;
import com.example.nghenhac.R;

/**
 * 🔔 NotificationBuilder — Tạo notification cho phát nhạc nền.
 *
 * ============================================================
 *  GIẢI THÍCH CHI TIẾT — DÀNH CHO BÁO CÁO ĐỒ ÁN
 * ============================================================
 *
 * ─── 1. VAI TRÒ ───
 * Tạo notification để:
 *   - Hiển thị bài hát đang phát (tên bài, nghệ sĩ)
 *   - Cho phép điều khiển: Play/Pause, Next, Previous
 *   - Mở lại app khi click vào notification
 *   - Chạy ở chế độ Foreground (có notification = Android không kill)
 *
 * ─── 2. VÍ DỤ THỰC TẾ ───
 * Giống như REMOTE ĐIỀU KHIỂN hiển thị trên màn hình khoá:
 *
 *   ┌─────────────────────────────────────────────────────┐
 *   │  🎵 Hãy trao cho anh                              │
 *   │     Sơn Tùng M-TP                                  │
 *   │                                                     │
 *   │        ◀⏪      ▶⏸/▶▶      ⏩▶                      │
 *   │        (Prev)  (Play/Pause) (Next)                  │
 *   └─────────────────────────────────────────────────────┘
 *
 * ─── 3. NOTIFICATION CHANNEL ───
 * Từ Android 8 (API 26), mọi notification phải thuộc 1 CHANNEL.
 * Channel này có:
 *   - ID: "MUSIC_CHANNEL_ID"
 *   - Tên: "Music Playback"
 *   - IMPORTANCE_LOW: không phát âm thanh riêng, không popup
 *   - Không hiển thị badge (số lượng) trên icon app
 *
 * ─── 4. LUỒNG CHI TIẾT ───
 *
 *   ┌──────────────────────────────────────────────────────┐
 *   │            LUỒNG TẠO NOTIFICATION                    │
 *   │                                                      │
 *   │  MusicService.onCreate()                             │
 *   │       │                                              │
 *   │       ▼                                              │
 *   │  NotificationBuilder(context)                        │
 *   │       │  Tạo builder với CHANNEL_ID                  │
 *   │       ▼                                              │
 *   │  createNotificationChannel()                         │
 *   │       │  Tạo channel (chạy 1 lần duy nhất)           │
 *   │       ▼                                              │
 *   │  build(isPlaying, title, artist, albumArtUri)        │
 *   │       │                                              │
 *   │       ├── setContentTitle(displayTitle)              │
 *   │       ├── setContentText(displayText)                │
 *   │       ├── setSmallIcon(play/pause)                   │
 *   │       ├── setContentIntent(mở app)                   │
 *   │       ├── setOngoing(isPlaying)                      │
 *   │       ├── setVisibility(VISIBILITY_PUBLIC)           │
 *   │       ├── setStyle(MediaStyle)                       │
 *   │       └── .addAction x 3:                            │
 *   │             ├── Action 0: ◀⏪ Previous                │
 *   │             ├── Action 1: ▶⏸ Play/Pause              │
 *   │             └── Action 2: ⏩▶ Next                    │
 *   │                                                      │
 *   │       ▼                                              │
 *   │  startForeground(NOTIFICATION_ID, notification)       │
 *   │       │  Android không kill service khi có notif     │
 *   │       ▼                                              │
 *   │  User thấy notification trên thanh trạng thái        │
 *   └──────────────────────────────────────────────────────┘
 *
 * ─── 5. CÁCH XỬ LÝ KHI USER BẤM NÚT ───
 *
 *   User bấm "Next" → buildPendingIntent(ACTION_NEXT)
 *       │
 *       ▼
 *   PendingIntent chứa Intent đến MusicService
 *       │  Intent.setAction("com.example.nghenhac.action.NEXT")
 *       ▼
 *   MusicService.onStartCommand(intent)
 *       │  Đọc intent.getAction()
 *       │  = "NEXT" → gọi musicPlayer.next()
 *       ▼
 *   ExoPlayer chuyển bài → nhạc phát bài tiếp theo
 *       │
 *       ▼
 *   Media3 tự động cập nhật notification với bài mới
 *
 * ─── 6. LƯU Ý ───
 * - IMPORTANCE_LOW: không gây ồn, không popup khi đang dùng app
 * - VISIBILITY_PUBLIC: hiển thị nội dung trên màn hình khoá
 * - setOngoing(true): không thể vuốt xoá khi đang phát
 * - Media3 DefaultMediaNotificationProvider tự động quản lý notification
 *   khi dùng MediaSessionService. Class này là FALLBACK.
 * - PendingIntent.FLAG_IMMUTABLE: bắt buộc từ Android 12+
 */
public class NotificationBuilder {

    public static final String CHANNEL_ID = "MUSIC_CHANNEL_ID";
    public static final int NOTIFICATION_ID = 1;

    private static final String ACTION_PLAY = "com.example.nghenhac.action.PLAY";
    private static final String ACTION_PAUSE = "com.example.nghenhac.action.PAUSE";
    private static final String ACTION_NEXT = "com.example.nghenhac.action.NEXT";
    private static final String ACTION_PREV = "com.example.nghenhac.action.PREV";
    private static final String ACTION_STOP = "com.example.nghenhac.action.STOP";

    private final Context context;
    private final PendingIntent sessionIntent;
    private androidx.media3.session.MediaSession mediaSession;

    /**
     * ─── KHỞI TẠO NOTIFICATION BUILDER ───
     *
     * Khi tạo NotificationBuilder, chúng ta chuẩn bị:
     *   1. Context (Service context) — để tạo Notification
     *   2. sessionIntent — PendingIntent để MỞ LẠI APP khi click notification
     *
     * sessionIntent: Khi user click vào notification → mở MainActivity
     *   - FLAG_ACTIVITY_SINGLE_TOP: nếu MainActivity đã mở thì không tạo mới
     *   - FLAG_IMMUTABLE: từ Android 12, PendingIntent phải là IMMUTABLE
     *   - FLAG_UPDATE_CURRENT: nếu Intent đã tồn tại, cập nhật dữ liệu mới
     *
     * @param context Context (Service context — truyền this từ MusicService)
     */
    public NotificationBuilder(@NonNull Context context) {
        this.context = context;

        // ── PREPARE INTENT ĐỂ MỞ APP ──
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.sessionIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * ─── GÁN MEDIASESSION ───
     *
     * Cho notification biết MediaSession nào đang hoạt động.
     * Nhờ đó, notification có thể:
     *   - Hiển thị trên LOCK SCREEN (màn hình khoá)
     *   - Điều khiển qua BLUETOOTH
     *   - Kết nối với Android Auto
     *
     * @param session MediaSession từ MusicService (chứa ExoPlayer bên trong)
     */
    public void setMediaSession(@NonNull androidx.media3.session.MediaSession session) {
        this.mediaSession = session;
    }

    /**
     * ─── TẠO NOTIFICATION CHANNEL ───
     *
     * Từ Android 8 (API 26), mọi notification PHẢI thuộc 1 channel.
     * Channel này định nghĩa:
     *   - ID: "MUSIC_CHANNEL_ID"
     *   - Tên hiển thị: "Music Playback"
     *   - IMPORTANCE_LOW: không ồn, không popup, không đánh thức màn hình
     *   - Không badge: không hiện số trên icon app
     *
     * QUAN TRỌNG: Chỉ gọi 1 lần duy nhất trong onCreate() của Service.
     * Gọi nhiều lần không sao nhưng vô ích.
     */
    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls music playback");
            channel.setShowBadge(false);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * ─── XÂY DỰNG NOTIFICATION ───
     *
     * Đây là phương thức CHÍNH tạo notification với đầy đủ controls.
     *
     * CÁC THÀNH PHẦN CỦA NOTIFICATION:
     *
     *   1. setContentTitle(displayTitle): TÊN BÀI HÁT
     *      - Nếu null → hiển thị "NgheNhac" (mặc định)
     *   2. setContentText(displayText): TÊN NGHỆ SĨ
     *      - Nếu null → "Now Playing" hoặc "Paused"
     *   3. setSmallIcon(): ICON NHỎ trên thanh trạng thái
     *   4. setContentIntent(sessionIntent): CLICK → mở app
     *   5. setOngoing(isPlaying): KHÔNG thể vuốt xoá khi đang phát
     *   6. setVisibility(VISIBILITY_PUBLIC): HIỂN THỊ trên màn hình khoá
     *   7. setStyle(MediaStyle): Kiểu notification NHẠC (3 nút compact)
     *   8. addAction x 3: Previous | Play/Pause | Next
     *
     * ─── CÁC NÚT ĐIỀU KHIỂN ───
     *
     *   Action 0 (trái):  ◀⏪ Previous
     *     → buildPendingIntent(ACTION_PREV)
     *     → MusicService xử lý → musicPlayer.previous()
     *
     *   Action 1 (giữa): ▶⏸ Play/Pause (đổi icon theo trạng thái)
     *     → Đang phát: icon Pause + "Pause"
     *     → Đang dừng: icon Play + "Play"
     *     → buildPendingIntent(ACTION_PAUSE hoặc ACTION_PLAY)
     *
     *   Action 2 (phải): ⏩▶ Next
     *     → buildPendingIntent(ACTION_NEXT)
     *     → MusicService xử lý → musicPlayer.next()
     *
     * ─── CÁC ACTION INTENT ───
     *   ACTION_PLAY  = "com.example.nghenhac.action.PLAY"
     *   ACTION_PAUSE = "com.example.nghenhac.action.PAUSE"
     *   ACTION_NEXT  = "com.example.nghenhac.action.NEXT"
     *   ACTION_PREV  = "com.example.nghenhac.action.PREV"
     *
     * @param isPlaying   true = đang phát (hiện icon Pause)
     *                    false = đang dừng (hiện icon Play)
     * @param title       Tên bài hát hiện tại (có thể null)
     * @param artist      Tên nghệ sĩ (có thể null)
     * @param albumArtUri URI ảnh bìa (Media3 tự xử lý)
     * @return Notification object sẵn sàng cho startForeground()
     */
    @NonNull
    public Notification build(boolean isPlaying,
                              @Nullable String title,
                              @Nullable String artist,
                              @Nullable String albumArtUri) {
        String displayTitle = (title != null && !title.isEmpty()) ? title : "NgheNhac";
        String displayText = (artist != null && !artist.isEmpty())
                ? artist
                : (isPlaying ? "Now Playing" : "Paused");

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(displayTitle)
                .setContentText(displayText)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(sessionIntent)
                .setOngoing(isPlaying)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new MediaStyle()
                        // Media3 DefaultMediaNotificationProvider tự động quản lý notification
                        // và kết nối với MediaSession. MediaStyle chỉ dùng làm fallback.
                        .setShowActionsInCompactView(0, 1, 2)
                )
                // Action 0: previous
                .addAction(android.R.drawable.ic_media_previous, "Previous",
                        buildPendingIntent(ACTION_PREV))
                // Action 1: play/pause
                .addAction(isPlaying ? android.R.drawable.ic_media_pause
                                     : android.R.drawable.ic_media_play,
                        isPlaying ? "Pause" : "Play",
                        buildPendingIntent(isPlaying ? ACTION_PAUSE : ACTION_PLAY))
                // Action 2: next
                .addAction(android.R.drawable.ic_media_next, "Next",
                        buildPendingIntent(ACTION_NEXT))
                .build();
    }

    /**
     * Tạo notification mặc định (khi không có bài hát nào đang phát).
     * Dùng để hiển thị notification tạm thời trước khi có bài hát.
     *
     * @return Notification mặc định
     */
    @NonNull
    public Notification buildDefault() {
        return build(false, "NgheNhac", "No song playing", null);
    }

    /**
     * ─── TẠO PENDINGINTENT CHO NÚT BẤM ───
     *
     * PendingIntent là "ý định chờ" — nó được thực thi khi user bấm nút.
     *
     * CÁCH HOẠT ĐỘNG:
     *   1. Tạo Intent đến MusicService với action cụ thể
     *      VD: Intent action = "NEXT" → MusicService nhận biết
     *   2. Bọc Intent trong PendingIntent.getService()
     *      → Android sẽ gọi MusicService.onStartCommand() khi user bấm
     *   3. action.hashCode() làm requestCode → phân biệt các nút
     *   4. FLAG_IMMUTABLE: từ Android 12+, không cho sửa Intent
     *   5. FLAG_UPDATE_CURRENT: nếu PendingIntent đã tồn tại, cập nhật
     *
     * @param action Action string (VD: ACTION_PLAY = "...action.PLAY")
     * @return PendingIntent để NotificationCompat.Builder dùng
     */
    @NonNull
    private PendingIntent buildPendingIntent(@NonNull String action) {
        // Tạo Intent đến MusicService
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(action);  // Gán action: PLAY, PAUSE, NEXT, PREV

        // Bọc trong PendingIntent
        return PendingIntent.getService(
                context,
                action.hashCode(),  // requestCode: phân biệt các nút
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }
}
