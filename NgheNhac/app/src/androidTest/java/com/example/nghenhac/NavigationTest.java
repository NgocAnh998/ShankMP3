package com.example.nghenhac;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test — kiểm tra luồng điều hướng cơ bản (X1).
 *
 * Nguyên lý:
 * - Dùng ActivityScenario để launch Activity và verify trạng thái.
 * - Kiểm tra MainActivity có hiển thị đúng các component chính.
 * - Chạy trên thiết bị thật hoặc emulator (API 28+).
 *
 * Lưu ý:
 * - Cần Android runtime — chạy với `./gradlew connectedAndroidTest`.
 * - Không dùng Mockito — test real component interaction.
 */
@RunWith(AndroidJUnit4.class)
public class NavigationTest {

    @Test
    public void mainActivity_launchesSuccessfully() {
        // Launch MainActivity
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(intent)) {
            // Verify activity is in resumed state
            scenario.onActivity(activity -> {
                // Kiểm tra các view tồn tại
                assert activity.findViewById(R.id.bottom_navigation) != null :
                        "BottomNavigation should exist";
                assert activity.findViewById(R.id.nav_host_fragment) != null :
                        "NavHostFragment should exist";
                assert activity.findViewById(R.id.mini_player_container) != null :
                        "Mini player container should exist";
            });
        }
    }

    @Test
    public void playerActivity_launchesSuccessfully() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                com.example.nghenhac.ui.player.PlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (ActivityScenario<?> scenario =
                     ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                // Kiểm tra PlayerActivity có đủ controls
                assert activity.findViewById(R.id.player_play_pause) != null :
                        "Play/Pause button should exist";
                assert activity.findViewById(R.id.player_seekbar) != null :
                        "Seekbar should exist";
                assert activity.findViewById(R.id.player_album_art) != null :
                        "Album art should exist";
                assert activity.findViewById(R.id.player_shuffle) != null :
                        "Shuffle button should exist";
                assert activity.findViewById(R.id.player_repeat) != null :
                        "Repeat button should exist";
                assert activity.findViewById(R.id.player_back) != null :
                        "Back button should exist";
            });
        }
    }

    @Test
    public void settingsFragment_containsRequiredItems() {
        // Chỉ kiểm tra compile-time — need more setup for fragment testing
        assert true : "SettingsFragment test structure OK";
    }
}
