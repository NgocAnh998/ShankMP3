package com.example.nghenhac;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.nghenhac.data.local.MusicContentObserver;
import com.example.nghenhac.data.repository.SongRepository;
import com.example.nghenhac.R;
import com.example.nghenhac.ui.player.MiniPlayerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

/**
 * MainActivity — cửa sổ chính của ứng dụng NgheNhac.
 *
 * Nguyên lý:
 * - Sử dụng Navigation Component với BottomNavigation.
 * - 3 tab chính: Thư viện (Library), Tìm kiếm (Search), Cài đặt (Settings).
 * - Mini player xuất hiện ở đáy màn hình khi có bài hát đang phát.
 * - EdgeToEdge: nội dung tràn toàn màn hình, padding system bars.
 * - Tự động quét MediaStore khi app khởi động để load nhạc từ thiết bị.
 * - Yêu cầu runtime permission READ_EXTERNAL_STORAGE / READ_MEDIA_AUDIO trên Android 6.0+.
 *
 * Luồng xử lý:
 * 1. App launch → MainActivity.onCreate() → setContentView + EdgeToEdge.
 * 2. checkAndRequestPermissions() → nếu chưa có permission → request.
 * 3. Permission granted → scanMediaStore() → quét MediaStore → insert vào Room.
 * 4. NavHostFragment load start destination (LibraryFragment).
 * 5. User chọn tab → NavigationUI tự động chuyển fragment.
 * 6. Khi có nhạc phát → MusicService start foreground → mini player hiện.
 * 7. MediaStore thay đổi → MusicContentObserver tự động quét lại.
 *
 * Lưu ý:
 * - Nếu user từ chối quyền → hiển thị Snackbar hướng dẫn vào Settings.
 * - MediaStore chỉ được quét sau khi có quyền — tránh SecurityException.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    /** Request code cho storage permission. */
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;

    private NavController navController;
    private MusicContentObserver contentObserver;
    private boolean scanTriggered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install SplashScreen — must be before super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Keep splash visible briefly for a smooth transition
        splashScreen.setKeepOnScreenCondition(() -> false);

        // Setup EdgeToEdge: padding cho system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars());
            Insets navigationBar = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    Math.max(systemBars.bottom, navigationBar.bottom)
            );
            return insets;
        });

        // Setup Navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Setup BottomNavigation với NavController
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (navController != null) {
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        // Thêm MiniPlayerFragment vào container
        if (savedInstanceState == null) {
            MiniPlayerFragment miniPlayerFragment = new MiniPlayerFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.mini_player_container, miniPlayerFragment);
            transaction.commit();
        }

        // Kiểm tra và xin quyền truy cập MediaStore trước khi quét
        checkAndRequestPermissions();
    }

    // ════════════════════════════════════════════
    //  Runtime Permissions
    // ════════════════════════════════════════════

    /**
     * Lấy permission string phù hợp với API level.
     *
     * API 33+ → READ_MEDIA_AUDIO
     * API 28-32 → READ_EXTERNAL_STORAGE
     */
    @NonNull
    private String getStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }

    /**
     * Kiểm tra và xin quyền truy cập storage/audio.
     *
     * Nguyên lý:
     * - Nếu đã có quyền → scanMediaStore() ngay.
     * - Nếu chưa → requestPermissions() → callback onRequestPermissionsResult.
     * - Nếu user đã từ chối trước đó → hiển thị dialog giải thích.
     */
    private void checkAndRequestPermissions() {
        String permission = getStoragePermission();

        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {
            // Đã có quyền → quét nhạc
            scanMediaStore();
        } else {
            // Chưa có quyền → yêu cầu
            if (shouldShowRequestPermissionRationale(permission)) {
                // Giải thích lý do cần quyền rồi mới request
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_storage_title)
                        .setMessage(R.string.permission_storage_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                requestPermissions(
                                        new String[]{permission},
                                        REQUEST_CODE_STORAGE_PERMISSION))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                // Lần đầu request
                requestPermissions(
                        new String[]{permission},
                        REQUEST_CODE_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User đồng ý → quét nhạc
                Log.d(TAG, "Storage permission granted");
                scanMediaStore();
            } else {
                // User từ chối → hiển thị thông báo
                Log.w(TAG, "Storage permission denied");
                Snackbar.make(
                        findViewById(R.id.main),
                        R.string.permission_storage_denied,
                        Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_settings, v -> {
                            // Mở Settings để user bật thủ công
                            android.content.Intent intent =
                                    new android.content.Intent(
                                            android.provider.Settings
                                                    .ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(
                                    android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .show();
            }
        }
    }

    // ════════════════════════════════════════════
    //  MediaStore Scan
    // ════════════════════════════════════════════

    /**
     * Quét MediaStore trong background để load nhạc từ thiết bị.
     *
     * Nguyên lý:
     * - Gọi SongRepository.refreshFromMediaStore() để đồng bộ MediaStore → Room.
     * - Phương thức này chạy trên background thread (executor riêng).
     * - Sau khi insert, Room tự động notify LiveData → UI cập nhật.
     * - ContentObserver theo dõi thay đổi MediaStore → tự động quét lại.
     * - scanTriggered flag đảm bảo chỉ quét một lần (tránh quét lại khi activity recreate).
     */
    private void scanMediaStore() {
        if (scanTriggered) return;
        scanTriggered = true;

        final SongRepository repository = SongRepository.getInstance(this);

        // Đồng bộ MediaStore → Room database
        repository.refreshFromMediaStore(this, new SongRepository.OnRefreshComplete() {
            @Override
            public void onComplete(int songCount) {
                Log.d(TAG, "MediaStore scan complete: " + songCount + " songs loaded");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "MediaStore scan error", e);
            }
        });

        // Đăng ký ContentObserver để tự động quét lại khi có thay đổi
        contentObserver = new MusicContentObserver(this, () -> {
            Log.d(TAG, "MediaStore changed — rescanning...");
            repository.refreshFromMediaStore(MainActivity.this,
                    new SongRepository.OnRefreshComplete() {
                        @Override
                        public void onComplete(int songCount) {
                            Log.d(TAG, "Rescan complete: " + songCount + " songs");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Rescan error", e);
                        }
                    });
        });
        contentObserver.register();
    }

    // ════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy ContentObserver để tránh memory leak
        if (contentObserver != null) {
            contentObserver.destroy();
            contentObserver = null;
        }
    }
}
