package com.example.nghenhac.ui.player;

import android.app.AlertDialog;
import android.content.Context;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.RadioGroup;

import com.example.nghenhac.R;

/**
 * ⏰ SleepTimerDialog — Hẹn giờ tự động tắt nhạc.
 *
 * ============================================================
 *  GIẢI THÍCH CHI TIẾT — DÀNH CHO BÁO CÁO ĐỒ ÁN
 * ============================================================
 *
 * ─── 1. VAI TRÒ ───
 * Cho phép người dùng hẹn giờ TẮT NHẠC SAU N PHÚT.
 * Hữu ích khi: nghe nhạc trước khi ngủ → nhạc tự tắt → không hao pin.
 *
 * ─── 2. CÔNG NGHỆ ───
 *   CountDownTimer: Bộ đếm ngược của Android.
 *   - Chạy trên MAIN thread (UI thread)
 *   - onTick() được gọi MỖI GIÂY (1000ms)
 *   - onFinish() được gọi khi hết thời gian
 *
 * ─── 3. VÍ DỤ THỰC TẾ ───
 *   Giống như bạn đặt BÁO THỨC:
 *   - Chọn "30 phút" → bắt đầu đếm
 *   - Hết 30 phút → chuông reo (ở đây: nhạc dừng)
 *   - Có thể HUỶ bất kỳ lúc nào
 *
 * ─── 4. LUỒNG CHI TIẾT ───
 *
 *   ┌─────────────────────────────────────────────────────┐
 *   │              LUỒNG SLEEP TIMER                      │
 *   │                                                     │
 *   │   User mở dialog → SleepTimerDialog.show(context)   │
 *   │       │                                             │
 *   │       ├── ĐANG CÓ TIMER? → showCancelDialog()      │
 *   │       │   → "Huỷ hẹn giờ?"                       │
 *   │       │                                             │
 *   │       └── CHƯA CÓ TIMER → RadioGroup chọn           │
 *   │           ├── ○ 10 phút                             │
 *   │           ├── ○ 15 phút                             │
 *   │           ├── ● 30 phút (mặc định)                 │
 *   │           ├── ○ 45 phút                             │
 *   │           └── ○ 60 phút                             │
 *   │               │                                     │
 *   │               ▼                                     │
 *   │           Bấm "Bắt đầu"                            │
 *   │               │                                     │
 *   │               ▼                                     │
 *   │   CountDownTimer.start(durationMs, 1000ms)          │
 *   │       │                                             │
 *   │       ├── onTick(): mỗi 1 giây                      │
 *   │       │   → Có thể cập nhật "Còn 29:59"          │
 *   │       │                                             │
 *   │       └── onFinish(): hết giờ                      │
 *   │           → listener.onSleepTimerEnd()              │
 *   │           → MusicPlayer.pause() → nhạc DỪNG        │
 *   │           → currentTimer = null (reset)            │
 *   │                                                     │
 *   │   User có thể HUỶ bất kỳ lúc:                      │
 *   │   → SleepTimerDialog.cancelTimer()                  │
 *   │   → currentTimer.cancel() + currentTimer = null    │
 *   └─────────────────────────────────────────────────────┘
 *
 * ─── 5. CÁC LỰA CHỌN THỜI GIAN ───
 *
 *   Lựa chọn  |  milliseconds
 *   ──────────┼──────────────
 *   10 phút   |  600,000 ms
 *   15 phút   |  900,000 ms
 *   30 phút   |  1,800,000 ms (mặc định)
 *   45 phút   |  2,700,000 ms
 *   60 phút   |  3,600,000 ms
 *
 * ─── 6. LƯU Ý ───
 *   - CountDownTimer chạy trên MAIN thread → KHÔNG block UI
 *   - Nếu app bị KILL, timer cũng mất (chạy trong process app)
 *   - Để chạy kể cả khi app tắt: cần AlarmManager hoặc WorkManager
 *   - onTick() mỗi 1 giây → phù hợp để cập nhật UI, không quá nặng
 *   - Static fields: chỉ 1 timer duy nhất cho toàn app
 *     → Tránh xung đột (2 timer cùng lúc)
 */
public class SleepTimerDialog {

    private static CountDownTimer currentTimer;
    private static AlertDialog activeDialog;
    private static long selectedDurationMs = 0;

    /**
     * ─── CALLBACK KHI TIMER KẾT THÚC ───
     *
     * Interface này được gọi khi CountDownTimer kết thúc.
     * Activity/Fragment implement interface này để xử lý:
     *   - Gọi MusicPlayer.pause() → dừng nhạc
     *   - Hiển thị thông báo "Đã hết giờ"
     *   - Cập nhật UI (nếu cần)
     */
    public interface OnSleepTimerListener {
        /** Được gọi khi timer hết giờ — nên gọi MusicPlayer.pause(). */
        void onSleepTimerEnd();
    }

