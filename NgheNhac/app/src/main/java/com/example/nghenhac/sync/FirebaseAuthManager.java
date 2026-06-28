package com.example.nghenhac.sync;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutionException;

/**
 * Quản lý xác thực Firebase — Email/Password authentication.
 *
 * Nguyên lý:
 * - Singleton pattern với FirebaseAuth instance.
 * - Cung cấp các phương thức đồng bộ (blocking) — caller phải chạy trên background thread.
 * - Xử lý exception dựa trên kiểu exception (FirebaseAuthException subclasses) thay vì string matching.
 * - Không tự quản lý thread pool — caller chịu trách nhiệm về threading.
 *
 * Luồng xử lý:
 * 1. App khởi động → kiểm tra FirebaseAuth.getInstance().getCurrentUser().
 * 2. User đăng nhập → login() → FirebaseAuth.signInWithEmailAndPassword().
 * 3. User đăng ký → register() → FirebaseAuth.createUserWithEmailAndPassword().
 * 4. User đăng xuất → logout() → FirebaseAuth.signOut().
 * 5. User quên mật khẩu → resetPassword() → FirebaseAuth.sendPasswordResetEmail().
 *
 * Input:
 * - Email và password do user nhập.
 *
 * Output:
 * - AuthResult chứa thông tin user nếu thành công.
 * - Exception với message mô tả lỗi nếu thất bại.
 *
 * Lưu ý:
 * - Tất cả phương thức đều blocking — gọi từ background thread.
 * - Tasks.await() throw ExecutionException wrapping FirebaseAuthException khi thất bại.
 * - Xử lý lỗi dựa trên exception class thay vì string matching (SDK-proof).
 * - Email/password được validate ở tầng UI trước khi gọi.
 */
public class FirebaseAuthManager {

    private static final String TAG = "FirebaseAuthManager";
    private static volatile FirebaseAuthManager instance;

    private final FirebaseAuth firebaseAuth;

    // ── Singleton ──

    private FirebaseAuthManager() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public static FirebaseAuthManager getInstance() {
        if (instance == null) {
            synchronized (FirebaseAuthManager.class) {
                if (instance == null) {
                    instance = new FirebaseAuthManager();
                }
            }
        }
        return instance;
    }

    // ════════════════════════════════════════════
    //  Public API — blocking methods
    // ════════════════════════════════════════════

