# 📋 TODO — NgheNhac Music Player App

> **Danh sách tất cả công việc cần làm để đạt điểm cao nhất**
> Ngôn ngữ: **Java** | Target API: **28–36** | UI: **Material Design 3**
> **Mục tiêu điểm: 7.5 → 9.5+ / 10**

---

## 🎯 Chiến lược tối ưu điểm

| Mức ưu tiên | Tác động điểm | Số tasks | Thời gian dự kiến |
|-------------|--------------|----------|-------------------|
| **P0 — Cấp cứu** | +1.0 ~ +1.5 | 13 tasks | 3-4 ngày |
| **P1 — Quan trọng** | +0.5 ~ +1.0 | 12 tasks | 2-3 ngày |
| **P2 — Củng cố** | +0.3 ~ +0.5 | 11 tasks | 1-2 ngày |
| **P3 — Vươn lên** | +0.2 ~ +0.3 | 8 tasks | 1 ngày |
| **Tổng** | **→ 9.5+** | **44 tasks** | **7-10 ngày** |

---

## 🆘 P0 — Cấp cứu (Cần làm ngay, tác động điểm lớn nhất)

> *Mục tiêu: từ ~7.5 lên ~8.5-9.0*
> *Giảng viên thường đánh giá cao: cloud sync, testing, tính năng hoàn thiện*

### ☁️ Firebase Auth + Login (từ Giai đoạn 5 cũ)

| # | Công việc | Mô tả | File cần tạo/sửa | Điểm cộng | Trạng thái |
|---|-----------|-------|------------------|-----------|------------|
| F1 | Tạo `FirebaseAuthManager` | Email/password login, register, logout, reset password | `sync/FirebaseAuthManager.java` | ⭐⭐⭐ | ✅ |
| F2 | Tạo `LoginActivity` + layout | UI đăng nhập / đăng ký, xử lý loading/error states | `ui/auth/LoginActivity.java`, `res/layout/activity_login.xml` | ⭐⭐⭐ | ✅ |
| F3 | Tạo `FirebaseSyncManager` | Đồng bộ playlist yêu thích lên Firebase Realtime Database | `sync/FirebaseSyncManager.java` | ⭐⭐⭐ | ✅ |
| F4 | Tạo `SongMatcher` | Matching algorithm (title+artist+album) khi đồng bộ giữa thiết bị | `sync/SongMatcher.java` | ⭐⭐⭐ | ✅ |
| F5 | Tạo `SyncWorker` | WorkManager đồng bộ background định kỳ (8-12h) | `sync/SyncWorker.java` | ⭐⭐ | ✅ |
| F6 | Tạo `ProfileActivity` + layout | Xem thông tin user, đăng xuất, đồng bộ thủ công | `ui/auth/ProfileActivity.java`, `res/layout/activity_profile.xml` | ⭐⭐ | ✅ |
| F7 | Cập nhật AndroidManifest + NavGraph | Thêm LoginActivity + ProfileActivity vào navigation | `AndroidManifest.xml`, `nav_graph.xml` | ⭐⭐ | ✅ |
| F8 | Cập nhật SettingsFragment | Thêm mục "Tài khoản" (đăng nhập/profile) + "Đồng bộ" | `ui/settings/SettingsFragment.java`, `strings.xml` | ⭐⭐ | ✅ |

### 🔬 Testing căn bản (từ Giai đoạn 9 cũ)

| # | Công việc | Mô tả | File cần tạo/sửa | Điểm cộng |
|---|-----------|-------|------------------|-----------|
| T1 | Unit test `PlaybackQueue` | Test shuffle, repeat (NONE/ONE/ALL), next, previous | `app/src/test/.../PlaybackQueueTest.java` | ⭐⭐⭐ |
| T2 | Unit test `PreferencesManager` | Test get/set các mode, first launch, clear | `app/src/test/.../PreferencesManagerTest.java` | ⭐⭐⭐ |
| T3 | Unit test `SongMatcher` (sau khi tạo) | Test matching với metadata | `app/src/test/.../SongMatcherTest.java` | ⭐⭐⭐ |

### 🐛 Fix tất cả TODO trong code (10 chỗ)

