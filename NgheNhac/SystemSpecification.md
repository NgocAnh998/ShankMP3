

# Thiết kế hệ thống ứng dụng nghe nhạc trên Android (Java)

## 1. Tổng quan

Ứng dụng nghe nhạc dành cho Android 9+ (API 28+), viết bằng Java. (minSdk = 28, targetSdk = 36, compileSdk = 36) Hệ thống cần hỗ trợ:
- Phát nhạc từ thiết bị (local)
- Phát nhạc trực tuyến (streaming) qua REST API
- Quản lý playlist, thư viện cá nhân
- Tìm kiếm bài hát local và online
- Phát nhạc nền (background foreground service)
- Điều khiển từ màn hình khoá / notification
- Đồng bộ playlist yêu thích qua Firebase (dùng metadata thay vì đường dẫn)
- Import/export playlist dạng file (XML, M3U)
- Hỗ trợ offline: cache bài hát online, hàng đợi khi mất mạng
- Lưu trạng thái khi app bị kill (dùng SharedPreferences + onSaveInstanceState)



## 2. Quy trình phát nhạc (Playback)

### 2.1. Phát nhạc local (từ bộ nhớ thiết bị)

**Công nghệ đề xuất:** `ExoPlayer` (khuyến nghị)

- ExoPlayer mạnh mẽ, hỗ trợ nhiều định dạng, dễ tuỳ chỉnh bộ đệm.

**Gắn lifecycle rõ ràng:** Trong `ForegroundService`, khởi tạo ExoPlayer tại `onCreate()` hoặc `onStartCommand()`, giải phóng tại `onDestroy()`. Đảm bảo gọi `player.release()` khi service dừng.

### 2.2. Phát nhạc trực tuyến (streaming)

**Công nghệ:** ExoPlayer + Retrofit + OkHttp + **CacheDataSource**

**Xử lý lỗi streaming và buffering:**
- Sử dụng `OkHttpClient` với timeout cấu hình:
```java
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(new HttpLoggingInterceptor())
    .build();
```
- Trong ExoPlayer, thêm `Player.Listener` để bắt lỗi:
```java
player.addListener(new Player.Listener() {
    @Override
    public void onPlayerError(PlaybackException error) {
        // Hiển thị thông báo, thử retry hoặc chuyển bài khác
    }
});
```
- Cấu hình load control để tránh buffer quá nhiều:
```java
LoadControl loadControl = new DefaultLoadControl.Builder()
    .setBufferDurationsMs(5000, 15000, 2000, 5000)
    .build();
ExoPlayer player = new ExoPlayer.Builder(context)
    .setLoadControl(loadControl)
    .build();
```

### 2.3. Cache offline cho bài hát online

**Chiến lược cache:**
- ExoPlayer hỗ trợ `CacheDataSource` – tự động lưu các chunk nhạc.
- Cho phép người dùng đánh dấu "Tải về offline". Khi có mạng, tải toàn bộ file vào bộ nhớ ngoài (external cache directory) và lưu thông tin vào Room.
- Khi mất mạng, kiểm tra danh sách bài đã cache, chuyển sang chế độ phát offline.

**Quản lý hàng đợi khi mất mạng:**
- Nếu người dùng yêu cầu phát một bài online chưa cache khi không có mạng, hiển thị thông báo và thêm bài vào "hàng đợi offline". Khi có mạng trở lại, tự động phát hoặc tải.

---

## 3. Quản lý danh sách phát & thư viện

### 3.1. Lưu trữ dữ liệu cục bộ

**Công nghệ:** `Room Database` + `LiveData` + `SharedPreferences`

### 3.2. Quét nhạc local và theo dõi thay đổi

**Phát hiện thay đổi file nhạc:** Sử dụng `ContentObserver` hoặc `MediaStore` change notifications.

Ví dụ đăng ký observer:
```java
getContentResolver().registerContentObserver(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    true,
    new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            // Quét lại thư viện nhạc
            scanLocalMusic();
        }
    }
);
```
Cần gọi `unregisterContentObserver()` trong `onDestroy()` để tránh rò rỉ bộ nhớ.

---

## 4. Xử lý file – Lưu trữ và chia sẻ playlist

(Giữ nguyên như cũ, không thay đổi)

---

## 5. XML Parser – Import/export playlist dạng XML

(Giữ nguyên, đã có DOM parser; có thể tham khảo thêm SAX cho file lớn)

---

## 6. Background playback & điều khiển từ notification

