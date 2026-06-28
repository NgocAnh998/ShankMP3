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
 * Xây dựng MediaStyle notification cho foreground music playback.
 *
 * Nguyên lý:
 * - Media3 DefaultMediaNotificationProvider tự động quản lý notification khi dùng MediaSessionService,
 *   bao gồm cả album art từ MediaMetadata. Custom NotificationBuilder này chỉ là fallback.
 * - Notification chứa: title, artist, play/pause, next, prev controls + pending intent mở app.
 * - Media3 tự động load album art vào notification thông qua MediaMetadata.
 * - Tương thích với Android 9-11 (API 28-30).
 *
 * Luồng xử lý:
 * 1. MusicService.onCreate() → createNotificationChannel() (một lần).
 * 2. build() → tạo notification với controls.
 * 3. Media3 tự động cập nhật notification metadata (title, artist, album art) khi bài hát thay đổi.
 * 4. Khi pause → đổi icon play thành pause.
 *
 * Input:
 * - Context (Service).
 * - isPlaying: true/false để chọn icon play/pause.
 * - title/artist: thông tin bài hát hiện tại.
 *
 * Output:
 * - Notification sẵn sàng cho startForeground().
 *
 * Lưu ý:
 * - Channel ID: "MUSIC_CHANNEL_ID" — đã tạo trong onCreate().
 * - IMPORTANCE_LOW: không phát âm thanh riêng, không hiển thị popup.
 * - Album art được Media3 xử lý tự động — không cần Glide ở đây.
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

    /**
     * @param context Context (Service context).
     */
    public NotificationBuilder(@NonNull Context context) {
        this.context = context;

        // PendingIntent mở MainActivity khi click notification
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.sessionIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * Tạo notification channel cho Android 8+ (API 26+).
     * Gọi một lần duy nhất trong Service.onCreate().
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
     * Xây dựng notification với controls play/pause, next, previous.
     *
     * Nguyên lý:
     * - Dùng NotificationCompat.Builder để tương thích ngược.
     * - Thêm action buttons: play/pause (tuỳ trạng thái), next, previous.
     * - Album art được Media3 tự động cập nhật qua MediaMetadata — không cần xử lý ở đây.
     * - setOngoing(true): user không thể vuốt xoá khi đang phát.
     *
     * Input:
     * @param isPlaying      true nếu đang phát, false nếu đang pause.
     * @param title          Tên bài hát (có thể null).
     * @param artist         Tên nghệ sĩ (có thể null).
     * @param albumArtUri    URI ảnh bìa (không dùng trực tiếp — Media3 tự xử lý).
     *
     * Output:
     * @return Notification object.
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
                        .setMediaSession(null)
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
     * Xây dựng notification ở trạng thái mặc định (không có bài hát đang phát).
     */
    @NonNull
    public Notification buildDefault() {
        return build(false, "NgheNhac", "No song playing", null);
    }

    /**
     * Tạo PendingIntent cho action buttons.
     *
     * @param action Action string (VD: ACTION_PLAY, ACTION_NEXT).
     * @return PendingIntent cho NotificationCompat.Builder.addAction().
     */
    @NonNull
    private PendingIntent buildPendingIntent(@NonNull String action) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                context, action.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }
}
