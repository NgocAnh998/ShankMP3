package com.example.nghenhac.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nghenhac.R;
import com.example.nghenhac.sync.FirebaseAuthManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity đăng nhập / đăng ký tài khoản.
 *
 * Nguyên lý:
 * - Chế độ "Đăng nhập" (mặc định) và "Đăng ký" (toggle qua TextView).
 * - Sử dụng FirebaseAuthManager để xử lý authentication.
 * - Gọi blocking API trên background thread (ExecutorService).
 * - Tất cả thông báo lỗi đều thân thiện bằng tiếng Việt.
 *
 * Luồng xử lý:
 * 1. User nhập email + password → click nút "Đăng nhập" / "Đăng ký".
 * 2. Validate input (không rỗng, email hợp lệ, password >= 6).
 * 3. Hiển thị ProgressBar, disable nút.
 * 4. Gửi request qua FirebaseAuthManager trên background thread.
 * 5. Thành công → finish() trả về RESULT_OK.
 * 6. Thất bại → hiển thị Snackbar lỗi, enable lại nút.
 * 7. User click "Quên mật khẩu?" → dialog nhập email → gửi reset password.
 * 8. User click "Chưa có tài khoản? Đăng ký" → toggle sang chế độ đăng ký.
 *
 * Input:
 * - Email và password do user nhập.
 *
 * Output:
 * - RESULT_OK nếu đăng nhập/đăng ký thành công.
 * - RESULT_CANCELED nếu user back.
 *
 * Lưu ý:
 * - Activity này khởi động bằng startActivityForResult hoặc Navigation.
 * - Không lưu password ở local (Firebase tự quản lý session).
 * - ProgressBar hiển thị trong suốt thời gian xử lý.
 */
public class LoginActivity extends AppCompatActivity {

    // ── Constants ──
    private static final int MIN_PASSWORD_LENGTH = 6;

    // ── Views ──
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private EditText emailInput;
    private EditText passwordInput;
    private Button actionButton;
    private TextView switchModeText;
    private TextView forgotPasswordText;
    private TextView titleText;
    private ProgressBar progressBar;

    // ── State ──
    private boolean isLoginMode = true;
    private FirebaseAuthManager authManager;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = FirebaseAuthManager.getInstance();
        executor = Executors.newSingleThreadExecutor();