    /**
     * Đăng nhập bằng email và password.
     *
     * Nguyên lý:
     * - Gửi request lên Firebase Authentication.
     * - Nếu thành công, Firebase tự động duy trì session qua token refresh.
     * - User có thể kiểm tra isLoggedIn() sau đó.
     *
     * Luồng xử lý:
     * 1. Gọi FirebaseAuth.signInWithEmailAndPassword().
     * 2. Tasks.await() blocking cho đến khi có kết quả.
     * 3. Trả về AuthResult nếu thành công.
     * 4. Bắt ExecutionException → unwrap FirebaseAuthException → throw Exception với message tiếng Việt.
     *
     * Input:
     * @param email    Email người dùng.
     * @param password Mật khẩu người dùng.
     *
     * Output:
     * @return AuthResult chứa thông tin user đã đăng nhập.
     *
     * Lưu ý:
     * - Gọi từ background thread.
     * - Exception message đã được xử lý để thân thiện với người dùng.
     */
    @NonNull
    public AuthResult login(@NonNull String email, @NonNull String password) throws Exception {
        try {
            Task<AuthResult> task = firebaseAuth.signInWithEmailAndPassword(email, password);
            return Tasks.await(task);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            Log.w(TAG, "Login failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
            throw new Exception(getFriendlyErrorMessage(cause));
        } catch (Exception e) {
            Log.w(TAG, "Login failed: " + e.getMessage());
            throw new Exception(getFriendlyErrorMessage(e));
        }
    }

    /**
     * Đăng ký tài khoản mới bằng email và password.
     *
     * Nguyên lý:
     * - Tạo tài khoản Firebase Authentication mới.
     * - Tự động đăng nhập sau khi đăng ký thành công.
     *
     * Luồng xử lý:
     * 1. Gọi FirebaseAuth.createUserWithEmailAndPassword().
     * 2. Tasks.await() blocking cho đến khi có kết quả.
     * 3. Trả về AuthResult nếu thành công.
     * 4. Bắt ExecutionException → unwrap FirebaseAuthException → throw Exception với message tiếng Việt.
     *
     * Input:
     * @param email    Email người dùng.
     * @param password Mật khẩu (tối thiểu 6 ký tự).
     *
     * Output:
     * @return AuthResult chứa thông tin user mới tạo.
     *
     * Lưu ý:
     * - Password phải >= 6 ký tự (theo yêu cầu Firebase).
     * - Gọi từ background thread.
     */
    @NonNull
    public AuthResult register(@NonNull String email, @NonNull String password) throws Exception {
        try {
            Task<AuthResult> task = firebaseAuth.createUserWithEmailAndPassword(email, password);
            return Tasks.await(task);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            Log.w(TAG, "Register failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
            throw new Exception(getFriendlyErrorMessage(cause));
        } catch (Exception e) {
            Log.w(TAG, "Register failed: " + e.getMessage());
            throw new Exception(getFriendlyErrorMessage(e));
        }
    }

    /**
     * Đăng xuất tài khoản hiện tại.
     *
     * Nguyên lý:
     * - FirebaseAuth.signOut() xoá session và token hiện tại.
     * - User cần đăng nhập lại để sử dụng các tính năng yêu cầu auth.
     * - Không throw exception (luôn thành công).
     */
    public void logout() {
        firebaseAuth.signOut();
        Log.d(TAG, "User logged out");
    }

    /**
     * Gửi email đặt lại mật khẩu.
     *
     * Nguyên lý:
     * - Firebase gửi email chứa link đặt lại mật khẩu đến địa chỉ email.
     * - User click link để tạo mật khẩu mới.
     * - Không cần user đăng nhập để gọi phương thức này.
     *
     * Luồng xử lý:
     * 1. Gọi FirebaseAuth.sendPasswordResetEmail().
     * 2. Tasks.await() blocking cho đến khi hoàn thành.
     *
     * Input:
     * @param email Email cần gửi link đặt lại mật khẩu.
     *
     * Lưu ý:
     * - Email phải tồn tại trong hệ thống Firebase (không báo lỗi nếu không tồn tại vì lý do bảo mật).
     * - Gọi từ background thread.
     */
    public void resetPassword(@NonNull String email) throws Exception {
        try {
            Task<Void> task = firebaseAuth.sendPasswordResetEmail(email);
            Tasks.await(task);
            Log.d(TAG, "Password reset email sent to: " + email);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            Log.w(TAG, "Password reset failed: " + (cause != null ? cause.getMessage() : e.getMessage()));
            throw new Exception(getFriendlyErrorMessage(cause));
        } catch (Exception e) {
            Log.w(TAG, "Password reset failed: " + e.getMessage());
            throw new Exception(getFriendlyErrorMessage(e));
        }
    }

    // ════════════════════════════════════════════
    //  Query methods
    // ════════════════════════════════════════════

    /**
     * Kiểm tra user đã đăng nhập chưa.
     *
     * Output:
     * @return true nếu có user đang đăng nhập, false nếu chưa.
     */
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Lấy thông tin user hiện tại.
     *
     * Output:
     * @return FirebaseUser hiện tại, null nếu chưa đăng nhập.
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Lấy email của user hiện tại.
     *
     * Output:
     * @return Email của user, "Chưa đăng nhập" nếu chưa có.
     */
    @NonNull
    public String getUserEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getEmail() != null ? user.getEmail() : "Không có email" : "Chưa đăng nhập";
    }

    /**
     * Lấy UID của user hiện tại.
     *
     * Output:
     * @return UID của user, null nếu chưa đăng nhập.
     */
    @Nullable
    public String getUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ════════════════════════════════════════════
    //  Private helpers
    // ════════════════════════════════════════════

    /**
     * Chuyển đổi exception từ Firebase sang message thân thiện với người dùng.
     *
     * Nguyên lý:
     - Firebase trả về các exception subclass cụ thể:
     *   - FirebaseAuthInvalidCredentialsException → email/password sai
     *   - FirebaseAuthInvalidUserException → user không tồn tại/bị disable
     *   - FirebaseAuthUserCollisionException → email đã được sử dụng
     *   - FirebaseAuthWeakPasswordException → password yếu
     - Dùng instanceof để phân loại exception thay vì string matching (bền vững hơn qua các phiên bản SDK).
     *
     * Input:
     * @param throwable Throwable từ Firebase (có thể là null).
     *
     * Output:
     * @return String message thân thiện với người dùng (tiếng Việt).
     */
    private String getFriendlyErrorMessage(Throwable throwable) {
        if (throwable == null) return "Lỗi không xác định";

        // FirebaseAuthWeakPasswordException — subclass của FirebaseAuthInvalidCredentialsException,
        // nên phải check trước
        if (throwable instanceof FirebaseAuthWeakPasswordException) {
            return "Mật khẩu quá yếu (tối thiểu 6 ký tự)";
        }

        if (throwable instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email hoặc mật khẩu không đúng";
        }

        if (throwable instanceof FirebaseAuthInvalidUserException) {
            String errorCode = ((FirebaseAuthInvalidUserException) throwable).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(errorCode)) {
                return "Tài khoản đã bị vô hiệu hoá";
            }
            return "Tài khoản không tồn tại";
        }

        if (throwable instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã được sử dụng";
        }

        // FirebaseAuthException tổng quát — dùng error code
        if (throwable instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) throwable).getErrorCode();
            switch (errorCode) {
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Quá nhiều yêu cầu, vui lòng thử lại sau";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Lỗi kết nối mạng, vui lòng kiểm tra internet";
                case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                    return "Tài khoản đã tồn tại với phương thức đăng nhập khác";
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Phương thức đăng nhập chưa được kích hoạt";
                case "ERROR_INVALID_CREDENTIAL":
                    return "Thông tin đăng nhập không hợp lệ";
                default:
                    return "Lỗi xác thực: " + errorCode;
            }
        }

        // Fallback: exception thông thường (network error, timeout...)
        String message = throwable.getMessage();
        if (message == null) return "Lỗi không xác định";

        if (message.contains("network") || message.contains("Network")) {
            return "Lỗi kết nối mạng, vui lòng kiểm tra internet";
        }
        if (message.contains("timeout") || message.contains("Timeout")) {
            return "Yêu cầu đã hết thời gian, vui lòng thử lại";
        }

        if (message.length() > 100) {
            message = message.substring(0, 100);
        }
        return message;
    }
}
