package com.example.nghenhac.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * WorkManager Worker — đồng bộ dữ liệu background định kỳ.
 *
 * Nguyên lý:
 * - Chạy background task qua WorkManager (đảm bảo chạy kể cả khi app bị kill).
 * - Đồng bộ playlist + yêu thích lên Firebase mỗi 8-12 giờ.
 * - Chỉ chạy khi có network (NetworkType.CONNECTED).
 * - Chỉ chạy nếu user đã đăng nhập.
 *
 * Luồng xử lý:
 * 1. WorkManager kích hoạt worker theo lịch (minInterval = 8h).
 * 2. Worker kiểm tra user đã đăng nhập chưa.
 * 3. Nếu đã đăng nhập → uploadToFirebase().
 * 4. Worker trả về Result.success() hoặc Result.failure().
 * 5. WorkManager tự động retry nếu thất bại (backoff policy).
 *
 * Lưu ý:
 * - Worker chạy trên background thread do WorkManager quản lý.
 * - Không throw exception trong worker (dùng try-catch + return Result).
 * - Dùng ExistingPeriodicWorkPolicy.KEEP để không tạo duplicate workers.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private static final String WORK_NAME = "nghenhac_sync";
    private static final long SYNC_INTERVAL_HOURS = 8;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SyncWorker started");

        // Kiểm tra đăng nhập
        if (!FirebaseAuthManager.getInstance().isLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping sync");
            return Result.success();
        }

        try {
            FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance(getApplicationContext());
            FirebaseSyncManager.SyncResult result = syncManager.uploadToFirebase(getApplicationContext());

            Log.d(TAG, "SyncWorker completed: " + result.getPlaylistsUploaded()
                    + " playlists, " + result.getFavoritesUploaded() + " favorites");
            return Result.success();

        } catch (FirebaseSyncManager.SyncException e) {
            Log.e(TAG, "SyncWorker failed: " + e.getMessage());
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "SyncWorker unexpected error", e);
            return Result.failure();
        }
    }

    /**
     * Lên lịch đồng bộ định kỳ.
     *
     * Nguyên lý:
     * - Tạo PeriodicWorkRequest với minInterval = 8h (Android lên lịch linh hoạt hơn).
     * - Chỉ chạy khi có internet (NetworkType.CONNECTED).
     * - ExistingPeriodicWorkPolicy.KEEP: nếu đã có lịch, không tạo mới.
     *
     * Input:
     * @param context Context.
     */
    public static void scheduleSync(@NonNull Context context) {
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
                .addTag(WORK_NAME)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncRequest
                );

        Log.d(TAG, "Sync scheduled: every " + SYNC_INTERVAL_HOURS + " hours");
    }

    /**
     * Huỷ lịch đồng bộ (dùng khi user logout).
     *
     * Input:
     * @param context Context.
     */
    public static void cancelSync(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Sync cancelled");
    }
}
