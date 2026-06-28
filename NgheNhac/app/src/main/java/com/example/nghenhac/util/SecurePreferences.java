package com.example.nghenhac.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * EncryptedSharedPreferences wrapper — lưu dữ liệu nhạy cảm (token, user data) an toàn.
 *
 * Nguyên lý:
 * - Dùng EncryptedSharedPreferences (AndroidX Security Crypto Library) để mã hoá AES-256.
 * - MasterKey được tạo và lưu trong Android KeyStore — không thể trích xuất.
 * - Tự động fallback sang SharedPreferences thường nếu không khởi tạo được MasterKey
 *   (VD: thiết bị cũ, không hỗ trợ KeyStore) — kèm warning log.
 *
 * Luồng xử lý:
 * 1. getInstance(context) → tạo MasterKey → mở EncryptedSharedPreferences.
 * 2. getString/putString → đọc/ghi dữ liệu tự động mã hoá.
 * 3. Nếu lỗi MasterKey → fallback sang SharedPreferences không mã hoá + log warning.
 * 4. clear() → xoá toàn bộ dữ liệu (gọi khi logout).
 *
 * Lưu ý:
 * - Không thay thế PreferencesManager — SecurePreferences chỉ dùng cho dữ liệu nhạy cảm.
 * - Gọi masterKey.delete() khi user logout nếu cần xoá hoàn toàn key (optional).
 * - Thread-safe: EncryptedSharedPreferences tự xử lý đồng bộ nội bộ.
 */
public class SecurePreferences {

    private static final String TAG = "SecurePreferences";
    private static final String PREF_NAME = "nghenhac_secure_prefs";

    /** Keys cho dữ liệu nhạy cảm. */
    public static final String KEY_AUTH_TOKEN = "auth_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_DISPLAY_NAME = "user_display_name";
    public static final String KEY_FIREBASE_TOKEN = "firebase_token";

    private static SecurePreferences instance;
    private SharedPreferences prefs;
    private boolean isEncrypted;

    private SecurePreferences(@NonNull Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            isEncrypted = true;
            Log.d(TAG, "EncryptedSharedPreferences initialized successfully");
        } catch (GeneralSecurityException | IOException e) {
            // Fallback: thiết bị không hỗ trợ KeyStore → dùng SharedPreferences thường
            Log.w(TAG, "Cannot create EncryptedSharedPreferences, falling back to plain SharedPreferences", e);
            prefs = context.getApplicationContext()
                    .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            isEncrypted = false;
        }
    }

    public static synchronized SecurePreferences getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new SecurePreferences(context.getApplicationContext());
        }
        return instance;
    }

    /** Kiểm tra xem có đang dùng mã hoá không. */
    public boolean isEncrypted() {
        return isEncrypted;
    }

    // ════════════════════════════════════════════
    //  String
    // ════════════════════════════════════════════

    /**
     * Lưu giá trị String.
     *
     * @param key   Key (dùng hằng số KEY_* trong class này).
     * @param value Giá trị cần lưu (null = xoá key).
     */
    public void putString(@NonNull String key, @Nullable String value) {
        if (value != null) {
            prefs.edit().putString(key, value).apply();
        } else {
            prefs.edit().remove(key).apply();
        }
    }

    /**
     * Lấy giá trị String đã lưu.
     *
     * @param key Key cần đọc.
     * @return Giá trị đã lưu, hoặc null nếu chưa có.
     */
    @Nullable
    public String getString(@NonNull String key) {
        return prefs.getString(key, null);
    }

    // ════════════════════════════════════════════
    //  Long
    // ════════════════════════════════════════════

    /** Lưu giá trị Long. */
    public void putLong(@NonNull String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }

    /** Lấy giá trị Long. Mặc định 0. */
    public long getLong(@NonNull String key) {
        return prefs.getLong(key, 0L);
    }

    // ════════════════════════════════════════════
    //  Boolean
    // ════════════════════════════════════════════

    /** Lưu giá trị Boolean. */
    public void putBoolean(@NonNull String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    /** Lấy giá trị Boolean. Mặc định false. */
    public boolean getBoolean(@NonNull String key) {
        return prefs.getBoolean(key, false);
    }

    // ════════════════════════════════════════════
    //  Auth token utilities
    // ════════════════════════════════════════════

    /** Lưu auth token. */
    public void saveAuthToken(@NonNull String token) {
        putString(KEY_AUTH_TOKEN, token);
    }

    /** Lấy auth token. Trả về null nếu chưa đăng nhập. */
    @Nullable
    public String getAuthToken() {
        return getString(KEY_AUTH_TOKEN);
    }

    /** Lưu refresh token. */
    public void saveRefreshToken(@NonNull String token) {
        putString(KEY_REFRESH_TOKEN, token);
    }

    /** Lấy refresh token. */
    @Nullable
    public String getRefreshToken() {
        return getString(KEY_REFRESH_TOKEN);
    }

    /** Kiểm tra user đã đăng nhập (có auth token). */
    public boolean isLoggedIn() {
        return getAuthToken() != null;
    }

    // ════════════════════════════════════════════
    //  User info
    // ════════════════════════════════════════════

    /** Lưu thông tin user. */
    public void saveUserInfo(@NonNull String userId, @NonNull String email, @Nullable String displayName) {
        putString(KEY_USER_ID, userId);
        putString(KEY_USER_EMAIL, email);
        putString(KEY_USER_DISPLAY_NAME, displayName);
    }

    /** Lấy user ID. */
    @Nullable
    public String getUserId() {
        return getString(KEY_USER_ID);
    }

    /** Lấy email. */
    @Nullable
    public String getUserEmail() {
        return getString(KEY_USER_EMAIL);
    }

    /** Lấy display name. */
    @Nullable
    public String getUserDisplayName() {
        return getString(KEY_USER_DISPLAY_NAME);
    }

    /** Lưu Firebase Cloud Messaging token. */
    public void saveFirebaseToken(@NonNull String token) {
        putString(KEY_FIREBASE_TOKEN, token);
    }

    /** Lấy FCM token. */
    @Nullable
    public String getFirebaseToken() {
        return getString(KEY_FIREBASE_TOKEN);
    }

    // ════════════════════════════════════════════
    //  Clear
    // ════════════════════════════════════════════

    /**
     * Xoá tất cả dữ liệu nhạy cảm (gọi khi logout).
     * Giữ instance để tránh tạo lại MasterKey mỗi lần.
     */
    public void clearAll() {
        prefs.edit().clear().apply();
        Log.d(TAG, "All secure preferences cleared");
    }

    /**
     * Kiểm tra có tồn tại key không.
     */
    public boolean contains(@NonNull String key) {
        return prefs.contains(key);
    }

    /**
     * Xoá một key cụ thể.
     */
    public void remove(@NonNull String key) {
        prefs.edit().remove(key).apply();
    }
}
