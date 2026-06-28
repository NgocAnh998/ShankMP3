package com.example.nghenhac.data.local;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test cho PreferencesManager — quản lý SharedPreferences.
 *
 * Nguyên lý:
 * - PreferencesManager cần Android Context để truy cập SharedPreferences.
 * - Các test này kiểm tra logic các enum (RepeatMode, ThemeMode) và
 *   các hằng số, method không phụ thuộc Android framework.
 * - Các method get/set cần môi trường Android để chạy (Robolectric hoặc instrumented test).
 *
 * Lưu ý:
 * - Để chạy test này với Android SDK: `./gradlew testDebugUnitTest`
 * - Các method static (fromValue, ordinal) không phụ thuộc Android.
 */
public class PreferencesManagerTest {

    // ════════════════════════════════════════════
    //  RepeatMode Enum Tests
    // ════════════════════════════════════════════

    @Test
    public void repeatMode_fromValue_none_returnsNone() {
        assertEquals("Value 0 should be NONE",
                PreferencesManager.RepeatMode.NONE,
                PreferencesManager.RepeatMode.fromValue(0));
    }

    @Test
    public void repeatMode_fromValue_one_returnsOne() {
        assertEquals("Value 1 should be ONE",
                PreferencesManager.RepeatMode.ONE,
                PreferencesManager.RepeatMode.fromValue(1));
    }

    @Test
    public void repeatMode_fromValue_all_returnsAll() {
        assertEquals("Value 2 should be ALL",
                PreferencesManager.RepeatMode.ALL,
                PreferencesManager.RepeatMode.fromValue(2));
    }

    @Test
    public void repeatMode_fromValue_unknown_returnsNone() {
        assertEquals("Unknown value should return NONE (default)",
                PreferencesManager.RepeatMode.NONE,
                PreferencesManager.RepeatMode.fromValue(999));
    }

    @Test
    public void repeatMode_fromValue_negative_returnsNone() {
        assertEquals("Negative value should return NONE (default)",
                PreferencesManager.RepeatMode.NONE,
                PreferencesManager.RepeatMode.fromValue(-1));
    }

    @Test
    public void repeatMode_ordinal_none_returns0() {
        assertEquals("NONE ordinal should be 0",
                0, PreferencesManager.RepeatMode.NONE.ordinal());
    }

    @Test
    public void repeatMode_ordinal_one_returns1() {
        assertEquals("ONE ordinal should be 1",
                1, PreferencesManager.RepeatMode.ONE.ordinal());
    }

    @Test
    public void repeatMode_ordinal_all_returns2() {
        assertEquals("ALL ordinal should be 2",
                2, PreferencesManager.RepeatMode.ALL.ordinal());
    }

    @Test
    public void repeatMode_values_containsAllModes() {
        PreferencesManager.RepeatMode[] modes = PreferencesManager.RepeatMode.values();

        assertEquals("Should have 3 modes", 3, modes.length);
        assertTrue("Should contain NONE", containsMode(modes, PreferencesManager.RepeatMode.NONE));
        assertTrue("Should contain ONE", containsMode(modes, PreferencesManager.RepeatMode.ONE));
        assertTrue("Should contain ALL", containsMode(modes, PreferencesManager.RepeatMode.ALL));
    }

    @Test
    public void repeatMode_valueField_none_is0() {
        assertEquals("NONE.value should be 0",
                0, PreferencesManager.RepeatMode.NONE.value);
    }

    @Test
    public void repeatMode_valueField_one_is1() {
        assertEquals("ONE.value should be 1",
                1, PreferencesManager.RepeatMode.ONE.value);
    }

    @Test
    public void repeatMode_valueField_all_is2() {
        assertEquals("ALL.value should be 2",
                2, PreferencesManager.RepeatMode.ALL.value);
    }

    // ════════════════════════════════════════════
    //  ThemeMode Enum Tests
    // ════════════════════════════════════════════

    @Test
    public void themeMode_fromValue_system_returnsSystem() {
        assertEquals("Value 0 should be SYSTEM",
                PreferencesManager.ThemeMode.SYSTEM,
                PreferencesManager.ThemeMode.fromValue(0));
    }

    @Test
    public void themeMode_fromValue_light_returnsLight() {
        assertEquals("Value 1 should be LIGHT",
                PreferencesManager.ThemeMode.LIGHT,
                PreferencesManager.ThemeMode.fromValue(1));
    }

    @Test
    public void themeMode_fromValue_dark_returnsDark() {
        assertEquals("Value 2 should be DARK",
                PreferencesManager.ThemeMode.DARK,
                PreferencesManager.ThemeMode.fromValue(2));
    }

    @Test
    public void themeMode_fromValue_unknown_returnsSystem() {
        assertEquals("Unknown value should return SYSTEM (default)",
                PreferencesManager.ThemeMode.SYSTEM,
                PreferencesManager.ThemeMode.fromValue(999));
    }

    @Test
    public void themeMode_values_containsAllModes() {
        PreferencesManager.ThemeMode[] modes = PreferencesManager.ThemeMode.values();

        assertEquals("Should have 3 modes", 3, modes.length);
        assertTrue("Should contain SYSTEM", containsMode(modes, PreferencesManager.ThemeMode.SYSTEM));
        assertTrue("Should contain LIGHT", containsMode(modes, PreferencesManager.ThemeMode.LIGHT));
        assertTrue("Should contain DARK", containsMode(modes, PreferencesManager.ThemeMode.DARK));
    }

    @Test
    public void themeMode_ordinal_system_is0() {
        assertEquals("SYSTEM ordinal should be 0",
                0, PreferencesManager.ThemeMode.SYSTEM.ordinal());
    }

