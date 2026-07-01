package com.example.nghenhac.ui.player;

import android.app.AlertDialog;
import android.content.Context;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.PreferencesManager;
import com.example.nghenhac.player.MusicPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * 🎛️ EqualizerDialog — Cân bằng âm thanh (Equalizer + Bass Boost).
 *
 * ============================================================
 *  GIẢI THÍCH CHI TIẾT — DÀNH CHO BÁO CÁO ĐỒ ÁN
 * ============================================================
 *
 * ─── 1. VAI TRÒ ───
 * Cho phép người dùng tuỳ chỉnh âm thanh:
 *   - Chọn PRESET có sẵn (Pop, Rock, Jazz, Classic,...)
 *   - Chỉnh từng DẢI TẦN SỐ (bass, mid, treble)
 *   - Bật/tắt TĂNG CƯỜNG BASS
 *
 * ─── 2. KIẾN THỨC NỀN: ÂM THANH LÀ GÌ? ───
 *
 *   Âm thanh = sóng dao động. Tần số càng cao → âm càng cao (the thé).
 *   Tần số càng thấp → âm càng trầm (bass).
 *
 *   60 Hz ───── Bass (trầm) — tiếng trống, bass guitar
 *   230 Hz ──── Low-mid — tiếng cello, piano trầm
 *   910 Hz ──── Mid — giọng nói, guitar
 *   3.6 kHz ─── High-mid — violin, harmonica
 *   14 kHz ─── Treble (cao) — tiếng chũm choẹ, sáo
 *
 * ─── 3. CÔNG NGHỆ ───
 *   Android AudioEffect API:
 *     - Equalizer: điều chỉnh từng dải tần số
 *     - BassBoost: tăng cường âm trầm (bass)
 *   Cần audioSessionId từ ExoPlayer để gán hiệu ứng.
 *
 * ─── 4. LUỒNG CHI TIẾT ───
 *
 *   Bước 1: User mở Equalizer từ PlayerActivity
 *           → EqualizerDialog.show(context)
 *
 *   Bước 2: Lấy MusicPlayer instance
 *           → Kiểm tra ExoPlayer != null
 *           → Lấy audioSessionId = exoPlayer.getAudioSessionId()
 *
 *   Bước 3: Tạo Equalizer và BassBoost
 *           equalizer = new Equalizer(0, audioSessionId)
 *           bassBoost = new BassBoost(0, audioSessionId)
 *           equalizer.setEnabled(true)
 *
 *   Bước 4: Khôi phục settings từ PreferencesManager
 *           → savedPreset (preset đã lưu lần trước)
 *           → savedBassBoost (bass đã bật/tắt lần trước)
 *
 *   Bước 5: Xây dựng UI dialog:
 *           ├── Preset selector (◀ Tên preset ▶)
 *           ├── Band sliders (SeekBar cho từng tần số)
 *           └── Bass Boost toggle (Switch)
 *
 *   Bước 6: User thay đổi → áp dụng NGAY LẬP TỨC
 *           + Lưu vào PreferencesManager (giữ cho lần sau)
 *
 *   Bước 7: Dialog đóng → release() Equalizer + BassBoost
 *           → Giải phóng tài nguyên audio
 *
 * ─── 5. SƠ ĐỒ ───
 *
 *   ┌──────────────┐     ┌──────────────────┐     ┌────────────────┐
 *   │  ExoPlayer   │────→│  audioSessionId  │────→│  Equalizer     │
 *   │  (phát nhạc) │     │  (mỗi bài 1 ID)  │     │  + BassBoost   │
 *   └──────────────┘     └──────────────────┘     └───────┬────────┘
 *                                                          │
 *                                                          ▼
 *                                            ┌────────────────────────┐
 *                                            │  Giao diện Dialog:    │
 *                                            │  - Preset selector    │
 *                                            │  - 5 band SeekBars    │
 *                                            │  - Bass toggle        │
 *                                            └────────────────────────┘
 *                                                          │
 *                                                          ▼
 *                                            ┌────────────────────────┐
 *                                            │  Lưu vào              │
 *                                            │  PreferencesManager   │
 *                                            │  → giữ cho lần sau    │
 *                                            └────────────────────────┘
 *
 * ─── 6. LƯU Ý ───
 *   - KHÔNG phải thiết bị nào cũng hỗ trợ Equalizer
 *     (thiết bị giá rẻ, Android tuỳ chỉnh)
 *   - Try-catch để bắt lỗi "Thiết bị không hỗ trợ Equalizer"
 *   - audioSessionId = 0 → ExoPlayer chưa phát → không thể tạo Equalizer
 *   - Static fields: chỉ 1 Equalizer instance cho toàn app
 *     → Tránh xung đột (2 dialog cùng lúc)
 */