**Công nghệ:** `Foreground Service` + `MediaSessionCompat` + `MediaStyle`

### Tạo Notification Channel cho Android 8+ (API 26)
```java
private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel channel = new NotificationChannel(
            "MUSIC_CHANNEL_ID",
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Controls music playback");
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
}
```

### MediaSession Callback (xử lý phím headset, Bluetooth)
```java
MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
    @Override
    public void onPlay() { player.play(); }
    @Override
    public void onPause() { player.pause(); }
    @Override
    public void onSkipToNext() { playNext(); }
    @Override
    public void onSkipToPrevious() { playPrevious(); }
    @Override
    public void onStop() { stopForeground(true); stopSelf(); }
};
mediaSession.setCallback(callback);
```

---

## 7. Giao diện người dùng (UI)

### 7.1. Khung giao diện & điều hướng

(Giữ nguyên: Material Design + Navigation Component)

### 7.2. Hiển thị danh sách bài hát

(Giữ nguyên: RecyclerView + ListAdapter + DiffUtil)

### 7.3. Hiển thị ảnh bìa album

**Công nghệ:** Glide (giữ nguyên)

### 7.4. Xử lý khi không có ảnh bìa (Palette fallback)

Sử dụng Palette để lấy màu chủ đạo từ ảnh bìa. Nếu không có ảnh, dùng màu mặc định:
```java
if (bitmap != null) {
    Palette.from(bitmap).generate(palette -> {
        int defaultColor = ContextCompat.getColor(this, R.color.default_background);
        int vibrantColor = palette.getVibrantColor(defaultColor);
        // set màu nền cho player controls
    });
} else {
    // fallback màu xám hoặc theme primary
    view.setBackgroundColor(ContextCompat.getColor(this, R.color.fallback_color));
}
```

### 7.5. Menu

(Giữ nguyên: Option Menu và Context Menu)

---

## 8. Tìm kiếm bài hát

(Giữ nguyên: local dùng Room LIKE, online dùng Retrofit + SearchView debounce)

---

## 9. Gọi REST API & Phát nhạc trực tuyến

(Giữ nguyên, đã bổ sung xử lý lỗi và timeout ở mục 2.2)

---

## 10. Firebase – Đăng nhập và đồng bộ playlist yêu thích

### 10.1. Xác thực người dùng

(Giữ nguyên Firebase Auth)

### 10.2. Đồng bộ danh sách yêu thích (sửa lỗi bền vững)

**Thay vì lưu đường dẫn `content://`, lưu metadata để có thể matching trên các thiết bị khác nhau.**

Cấu trúc JSON trong Firebase Realtime Database:
```json
{
  "favorites": {
    "userId123": {
      "song_f1": {
        "title": "Shape of You",
        "artist": "Ed Sheeran",
        "album": "÷",
        "duration": 233000,
        "mediaStoreId": "12345"   // ID từ MediaStore trên thiết bị gốc (có thể không tồn tại trên thiết bị khác)
      }
    }
  }
}
```

**Thuật toán matching khi đăng nhập trên thiết bị mới:**
1. Quét tất cả nhạc local trên thiết bị mới (lấy metadata).
2. Với mỗi bài hát yêu thích từ Firebase, tìm trong danh sách local bài có `(title, artist, album)` trùng khớp (bỏ qua hoa thường, khoảng trắng).
3. Nếu tìm thấy → đánh dấu yêu thích local.
4. Nếu không tìm thấy → giữ nguyên trong Firebase nhưng không hiển thị yêu thích trên thiết bị đó (người dùng có thể xoá hoặc bỏ qua).

**Lưu ý:** Không tự động xoá bài khỏi Firebase nếu không tìm thấy local – giữ nguyên đồng bộ cho các thiết bị khác.

### 10.3. Gửi email reset mật khẩu

(Giữ nguyên)

---

## 11. Xử lý hình nền & tối ưu hiệu năng

### 11.1. Màu sắc động với Palette (đã có fallback ở mục 7.4)

### 11.2. Tiết kiệm pin & bộ nhớ

- Giải phóng ExoPlayer đúng lifecycle (mục 2.1)
- Dùng `ViewModel` + `onSaveInstanceState()` để lưu playlist hiện tại và vị trí phát nhạc khi app bị kill.