| # | Công việc | File | Giải pháp | Điểm cộng | Trạng thái |
|---|-----------|------|-----------|-----------|------------|
| D1 | Kết nối SearchFragment với SongRepository | `SearchFragment.java` | Thêm SongAdapter + gọi repository.search() LiveData | ⭐⭐⭐ | ✅ |
| D2 | Apply palette colors vào PlayerActivity background | `PlayerActivity.java` | GradientDrawable vibrant→muted từ PaletteHelper | ⭐⭐ | ✅ |
| D3 | BottomSheetDialog khi click "more" trên bài hát | `SongListFragment.java`, `AlbumDetailActivity.java` | SongBottomSheetDialog (thêm playlist, yêu thích) | ⭐⭐ | ✅ |
| D4 | Dialog rename playlist | `PlaylistListFragment.java` | AlertDialog EditText + repository.updatePlaylist() | ⭐⭐ | ✅ |
| D5 | Dialog chọn bài hát từ thư viện (FAB add) | `PlaylistDetailActivity.java` | Multi-choice AlertDialog + addSongsToPlaylist() | ⭐⭐ | ✅ |
| D6 | Load ảnh bìa playlist (từ bài hát đầu tiên) | `PlaylistAdapter.java`, `PlaylistListFragment.java` | playlistArtMap + getFirstSongAlbumArtUri() query | ⭐ | ✅ |
| D7 | Gắn SettingsAdapter cho SettingsFragment | `SettingsFragment.java` | SettingsAdapter (đã làm ở F8) | ⭐ | ✅ |
| D8 | Xử lý ảnh bìa trong PlaylistDetailActivity | `PlaylistDetailActivity.java` | loadPlaylistCover() từ first song album art | ⭐ | ✅ |

### 🔬 Testing căn bản (từ Giai đoạn 9 cũ)

| # | Công việc | Mô tả | File cần tạo/sửa | Điểm cộng |
|---|-----------|-------|------------------|-----------|
| T1 | Unit test `PlaybackQueue` | Test shuffle, repeat (NONE/ONE/ALL), next, previous | `app/src/test/.../PlaybackQueueTest.java` | ⭐⭐⭐ |
| T2 | Unit test `PreferencesManager` | Test get/set các mode, first launch, clear | `app/src/test/.../PreferencesManagerTest.java` | ⭐⭐⭐ |
| T3 | Unit test `SongMatcher` (sau khi tạo) | Test matching với metadata | `app/src/test/.../SongMatcherTest.java` | ⭐⭐⭐ |

---

## 🔶 P1 — Quan trọng (Tăng chất lượng, gây ấn tượng với giảng viên)

> *Mục tiêu: từ ~8.5 lên ~9.0*

### 🎨 UI/UX Nâng cao

| # | Công việc | Mô tả | Files | Điểm cộng |
|---|-----------|-------|-------|-----------|
| U1 | **Splash Screen** (Android 12+ SplashScreen API) | Màn hình chào khi mở app, animation logo | `res/values/themes.xml`, `AndroidManifest.xml` | ⭐⭐⭐ |
| U2 | **Launcher icon** | Icon ứng dụng chuyên nghiệp (adaptive icon) | `res/mipmap-*/ic_launcher.xml`, `res/drawable/ic_launcher_foreground.xml` | ⭐⭐ |
| U3 | **Empty states** cho tất cả danh sách | UI thông báo "Chưa có bài hát", "Không tìm thấy kết quả" | `fragment_song_list.xml`, `fragment_search.xml`, `fragment_playlist_list.xml`, `activity_playlist_detail.xml` | ⭐⭐ |
| U4 | **Favorite button** trên item_song | Trái tim để thích/bỏ thích trực tiếp từ danh sách | `item_song.xml`, `SongAdapter.java`, `SongDao.java` | ⭐⭐ |
| U5 | **Duration hiển thị** trên item_song | Hiển thị thời lượng bài hát trong danh sách | `item_song.xml`, `SongAdapter.java` | ⭐ |
| U6 | **Loading indicator** khi quét MediaStore | ProgressBar/Shimmer khi app đang scan nhạc | `MainActivity.java`, `fragment_library.xml` | ⭐⭐ |
| U7 | **Thông báo Snackbar** cho tất cả actions | "Đã thêm vào playlist", "Đã xoá", "Đã tải xong"... | Nhiều file | ⭐⭐ |

### 🎵 Playback Experience

