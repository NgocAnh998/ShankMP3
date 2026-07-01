package com.example.nghenhac.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nghenhac.R;
import com.example.nghenhac.sync.FirebaseAuthManager;
import com.example.nghenhac.sync.FirebaseSyncManager;
import com.example.nghenhac.sync.SyncWorker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity hiển thị thông tin tài khoản người dùng.
 *
 * Nguyên lý:
 * - Hiển thị email, UID của user đang đăng nhập.
 * - Cho phép đăng xuất, đồng bộ thủ công.
 * - Hiển thị trạng thái đồng bộ (lần cuối, kết quả).
 *
 * Luồng xử lý:
 * 1. Activity mở → load thông tin user từ FirebaseAuthManager.
 * 2. User click "Đồng bộ ngay" → chạy sync trên background thread → Snackbar kết quả.
 * 3. User click "Đăng xuất" → AlertDialog xác nhận → logout → quay về LoginActivity.
 * 4. User back → quay về Settings.
 *
 * Lưu ý:
 * - Activity được khởi động từ SettingsFragment.
 * - Nếu user chưa đăng nhập, chuyển hướng về LoginActivity.
 */
public class ProfileActivity extends AppCompatActivity {

    // ── Views ──
    private TextView emailText;
    private TextView uidText;
    private TextView syncStatusText;
    private MaterialButton syncButton;
    private MaterialButton logoutButton;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private View profileContent;
    private View loginPrompt;

    private FirebaseAuthManager authManager;
    private FirebaseSyncManager syncManager;
    private ExecutorService executor;
    private ActivityResultLauncher<Intent> loginLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authManager = FirebaseAuthManager.getInstance();
        syncManager = FirebaseSyncManager.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        // Đăng ký ActivityResultLauncher (thay thế startActivityForResult deprecated)
        loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        updateUI();
                        showSnackbar("Đăng nhập thành công");
                    }
                });

        initViews();
        updateUI();
    }

    /**
     * Khởi tạo tất cả view references.
     */
    private void initViews() {
        emailText = findViewById(R.id.profile_email);
        uidText = findViewById(R.id.profile_uid);
        syncStatusText = findViewById(R.id.profile_sync_status);
        syncButton = findViewById(R.id.profile_sync_button);
        logoutButton = findViewById(R.id.profile_logout_button);
        loginButton = findViewById(R.id.profile_login_button);
        progressBar = findViewById(R.id.profile_progress);
        profileContent = findViewById(R.id.profile_content);
        loginPrompt = findViewById(R.id.profile_login_prompt);

        // Action listeners
        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.profile_back_button))
                .setNavigationOnClickListener(v -> finish());
        syncButton.setOnClickListener(v -> performManualSync());
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            loginLauncher.launch(intent);
        });
    }

    /**
     * Cập nhật UI dựa trên trạng thái đăng nhập.
     *
     * Nguyên lý:
     * - Đã đăng nhập: hiển thị profile content (email, UID, sync, logout button).
     * - Chưa đăng nhập: hiển thị login prompt (button "Đăng nhập").
     */
    private void updateUI() {
        if (authManager.isLoggedIn()) {
            profileContent.setVisibility(View.VISIBLE);
            loginPrompt.setVisibility(View.GONE);

            emailText.setText(authManager.getUserEmail());
            uidText.setText("ID: " + (authManager.getUserId() != null ? authManager.getUserId() : "—"));
            syncStatusText.setText("Đồng bộ tự động: mỗi 8 giờ");
        } else {
            profileContent.setVisibility(View.GONE);
            loginPrompt.setVisibility(View.VISIBLE);
        }
    }

    // Xoá onActivityResult — dùng loginLauncher thay thế

    /**
     * Thực hiện đồng bộ thủ công.
     *
     * Nguyên lý:
     * - Chạy FirebaseSyncManager.fullSync() trên background thread.
     * - Hiển thị loading khi đang đồng bộ.
     * - Snackbar kết quả khi hoàn thành.
     */
    private void performManualSync() {
        if (!authManager.isLoggedIn()) {
            showSnackbar("Vui lòng đăng nhập để đồng bộ");
            return;
        }

        setLoading(true);
        executor.execute(() -> {
            try {
                FirebaseSyncManager.SyncResult result = syncManager.fullSync(this);
                runOnUiThread(() -> {
                    setLoading(false);
                    String message = "Đồng bộ thành công: "
                            + result.getPlaylistsUploaded() + " playlist upload, "
                            + result.getFavoritesDownloaded() + " yêu thích download";
                    showSnackbar(message);
                    syncStatusText.setText("Lần đồng bộ cuối: " + java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date()));
                });
            } catch (FirebaseSyncManager.SyncException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showSnackbar("Đồng bộ thất bại: " + e.getMessage());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showSnackbar("Lỗi: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Hiển thị dialog xác nhận đăng xuất.
     *
     * Nguyên lý:
     * - AlertDialog với message xác nhận.
     * - Nếu user xác nhận: logout → huỷ sync schedule → quay về login state.
     */
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.action_logout, (dialog, which) -> performLogout())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * Thực hiện đăng xuất.
     *
     * Nguyên lý:
     * - Huỷ lịch đồng bộ background.
     * - Gọi FirebaseAuthManager.logout().
     * - Cập nhật UI về trạng thái chưa đăng nhập.
     */
    private void performLogout() {
        SyncWorker.cancelSync(this);
        authManager.logout();
        updateUI();
        showSnackbar("Đã đăng xuất");
    }

    /**
     * Bật/tắt loading state.
     */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        syncButton.setEnabled(!loading);
        syncButton.setText(loading ? "Đang đồng bộ…" : "Đồng bộ ngay");
    }

    /**
     * Hiển thị Snackbar thông báo.
     */
    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}