    /**
     * ─── HIỂN THỊ SLEEP TIMER DIALOG ───
     *
     * LUỒNG:
     *   1. Kiểm tra có timer đang chạy không?
     *      - CÓ → showCancelDialog() (hỏi user có muốn huỷ không)
     *      - KHÔNG → Hiển thị RadioGroup chọn thời gian
     *   2. User chọn thời gian → bấm "Bắt đầu"
     *   3. startTimer(context, durationMs, listener)
     *
     * @param context  Context để tạo AlertDialog
     * @param listener Callback khi timer kết thúc (gọi MusicPlayer.pause())
     */
    public static void show(Context context, OnSleepTimerListener listener) {
        // Nếu đang có timer chạy, hiển thị dialog huỷ
        if (currentTimer != null) {
            showCancelDialog(context);
            return;
        }

        // Tạo dialog chọn thời gian
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Hẹn giờ tắt nhạc");

        // Layout chứa RadioGroup
        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setPadding(32, 16, 32, 16);

        String[] options = {"10 phút", "15 phút", "30 phút", "45 phút", "60 phút"};
        long[] durations = {10 * 60 * 1000L, 15 * 60 * 1000L, 30 * 60 * 1000L,
                            45 * 60 * 1000L, 60 * 60 * 1000L};

        for (int i = 0; i < options.length; i++) {
            android.widget.RadioButton radioButton = new android.widget.RadioButton(context);
            radioButton.setText(options[i]);
            radioButton.setId(i);
            radioButton.setPadding(16, 8, 16, 8);
            radioGroup.addView(radioButton);
        }

        // Mặc định chọn 30 phút
        radioGroup.check(2);

        builder.setView(radioGroup);
        builder.setPositiveButton("Bắt đầu", (dialog, which) -> {
            int checkedId = radioGroup.getCheckedRadioButtonId();
            if (checkedId >= 0 && checkedId < durations.length) {
                selectedDurationMs = durations[checkedId];
                startTimer(context, selectedDurationMs, listener);
            }
        });
        builder.setNegativeButton("Huỷ", null);
        builder.show();
    }

    /**
     * ─── BẮT ĐẦU ĐẾM NGƯỢC ───
     *
     * Tạo CountDownTimer và bắt đầu đếm.
     *
     * @param context    Context
     * @param durationMs Thời gian tính bằng milliseconds
     *                   (VD: 30 phút = 30 * 60 * 1000 = 1,800,000ms)
     * @param listener   Callback khi hết giờ
     */
    private static void startTimer(Context context, long durationMs,
                                   OnSleepTimerListener listener) {
        // Huỷ timer cũ nếu có
        cancelTimer();

        currentTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Có thể cập nhật UI nếu cần
            }

            @Override
            public void onFinish() {
                if (listener != null) {
                    listener.onSleepTimerEnd();
                }
                currentTimer = null;
                selectedDurationMs = 0;
            }
        }.start();
    }

    /**
     * ─── HIỂN THỊ DIALOG HUỶ TIMER ───
     *
     * Khi user mở SleepTimerDialog lần nữa (timer đang chạy):
     *   - Hiển thị "Đang hẹn giờ... Bạn có muốn huỷ không?"
     *   - "Huỷ hẹn giờ" → cancelTimer()
     *   - "Giữ nguyên" → đóng dialog, timer tiếp tục
     *
     * @param context Context để tạo AlertDialog
     */
    private static void showCancelDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Hẹn giờ tắt nhạc");
        builder.setMessage("Đang hẹn giờ tắt nhạc. Bạn có muốn huỷ không?");
        builder.setPositiveButton("Huỷ hẹn giờ", (dialog, which) -> cancelTimer());
        builder.setNegativeButton("Giữ nguyên", null);
        builder.show();
    }

    /**
     * ─── HUỶ TIMER ───
     *
     * Dừng CountDownTimer nếu đang chạy.
     * Gọi trong các trường hợp:
     *   - User bấm "Huỷ hẹn giờ"
     *   - Activity/Fragment bị destroy (tránh memory leak)
     *   - User chọn bài hát khác (tuỳ chọn)
     */
    public static void cancelTimer() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
        selectedDurationMs = 0;
    }

    /**
     * Kiểm tra có timer đang chạy không.
     *
     * Output:
     * @return true nếu đang có timer hoạt động.
     */
    public static boolean isTimerActive() {
        return currentTimer != null;
    }

    /**
     * Lấy thời gian còn lại (milliseconds).
     *
     * Output:
     * @return Thời gian còn lại, 0 nếu không có timer.
     */
    public static long getRemainingTime() {
        return selectedDurationMs;
    }
}