        initViews();
        setupListeners();
        updateUIForMode();
    }

    /**
     * Khởi tạo tất cả view references.
     *
     * Nguyên lý:
     * - findViewById pattern (không dùng ViewBinding/DataBinding).
     * - Tất cả views đều được khởi tạo ở đây để dễ quản lý.
     */
    private void initViews() {
        emailLayout = findViewById(R.id.login_email_layout);
        passwordLayout = findViewById(R.id.login_password_layout);
        emailInput = findViewById(R.id.login_email_input);
        passwordInput = findViewById(R.id.login_password_input);
        actionButton = findViewById(R.id.login_action_button);
        switchModeText = findViewById(R.id.login_switch_mode);
        forgotPasswordText = findViewById(R.id.login_forgot_password);
        titleText = findViewById(R.id.login_title);
        progressBar = findViewById(R.id.login_progress);
    }

    /**
     * Thiết lập tất cả listeners cho các views.
     *
     * Nguyên lý:
     * - actionButton: đăng nhập hoặc đăng ký tuỳ theo mode.
     * - switchModeText: toggle giữa login và register mode.
     * - forgotPasswordText: hiển thị dialog nhập email.
     */
    private void setupListeners() {
        actionButton.setOnClickListener(v -> performAuthAction());
        switchModeText.setOnClickListener(v -> toggleMode());
        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());
    }

    /**
     * Toggle giữa chế độ đăng nhập và đăng ký.
     *
     * Nguyên lý:
     * - Chuyển isLoginMode và cập nhật UI text.
     * - Xoá lỗi cũ và focus vào email input.
     */
    private void toggleMode() {
        isLoginMode = !isLoginMode;
        updateUIForMode();
        emailLayout.setError(null);
        passwordLayout.setError(null);
        emailInput.requestFocus();
    }

    /**
     * Cập nhật UI text dựa trên mode hiện tại.
     *
     * Nguyên lý:
     * - Login mode: title "Đăng nhập", button "Đăng nhập", switch "Chưa có tài khoản? Đăng ký".
     * - Register mode: title "Đăng ký", button "Đăng ký", switch "Đã có tài khoản? Đăng nhập".
     */
    private void updateUIForMode() {
        if (isLoginMode) {
            titleText.setText(R.string.login_title);
            actionButton.setText(R.string.action_login);
            switchModeText.setText(R.string.login_switch_to_register);
            forgotPasswordText.setVisibility(View.VISIBLE);
        } else {
            titleText.setText(R.string.register_title);
            actionButton.setText(R.string.action_register);
            switchModeText.setText(R.string.login_switch_to_login);
            forgotPasswordText.setVisibility(View.GONE);
        }
    }

    /**
     * Thực hiện hành động đăng nhập hoặc đăng ký.
     *
     * Nguyên lý:
     * - Validate input trước khi gửi request.
     * - Chạy blocking API trên background thread.
     * - Cập nhật UI dựa trên kết quả.
     *
     * Luồng xử lý:
     * 1. Lấy email + password từ input.
     * 2. Validate → nếu lỗi hiển thị ngay, không gửi request.
     * 3. Hiển thị loading (progressBar + disable button).
     * 4. ExecutorService.execute → gọi authManager.login() hoặc register().
     * 5. Thành công → runOnUiThread → finish().
     * 6. Thất bại → runOnUiThread → hiển thị lỗi + enable button.
     */
    private void performAuthAction() {
        // Lấy input
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate
        if (!validateInput(email, password)) {
            return;
        }

        // Hiển thị loading
        setLoading(true);

        String finalEmail = email;
        String finalPassword = password;
        executor.execute(() -> {
            try {
                if (isLoginMode) {
                    authManager.login(finalEmail, finalPassword);
                } else {
                    authManager.register(finalEmail, finalPassword);
                }

                // Thành công — quay lại màn hình trước
                runOnUiThread(() -> {
                    setLoading(false);
                    setResult(RESULT_OK);
                    finish();
                });

            } catch (Exception e) {
                // Thất bại — hiển thị lỗi
                runOnUiThread(() -> {
                    setLoading(false);
                    showSnackbar(e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
                });
            }
        });
    }

    /**
     * Validate input email và password.
     *
     * Nguyên lý:
     * - Email không rỗng, chứa @.
     * - Password không rỗng, >= 6 ký tự.
     * - Hiển thị lỗi trực tiếp trên TextInputLayout.
     *
     * Input:
     * @param email    Email cần validate.
     * @param password Password cần validate.
     *
     * Output:
     * @return true nếu hợp lệ, false nếu không.
     */
    private boolean validateInput(String email, String password) {
        boolean valid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Vui lòng nhập email");
            valid = false;
        } else if (!email.contains("@")) {
            emailLayout.setError("Email không hợp lệ");
            valid = false;
        } else {
            emailLayout.setError(null);
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Vui lòng nhập mật khẩu");
            valid = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            passwordLayout.setError("Mật khẩu tối thiểu " + MIN_PASSWORD_LENGTH + " ký tự");
            valid = false;
        } else {
            passwordLayout.setError(null);
        }

        return valid;
    }

    /**
     * Hiển thị dialog quên mật khẩu.
     *
     * Nguyên lý:
     * - AlertDialog với EditText để nhập email.
     * - Gửi email reset password qua FirebaseAuthManager.
     * - Hiển thị thông báo thành công hoặc lỗi.
     *
     * Luồng xử lý:
     * 1. Tạo AlertDialog với EditText email.
     * 2. User click "Gửi" → validate email.
     * 3. Disable dialog buttons → executor.execute → resetPassword().
     * 4. Thành công → Snackbar "Đã gửi email đặt lại mật khẩu".
     * 5. Thất bại → Snackbar lỗi.
     */
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đặt lại mật khẩu");
        builder.setMessage("Nhập email của bạn để nhận link đặt lại mật khẩu:");

        // EditText input
        final EditText input = new EditText(this);
        input.setHint("Email của bạn");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input, 48, 16, 48, 16);

        builder.setPositiveButton("Gửi", null); // Set later to prevent auto-dismiss
        builder.setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override positive button to prevent dismiss on validation error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = input.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !email.contains("@")) {
                input.setError("Email không hợp lệ");
                return;
            }

            // Disable buttons and show progress
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

            executor.execute(() -> {
                try {
                    authManager.resetPassword(email);
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        showSnackbar("Đã gửi email đặt lại mật khẩu đến " + email);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                        showSnackbar(e.getMessage() != null ? e.getMessage() : "Lỗi gửi email");
                    });
                }
            });
        });
    }

    /**
     * Bật/tắt loading state.
     *
     * Nguyên lý:
     * - Khi loading: hiển thị ProgressBar, disable button.
     * - Khi không loading: ẩn ProgressBar, enable button.
     *
     * Input:
     * @param loading true nếu đang xử lý, false nếu không.
     */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        actionButton.setEnabled(!loading);
        actionButton.setText(loading
                ? (isLoginMode ? "Đang đăng nhập…" : "Đang đăng ký…")
                : (isLoginMode ? "Đăng nhập" : "Đăng ký"));
    }

    /**
     * Hiển thị Snackbar thông báo.
     *
     * Nguyên lý:
     * - Snackbar với thời gian LENGTH_LONG.
     * - Tìm root view từ window.decorView.
     *
     * Input:
     * @param message Nội dung thông báo.
     */
    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAnchorView(findViewById(R.id.login_action_button))
                    .show();
        }
    }

    /**
     * Giải phóng executor service khi activity destroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