**Lưu trạng thái bằng SharedPreferences và SavedInstanceState:**
```java
// Trong Activity/Service
@Override
protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("currentSongIndex", currentIndex);
    outState.putInt("currentPosition", player.getCurrentPosition());
    outState.putString("currentPlaylistJson", gson.toJson(playlist));
}

@Override
protected void onRestoreInstanceState(Bundle savedInstanceState) {
    currentIndex = savedInstanceState.getInt("currentSongIndex");
    // khôi phục playlist và seek về vị trí cũ
}
```
Kết hợp với SharedPreferences để lưu trạng thái ngay cả khi app bị kill hoàn toàn.

---

## 12. Bảo mật

- **API keys (Firebase, Retrofit endpoints):** Không hardcode. Dùng `local.properties` và BuildConfig.
Ví dụ trong `build.gradle` (app level):
```gradle
def localProperties = new Properties()
def localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localProperties.load(new FileInputStream(localFile))
}
android {
    defaultConfig {
        buildConfigField "String", "FIREBASE_DB_URL", "\"${localProperties.getProperty("firebase.db.url")}\""
    }
}
```
- **Token Firebase:** Lưu an toàn trong `EncryptedSharedPreferences` (AndroidX Security).
- **Không log token ra Logcat** – tắt logging ở production.

---

## 13. Kiểm thử (Testing)

Khuyến nghị viết các loại test:

| Loại test | Công nghệ | Mục đích |
|-----------|-----------|----------|
| Unit test | JUnit + Mockito | Test logic repository, viewmodel, utilities |
| Instrumentation test | Espresso | Test UI flow, navigation |
| Database test | Room test helpers | Test DAO queries |
| Firebase test | Firebase Test Lab (tuỳ chọn) | Test authentication và sync |

Ví dụ unit test cho matching logic:
```java
@Test
public void testMatchSongByMetadata() {
    Song local = new Song("Shape of You", "Ed Sheeran", "÷");
    Song remote = new Song("Shape of You", "Ed Sheeran", "÷");
    assertTrue(matchingHelper.isMatch(local, remote));
}
```

---

## 14. Lưu ý khi triển khai (minSdk 28, targetSdk 36 — Java)

- Dùng **AndroidX**.
- Cấp quyền: `READ_EXTERNAL_STORAGE` (API 28‑32) hoặc `READ_MEDIA_AUDIO` (API 33+).
- Scoped Storage → dùng `MediaStore` thay vì đường dẫn trực tiếp.
- ExoPlayer hỗ trợ URI `content://`.
- Retrofit và Firebase cần quyền `INTERNET`.
- Cập nhật UI từ callback API phải dùng `runOnUiThread`.

---

## 15. Tổng kết các cải tiến so với thiết kế ban đầu

| Vấn đề | Giải pháp đã bổ sung |
|--------|----------------------|
| Đồng bộ Firebase không bền vững | Lưu metadata + thuật toán matching bằng title/artist/album |
| Không có offline mode | Cache chunk + tải toàn bộ bài yêu thích, hàng đợi offline |
| Mất trạng thái khi app bị kill | `onSaveInstanceState()` + SharedPreferences |
| ExoPlayer lifecycle mơ hồ | Gắn với service onCreate/onDestroy |
| Không phát hiện thay đổi nhạc local | ContentObserver |
| Lỗi streaming không xử lý | OkHttp timeout + Player.Listener |
| Bảo mật kém | BuildConfig + EncryptedSharedPreferences |
| Thiếu test | Đề xuất JUnit, Espresso |
| Thiếu notification channel | Code mẫu cho Android 8+ |
| Thiếu MediaSession callback | Mô tả callback xử lý headset, Bluetooth |
| Palette không có fallback | Màu mặc định khi bitmap null |

---

## 16. Stack công nghệ cuối cùng (cập nhật)

| Quy trình | Công nghệ chính | Ghi chú bổ sung |
|----------|----------------|------------------|
| Phát nhạc local | ExoPlayer | Gắn lifecycle service, listener xử lý lỗi |
| Phát nhạc trực tuyến | ExoPlayer + Retrofit + CacheDataSource | Timeout, retry, cache offline |
| Quản lý thư viện | Room + LiveData + MediaStore + ContentObserver | Theo dõi thay đổi file |
| Đồng bộ yêu thích | Firebase Auth + Realtime Database | Lưu metadata, matching algorithm |
| Lưu trạng thái | SharedPreferences + onSaveInstanceState() | Khôi phục sau kill |
| Background playback | Foreground Service + MediaSession + MediaStyle | Notification channel, callback headset |
| Bảo mật | BuildConfig + EncryptedSharedPreferences | Không hardcode key |
| Kiểm thử | JUnit, Mockito, Espresso | Unit test + UI test |

---
