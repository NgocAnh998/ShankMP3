package com.example.nghenhac.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Quản lý SharedPreferences — lưu trạng thái ứng dụng để khôi phục sau khi app bị kill.
 *
 * Nguyên lý:
 * - SharedPreferences là giải pháp lưu key-value đơn giản, không cần database.
 * - Dùng để lưu các giá trị cần khôi phục nhanh khi app khởi động lại.
 * - Các trạng thái được lưu: bài hát cuối cùng, vị trí phát, chế độ lặp, xáo trộn.
 * - Kết hợp với onSaveInstanceState() để khôi phục state khi Activity bị recreate.
 *
 * Luồng xử lý:
 * 1. App khởi động → PreferencesManager khởi tạo → đọc các giá trị đã lưu.
 * 2. User phát nhạc → lưu lastSongId, lastPosition định kỳ.
 * 3. User thay đổi repeat/shuffle → lưu ngay lập tức.
 * 4. App bị kill → khi mở lại, đọc từ SharedPreferences → khôi phục trạng thái.
 *
 * Lưu ý:
 * - Không dùng cho dữ liệu nhạy cảm (dùng SecurePreferences cho token/password).
 * - Tất cả phương thức đều chạy trên main thread (SharedPreferences nhanh, synchronous).
 * - Editor.apply() được dùng thay vì commit() (async, không block UI).
 */
public class PreferencesManager {

    private static final String PREF_NAME = "nghenhac_preferences";

    private static final String KEY_LAST_SONG_ID = "last_song_id";
    private static final String KEY_LAST_POSITION = "last_position";
    private static final String KEY_REPEAT_MODE = "repeat_mode";
    private static final String KEY_SHUFFLE_MODE = "shuffle_mode";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_LAST_PLAYLIST_ID = "last_playlist_id";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Equalizer settings
    private static final String KEY_EQ_PRESET = "eq_preset";
    private static final String KEY_EQ_BAND_LEVELS = "eq_band_levels";
    private static final String KEY_BASS_BOOST = "bass_boost";

    private final SharedPreferences prefs;

    // ════════════════════════════════════════════
    //  Repeat Mode Enum
    // ════════════════════════════════════════════

    /** Chế độ lặp lại. */
    public enum RepeatMode {
        /** Không lặp — phát hết danh sách thì dừng. */
        NONE(0),
        /** Lặp lại một bài hát hiện tại. */
        ONE(1),
        /** Lặp lại toàn bộ danh sách phát. */
        ALL(2);

        final int value;
        RepeatMode(int value) { this.value = value; }

        public static RepeatMode fromValue(int value) {
            for (RepeatMode mode : values()) {
                if (mode.value == value) return mode;
            }
            return NONE;
        }
    }

    /** Chế độ giao diện. */
    public enum ThemeMode {
        SYSTEM(0),
        LIGHT(1),
        DARK(2);

        final int value;
        ThemeMode(int value) { this.value = value; }

        public static ThemeMode fromValue(int value) {
            for (ThemeMode mode : values()) {
                if (mode.value == value) return mode;
            }
            return SYSTEM;
        }
    }

    // ── Singleton ──

    private static PreferencesManager instance;

    private PreferencesManager(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferencesManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
        return instance;
    }

    // ════════════════════════════════════════════
    //  Last Song & Position
    // ════════════════════════════════════════════

    /**
     * Lưu ID bài hát cuối cùng đã phát.
     * Dùng để khôi phục khi app mở lại.
     */
    public void setLastSongId(long songId) {
        prefs.edit().putLong(KEY_LAST_SONG_ID, songId).apply();
    }

    /**
     * Lấy ID bài hát cuối cùng đã phát.
     * Trả về -1 nếu chưa có bài hát nào.
     */
    public long getLastSongId() {
        return prefs.getLong(KEY_LAST_SONG_ID, -1L);
    }

    /**
     * Lưu vị trí phát hiện tại (milliseconds).
     * Lưu định kỳ (mỗi 5-10 giây) hoặc khi pause/stop.
     */
    public void setLastPosition(long positionMs) {
        prefs.edit().putLong(KEY_LAST_POSITION, positionMs).apply();
    }

    /** Lấy vị trí phát cuối cùng (milliseconds). Mặc định 0. */
    public long getLastPosition() {
        return prefs.getLong(KEY_LAST_POSITION, 0L);
    }

    /**
     * Lưu ID playlist cuối cùng đang phát.
     * Dùng để khôi phục danh sách phát khi app mở lại.
     */
    public void setLastPlaylistId(long playlistId) {
        prefs.edit().putLong(KEY_LAST_PLAYLIST_ID, playlistId).apply();
    }

    /** Lấy ID playlist cuối cùng. Trả về -1 nếu không có. */
    public long getLastPlaylistId() {
        return prefs.getLong(KEY_LAST_PLAYLIST_ID, -1L);
    }

    // ════════════════════════════════════════════
    //  Repeat & Shuffle
    // ════════════════════════════════════════════

    /**
     * Lưu chế độ lặp hiện tại.
     *
     * Input:
     * @param mode RepeatMode: NONE (0), ONE (1), ALL (2).
     */
    public void setRepeatMode(@NonNull RepeatMode mode) {
        prefs.edit().putInt(KEY_REPEAT_MODE, mode.value).apply();
    }

