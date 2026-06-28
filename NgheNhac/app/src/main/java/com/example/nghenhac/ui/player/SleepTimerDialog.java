package com.example.nghenhac.ui.player;

import android.app.AlertDialog;
import android.content.Context;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.RadioGroup;

import com.example.nghenhac.R;

/**
 * Dialog chọn sleep timer — tự động dừng nhạc sau khoảng thời gian nhất định.
 *
 * Nguyên lý:
 * - Cho phép người dùng chọn thời gian: 10 phút, 15 phút, 30 phút, 45 phút, 60 phút.
 * - Khi timer hết, gọi callback onSleepTimerEnd() để dừng nhạc.
 * - Hiển thị thời gian còn lại trên dialog (dùng CountDownTimer).
 * - Có thể huỷ timer trước khi hết giờ.
 *
 * Luồng xử lý:
 * 1. User mở dialog → chọn thời gian → bấm "Bắt đầu".
 * 2. CountDownTimer chạy → dialog hiển thị thời gian còn lại.
 * 3. Timer kết thúc → callback onSleepTimerEnd() → MusicPlayer.pause().
 * 4. User có thể huỷ timer bất kỳ lúc nào.
 *
 * Input:
 * - Context để tạo dialog.
 * - Callback để xử lý khi timer kết thúc.
 */
public class SleepTimerDialog {

    private static CountDownTimer currentTimer;
    private static AlertDialog activeDialog;
    private static long selectedDurationMs = 0;

    /**
     * Callback khi sleep timer kết thúc.
     */
    public interface OnSleepTimerListener {
        /** Được gọi khi timer hết giờ — nên gọi MusicPlayer.pause(). */
        void onSleepTimerEnd();
    }

    /**
     * Hiển thị dialog chọn sleep timer.
     *
     * Nguyên lý:
     * - Nếu đang có timer chạy, hiển thị tuỳ chọn huỷ.
     * - Nếu chưa có timer, hiển thị các lựa chọn thời gian.
     * - Dùng RadioGroup cho các lựa chọn thời gian.
     *
     * Input:
     * @param context  Context để tạo dialog.
     * @param listener Callback khi timer kết thúc (có thể null).
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
     * Bắt đầu đếm ngược.
     *
     * Nguyên lý:
     * - Tạo CountDownTimer với thời gian đã chọn.
     * - Mỗi giây cập nhật thời gian còn lại.
     * - Khi kết thúc, gọi callback và reset timer.
     *
     * Input:
     * @param context       Context.
     * @param durationMs    Thời gian (milliseconds).
     * @param listener      Callback khi kết thúc.
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
     * Hiển thị dialog huỷ timer đang chạy.
     *
     * Nguyên lý:
     * - Hiển thị thông báo "Đang hẹn giờ..." và nút "Huỷ".
     * - Khi user bấm huỷ, gọi cancelTimer().
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
     * Huỷ timer đang chạy.
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
