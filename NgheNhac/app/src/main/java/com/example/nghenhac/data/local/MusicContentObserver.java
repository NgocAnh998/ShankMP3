package com.example.nghenhac.data.local;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Theo dõi thay đổi trong MediaStore (thêm/xoá/sửa file nhạc) và tự động kích hoạt quét lại.
 *
 * Nguyên lý:
 * - Đăng ký ContentObserver với MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.
 * - Khi có thay đổi (user thêm nhạc mới, xoá nhạc, edit tag), MediaStore gửi notification.
 * - Sử dụng debounce 1.5 giây: nếu nhiều thay đổi xảy ra liên tiếp, chỉ trigger một lần duy nhất.
 * - Listener được gọi trên main thread để an toàn cập nhật UI.
 *
 * Luồng xử lý:
 * 1. register() → tạo ContentObserver → đăng ký với ContentResolver.
 * 2. MediaStore thay đổi → onChange() được gọi (trên main thread).
 * 3. handleChange() reset debounce timer (nếu thay đổi mới đến trong vòng 1.5s).
 * 4. Hết 1.5s im lặng → debounceRunnable chạy → gọi listener.onMusicChanged().
 * 5. unregister() → huỷ ContentObserver → tránh memory leak.
 *
 * Input:
 * - Context: dùng getApplicationContext() để lấy ContentResolver.
 * - OnMusicChangeListener: callback khi phát hiện thay đổi.
 *
 * Output:
 * - Gọi listener.onMusicChanged() sau 1.5 giây không có thay đổi mới.
 *
 * Lưu ý:
 * - Bắt buộc gọi unregister() trong onDestroy() để tránh memory leak.
 * - destroy() gọi unregister() + xoá tất cả callback còn pending.
 * - Thread-safe: synchronized trên handleChange().
 */
public class MusicContentObserver {

    private static final String TAG = "MusicContentObserver";
    private static final long DEBOUNCE_MS = 1500L;

    private final ContentResolver contentResolver;
    private final OnMusicChangeListener listener;
    private final Handler mainHandler;

    private ContentObserver observer;
    private boolean isRegistered;
    private long lastChangeTime;

    /**
     * Listener được gọi khi có thay đổi trong MediaStore và debounce hoàn tất.
     *
     * Callback chạy trên main thread, an toàn để cập nhật UI.
     */
    public interface OnMusicChangeListener {
        /** Được gọi sau 1.5s không có thay đổi mới. */
        void onMusicChanged();
    }

    /**
     * @param context  Context dùng để lấy ContentResolver (application context).
     * @param listener Callback khi có thay đổi (chạy trên main thread).
     */
    public MusicContentObserver(@NonNull android.content.Context context,
                                @NonNull OnMusicChangeListener listener) {
        this.contentResolver = context.getApplicationContext().getContentResolver();
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isRegistered = false;
        this.lastChangeTime = 0L;
    }

    /**
     * Đăng ký ContentObserver để bắt đầu theo dõi thay đổi MediaStore.
     *
     * Nguyên lý:
     * - Tạo ContentObserver với mainHandler (onChange chạy trên main thread).
     * - notifyForDescendants = true: theo dõi cả URI con (thay đổi ở cấp file cụ thể).
     * - Kiểm tra isRegistered để tránh double-register.
     *
     * Lưu ý:
     * - Bắt buộc gọi unregister() trong onDestroy() hoặc khi không còn cần theo dõi.
     */
    public void register() {
        if (isRegistered) return;

        observer = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);
                handleChange(uri);
            }

            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                handleChange(null);
            }
        };

        contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
        );

        isRegistered = true;
        Log.d(TAG, "ContentObserver registered for MediaStore.Audio.Media");
    }

    /**
     * Hủy đăng ký ContentObserver.
     *
     * Nguyên lý:
     * - Gọi unregisterContentObserver() để giải phóng observer.
     * - Bắt exception để tránh crash nếu observer đã được unregister trước đó.
     * - Set observer = null để GC thu dọn.
     *
     * Lưu ý: Gọi trong onDestroy() hoặc khi Fragment/Activity bị destroy.
     */
    public void unregister() {
        if (!isRegistered || observer == null) return;

        try {
            contentResolver.unregisterContentObserver(observer);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering ContentObserver", e);
        }

        isRegistered = false;
        observer = null;
        Log.d(TAG, "ContentObserver unregistered");
    }

    /** Kiểm tra trạng thái đăng ký. */
    public boolean isRegistered() {
        return isRegistered;
    }

    // ── Private ──

    /**
     * Xử lý khi có thay đổi từ MediaStore với debounce.
     *
     * Nguyên lý:
     * - Ghi nhận thời điểm thay đổi cuối cùng.
     * - Nếu thay đổi mới đến trong vòng 1.5s kể từ lần trước → reset timer.
     * - Chỉ gọi listener sau 1.5s im lặng liên tục.
     * - Tránh gọi listener quá nhiều lần khi import hàng loạt file nhạc.
     *
     * Luồng xử lý:
     * 1. Lấy current time → so sánh với lastChangeTime.
     * 2. Nếu khoảng cách < DEBOUNCE_MS → xoá pending callback cũ.
     * 3. Post callback mới sau DEBOUNCE_MS.
     *
     * Thread-safe nhờ synchronized.
     */
    private synchronized void handleChange(@Nullable Uri uri) {
        long now = System.currentTimeMillis();
        long timeSinceLastChange = now - lastChangeTime;

        Log.d(TAG, "MediaStore change detected: " + uri +
                " (time since last: " + timeSinceLastChange + "ms)");

        lastChangeTime = now;

        if (timeSinceLastChange < DEBOUNCE_MS) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        mainHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
    }

    /**
     * Runnable được gọi sau debounce hoàn tất.
     * Gửi sự kiện đến listener (trên main thread).
     */
    private final Runnable debounceRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Debounce complete — notifying listener");
            listener.onMusicChanged();
        }
    };

    /**
     * Giải phóng toàn bộ tài nguyên.
     *
     * Nguyên lý:
     * - Gọi unregister() để huỷ ContentObserver.
     * - Xoá tất cả callback pending trên mainHandler.
     * - Gọi khi observer không còn được dùng nữa (VD: app destroy).
     */
    public void destroy() {
        unregister();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