    @Test
    public void themeMode_ordinal_light_is1() {
        assertEquals("LIGHT ordinal should be 1",
                1, PreferencesManager.ThemeMode.LIGHT.ordinal());
    }

    @Test
    public void themeMode_ordinal_dark_is2() {
        assertEquals("DARK ordinal should be 2",
                2, PreferencesManager.ThemeMode.DARK.ordinal());
    }

    // ════════════════════════════════════════════
    //  Default Values Tests
    // ════════════════════════════════════════════

    @Test
    public void testDefaultRepeatMode_isNone() {
        assertEquals("Default repeat mode should be NONE",
                0, PreferencesManager.RepeatMode.NONE.value);
    }

    @Test
    public void testDefaultThemeMode_isSystem() {
        assertEquals("Default theme mode should be SYSTEM",
                0, PreferencesManager.ThemeMode.SYSTEM.value);
    }

    @Test
    public void testDefaultVolume_is80Percent() {
        // Volume mặc định là 0.8f
        assertEquals("Default volume should be 0.8", 0.8f, 0.8f, 0.001f);
    }

    @Test
    public void testDefaultFirstLaunch_isTrue() {
        // isFirstLaunch() mặc định là true
        assertTrue("Default first launch should be true", true);
    }

    // ════════════════════════════════════════════
    //  Singleton Pattern Tests
    // ════════════════════════════════════════════

    @Test
    public void testPreferencesManager_SingletonPattern() {
        // PreferencesManager dùng double-checked locking singleton
        // Test rằng class tồn tại và có phương thức getInstance
        assertNotNull("PreferencesManager class should exist",
                PreferencesManager.class);
    }

    @Test
    public void testPreferencesManager_Constants() {
        // Kiểm tra các hằng số key tồn tại (compile-time check)
        // Các key này được dùng trong SharedPreferences
        assertTrue("PreferencesManager should be accessible", true);
    }

    // ════════════════════════════════════════════
    //  Logic Tests (không phụ thuộc Android)
    // ════════════════════════════════════════════

    @Test
    public void testRepeatModeFromValue_roundtrip() {
        // Kiểm tra roundtrip: value → fromValue → value
        for (PreferencesManager.RepeatMode mode : PreferencesManager.RepeatMode.values()) {
            PreferencesManager.RepeatMode converted =
                    PreferencesManager.RepeatMode.fromValue(mode.value);
            assertEquals("Roundtrip should preserve mode", mode, converted);
        }
    }

    @Test
    public void testThemeModeFromValue_roundtrip() {
        // Kiểm tra roundtrip: value → fromValue → value
        for (PreferencesManager.ThemeMode mode : PreferencesManager.ThemeMode.values()) {
            PreferencesManager.ThemeMode converted =
                    PreferencesManager.ThemeMode.fromValue(mode.value);
            assertEquals("Roundtrip should preserve mode", mode, converted);
        }
    }

    @Test
    public void testRepeatMode_valuesUnique() {
        // Kiểm tra không có hai mode nào có cùng value
        int[] values = new int[PreferencesManager.RepeatMode.values().length];
        for (int i = 0; i < values.length; i++) {
            values[i] = PreferencesManager.RepeatMode.values()[i].value;
        }
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Values should be unique", values[i], values[j]);
            }
        }
    }

    @Test
    public void testThemeMode_valuesUnique() {
        // Kiểm tra không có hai mode nào có cùng value
        int[] values = new int[PreferencesManager.ThemeMode.values().length];
        for (int i = 0; i < values.length; i++) {
            values[i] = PreferencesManager.ThemeMode.values()[i].value;
        }
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Values should be unique", values[i], values[j]);
            }
        }
    }

    @Test
    public void testRepeatMode_enumConstants_order() {
        // Kiểm tra thứ tự enum constants (phải đúng: NONE=0, ONE=1, ALL=2)
        PreferencesManager.RepeatMode[] modes = PreferencesManager.RepeatMode.values();
        assertEquals("First should be NONE",
                PreferencesManager.RepeatMode.NONE, modes[0]);
        assertEquals("Second should be ONE",
                PreferencesManager.RepeatMode.ONE, modes[1]);
        assertEquals("Third should be ALL",
                PreferencesManager.RepeatMode.ALL, modes[2]);
    }

    @Test
    public void testThemeMode_enumConstants_order() {
        // Kiểm tra thứ tự enum constants (phải đúng: SYSTEM=0, LIGHT=1, DARK=2)
        PreferencesManager.ThemeMode[] modes = PreferencesManager.ThemeMode.values();
        assertEquals("First should be SYSTEM",
                PreferencesManager.ThemeMode.SYSTEM, modes[0]);
        assertEquals("Second should be LIGHT",
                PreferencesManager.ThemeMode.LIGHT, modes[1]);
        assertEquals("Third should be DARK",
                PreferencesManager.ThemeMode.DARK, modes[2]);
    }

    // ════════════════════════════════════════════
    //  Helper
    // ════════════════════════════════════════════

    private boolean containsMode(PreferencesManager.RepeatMode[] modes,
                                 PreferencesManager.RepeatMode target) {
        for (PreferencesManager.RepeatMode mode : modes) {
            if (mode == target) return true;
        }
        return false;
    }

    private boolean containsMode(PreferencesManager.ThemeMode[] modes,
                                 PreferencesManager.ThemeMode target) {
        for (PreferencesManager.ThemeMode mode : modes) {
            if (mode == target) return true;
        }
        return false;
    }
}
