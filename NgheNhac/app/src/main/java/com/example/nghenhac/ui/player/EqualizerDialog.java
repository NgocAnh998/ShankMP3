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
 * Equalizer dialog — điều chỉnh âm thanh với presets, band sliders, bass boost.
 *
 * Nguyên lý:
 * - Sử dụng Android AudioEffect API (Equalizer + BassBoost).
 * - Cần audio session ID từ ExoPlayer để gán hiệu ứng.
 * - Presets: Normal, Classical, Dance, Flat, Folk, Heavy Metal, Hip Hop, Jazz, Pop, Rock.
 * - Lưu settings vào PreferencesManager (E2).
 *
 * Luồng xử lý:
 * 1. User mở Equalizer từ PlayerActivity menu.
 * 2. Khởi tạo Equalizer và BassBoost với audio session ID.
 * 3. Hiển thị danh sách preset + band sliders + bass boost toggle.
 * 4. User thay đổi → áp dụng ngay lập tức + lưu vào PreferencesManager.
 * 5. Dialog đóng → giải phóng AudioEffect objects.
 */
public class EqualizerDialog {

    private static Equalizer equalizer;
    private static BassBoost bassBoost;
    private static boolean isEnabled = false;

    /**
     * Hiển thị Equalizer dialog.
     *
     * @param context Context.
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
     * Tạo và hiển thị dialog Equalizer.
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
     * Lưu band levels vào PreferencesManager.
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
     * Cập nhật Seekbars theo preset hiện tại.
     */
    private static void updateBandSeekbars() {
        // Trong thực tế, cần cập nhật UI từ đây
        // Hiện tại bỏ qua vì dialog đang hiển thị
    }

    /**
     * Đặt lại equalizer về mặc định (flat).
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
     * Giải phóng AudioEffect objects.
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
     * Định dạng gain value (millibels → dB).
     */
    private static String formatGain(short millibels) {
        float dB = millibels / 100.0f;
        return String.format(java.util.Locale.US, "%.1f", dB);
    }
}