public class EqualizerDialog {

    private static Equalizer equalizer;
    private static BassBoost bassBoost;
    private static boolean isEnabled = false;

    /**
     * ─── HIỂN THỊ EQUALIZER DIALOG ───
     *
     * LUỒNG:
     *   1. Lấy MusicPlayer instance
     *   2. Kiểm tra ExoPlayer != null (chưa phát → không có audio session)
     *   3. Lấy audioSessionId = exoPlayer.getAudioSessionId()
     *   4. Nếu audioSessionId == 0 → báo lỗi "chưa sẵn sàng"
     *   5. Gọi showDialog(context, audioSessionId)
     *
     * @param context Context (Activity/Context)
     */
    public static void show(Context context) {
        MusicPlayer musicPlayer = MusicPlayer.getInstance(context);
        if (musicPlayer.getExoPlayer() == null) {
            android.widget.Toast.makeText(context, "Player chưa sẵn sàng",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        int audioSessionId = musicPlayer.getExoPlayer().getAudioSessionId();
        if (audioSessionId == 0) {
            android.widget.Toast.makeText(context, "Không thể kết nối audio session",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            showDialog(context, audioSessionId);
        } catch (Exception e) {
            android.widget.Toast.makeText(context,
                    "Thiết bị không hỗ trợ Equalizer: " + e.getMessage(),
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ─── TẠO VÀ HIỂN THỊ EQUALIZER DIALOG ───
     *
     * LUỒNG:
     *   1. Giải phóng Equalizer cũ (nếu có) → tránh xung đột
     *   2. Tạo Equalizer + BassBoost với audioSessionId
     *   3. Bật equalizer.setEnabled(true)
     *   4. Khôi phục settings từ PreferencesManager
     *      → savedPreset: preset lần trước user chọn
     *      → savedBassBoost: bass bật/tắt lần trước
     *   5. Xây dựng UI:
     *      - Preset: LinearLayout với 2 nút ◀ ▶ + TextView
     *      - Bands: vòng lặp for các tần số, mỗi tần số 1 SeekBar
     *      - Bass: Switch
     *   6. Gắn sự kiện:
     *      - SeekBar change → equalizer.setBandLevel() + lưu
     *      - Bass Switch → bassBoost.setStrength() + lưu
     *      - Nút Đặt lại → resetToDefault() + clear preferences
     *      - Nút Đóng / Dismiss → release() giải phóng
     *
     * @param context        Context
     * @param audioSessionId ID của audio session từ ExoPlayer
     * @throws Exception Nếu thiết bị không hỗ trợ Equalizer
     */
    private static void showDialog(Context context, int audioSessionId) throws Exception {
        // Giải phóng instance cũ nếu có
        release();

        equalizer = new Equalizer(0, audioSessionId);
        bassBoost = new BassBoost(0, audioSessionId);
        equalizer.setEnabled(true);
        bassBoost.setEnabled(true);
        isEnabled = true;

        // Khôi phục settings từ PreferencesManager
        PreferencesManager prefs = PreferencesManager.getInstance(context);
        int savedPreset = prefs.getEqualizerPreset();
        boolean savedBassBoost = prefs.isBassBoostEnabled();

        // Nếu có preset đã lưu, áp dụng
        if (savedPreset >= 0 && savedPreset < equalizer.getNumberOfPresets()) {
            equalizer.usePreset((short) savedPreset);
        }

        // Nếu có bass boost đã lưu
        if (savedBassBoost) {
            bassBoost.setStrength((short) 500); // 50%
        }

        // Xây dựng UI
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Cân bằng âm thanh");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        // ── Presets ──
        TextView presetLabel = new TextView(context);
        presetLabel.setText("Preset");
        presetLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        layout.addView(presetLabel);

        // Tạo mảng tên preset
        short numPresets = equalizer.getNumberOfPresets();
        String[] presetNames = new String[numPresets];
        for (short i = 0; i < numPresets; i++) {
            presetNames[i] = equalizer.getPresetName(i);
        }

        // Preset selector
        LinearLayout presetRow = new LinearLayout(context);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);

        // Nút Previous
        Button prevBtn = new Button(context);
        prevBtn.setText("\u25C0");
        prevBtn.setOnClickListener(v -> {
            short current = equalizer.getCurrentPreset();
            short next = (short) ((current - 1 + numPresets) % numPresets);
            equalizer.usePreset(next);
            prefs.setEqualizerPreset(next);
            updateBandSeekbars();
        });

        // Nút Next
        Button nextBtn = new Button(context);
        nextBtn.setText("\u25B6");
        nextBtn.setOnClickListener(v -> {
            short current = equalizer.getCurrentPreset();
            short next = (short) ((current + 1) % numPresets);
            equalizer.usePreset(next);
            prefs.setEqualizerPreset(next);
            updateBandSeekbars();
        });

        // Hiển thị preset hiện tại
        TextView currentPreset = new TextView(context);
        currentPreset.setText(presetNames[equalizer.getCurrentPreset()]);
        currentPreset.setPadding(16, 0, 16, 0);
        currentPreset.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        currentPreset.setGravity(android.view.Gravity.CENTER);

        presetRow.addView(prevBtn);
        presetRow.addView(currentPreset);
        presetRow.addView(nextBtn);
        layout.addView(presetRow);

        // ── Band Sliders ──
        TextView bandsLabel = new TextView(context);
        bandsLabel.setText("Tần số");
        bandsLabel.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        bandsLabel.setPadding(0, 16, 0, 8);
        layout.addView(bandsLabel);

        final Map<Integer, SeekBar> bandSeekbars = new HashMap<>();
        short numBands = equalizer.getNumberOfBands();

        for (short i = 0; i < numBands; i++) {
            final short band = i;

            // Tần số của band này (Hz)
            int freq = equalizer.getCenterFreq(band);
            String freqText = freq > 1000 ? (freq / 1000) + " kHz" : freq + " Hz";

            // Giới hạn gain
            short minLevel = equalizer.getBandLevelRange()[0];
            short maxLevel = equalizer.getBandLevelRange()[1];
            short currentLevel = equalizer.getBandLevel(band);

            // Label + value
            LinearLayout bandRow = new LinearLayout(context);
            bandRow.setOrientation(LinearLayout.HORIZONTAL);
            bandRow.setPadding(0, 4, 0, 4);

            TextView bandLabel = new TextView(context);
            bandLabel.setText(freqText);
            bandLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    80, LinearLayout.LayoutParams.WRAP_CONTENT));

            SeekBar seekBar = new SeekBar(context);
            seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            seekBar.setMax(maxLevel - minLevel);
            seekBar.setProgress(currentLevel - minLevel);

            TextView valueLabel = new TextView(context);
            valueLabel.setText(formatGain(currentLevel));
            valueLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    56, LinearLayout.LayoutParams.WRAP_CONTENT));
            valueLabel.setGravity(android.view.Gravity.END);

            seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && equalizer != null) {
                        short level = (short) (progress + minLevel);
                        equalizer.setBandLevel(band, level);
                        valueLabel.setText(formatGain(level));
                        // Lưu band levels vào preferences
                        saveBandLevels(prefs);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            bandRow.addView(bandLabel);
            bandRow.addView(seekBar);
            bandRow.addView(valueLabel);
            layout.addView(bandRow);
            bandSeekbars.put((int) band, seekBar);
        }

        // ── Bass Boost ──
        LinearLayout bassRow = new LinearLayout(context);
        bassRow.setPadding(0, 16, 0, 0);

        TextView bassLabel = new TextView(context);
        bassLabel.setText("Tăng cường bass");
        bassLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Switch bassSwitch = new Switch(context);
        bassSwitch.setChecked(savedBassBoost);
        bassSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bassBoost != null) {
                bassBoost.setStrength(isChecked ? (short) 500 : (short) 0);
                prefs.setBassBoostEnabled(isChecked);
            }
        });

        bassRow.addView(bassLabel);
        bassRow.addView(bassSwitch);
        layout.addView(bassRow);

        builder.setView(layout);
        builder.setPositiveButton("Đóng", (dialog, which) -> release());
        builder.setNegativeButton("Đặt lại", (dialog, which) -> {
            resetToDefault();
            prefs.clearEqualizerSettings();
        });
        builder.setOnDismissListener(dialog -> release());
        builder.show();
    }

    /**
     * ─── LƯU BAND LEVELS ───
     *
     * Lưu mức gain của từng dải tần số vào PreferencesManager.
     * Lần sau mở Equalizer → khôi phục lại.
     *
     * @param prefs PreferencesManager instance
     */
    private static void saveBandLevels(PreferencesManager prefs) {
        if (equalizer == null) return;
        short numBands = equalizer.getNumberOfBands();
        short[] levels = new short[numBands];
        for (short i = 0; i < numBands; i++) {
            levels[i] = equalizer.getBandLevel(i);
        }
        prefs.setEqualizerBandLevels(levels);
    }

    /**
     * ─── CẬP NHẬT SEEK BARS THEO PRESET ───
     *
     * Khi user chọn preset mới, các SeekBar cần được cập nhật
     * để hiển thị mức gain tương ứng.
     *
     * TODO: Tìm các SeekBar trong dialog và cập nhật progress.
     * Hiện tại bỏ qua vì dialog đang hiển thị.
     */
    private static void updateBandSeekbars() {
        // Trong thực tế, cần cập nhật UI từ đây
        // Hiện tại bỏ qua vì dialog đang hiển thị
    }

    /**
     * ─── ĐẶT LẠI EQUALIZER ───
     *
     * Đưa equalizer về preset đầu tiên (thường là Normal/Flat).
     * Tắt Bass Boost.
     */
    private static void resetToDefault() {
        try {
            if (equalizer != null) {
                equalizer.usePreset((short) 0); // Normal/Flat preset
            }
            if (bassBoost != null) {
                bassBoost.setStrength((short) 0);
            }
        } catch (Exception ignored) {}
    }

    /**
     * ─── GIẢI PHÓNG AUDIO EFFECT ───
     *
     * LUỒNG:
     *   1. equalizer.setEnabled(false) → tắt hiệu ứng
     *   2. equalizer.release() → giải phóng tài nguyên native
     *   3. equalizer = null → cho GC dọn dẹp
     *   4. Tương tự với BassBoost
     *
     * QUAN TRỌNG:
     *   - Phải gọi release() khi dialog đóng
     *   - Nếu không, equalizer vẫn chạy ngầm → tốn pin
     *   - Nếu không, lần mở sau không tạo được equalizer mới
     */
    private static void release() {
        try {
            if (equalizer != null) {
                equalizer.setEnabled(false);
                equalizer.release();
                equalizer = null;
            }
            if (bassBoost != null) {
                bassBoost.setEnabled(false);
                bassBoost.release();
                bassBoost = null;
            }
        } catch (Exception ignored) {}
        isEnabled = false;
    }

    /**
     * ─── ĐỊNH DẠNG GAIN ───
     *
     * Chuyển từ millibels (đơn vị của AudioEffect API) sang dB.
     *   - 0 millibels = 0.0 dB (không thay đổi)
     *   - 500 millibels = 5.0 dB (tăng)
     *   - -500 millibels = -5.0 dB (giảm)
     *
     * @param millibels Giá trị gain từ AudioEffect API (1/100 dB)
     * @return Chuỗi hiển thị: "+5.0 dB", "-2.0 dB", "0.0 dB"
     */
    private static String formatGain(short millibels) {
        float dB = millibels / 100.0f;
        return String.format(java.util.Locale.US, "%.1f", dB);
    }
}