| # | Công việc | Mô tả | Files | Điểm cộng |
|---|-----------|-------|-------|-----------|
| P1 | **Now Playing indicator** trong danh sách | Highlight bài hát đang phát trong SongAdapter | `SongAdapter.java`, `SongDiffUtil.java` | ⭐⭐ |
| P2 | **Cập nhật Notification** với album art thật | NotificationBuilder dùng Glide load ảnh bìa | `NotificationBuilder.java`, `MusicService.java` | ⭐⭐ |
| P3 | **Sleep timer** | Tự động dừng nhạc sau N phút | `ui/player/SleepTimerDialog.java`, `MusicPlayer.java`, `MusicService.java` | ⭐⭐⭐ |
| P4 | **Player queue view** | Xem và quản lý danh sách phát sắp tới | `ui/player/QueueActivity.java`, `activity_queue.xml` | ⭐⭐ |

### ⚙️ Cài đặt & Cấu hình

| # | Công việc | Mô tả | Files | Điểm cộng |
|---|-----------|-------|-------|-----------|
| S1 | **SettingsAdapter** hoàn chỉnh | Theme (System/Light/Dark), xoá cache, about, version | `ui/settings/SettingsAdapter.java`, `SettingsFragment.java` | ⭐⭐ |
| S2 | **Cache management UI** | Hiển thị dung lượng cache, nút xoá, progress | `ui/settings/SettingsFragment.java` | ⭐ |
| S3 | **About dialog** | Thông tin app, version, tác giả, license | `ui/settings/AboutDialog.java` | ⭐ |

---

## 🟡 P2 — Củng cố (Nâng cao chất lượng tổng thể)

> *Mục tiêu: từ ~9.0 lên ~9.3*

### 🧪 Testing mở rộng

| # | Công việc | Files | Điểm cộng |
|---|-----------|-------|-----------|
| X1 | Instrumentation test: Navigation flow | `app/src/androidTest/.../NavigationTest.java` | ⭐⭐ |
| X2 | Instrumentation test: Player controls | `app/src/androidTest/.../PlayerTest.java` | ⭐⭐ |
| X3 | Room DAO test với MigrationTestHelper | `app/src/androidTest/.../SongDaoTest.java` | ⭐⭐ |
| X4 | Test OfflineDownloadManager (mock network) | `app/src/test/.../OfflineManagerTest.java` | ⭐ |
| X5 | Test MediaStoreScanner (mock ContentResolver) | `app/src/test/.../MediaStoreScannerTest.java` | ⭐ |

### 🎭 Animations & Transitions

| # | Công việc | Files | Điểm cộng |
|---|-----------|-------|-----------|
| A1 | **Layout transitions** khi chuyển Activity/Fragment | `res/transition/`, ActivityOptions | ⭐⭐ |
| A2 | **Shared element transition** album art → PlayerActivity | Album art "bay" từ danh sách lên player | ⭐⭐⭐ |
| A3 | **Crossfade** khi chuyển bài | Animation nhẹ khi chuyển bài hát | ⭐ |
| A4 | **RecyclerView item animation** | Animation khi list xuất hiện | ⭐ |

### 🎛️ Equalizer & Audio Effects

| # | Công việc | Files | Điểm cộng |
|---|-----------|-------|-----------|
| E1 | **Equalizer dialog** (AudioEffect API) | Bass boost, equalizer presets | `ui/player/EqualizerDialog.java`, `MusicPlayer.java` | ⭐⭐⭐ |
| E2 | **Lưu equalizer settings** | SharedPreferences cho equalizer preset | `PreferencesManager.java` | ⭐ |

---

## 🟢 P3 — Vươn lên (Tạo khác biệt, điểm cộng từ giảng viên)

> *Mục tiêu: từ ~9.3 lên ~9.5+*

### 🌐 Streaming & Online Features

| # | Công việc | Files | Điểm cộng |
|---|-----------|-------|-----------|
| O1 | Kết nối Retrofit API thật | Triển khai server mock hoặc dùng API thật | `MusicApiService.java`, `RetrofitClient.java` | ⭐⭐⭐ |
| O2 | Streaming online songs | Play từ URL qua ExoPlayer + CacheDataSource | `MusicPlayer.java`, `SongRepository.java` | ⭐⭐⭐ |
| O3 | **Search online + local** | SearchFragment kết hợp cả hai nguồn | `SearchFragment.java`, `SearchAdapter.java` | ⭐⭐ |

### 🔒 Bảo mật & An toàn

