package com.example.nghenhac;

import android.app.Application;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.nghenhac.data.remote.RetrofitClient;
import com.example.nghenhac.sync.FirebaseAuthManager;
import com.example.nghenhac.sync.SyncWorker;

/**
 * Application class — điểm khởi đầu của ứng dụng NgheNhac.
 *
 * Nguyên lý:
 * - Khởi tạo các thành phần global (Retrofit) ngay khi app start.
 * - Cung cấp instance singleton (getInstance()) cho toàn app.
 * - Quản lý bộ nhớ Glide qua onLowMemory() và onTrimMemory().
 * - Xử lý exception toàn cục qua UncaughtExceptionHandler: ghi log + không crash silently.
 * - Lên lịch đồng bộ Firebase background nếu user đã đăng nhập.
 *
 * Luồng xử lý:
 * 1. Android OS tạo process → gọi onCreate().
 * 2. onCreate() → set instance → setupUncaughtExceptionHandler() → RetrofitClient.init().
 * 3. Kiểm tra FirebaseAuth → nếu đã đăng nhập → SyncWorker.scheduleSync().
 * 4. App chạy → các component gọi NgheNhacApp.getInstance() nếu cần context.
 * 5. Khi bộ nhớ thấp → onLowMemory/onTrimMemory → Glide giải phóng cache.
 * 6. Exception không bắt → UncaughtExceptionHandler ghi log → kết thúc process.
 *
 * Lưu ý:
 * - Referenced trong AndroidManifest dưới dạng android:name=".NgheNhacApp".
 * - Không khởi tạo Room ở đây (lazy init qua getInstance() trong các Repository).
 */
public class NgheNhacApp extends Application {

    private static final String TAG = "NgheNhacApp";
    private static NgheNhacApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Thiết lập exception handler toàn cục
        setupUncaughtExceptionHandler();

        // Khởi tạo Retrofit (lazy — lần gọi đầu tiên tạo instance)
        RetrofitClient.init();

        // Lên lịch đồng bộ Firebase background nếu user đã đăng nhập
        if (FirebaseAuthManager.getInstance().isLoggedIn()) {
            SyncWorker.scheduleSync(this);
        }
    }

    /**
     * Thiết lập UncaughtExceptionHandler toàn cục.
     *
     * Nguyên lý:
     * - Bắt tất cả exception không được try-catch trong toàn bộ app.
     * - Ghi log chi tiết (stack trace, cause) để debug.
     * - Sau đó gọi handler mặc định để Android kết thúc process.
     * - Tránh crash âm thầm (silent crash) gây khó debug.
     */
    private void setupUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler =
                Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "=== UNCAUGHT EXCEPTION ===");
            Log.e(TAG, "Thread: " + thread.getName() + " (" + thread.getId() + ")");
            Log.e(TAG, "Exception: " + throwable.getClass().getName() + ": " + throwable.getMessage());

            // Ghi stack trace
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : throwable.getStackTrace()) {
                stackTrace.append("  at ").append(element.toString()).append("\n");
            }
            Log.e(TAG, "Stack trace:\n" + stackTrace.toString());

            // Ghi cause chain
            Throwable cause = throwable.getCause();
            while (cause != null) {
                Log.e(TAG, "Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                cause = cause.getCause();
            }

            Log.e(TAG, "=== END UNCAUGHT EXCEPTION ===");

            // Chuyển tiếp cho handler mặc định (Android kết thúc process)
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }

    public static NgheNhacApp getInstance() {
        return instance;
    }

    /**
     * Giải phóng bộ nhớ Glide khi hệ thống yêu cầu.
     * Gọi Glide.onLowMemory() để clear cache ảnh.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).onLowMemory();
    }

    /**
     * Giải phóng bộ nhớ Glide theo mức độ (level).
     * Android gọi method này khi app ở background hoặc bộ nhớ thấp.
     *
     * Input:
     * @param level Mức độ trim memory (TRIM_MEMORY_UI_HIDDEN, TRIM_MEMORY_RUNNING_LOW, ...).
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).onTrimMemory(level);
    }
}