    /** Lấy chế độ lặp hiện tại. Mặc định NONE. */
    @NonNull
    public RepeatMode getRepeatMode() {
        return RepeatMode.fromValue(prefs.getInt(KEY_REPEAT_MODE, RepeatMode.NONE.value));
    }

    /**
     * Bật/tắt chế độ xáo trộn.
     *
     * Input:
     * @param enabled true = bật xáo trộn, false = tắt.
     */
    public void setShuffleMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHUFFLE_MODE, enabled).apply();
    }

    /** Kiểm tra chế độ xáo trộn có đang bật không. Mặc định false. */
    public boolean isShuffleMode() {
        return prefs.getBoolean(KEY_SHUFFLE_MODE, false);
    }

    // ════════════════════════════════════════════
    //  Volume
    // ════════════════════════════════════════════

    /**
     * Lưu âm lượng hiện tại (0.0 - 1.0).
     *
     * Input:
     * @param volume Giá trị âm lượng từ 0.0 (câm) đến 1.0 (max).
     */
    public void setVolume(float volume) {
        prefs.edit().putFloat(KEY_VOLUME, volume).apply();
    }

    /** Lấy âm lượng hiện tại. Mặc định 0.8. */
    public float getVolume() {
        return prefs.getFloat(KEY_VOLUME, 0.8f);
    }

    // ════════════════════════════════════════════
    //  Theme
    // ════════════════════════════════════════════

    /**
     * Lưu chế độ giao diện (theme).
     *
     * Input:
     * @param mode ThemeMode: SYSTEM (theo hệ thống), LIGHT, DARK.
     */
    public void setThemeMode(@NonNull ThemeMode mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.value).apply();
    }

    /** Lấy chế độ giao diện. Mặc định SYSTEM (theo hệ thống). */
    @NonNull
    public ThemeMode getThemeMode() {
        return ThemeMode.fromValue(prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.value));
    }

    // ════════════════════════════════════════════
    //  First Launch
    // ════════════════════════════════════════════

    /**
     * Kiểm tra đây có phải lần đầu tiên app khởi chạy không.
     *
     * Nguyên lý:
     * - Lần đầu chạy: trả về true và set flag = false.
     * - Dùng để hiển thị onboarding screen hoặc quét MediaStore lần đầu.
     *
     * Output:
     * @return true nếu là lần đầu chạy, false nếu đã chạy trước đó.
     */
    public boolean isFirstLaunch() {
        boolean first = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        if (first) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        }
        return first;
    }

    // ════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════

    // ════════════════════════════════════════════
    //  Equalizer
    // ════════════════════════════════════════════

    /**
     * Lưu preset equalizer hiện tại.
     */
    public void setEqualizerPreset(int preset) {
        prefs.edit().putInt(KEY_EQ_PRESET, preset).apply();
    }

    /**
     * Lấy preset equalizer đã lưu. Mặc định 0 (Normal).
     */
    public int getEqualizerPreset() {
        return prefs.getInt(KEY_EQ_PRESET, 0);
    }

    /**
     * Lưu band levels của equalizer.
     */
    public void setEqualizerBandLevels(short[] levels) {
        StringBuilder sb = new StringBuilder();
        for (short level : levels) {
            sb.append(level).append(",");
        }
        prefs.edit().putString(KEY_EQ_BAND_LEVELS, sb.toString()).apply();
    }

    /**
     * Lấy band levels đã lưu. Mặc định null.
     */
    @Nullable
    public short[] getEqualizerBandLevels() {
        String data = prefs.getString(KEY_EQ_BAND_LEVELS, null);
        if (data == null || data.isEmpty()) return null;
        String[] parts = data.split(",");
        short[] levels = new short[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                levels[i] = Short.parseShort(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return levels;
    }

    /**
     * Bật/tắt bass boost.
     */
    public void setBassBoostEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BASS_BOOST, enabled).apply();
    }

    /**
     * Kiểm tra bass boost có đang bật không. Mặc định false.
     */
    public boolean isBassBoostEnabled() {
        return prefs.getBoolean(KEY_BASS_BOOST, false);
    }

    /**
     * Xoá tất cả equalizer settings.
     */
    public void clearEqualizerSettings() {
        prefs.edit()
                .remove(KEY_EQ_PRESET)
                .remove(KEY_EQ_BAND_LEVELS)
                .remove(KEY_BASS_BOOST)
                .apply();
    }

    /**
     * Xoá tất cả preferences (dùng khi user logout hoặc reset app).
     *
     * Nguyên lý:
     * - Xoá toàn bộ key-value trong SharedPreferences.
     * - Không gọi khi chỉ muốn reset một vài giá trị cụ thể.
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }

    /**
     * Lưu playback state tổng hợp (gọi khi pause hoặc app về background).
     *
     * Nguyên lý:
     * - Lưu đồng thời songId, position, playlistId trong một lần gọi.
     * - Chỉ ghi một lần thay vì nhiều lần gọi riêng lẻ.
     *
     * Input:
     * @param songId     ID bài hát hiện tại.
     * @param positionMs Vị trí phát (milliseconds).
     * @param playlistId ID playlist hiện tại (-1 nếu không có).
     */
    public void savePlaybackState(long songId, long positionMs, long playlistId) {
        prefs.edit()
                .putLong(KEY_LAST_SONG_ID, songId)
                .putLong(KEY_LAST_POSITION, positionMs)
                .putLong(KEY_LAST_PLAYLIST_ID, playlistId)
                .apply();
    }
}