| # | Công việc | Files | Điểm cộng |
|---|-----------|-------|-----------|
| B1 | **ProGuard rules hoàn chỉnh** | Rules cho tất cả thư viện, test obfuscation | `app/proguard-rules.pro` | ⭐⭐ |
| B2 | **Kiểm tra thread safety** | Review tất cả singleton, synchronized | Nhiều file | ⭐ |
| B3 | **Input validation** | Validate user input ở tất cả dialog | `CreatePlaylistDialog.java`, `LoginActivity.java` | ⭐ |

### 🚀 Hoàn thiện

| # | Công việc | Files | Điểm cộng |
|---|-----------|-------|-----------|
| R1 | **APK optimization** | Resource shrinking, split APK, bundle | `app/build.gradle.kts` | ⭐⭐ |
| R2 | **Xoá unused resources** | Lint unused resources, xoá layouts/strings thừa | Toàn bộ res/ | ⭐ |
| R3 | **Review toàn bộ code** | Refactor code smell, unused imports | Nhiều file | ⭐⭐ |
| R4 | **Viết báo cáo đồ án** | Mô tả kiến trúc, công nghệ, kết quả đạt được | `BaoCao.md` | ⭐⭐⭐ |

---

## ✅ Giai đoạn đã hoàn thành (73/88 tasks cũ)

### Giai đoạn 0: Thiết lập dự án ✅

| # | Công việc | Trạng thái |
|---|-----------|------------|
| 0.1 | Thêm phiên bản thư viện vào `libs.versions.toml` | ✅ |
| 0.2 | Cập nhật `app/build.gradle.kts` với tất cả dependencies | ✅ |
| 0.3 | Cấu hình Firebase (`google-services.json`) | ✅ |
| 0.4 | Cấu hình ProGuard rules | ✅ |
| 0.5 | Tạo cấu trúc package | ✅ |
| 0.6 | Cấu hình BuildConfig cho API keys | ✅ |
| 0.7 | Thêm permissions vào AndroidManifest | ✅ |

### Giai đoạn 1: Tầng Dữ liệu ✅

| # | Công việc | Trạng thái |
|---|-----------|------------|
| 1.1 | SongEntity | ✅ |
| 1.2 | PlaylistEntity | ✅ |
| 1.3 | PlaylistSongCrossRef | ✅ |
| 1.4 | CachedSongEntity | ✅ |
| 1.5 | SongDao | ✅ |
| 1.6 | PlaylistDao | ✅ |
| 1.7 | CachedSongDao | ✅ |
| 1.8 | AppDatabase | ✅ |
| 1.9 | MediaStoreScanner | ✅ |
| 1.10 | MusicContentObserver | ✅ |
| 1.11 | SongRepository | ✅ |
| 1.12 | PlaylistRepository | ✅ |
| 1.13 | PreferencesManager | ✅ |

### Giai đoạn 2: Tầng Phát nhạc ✅

| # | Công việc | Trạng thái |
|---|-----------|------------|
| 2.1 | MusicPlayer hoàn chỉnh | ✅ |
| 2.2 | ExoPlayer config (LoadControl, AudioFocus) | ✅ |
| 2.3 | PlayerEventListener | ✅ |
| 2.4 | PlaybackQueue (repeat/shuffle) | ✅ |
| 2.5 | RetrofitClient | ✅ |
| 2.6 | MusicApiService | ✅ |
| 2.7 | CacheDataSourceFactory | ✅ |
| 2.8 | OfflineDownloadManager | ✅ |

### Giai đoạn 3: Background Service ✅

| # | Công việc | Trạng thái |
|---|-----------|------------|
| 3.1 | Notification Channel | ✅ |
| 3.2 | MusicService (MediaSessionService) | ✅ |
| 3.3 | MediaSessionManager | ✅ |
| 3.4 | NotificationBuilder (MediaStyle) | ✅ |
| 3.5 | Audio Focus | ✅ |
| 3.6 | Manifest registration | ✅ |

### Giai đoạn 4: Giao diện Người dùng ✅

