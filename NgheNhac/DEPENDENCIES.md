# 📦 Tổng hợp Dependencies — NgheNhac Music Player

> **File này liệt kê tất cả thư viện được sử dụng trong dự án, giải thích vai trò của từng thư viện và cách chúng phối hợp với nhau.**
> Ngôn ngữ: **Java** | Build system: **Gradle Kotlin DSL (`.kts`)** | Quản lý phiên bản: **Version Catalog (`libs.versions.toml`)**

---

## 🗂 Mục lục

1. [AndroidX Core](#-androidx-core)
2. [Media3 / ExoPlayer](#-media3--exoplayer)
3. [Room Database](#-room-database)
4. [Network (Retrofit + OkHttp)](#-network-retrofit--okhttp)
5. [Image Loading (Glide)](#-image-loading-glide)
6. [Firebase](#-firebase)
7. [Navigation](#-navigation)
8. [Lifecycle (LiveData + ViewModel)](#-lifecycle-livedata--viewmodel)
9. [Background Work (WorkManager)](#-background-work-workmanager)
10. [Security](#-security)
11. [Splash Screen](#-splash-screen)
12. [Testing](#-testing)
13. [Plugins (Gradle)](#-plugins-gradle)
14. [Cách chúng phối hợp với nhau](#-cách-chúng-phối-hợp-với-nhau)
15. [Cấu trúc file dependency](#-cấu-trúc-file-dependency)

---

## 📱 AndroidX Core

### `androidx.appcompat:appcompat` — **v1.7.1**
- **Vai trò:** Thư viện AndroidX nền tảng, cung cấp backward compatibility cho các tính năng UI từ Android 9 đến 11.
- **Cung cấp:** `AppCompatActivity`, `AlertDialog`, `AppCompatDelegate` (theme), `ActionBar`.
- **Lý do dùng:** Tất cả Activity trong app đều extends `AppCompatActivity`. Cần thiết cho mọi dự án Android.

### `com.google.android.material:material` — **v1.14.0**
- **Vai trò:** Material Design 3 (Material You) components library.
- **Cung cấp:** `BottomNavigationView`, `TabLayout`, `MaterialCardView`, `MaterialButton`, `MaterialAlertDialog`, `Slider`, `BottomSheetDialog`, Material Color Scheme (`Theme.Material3.*`).
- **Lý do dùng:** App sử dụng Material Design 3 làm hệ thống UI chính.

### `androidx.activity:activity` — **v1.13.0**
- **Vai trò:** Activity 1.13+ cung cấp `ComponentActivity`, `ActivityResultRegistry` (launcher API), Edge-to-edge support.
- **Cung cấp:** `registerForActivityResult()`, `ActivityResultContracts`.
- **Lý do dùng:** Cần cho Edge-to-edge (đã dùng trong `MainActivity.java`), xử lý runtime permissions, file picker.

### `androidx.constraintlayout:constraintlayout` — **v2.2.1**
- **Vai trò:** Layout engine linh hoạt cho XML layouts, cho phép tạo UI phức tạp với flat view hierarchy.
- **Cung cấp:** `ConstraintLayout`, `ConstraintSet`, `MotionLayout` (animation).
- **Lý do dùng:** Layout chính cho tất cả màn hình. Hiệu năng tốt hơn các layout lồng nhau.

### `androidx.recyclerview:recyclerview` — **v1.3.1**
- **Vai trò:** View hiệu suất cao cho danh sách cuộn được, thay thế ListView.
- **Cung cấp:** `RecyclerView`, `LinearLayoutManager`, `GridLayoutManager`, `ItemDecoration`, `DiffUtil`.
- **Lý do dùng:** Hiển thị danh sách bài hát, album, playlist. Có `DiffUtil` để cập nhật danh sách hiệu quả.

### `androidx.viewpager2:viewpager2` — **v1.1.0**
- **Vai trờ:** ViewPager hiện đại, thay thế ViewPager cũ, dùng RecyclerView bên trong.
- **Cung cấp:** `ViewPager2`, `TabLayoutMediator` (kết hợp với Material TabLayout).
- **Lý do dùng:** Chuyển tab giữa "Bài hát / Album / Nghệ sĩ" trong `LibraryFragment`.

---

## 🎵 Media3 / ExoPlayer

### `androidx.media3:media3-exoplayer` — **v1.10.1**
- **Vai trò:** Lõi phát nhạc — thay thế ExoPlayer cũ (`com.google.android.exoplayer`).
- **Cung cấp:** `ExoPlayer.Builder`, `Player` interface (play/pause/seek/next/prev), `MediaItem`.
- **Lý do dùng:** Phát nhạc local (từ file/URI) và streaming. Đây là trái tim của ứng dụng.
- **Ghi chú:** Media3 là phiên bản kế thừa chính thức của ExoPlayer, được chuyển vào AndroidX.

### `androidx.media3:media3-exoplayer-hls` — **v1.10.1**
- **Vai trò:** Hỗ trợ phát luồng HLS (HTTP Live Streaming).
- **Cung cấp:** `HlsMediaSource`.
- **Lý do dùng:** Nếu server streaming trả về HLS, module này xử lý việc tải và phát.

### `androidx.media3:media3-ui` — **v1.10.1**
- **Vai trò:** Component UI cho player.
- **Cung cấp:** `PlayerView` (hoặc `StyledPlayerView`) — View tích hợp sẵn play/pause, seekbar, time display.
- **Lý do dùng:** Dùng làm mini-player và full-screen player UI, tiết kiệm công sức custom controls.

### `androidx.media3:media3-session` — **v1.10.1**
- **Vai trò:** Kết nối ExoPlayer với Android MediaSession.
- **Cung cấp:** `MediaSessionService`, `MediaSession`, `MediaController`, `MediaLibraryService`.
- **Lý do dùng:** Cho phép điều khiển nhạc từ notification, lock screen, Bluetooth, Android Auto.

### `androidx.media3:media3-datasource-okhttp` — **v1.10.1**
- **Vai trò:** Cầu nối giữa ExoPlayer và OkHttp để tải dữ liệu mạng.
- **Cung cấp:** `OkHttpDataSource`.
- **Lý do dùng:** Dùng OkHttp (đã config timeout, logging) làm HTTP stack cho ExoPlayer streaming, thay vì DefaultHttpDataSource mặc định.

### `androidx.media:media` — **v1.8.0**
- **Vai trò:** AndroidX Media — cung cấp `MediaSessionCompat`, `MediaStyle` notification.
- **Cung cấp:** `MediaSessionCompat`, `MediaControllerCompat`, `MediaMetadataCompat`, `MediaStyle` (NotificationCompat.MediaStyle).
- **Lý do dùng:** Dùng làm MediaSession "truyền thống" hỗ trợ Android 9+, tương thích với headset/Bluetooth controls.
- **Lưu ý:** Có thể coi như lớp hỗ trợ bổ sung bên cạnh Media3 Session.

---

## 🗄 Room Database

### `androidx.room:room-runtime` — **v2.8.4**
- **Vai trò:** ORM (Object-Relational Mapping) cho SQLite, cho phép thao tác database qua annotation.
- **Cung cấp:** `Room.databaseBuilder()`, `@Entity`, `@Dao`, `@Database`, `@Query`, `@Insert`, `@Update`, `@Delete`.
- **Lý do dùng:** Lưu cache danh sách bài hát, playlist, trạng thái offline. Room an toàn hơn SQLite raw và tự động sinh code.

### `androidx.room:room-compiler` — **v2.8.4** *(annotationProcessor)*
- **Vai trò:** Annotation processor — sinh code implementation cho DAO và Database trong lúc compile.
- **Cung cấp:** Tự động sinh `SongDao_Impl`, `AppDatabase_Impl`.
- **Lý do dùng:** Bắt buộc cho Room hoạt động. Dùng `annotationProcessor` (không phải `implementation`) vì project dùng Java.

### `androidx.room:room-testing` — **v2.8.4** *(androidTestImplementation)*
- **Vai trò:** Helper cho migration tests.
- **Cung cấp:** `MigrationTestHelper`.
- **Lý do dùng:** Test Room database migration khi cập nhật schema.

---

## 🌐 Network (Retrofit + OkHttp)

### `com.squareup.retrofit2:retrofit` — **v2.11.0**
- **Vai trò:** HTTP client type-safe cho Android/Java.
- **Cung cấp:** `Retrofit.Builder()`, interface-based API definitions với annotations (`@GET`, `@POST`, `@Path`, `@Query`).
- **Lý do dùng:** Gọi REST API từ server streaming nhạc. Cho phép define API endpoints rõ ràng trong interface `MusicApiService`.

### `com.squareup.retrofit2:converter-gson` — **v2.11.0**
- **Vai trò:** Converter tự động chuyển JSON response thành Java objects (DTO).
- **Cung cấp:** `GsonConverterFactory`.
- **Lý do dùng:** Tự động parse JSON từ server thành `SongDto` và các model khác.

### `com.squareup.okhttp3:okhttp` — **v4.12.0**
- **Vai trò:** HTTP/HTTPS client hiệu suất cao — là nền tảng cho Retrofit.
- **Cung cấp:** `OkHttpClient`, `Request`, `Response`, caching, connection pooling, timeout, interceptors.
- **Lý do dùng:** Retrofit dùng OkHttp bên trong. Cấu hình timeout, retry, logging interceptor.

### `com.squareup.okhttp3:logging-interceptor` — **v4.12.0**
- **Vai trò:** Log chi tiết request/response HTTP.
- **Cung cấp:** `HttpLoggingInterceptor` (log level: NONE, BASIC, HEADERS, BODY).
- **Lý do dùng:** Debug network calls trong development. Bật/tắt qua `BuildConfig.LOG_HTTP`.

### `com.google.code.gson:gson` — **v2.14.0**
- **Vai trò:** Thư viện serialize/deserialize JSON <-> Java objects.
- **Cung cấp:** `Gson`, `GsonBuilder`, `@SerializedName`, `@Expose`.
- **Lý do dùng:** Parse JSON. Được Retrofit (converter-gson) dùng tự động, và cũng dùng trực tiếp nếu cần parse JSON thủ công.

---

## 🖼 Image Loading (Glide)

### `com.github.bumptech.glide:glide` — **v4.16.0**
- **Vai trò:** Thư viện tải, cache và hiển thị ảnh nhanh cho Android.
- **Cung cấp:** `Glide.with(context).load(url).into(imageView)`, `RequestOptions` (placeholder, error, crop), `DiskCacheStrategy`, `AppGlideModule`.
- **Lý do dùng:** Tải ảnh bìa album từ URI local (MediaStore) hoặc URL (streaming). Xử lý cache, resize, memory management tự động.

### `com.github.bumptech.glide:compiler` — **v4.16.0** *(annotationProcessor)*
- **Vai trò:** Annotation processor cho Glide, sinh code cho custom `AppGlideModule`.
- **Cung cấp:** Sinh `GlideApp` class với custom methods.
- **Lý do dùng:** Tối ưu Glide cho app-specific use cases. Dùng `annotationProcessor` cho Java.

---

## ☁️ Firebase

### `com.google.firebase:firebase-bom` — **v34.14.1**
- **Vai trò:** Bill of Materials (BoM) — tự động quản lý phiên bản tương thích cho tất cả Firebase dependencies.
- **Cách dùng:** Import bằng `implementation(platform(libs.firebase.bom))`. Các thư viện Firebase con không cần chỉ định version.
- **Lý do dùng:** Đảm bảo tất cả Firebase libraries dùng phiên bản tương thích với nhau, tránh xung đột.

### `com.google.firebase:firebase-auth`
- **Vai trò:** Xác thực người dùng.
- **Cung cấp:** `FirebaseAuth`, `createUserWithEmailAndPassword()`, `signInWithEmailAndPassword()`, `signOut()`, `addAuthStateListener()`.
- **Lý do dùng:** Đăng nhập/đăng ký người dùng để đồng bộ playlist, bài hát yêu thích lên cloud.

### `com.google.firebase:firebase-database`
- **Vai trò:** Realtime Database — đồng bộ dữ liệu thời gian thực.
- **Cung cấp:** `FirebaseDatabase`, `DatabaseReference`, `addValueEventListener()`, `setValue()`, `push()`.
- **Lý do dùng:** Đồng bộ playlist yêu thích, danh sách bài hát giữa các thiết bị. Hỗ trợ offline mode (dữ liệu được cache local).

---

## 🧭 Navigation

### `androidx.navigation:navigation-fragment` — **v2.9.8**
- **Vai trò:** Navigation Component — quản lý điều hướng giữa các Fragment.
- **Cung cấp:** `NavHostFragment`, `NavController`, `NavDirections`, `Safe Args`.
- **Lý do dùng:** Quản lý navigation giữa Library, Search, Settings fragments qua BottomNavigationView.

### `androidx.navigation:navigation-ui` — **v2.9.8**
- **Vai trò:** Tích hợp Navigation Component với UI components.
- **Cung cấp:** `NavigationUI.setupWithNavController()` (kết nối BottomNavigationView với NavController), `NavigationUI.setupActionBarWithNavController()`.
- **Lý do dùng:** Tự động cập nhật selected state của BottomNavigation khi chuyển tab.

---

## 🔄 Lifecycle (LiveData + ViewModel)

### `androidx.lifecycle:lifecycle-livedata` — **v2.10.0**
- **Vai trò:** LiveData — container dữ liệu có thể quan sát, tự động cập nhật UI khi dữ liệu thay đổi.
- **Cung cấp:** `LiveData<T>`, `MutableLiveData<T>`, `MediatorLiveData`, `Transformations`.
- **Lý do dùng:** Kết nối dữ liệu từ Room (quan sát được) với UI. Khi database thay đổi, UI tự động cập nhật.

### `androidx.lifecycle:lifecycle-viewmodel` — **v2.10.0**
- **Vai trò:** ViewModel — quản lý dữ liệu UI bền vững qua configuration changes (xoay màn hình).
- **Cung cấp:** `ViewModel`, `ViewModelProvider`, `AndroidViewModel` (có Context).
- **Lý do dùng:** Mỗi Fragment/Activity có ViewModel riêng giữ dữ liệu, tránh mất state khi xoay màn hình.

### `androidx.lifecycle:lifecycle-common-java8` — **v2.10.0**
- **Vai trò:** Lifecycle API cho Java 8.
- **Cung cấp:** `DefaultLifecycleObserver` (interface với default methods), thay thế `LifecycleObserver` cũ (deprecated).
- **Lý do dùng:** Observer lifecycle events (onCreate, onStart, onResume, onPause, onStop, onDestroy) để quản lý player.

---

## 🔧 Background Work (WorkManager)

### `androidx.work:work-runtime` — **v2.11.2**
- **Vai trò:** Thư viện chạy tác vụ background chịu lỗi, có lịch trình.
- **Cung cấp:** `Worker`, `WorkManager`, `PeriodicWorkRequest`, `OneTimeWorkRequest`, `Constraints`, `NetworkType`.
- **Lý do dùng:** 
  - Đồng bộ playlist định kỳ với Firebase (PeriodicWorkRequest, mỗi 8-12 giờ)
  - Tải nhạc offline (OneTimeWorkRequest với Constraints yêu cầu WiFi)
  - Xoá cache cũ (PeriodicWorkRequest)

---

## 🛡 Security

### `androidx.security:security-crypto` — **v1.1.0**
- **Vai trò:** Mã hoá dữ liệu nhạy cảm ở local.
- **Cung cấp:** `EncryptedSharedPreferences`, `MasterKey`, `EncryptedFile`.
- **Lý do dùng:** Lưu token Firebase, thông tin đăng nhập, API keys dưới dạng mã hoá, không plaintext.

---

## 🌟 Splash Screen

### `androidx.core:core-splashscreen` — **v1.0.1**
- **Vai trò:** Splash Screen API (Android 12+) để hiển thị màn hình chào khi khởi động app.
- **Cung cấp:** `SplashScreen` (install via `installSplashScreen()` trong Activity.onCreate).
- **Lý do dùng:** Hiển thị splash screen khi app đang khởi tạo, kiểm tra đăng nhập, load dữ liệu.

---

## 🧪 Testing

### `junit:junit` — **v4.13.2** *(testImplementation)*
- **Vai trò:** Unit testing framework cho Java.
- **Cung cấp:** `@Test`, `@Before`, `@After`, `assertEquals()`, `assertNotNull()`, `assertTrue()`.
- **Lý do dùng:** Unit test cho Player logic, PlaybackQueue, SongMatcher, Repository.

### `androidx.test.ext:junit` — **v1.3.0** *(androidTestImplementation)*
- **Vai trò:** JUnit extension cho Android instrumentation tests.
- **Cung cấp:** `ActivityScenarioRule`, `ApplicationProvider`.
- **Lý do dùng:** Chạy test trên thiết bị thật hoặc emulator.

### `androidx.test.espresso:espresso-core` — **v3.7.0** *(androidTestImplementation)*
- **Vai trò:** UI testing framework — mô phỏng tương tác người dùng.
- **Cung cấp:** `onView()`, `withId()`, `perform(click())`, `check(matches(isDisplayed()))`.
- **Lý do dùng:** Test UI flows: navigation, player controls, search, playlist management.

---

## 🛠 Plugins (Gradle)

### `com.android.application` — **v9.1.1**
- **Vai trò:** Android Gradle Plugin (AGP) — biên dịch code Android thành APK/AAB.
- **Cung cấp:** Tasks: `assembleDebug`, `installDebug`, `lint`, `test`. Cấu hình: `compileSdk`, `buildTypes`, `buildFeatures`.
- **Ghi chú:** Phiên bản 9.1.1 đi kèm với `compileSdk` là 36.

### `com.google.gms.google-services` — **v4.4.2**
- **Vai trò:** Google Services Plugin — xử lý file `google-services.json` và cấu hình Firebase cho app.
- **Cung cấp:** Tự động thêm Firebase project ID, API keys vào BuildConfig. Yêu cầu file `app/google-services.json`.
- **Lưu ý:** Cần tải `google-services.json` từ Firebase Console sau khi tạo project.

---

## 🔗 Cách chúng phối hợp với nhau

### Luồng dữ liệu tổng thể

```
                     ┌──────────────────────────────────┐
                     │          User Interface           │
                     │  (Activity / Fragment / Adapter)  │
                     └──────────┬───────────┬───────────┘
                                │           │
                    LiveData    │    Glide  │
                   (quan sát)   │  (ảnh bìa) │
                                ▼           ▼
                     ┌──────────────────────────────────┐
                     │         ViewModel / LiveData      │
                     │  (quản lý state, dữ liệu cho UI)  │
                     └──────────┬───────────┬───────────┘
                                │           │
                                ▼           ▼
              ┌────────────────────┐  ┌──────────────────┐
              │    Repository      │  │   MusicPlayer    │
              │ (single source of  │  │ (ExoPlayer)      │
              │      truth)        │  │                  │
              └───┬────────────┬───┘  └───┬──────┬───────┘
                  │            │          │      │
                  ▼            ▼          ▼      ▼
           ┌──────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐
           │   Room   │ │MediaStore│ │Retrofit│ │Cache     │
           │ (SQLite) │ │(local)   │ │(stream)│ │DataSource│
           └──────────┘ └──────────┘ └────────┘ └──────────┘
                               │          │
                               ▼          ▼
                        ┌──────────────────────┐
                        │     Firebase Auth     │
                        │   + Realtime Database │
                        └──────────────────────┘
```

### Nhóm chức năng

| Chức năng | Libraries liên quan |
|-----------|-------------------|
| **Phát nhạc local** | ExoPlayer → MediaStore (URI) → Glide (album art) |
| **Streaming** | ExoPlayer → Retrofit/OkHttp → server → CacheDataSource |
| **Cache offline** | CacheDataSource + WorkManager + Room (metadata) |
| **Đồng bộ cloud** | Firebase Auth → Firebase Database → Room (local copy) |
| **Background playback** | MusicService (Foreground) → MediaSession → MediaStyle Notification |
| **Navigation** | BottomNavigationView + Navigation Component (NavHostFragment) |
| **State persistence** | ViewModel + LiveData + SharedPreferences / EncryptedSharedPreferences |
| **Bảo mật** | EncryptedSharedPreferences (token) + BuildConfig (API keys) |

---

## 📂 Cấu trúc file dependency

```
gradle/
  libs.versions.toml        ← Định nghĩa tất cả versions + libraries + plugins
build.gradle.kts            ← Plugin declarations (apply false)
settings.gradle.kts         ← Module include
app/
  build.gradle.kts          ← dependencies { ... } — dùng libs.xxx từ version catalog
  proguard-rules.pro        ← Rules cho ProGuard/R8 (giữ classes không bị obfuscate)
  google-services.json      ← Firebase config (placeholder, cần tải file thật)
```

---
*Cập nhật lần cuối: 12/06/2026 — tương ứng với Giai đoạn 0 (Thiết lập dự án) hoàn thành*
