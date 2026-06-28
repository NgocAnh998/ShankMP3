package com.example.nghenhac.data.remote;

import com.example.nghenhac.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client cho streaming music API.
 *
 * Nguyên lý:
 * - Cấu hình Retrofit + OkHttp để gọi REST API từ server nhạc.
 * - Base URL được cấu hình qua BuildConfig.BASE_API_URL (từ local.properties).
 * - Hỗ trợ logging HTTP (bật/tắt qua BuildConfig.LOG_HTTP).
 * - Timeouts được cấu hình: connect 10s, read 30s, write 30s.
 *
 * Luồng xử lý:
 * 1. App khởi động → RetrofitClient.init() (gọi từ NgheNhacApp.onCreate()).
 * 2. init() → OkHttpClient với logging interceptor → Retrofit với GsonConverter → MusicApiService.
 * 3. Repository gọi getApiService() → thực hiện API calls.
 * 4. Nếu chưa init → tự động gọi init() (lazy initialization).
 *
 * Lưu ý:
 * - BASE_API_URL mặc định: http://10.0.2.2:8080 (localhost từ Android emulator).
 * - Logging interceptor BODY trong debug, NONE trong release.
 * - Timeout được thiết kế cho streaming: read timeout 30s (cao hơn connect/write).
 */
public class RetrofitClient {

    private static Retrofit retrofit;
    private static MusicApiService apiService;

    /**
     * Khởi tạo Retrofit client.
     *
     * Nguyên lý:
     * - Tạo OkHttpClient với logging interceptor (cấu hình theo debug/release).
     * - Base URL được normalize (thêm "/" cuối nếu thiếu).
     * - GsonConverterFactory cho JSON serialization/deserialization.
     *
     * Có thể gọi nhiều lần — init() chỉ chạy một lần (kiểm tra retrofit != null).
     */
    public static void init() {
        if (retrofit != null) return;

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(
                BuildConfig.LOG_HTTP
                        ? HttpLoggingInterceptor.Level.BODY
                        : HttpLoggingInterceptor.Level.NONE
        );

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        String baseUrl = BuildConfig.BASE_API_URL;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(MusicApiService.class);
    }

    /**
     * Lấy Retrofit instance (lazy init).
     *
     * Output: Retrofit instance (singleton).
     */
    public static Retrofit getInstance() {
        if (retrofit == null) {
            init();
        }
        return retrofit;
    }

    /**
     * Lấy MusicApiService để gọi API (lazy init).
     *
     * Output: MusicApiService instance (singleton).
     */
    public static MusicApiService getApiService() {
        if (apiService == null) {
            init();
        }
        return apiService;
    }
}