| # | Công việc | Trạng thái |
|---|-----------|------------|
| 4.1 | Navigation Graph | ✅ |
| 4.2 | MainActivity (EdgeToEdge + BottomNav) | ✅ |
| 4.3 | LibraryFragment (TabLayout + ViewPager2) | ✅ |
| 4.4 | fragment_library layout | ✅ |
| 4.5 | SearchFragment (debounce) | ✅ |
| 4.6 | fragment_search layout | ✅ |
| 4.7 | SettingsFragment | ✅ |
| 4.8 | fragment_settings layout | ✅ |
| 4.9 | PlayerActivity (full-screen) | ✅ |
| 4.10 | activity_player layout | ✅ |
| 4.11 | MiniPlayerFragment | ✅ |
| 4.12 | layout_mini_player | ✅ |
| 4.13 | SongAdapter | ✅ |
| 4.14 | AlbumAdapter | ✅ |
| 4.15 | PlaylistAdapter | ✅ |
| 4.16 | SongDiffUtil | ✅ |
| 4.17 | item_song layout | ✅ |
| 4.18 | item_album layout | ✅ |
| 4.19 | item_playlist layout | ✅ |
| 4.20 | PlaylistDetailActivity | ✅ |
| 4.21 | activity_playlist_detail layout | ✅ |
| 4.22 | CreatePlaylistDialog | ✅ |
| 4.23 | AlbumDetailActivity | ✅ |
| 4.24 | activity_album_detail layout | ✅ |
| 4.25 | ArtistListFragment | ✅ |
| 4.26 | ArtistAdapter | ✅ |

### Giai đoạn 6: Hình ảnh & Theme ✅

| # | Công việc | Trạng thái |
|---|-----------|------------|
| 6.1 | ImageLoader (Glide wrapper) | ✅ |
| 6.2 | PaletteHelper | ✅ |
| 6.3 | Theme colors (values/) | ✅ |
| 6.4 | Dark theme (values-night/) | ✅ |

### Giai đoạn 7: Import/Export ✅

| # | Công việc | Trạng thái |
|---|-----------|------------|
| 7.1 | PlaylistExporter (M3U/XML) | ✅ |
| 7.2 | PlaylistImporter (M3U/XML) | ✅ |
| 7.3 | FilePickerUtil (SAF helper) | ✅ |

### Giai đoạn 8: Bảo mật & Tối ưu ✅

| # | Công việc | Trạng thái |
|---|-----------|------------|
| 8.1 | SecurePreferences (EncryptedSharedPreferences) | ✅ |
| 8.2 | BuildConfig API keys | ✅ |
| 8.3 | Lifecycle management | ✅ |
| 8.4 | RecyclerView optimization | ✅ |
| 8.5 | Global error handling (UncaughtExceptionHandler) | ✅ |

---

## 📊 Tổng quan tiến độ mới

| Hạng mục | Tasks | Hoàn thành | Còn lại |
|----------|-------|------------|---------|
| ✅ Đã hoàn thành (cũ) | 73 | 73 | 0 |
| 🆘 **P0 — Cấp cứu** | 19 | 16 | **3** |
| 🔶 **P1 — Quan trọng** | 12 | 3 | **9** |
| 🟡 **P2 — Củng cố** | 11 | 0 | **11** |
| 🟢 **P3 — Vươn lên** | 8 | 0 | **8** |
| **Tổng** | **123** | **92** | **31** |

---

## 📈 Dự kiến điểm theo tiến độ

| Mốc | Tasks hoàn thành | Điểm dự kiến | Ghi chú |
|-----|-----------------|---------------|---------|
| Hiện tại | 92/123 (75%) | **~8.5 / 10** | Firebase + fix TODO + Splash + Launcher + Empty states |
| P1 xong | 98/123 (80%) | **9.0-9.3** | Còn 9 tasks P1: Favorite button, Duration, Loading, Now Playing, Notification, Sleep timer, Queue, Cache, About |
| P1 xong | 98/117 (84%) | **9.0-9.3** | UI nâng cao, settings, animations |
| P2 xong | 109/117 (93%) | **9.3-9.5** | Test mở rộng, equalizer, transitions |
| P3 xong | 117/117 (100%) | **9.5+** | Streaming thật, báo cáo, polish |

---

> 📝 **Khuyến nghị:** Làm theo thứ tự ưu tiên **P0 → P1 → P2 → P3**. Chỉ 13 tasks P0 đã có thể nâng từ 7.5 lên 8.5-9.0!
> 
> **Thời gian tối thiểu để đạt 9.0:** ~4-5 ngày (chỉ làm P0 + P1)
> **Thời gian để đạt 9.5+:** ~7-10 ngày (làm tất cả)
