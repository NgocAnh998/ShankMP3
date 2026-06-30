# 🎵 Tài Liệu Kỹ Thuật — Hệ Thống Streaming Nhạc NgheNhac

> **Phiên bản:** 1.0  
> **Ngày cập nhật:** 2026-06-28  
> **Ngôn ngữ:** Java (Android, API 28-36)  
> **Player Engine:** Media3 ExoPlayer 1.10.1  
> **Backend:** Node.js/Express (demo) hoặc Spring Boot (chính thức)

---

## Mục lục

1. [Tổng quan kiến trúc Streaming](#1-tổng-quan-kiến-trúc-streaming)
2. [Luồng dữ liệu chi tiết từng component](#2-luồng-dữ-liệu-chi-tiết-từng-component)
3. [Các lỗi đã gặp — Nguyên nhân & Cách fix](#3-các-lỗi-đã-gặp--nguyên-nhân--cách-fix)
4. [Kiến thức nền tảng liên quan](#4-kiến-thức-nền-tảng-liên-quan)
5. [Hướng dẫn vận hành & Debug](#5-hướng-dẫn-vận-hành--debug)

---

## 1. Tổng quan kiến trúc Streaming

### 1.1. Sơ đồ tổng thể

```
┌─────────────────────────────────────────────────────────────────────┐
│                        📱 Android App (NgheNhac)                    │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    UI Layer (Activities/Fragments)            │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │   │
│  │  │ SearchFragment│  │ SongListFrag │  │ PlayerActivity   │   │   │
│  │  │ (tìm kiếm)   │  │ (danh sách)  │  │ (phát toàn màn)  │   │   │
│  │  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘   │   │
│  └─────────┼─────────────────┼───────────────────┼──────────────┘   │
│            │                 │                   │                   │
│  ┌─────────▼─────────────────▼───────────────────▼──────────────┐   │
│  │                   Player Layer                               │   │
│  │  ┌──────────────────────────────────────────────────────┐   │   │
│  │  │  MusicPlayer (Singleton - ExoPlayer wrapper)         │   │   │
│  │  │  ├── ExoPlayer (Engine)                              │   │   │
│  │  │  ├── PlaybackQueue (Repeat/Shuffle)                  │   │   │
│  │  │  ├── PlayerEventListener (Event bus)                 │   │   │
│  │  │  └── CacheDataSourceFactory (Cache layer)            │   │   │
│  │  └──────────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────┘   │
│            │                                                         │
│  ┌─────────▼─────────────────┬──────────────────────────────────┐   │
│  │    Data Layer             │    Service Layer                  │   │
│  │                          │                                    │   │
│  │  ┌────────────────────┐  │  ┌───────────────────────────┐    │   │
│  │  │ SongRepository     │  │  │ MusicService              │    │   │
│  │  │ (Room + LiveData)  │  │  │ (MediaSessionService)     │    │   │
│  │  ├────────────────────┤  │  ├───────────────────────────┤    │   │
│  │  │ RetrofitClient     │  │  │ MediaSession (Bluetooth)  │    │   │
│  │  │ (OkHttp + Gson)    │  │  │ Notification (MediaStyle) │    │   │
│  │  ├────────────────────┤  │  └───────────────────────────┘    │   │
│  │  │ SongMatcher        │  │                                    │   │
│  │  │ (Firebase sync)    │  │                                    │   │
│  │  └────────────────────┘  │                                    │   │
│  └──────────────────────────┴──────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                          │                        ▲
                          │  HTTP/HTTPS            │ Cached chunks
                          ▼                        │
┌─────────────────────────────────────────────────────────────────────┐
│                   🌐 Backend Server                                  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Node.js/Express (backend-node/server.js)                     │   │
│  │  hoặc Spring Boot (backend/)                                  │   │
│  │                                                                │   │
│  │  Endpoints:                                                    │   │
│  │  ├── GET  /songs              → Danh sách bài hát              │   │
│  │  ├── GET  /songs/search?q=    → Tìm kiếm                       │   │
│  │  ├── GET  /songs/:id          → Chi tiết bài hát               │   │
│  │  └── GET  /songs/:id/stream   → Stream URL                    │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  MP3 Audio Files (SoundHelix public domain)                   │   │
│  │  https://www.soundhelix.com/examples/mp3/SoundHelix-Song-*   │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2. Kiến trúc 3 lớp (Three-Layer Architecture)

Hệ thống streaming được tổ chức theo mô hình 3 lớp rõ ràng:

| Layer | Thành phần | Ngôn ngữ | Chức năng |
|-------|-----------|----------|-----------|
| **Frontend** | App Android (Java) | Java + Media3 ExoPlayer | UI, playback, cache local |
| **Backend API** | Node.js/Express hoặc Spring Boot | JavaScript hoặc Java | REST API, dữ liệu bài hát |
| **Content Source** | SoundHelix MP3 (hoặc server tự host) | — | File nhạc thật để stream |

### 1.3. Các thành phần chính trong hệ thống streaming

#### 📡 RetrofitClient + MusicApiService (Tầng Network)

**File:** `data/remote/RetrofitClient.java`, `data/remote/MusicApiService.java`

- **Vai trò:** Cầu nối HTTP giữa app và backend server
- **Công nghệ:** Retrofit 2.11.0 + OkHttp 4.12.0 + Gson 2.14.0
- **Base URL:** Được cấu hình qua `BuildConfig.BASE_API_URL` (đọc từ `local.properties`)
- **OkHttp Timeouts:** connect 10s, read 30s, write 30s
- **Logging interceptor:** Bật `BODY` level khi debug, `NONE` khi release

```java
// RetrofitClient — Cấu hình OkHttp + Retrofit
OkHttpClient okHttpClient = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)  // Streaming cần read timeout cao
    .writeTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(loggingInterceptor)
    .build();

// Retrofit với Gson converter
retrofit = new Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build();
```

#### 🎮 MusicPlayer (Tầng Player)

**File:** `player/MusicPlayer.java`

- **Vai trò:** Singleton wrapper quanh ExoPlayer — trái tim của toàn bộ hệ thống phát nhạc
- **Engine:** Media3 ExoPlayer 1.10.1
- **LoadControl:** Buffer 15s (min) – 50s (max), play start 2.5s, rebuffer 5s
- **Audio Focus:** ExoPlayer tự động request/release khi play/pause
- **Error Handling:** Retry tối đa 3 lần, mỗi lần cách 1s
- **Listener:** PlayerEventListener → forward sự kiện ra UI

```java
// Cấu hình ExoPlayer trong MusicPlayer.initialize()
DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
    .setBufferDurationsMs(15_000, 50_000, 2_500, 5_000)
    .setPrioritizeTimeOverSizeThresholds(true)
    .build();

// CacheDataSourceFactory làm upstream (tự động cache chunk)
CacheDataSource.Factory cacheFactory =
    CacheDataSourceFactory.getInstance(context).getCacheDataSourceFactory();
DefaultMediaSourceFactory mediaSourceFactory =
    new DefaultMediaSourceFactory(cacheFactory);

exoPlayer = new ExoPlayer.Builder(context)
    .setLoadControl(loadControl)
    .setMediaSourceFactory(mediaSourceFactory)
    .setAudioAttributes(audioAttributes, true)
    .build();
```

#### 💾 CacheDataSourceFactory (Tầng Cache)

**File:** `player/CacheDataSourceFactory.java`

- **Vai trò:** Tạo layer cache giữa ExoPlayer và nguồn dữ liệu
- **Dung lượng:** 500MB (LRU evictor)
- **Vị trí cache:** `context.getCacheDir()/media_cache/`
- **Upstream:** `DefaultDataSource.Factory(context, new DefaultHttpDataSource.Factory())`
- **Flag:** `FLAG_IGNORE_CACHE_ON_ERROR` — fallback về stream trực tiếp nếu cache lỗi

> ⚠️ **Lưu ý quan trọng:** Upstream phải dùng `DefaultDataSource.Factory` thay vì `DefaultHttpDataSource.Factory` để hỗ trợ cả `content://` URI (nhạc local) lẫn `http://` URI (streaming online).

```java
// CacheDataSourceFactory — Cấu hình upstream + cache
DataSource.Factory upstreamFactory = new DefaultDataSource.Factory(
    context,
    new DefaultHttpDataSource.Factory()  // fallback cho HTTP streaming
);

this.cacheDataSourceFactory = new CacheDataSource.Factory()
    .setCache(simpleCache)
    .setUpstreamDataSourceFactory(upstreamFactory)
    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
```

#### 🎨 SearchFragment (Tầng UI — kết nối streaming)

**File:** `ui/search/SearchFragment.java`

- **Vai trò:** Giao diện tìm kiếm, kết hợp kết quả local + online
- **Debounce:** 300ms (tránh gọi API quá nhiều khi gõ)
- **Online search:** Luôn chạy song song với local search
- **Error handling:** Toast/Snackbar cho các lỗi: ConnectException, UnknownHostException, HTTP error
- **Thread safety:** `runOnUiThreadSafe()` kiểm tra `isAdded()` trước khi cập nhật UI

#### 🖥️ Backend Server (Tầng Server)

**File (Node.js):** `backend-node/server.js`  
**File (Spring Boot):** `backend/`  

- **Base URL mặc định:** `http://0.0.0.0:8080`
- **Dữ liệu mẫu:** 30 bài hát, 5 thể loại, 10 nghệ sĩ
- **MP3 Source:** SoundHelix public domain music
- **Ảnh bìa:** picsum.photos (random images theo seed)

---

## 2. Luồng dữ liệu chi tiết từng component

### 2.1. Luồng Streaming tổng thể

```
User gõ từ khoá trên SearchFragment
  │
  ▼
[Debounce 300ms] → performSearch(query)
  │
  ├─── Local Search: SongRepository.search(query) → Room LIKE query
  │     └── Kết quả: LiveData<List<SongEntity>> → adapter.submitList()
  │
  └─── Online Search (song song): searchOnline(query, allResults)
        │
        ▼
        ExecutorService.execute() [background thread]
          │
          ▼
          apiService.searchSongs(query) → Retrofit Call
            │
            ▼
            HTTP GET http://192.168.1.101:8080/songs/search?q=jazz
              │
              ▼
            Backend Server xử lý:
              → Filter songs array theo title/artist/album
              → Trả về JSON array
              │
              ▼
            Response 200 OK → Gson parse → List<SongDto>
              │
              ▼
            Map từng SongDto → SongEntity:
              entity.filePath = dto.streamUrl  (URL MP3)
              entity.mediaStoreId = null        (online song)
              │
              ▼
            Merge vào allResults → adapter.submitList()
              │
              ▼
            UI hiển thị kết quả + online indicator ẩn
```

### 2.2. Luồng Phát nhạc Streaming

```
User click bài hát từ kết quả tìm kiếm
  │
  ▼
MusicPlayer.getInstance(context).play(song)
  │
  ▼
MusicPlayer.initialize() [nếu chưa có ExoPlayer]
  │  ├── Tạo ExoPlayer với LoadControl (15s-50s)
  │  ├── Set AudioAttributes (USAGE_MEDIA + CONTENT_TYPE_MUSIC)
  │  ├── Set MediaSourceFactory (CacheDataSource)
  │  └── Add Player.Listener (handle end/error)
  │
  ▼
buildMediaItem(song) → MediaItem
  │  └── URI = song.filePath (VD: "https://www.soundhelix.com/...mp3")
  │  └── MediaMetadata = title, artist, album, artworkUri
  │
  ▼
exoPlayer.setMediaItem(mediaItem)
  │
  ▼
exoPlayer.prepare()
  │
  ▼
  ┌────────────────────────────────────────────────────────┐
  │  ExoPlayer bắt đầu prepare → CacheDataSource hoạt động │
  │                                                        │
  │  1. CacheDataSource.checkCache(URI)                    │
  │     ├── Cache HIT → đọc từ SimpleCache (file local)    │
  │     └── Cache MISS → gọi upstream DataSource           │
  │                                                        │
  │  2. Upstream (DefaultDataSource)                        │
  │     ├── URI scheme = "http"                             │
  │     └── → DefaultHttpDataSource → HTTP GET stream       │
  │                                                        │
  │  3. CacheDataSource tự động cache chunk khi download    │
  │     └── Lần sau play → cache HIT → phát ngay không cần │
  │         đợi tải mạng                                    │
  └────────────────────────────────────────────────────────┘
  │
  ▼
exoPlayer.play() → Phát nhạc từ loa/tai nghe
  │
  ▼
PlayerEventListener forward sự kiện:
  ├── onPlaybackStateChanged(STATE_READY) → UI update
  ├── onIsPlayingChanged(true) → Play icon → Pause icon
  └── onMediaItemTransition → cập nhật title/artist/album art
```

### 2.3. Luồng Cache khi Streaming

```
┌─────────────────────────────────────────────────────────────────┐
│                   CacheDataSource Flow                          │
│                                                                 │
│  CacheDataSource.read()                                         │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────┐                                                    │
│  │ Cache   │ ←── SimpleCache (LRU 500MB)                       │
│  │ Lookup  │     ├── Disk: context.getCacheDir()/media_cache/  │
│  └────┬────┘     └── Index: StandaloneDatabaseProvider         │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────┐                                                    │
│  │  HIT?   │                                                    │
│  └────┬────┘                                                    │
│       │                                                         │
│  YES  │           NO                                            │
│       ▼           ▼                                             │
│  ┌─────────┐  ┌────────────────────────────┐                   │
│  │ Đọc từ  │  │ DefaultDataSource          │                   │
│  │ cache   │  │  ├── content:// → ContentDS│                   │
│  │ (nhanh) │  │  ├── http://    → HttpDS   │                   │
│  └─────────┘  │  ├── file://    → FileDS   │                   │
│               │  └── asset://  → AssetDS   │                   │
│               └───────────┬────────────────┘                   │
│                           │                                     │
│                           ▼                                     │
│                    ┌──────────────┐                             │
│                    │ HTTP Stream  │                             │
│                    │ từ Internet  │                             │
│                    └──────┬───────┘                             │
│                           │                                     │
│                           ▼                                     │
│                    ┌──────────────┐                             │
│                    │ Ghi vào      │                             │
│                    │ SimpleCache  │ ← Tự động, trong suốt      │
│                    │ (chunk 256KB)│                             │
│                    └──────────────┘                             │
│                                                                 │
│  FLAG_IGNORE_CACHE_ON_ERROR:                                    │
│  Nếu cache bị lỗi (VD: disk full) → bỏ qua cache, stream trực  │
│  tiếp từ upstream — không làm gián đoạn playback               │
└─────────────────────────────────────────────────────────────────┘
```

### 2.4. Luồng Dữ liệu từ Backend → Android

```
Backend Node.js (server.js)              Android App
─────────────────────                   ─────────────────────

songs array (30 bài)                    RetrofitClient
  │                                        │
  ▼                                        ▼
CORS: app.use(cors())                    OkHttp → HTTP GET
  │                                        │
  ▼                                        ▼
Express Router                           MusicApiService
  │                                        │  @GET("songs/search")
  ▼                                        ▼
GET /songs/search?q=jazz                Call<List<SongDto>>.execute()
  │                                        │
  ▼                                        ▼
Filter songs:                            Response<List<SongDto>>
  title.includes("jazz")                   │
  OR artist.includes("jazz")               ▼
  OR album.includes("jazz")             Map → List<SongEntity>
  │                                        │  filePath = streamUrl
  ▼                                        ▼
JSON Response                           SearchFragment
  [                                      adapter.submitList()
    {                                     │
      id: "11",                           ▼
      title: "Jazz Cafe",              RecyclerView hiển thị
      artist: "Mellow Tones",
      streamUrl: "https://...",
      albumArtUrl: "https://..."
    },
    ...
  ]
  
  Khi user click bài:
  
  MusicPlayer.play(song)
    │
    ▼
  ExoPlayer.setUri(song.filePath)
    │  = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
    ▼
  CacheDataSource → DefaultHttpDataSource
    │
    ▼
  SoundHelix MP3 Server → Audio Track → Loa
```

### 2.5. Luồng Xử Lý Lỗi Streaming Chi Tiết

```
User click bài hát
  │
  ▼
MusicPlayer.play(song)
  │
  ├── ✅ Thành công: ExoPlayer.STATE_READY → play
  │
  └── ❌ Thất bại: ExoPlayer.onPlayerError(PlaybackException)
        │
        ▼
        MusicPlayer.handlePlayerError(error)
          │
          ▼
          retryCount++ (tối đa 3 lần)
          │
          ├── retryCount <= 3:
          │     └── Handler.postDelayed(prepare() + play(), 1000ms)
          │
          └── retryCount > 3:
                └── exoPlayer.pause() → Giữ queue, user retry manual
                      │
                      ▼
                PlayerActivity nhận onPlayerError event:
                  └── Snackbar: "Lỗi phát nhạc: <errorMsg>"
                        └── Action "Thử lại" → musicPlayer.play()
```

### 2.6. Luồng từ SearchFragment

```
SearchFragment.performSearch("jazz")
  │
  ├── currentQuery = "jazz"
  │
  ├── Query rỗng? → Không
  │
  ├── Xoá observer cũ (nếu có)
  │
  ├── Hiển thị online_loading ProgressBar
  │
  ├── Gọi local search:
  │     SongRepository.search("jazz") → LiveData
  │     └── observe() → localSongs adapter.submitList()
  │
  └── Gọi online search (song song):
        searchOnline("jazz", allResults)
          │
          ▼
        executor.execute(() -> {
            try {
                // Gọi API
                Call<List<SongDto>> call = apiService.searchSongs("jazz");
                Response<List<SongDto>> response = call.execute();
                
                // Ẩn loading
                runOnUiThreadSafe(() -> onlineLoading.setVisibility(GONE));
                
                if (response.isSuccessful() && response.body() != null) {
                    // Map và merge kết quả
                    for (SongDto dto : response.body()) {
                        SongEntity entity = new SongEntity(
                            dto.getTitle(), dto.getArtist(), dto.getAlbum(),
                            dto.getDuration(),
                            dto.getStreamUrl(),    // ← URI để ExoPlayer phát
                            dto.getAlbumArtUrl(),  // ← URI ảnh bìa
                            null,                  // ← mediaStoreId = null (online)
                            System.currentTimeMillis()
                        );
                        allResults.add(entity);
                    }
                    // Cập nhật UI
                    runOnUiThreadSafe(() -> {
                        adapter.submitList(new ArrayList<>(allResults));
                        emptyState.setVisibility(GONE);
                    });
                }
            } catch (ConnectException e) {
                // Không kết nối được server
                runOnUiThreadSafe(() -> {
                    onlineLoading.setVisibility(GONE);
                    if (allResults.isEmpty()) {
                        Snackbar: "Không thể kết nối server..."
                    }
                });
            } catch (UnknownHostException e) {
                // Không có internet
                runOnUiThreadSafe(() -> {
                    onlineLoading.setVisibility(GONE);
                    Snackbar: "Không có kết nối Internet"
                });
            } catch (Exception e) {
                // Lỗi khác
                runOnUiThreadSafe(() -> {
                    onlineLoading.setVisibility(GONE);
                    Snackbar: error message
                });
            }
        });
```

---

## 3. Các lỗi đã gặp — Nguyên nhân & Cách fix

### 🐛 Lỗi 1: SOURCE_ERROR — Không phát được nhạc local

**Triệu chứng:** App báo "source error" khi phát nhạc từ điện thoại.

**Nguyên nhân gốc rễ:**

```
MediaStoreScanner trả về URI dạng content://
→ content://media/external/audio/media/12345

CacheDataSourceFactory dùng DefaultHttpDataSource.Factory làm upstream
→ DefaultHttpDataSource CHỈ xử lý http:// và https://
→ Không hiểu content:// → FAIL → SOURCE_ERROR
```

**File bị lỗi:** `player/CacheDataSourceFactory.java`

```java
// ❌ CODE CŨ — CHỈ hỗ trợ HTTP
DataSource.Factory upstreamFactory = new DefaultHttpDataSource.Factory();

// ✅ CODE MỚI — Hỗ trợ content:// + http:// + file:// + asset://
DataSource.Factory upstreamFactory = new DefaultDataSource.Factory(
    context,
    new DefaultHttpDataSource.Factory()
);
```

**Giải thích:** `DefaultDataSource.Factory` là một "router" thông minh — tự động chọn DataSource phù hợp dựa trên URI scheme:

| URI Scheme | DataSource | Mục đích |
|-----------|-----------|----------|
| `content://...` | `ContentDataSource` | Nhạc local từ MediaStore (Android 10+ Scoped Storage) |
| `file://...` | `FileDataSource` | Đường dẫn file vật lý |
| `http://...` hoặc `https://...` | `DefaultHttpDataSource` | Streaming online |
| `asset://...` | `AssetDataSource` | File trong thư mục assets |
| `rawresource://...` | `RawResourceDataSource` | File trong thư mục res/raw |

---

### 🐛 Lỗi 2: Android chặn HTTP (Cleartext Traffic)

**Triệu chứng:** App không kết nối được backend mặc dù server đang chạy, không có thông báo lỗi rõ ràng.

**Nguyên nhân gốc rễ:**

```
Android 9 (API 28) trở lên MẶC ĐỊNH chặn HTTP (cleartext)
→ App gọi http://192.168.1.101:8080
→ Bị chặn ngầm, không có exception hay log rõ ràng
→ Retrofit báo lỗi kết nối nhưng không biết tại sao
```

**File bị lỗi:** `app/src/main/AndroidManifest.xml`

```xml
<!-- ❌ CODE CŨ — Thiếu flag cho phép HTTP -->
<application
    android:icon="@mipmap/ic_launcher"
    ...>

<!-- ✅ CODE MỚI — Thêm usesCleartextTraffic="true" -->
<application
    android:icon="@mipmap/ic_launcher"
    ...
    android:usesCleartextTraffic="true">
```

**Giải thích:** Từ Android 9 (API 28), Google mặc định chặn mọi kết nối HTTP không mã hoá (cleartext) vì lý do bảo mật. Có 3 cách giải quyết:

1. **`android:usesCleartextTraffic="true"`** (đã dùng) — Cho phép HTTP với tất cả domain. Đơn giản nhưng kém bảo mật.
2. **Network Security Config** — File XML cấu hình chi tiết: cho phép HTTP chỉ với domain/IP cụ thể, chặn các domain khác. An toàn hơn.
3. **Dùng HTTPS** — Cách tốt nhất. Nhưng cần chứng chỉ SSL cho backend, phức tạp hơn với backend local.

> ⚠️ Với đồ án/demo, cách 1 là chấp nhận được. Với production, nên dùng cách 2 hoặc 3.

---

### 🐛 Lỗi 3: Snackbar thông báo lỗi trống

**Triệu chứng:** Khi không kết nối được server, Snackbar hiện ra nhưng không có chữ, chỉ thấy thanh màu.

**Nguyên nhân gốc rễ:**

```
Exception.getMessage() trả về null
→ Snackbar.make(view, null, LENGTH_SHORT).show()
→ Snackbar không hiển thị message
→ User không biết lỗi gì
```

**File bị lỗi:** `ui/search/SearchFragment.java`

```java
// ❌ CODE CŨ — Không xử lý null message
} catch (Exception e) {
    Snackbar.make(searchInput, "Lỗi: " + e.getMessage(), ...).show();
    // e.getMessage() = null → Snackbar hiển thị "Lỗi: null" hoặc trống
}

// ✅ CODE MỚI — Kiểm tra null và fallback message
} catch (Exception e) {
    runOnUiThreadSafe(() -> {
        String errMsg = e.getMessage();
        if (errMsg == null || errMsg.isEmpty()) {
            errMsg = "Lỗi kết nối server. Kiểm tra IP và firewall";
        }
        Snackbar.make(searchResults != null ? searchResults : requireView(),
                errMsg, Snackbar.LENGTH_LONG).show();
    });
}
```

---

### 🐛 Lỗi 4: IP sai trong local.properties

**Triệu chứng:** App chạy trên điện thoại thật không kết nối được backend.

**Nguyên nhân gốc rễ:**

```
local.properties có:
  base.api.url=http://10.0.2.2:8080
  
10.0.2.2 là IP đặc biệt của Android Emulator để truy cập localhost host machine.
Trên điện thoại thật, 10.0.2.2 KHÔNG TỒN TẠI — cần IP thật của máy tính.
```

**Fix:** Cập nhật `local.properties` với IP thật của máy tính trong mạng LAN:
```properties
base.api.url=http://192.168.1.101:8080
```

**Cách kiểm tra IP trên Windows:**
```bash
ipconfig | findstr IPv4
# Output: IPv4 Address. . . . . . . . . . . : 192.168.1.101
```

> ⚠️ **Lưu ý:** `local.properties` chỉ được đọc khi BUILD. Nếu sửa file này, phải build lại APK.

---

### 🐛 Lỗi 5: Null Exception trong Catch Block

**Triệu chứng:** App crash khi có lỗi mạng do `e.getMessage()` null.

**Nguyên nhân:** Một số exception (đặc biệt là exception từ network layer) có message = null. Các khối catch gọi `e.getMessage()` mà không kiểm tra null.

**Fix:** Kiểm tra null trước khi dùng `e.getMessage()` và dùng `errMsg` fallback.

---

### 🐛 Lỗi 6: Fragment Detach Crash

**Triệu chứng:** App crash khi SearchFragment bị detach trong khi đang chờ network response.

**Nguyên nhân:** `requireActivity().runOnUiThread()` crash với `IllegalStateException` nếu fragment không còn attached.

**Fix:** Thêm `runOnUiThreadSafe()` kiểm tra `isAdded()`:

```java
private void runOnUiThreadSafe(@NonNull Runnable action) {
    if (isAdded() && getActivity() != null) {
        requireActivity().runOnUiThread(action);
    }
}
```

---

### 🐛 Lỗi 7: Không tìm thấy nhạc trên thiết bị

**Triệu chứng:** Mở app, tab Library/Bài hát trống.

**Nguyên nhân:** 3 khả năng:

| Nguyên nhân | API Level | Cách kiểm tra |
|------------|-----------|---------------|
| Thiếu quyền READ_MEDIA_AUDIO | API 33+ | Settings → Ứng dụng → NgheNhac → Quyền |
| Thiếu quyền READ_EXTERNAL_STORAGE | API 28-32 | Settings → Ứng dụng → NgheNhac → Quyền |
| Không có file nhạc hợp lệ | Tất cả | MediaStore lọc `IS_MUSIC=1 AND DURATION>0` |

**Cách khắc phục:** 
- Cấp quyền qua Settings
- Hoặc thêm nhạc vào thiết bị
- Hoặc chỉ dùng streaming (bỏ qua local)

---

### Tổng kết các lỗi

| # | Lỗi | Nguyên nhân | Hậu quả | Fix |
|:-:|-----|------------|---------|-----|
| 1 | SOURCE_ERROR | `DefaultHttpDataSource` không hiểu `content://` | Không phát được nhạc local | Đổi upstream thành `DefaultDataSource.Factory` |
| 2 | Không kết nối server | Android 9+ chặn HTTP mặc định | Không tìm kiếm online được | Thêm `usesCleartextTraffic="true"` vào Manifest |
| 3 | Snackbar trống | `e.getMessage()` null | User không thấy lỗi | Fallback message + null check |
| 4 | Không kết nối từ phone thật | Dùng `10.0.2.2` (emulator-only IP) | Chỉ emulator chạy được | Dùng IP thật của máy tính |
| 5 | Crash khi có lỗi mạng | Null pointer exception | App crash | Null check + fallback |
| 6 | Crash khi back nhanh | Gọi UI từ fragment đã detach | App crash | `runOnUiThreadSafe()` |
| 7 | Danh sách nhạc trống | Thiếu quyền / không có nhạc | User nghĩ app lỗi | Hướng dẫn cấp quyền |

---

## 4. Kiến thức nền tảng liên quan

### 4.1. Media3 ExoPlayer

**Media3** là phiên bản kế thừa chính thức của **ExoPlayer**, được Google chuyển vào AndroidX. Đây là thư viện phát media mạnh mẽ, linh hoạt thay thế MediaPlayer.

#### Kiến trúc ExoPlayer

```
┌─────────────────────────────────────────────────────────────┐
│                         ExoPlayer                           │
├─────────────────────────────────────────────────────────────┤
│  Player Interface                                           │
│  ├── play() / pause() / stop() / seekTo()                  │
│  ├── setMediaItems() / addMediaItem()                      │
│  ├── setRepeatMode() / setShuffleModeEnabled()             │
│  ├── addListener(Player.Listener)                           │
│  └── getPlaybackState() / getCurrentPosition()             │
├─────────────────────────────────────────────────────────────┤
│  Internal Components                                        │
│  ├── TrackSelector → Chọn track audio/video/text           │
│  ├── LoadControl → Quyết định khi nào load/bỏ buffer       │
│  ├── MediaSource → Cung cấp media data (từ URI/file)       │
│  ├── Renderer → Decode + render audio/video                │
│  └── ExoPlayerImpl → Phối hợp tất cả components            │
└─────────────────────────────────────────────────────────────┘
```

#### Các state của ExoPlayer

```
STATE_IDLE (0) → STATE_BUFFERING (1) → STATE_READY (3) → STATE_ENDED (4)
                    ↑                      │
                    └──────────────────────┘
                         (rebuffering)

STATE_IDLE:   Player vừa tạo, chưa có media
STATE_BUFFERING: Đang tải/demux/seek, chưa phát được
STATE_READY:  Đã sẵn sàng, có thể play hoặc tạm dừng
STATE_ENDED:  Đã phát xong media hiện tại
```

#### Các loại lỗi thường gặp (PlaybackException)

| Error Code | Ý nghĩa | Nguyên nhân thường gặp |
|-----------|---------|----------------------|
| `SOURCE_ERROR` (1001) | Không thể đọc source | URI sai, không có quyền, server offline |
| `RENDERER_ERROR` (1002) | Lỗi decoder | Codec không hỗ trợ, file hỏng |
| `UNEXPECTED` (1003) | Lỗi không xác định | Bug, race condition |
| `REMOTE_ERROR` (1004) | Lỗi remote player | MediaSession remote controller |

#### LoadControl — Quản lý Buffer

```java
// Cấu hình LoadControl trong NgheNhac
DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        15_000,   // minBufferMs: Buffer tối thiểu (15s) — đủ để chống giật
        50_000,   // maxBufferMs: Buffer tối đa (50s) — tiết kiệm RAM hơn video
        2_500,    // bufferForPlaybackMs: Cần 2.5s buffer để bắt đầu phát
        5_000     // bufferForPlaybackAfterRebufferMs: Cần 5s sau khi rebuffer
    )
    .setPrioritizeTimeOverSizeThresholds(true)
    .build();
```

> 📌 **Tại sao buffer nhạc nhỏ hơn video?**  
> Âm thanh có bitrate thấp hơn nhiều so với video (~128kbps vs ~5Mbps). Với nhạc, 15s buffer chỉ tốn ~240KB RAM, trong khi video 15s cần ~9MB. Buffer vừa đủ để chống giật nhưng không lãng phí RAM.

---

### 4.2. CacheDataSource — Caching thông minh

#### Cách hoạt động

`CacheDataSource` là một wrapper DataSource nằm giữa ExoPlayer và DataSource thật:

```
ExoPlayer
    │
    ▼
CacheDataSource ←── SimpleCache (LRU 500MB)
    │                     ├── Cache files
    ▼                     └── Index (SQLite)
DefaultDataSource
    ├── ContentDataSource (content://)
    ├── FileDataSource (file://)
    ├── DefaultHttpDataSource (http://)
    └── AssetDataSource (asset://)
```

#### Khi nào cache được ghi?

- `CacheDataSource` tự động cache các **chunk** dữ liệu (mặc định 256KB) khi đọc từ upstream
- Không cần code thủ công — ExoPlayer + CacheDataSource tự xử lý
- Cache được ghi trong quá trình play/stream bình thường

#### Khi nào cache được đọc?

- Lần phát sau → CacheDataSource kiểm tra cache trước
- Cache HIT → đọc từ disk (nhanh, không tốn bandwidth)
- Cache MISS → stream từ mạng → đồng thời ghi cache

#### FLAG_IGNORE_CACHE_ON_ERROR

```java
CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
```

- Nếu có lỗi đọc/ghi cache (VD: disk full, file corrupt) → bỏ qua cache
- Fallback về stream trực tiếp từ upstream
- Đảm bảo playback không bị gián đoạn vì lỗi cache

---

### 4.3. DefaultDataSource — URI Scheme Router

**`DefaultDataSource`** là một DataSource "thông minh" tự động chọn DataSource phù hợp dựa trên URI scheme:

```java
// Cấu trúc bên trong DefaultDataSource (đơn giản hoá)
DataSource selectDataSource(Uri uri) {
    String scheme = uri.getScheme();
    switch (scheme) {
        case "http":
        case "https":
            return new DefaultHttpDataSource();
        case "content":
            return new ContentDataSource(context);
        case "file":
            return new FileDataSource();
        case "asset":
            return new AssetDataSource();
        case "rawresource":
            return new RawResourceDataSource();
        default:
            throw new IllegalArgumentException("Unsupported scheme: " + scheme);
    }
}
```

**Tại sao cần `DefaultDataSource` trong NgheNhac?**

Vì app hỗ trợ **2 loại URI**:
- Nhạc local: `content://media/external/audio/media/12345`
- Nhạc streaming: `http://192.168.1.101:8080/songs/1/stream`

Nếu chỉ dùng `DefaultHttpDataSource`, URI `content://` sẽ bị lỗi.

---

### 4.4. MediaSessionService — Background Playback

**`MediaSessionService`** là service đặc biệt của Media3 cho phép:
- Phát nhạc nền (Foreground Service)
- Điều khiển từ notification
- Điều khiển từ lock screen
- Điều khiển từ Bluetooth

#### Kiến trúc MediaSession

```
┌──────────────────────────────────────────┐
│              MediaSessionService         │
│                                          │
│  onCreate():                             │
│    musicPlayer = MusicPlayer.getInstance │
│    musicPlayer.initialize()              │
│    mediaSession = MediaSession.Builder(  │
│        this, musicPlayer.getExoPlayer()  │
│    ).build()                             │
│                                          │
│  onGetSession(ControllerInfo):           │
│    return mediaSession                   │
│                                          │
│  onDestroy():                            │
│    mediaSession.release()                │
│    musicPlayer.release()                 │
│    super.onDestroy()                     │
└──────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│    MediaController (Client kết nối)      │
│                                          │
│  ┌─────────────────────┐                │
│  │ Notification        │ ← Media3 tự tạo│
│  │ (MediaStyle)        │                │
│  └─────────────────────┘                │
│  ┌─────────────────────┐                │
│  │ Lock Screen         │                │
│  │ (Android system)    │                │
│  └─────────────────────┘                │
│  ┌─────────────────────┐                │
│  │ Bluetooth            │                │
│  │ (AVRCP profile)     │                │
│  └─────────────────────┘                │
│  ┌─────────────────────┐                │
│  │ Android Auto        │                │
│  └─────────────────────┘                │
└──────────────────────────────────────────┘
```

#### Các lệnh MediaSession

| Lệnh | Callback | Hành động |
|------|----------|-----------|
| Play | `onPlay()` → | `musicPlayer.play()` |
| Pause | `onPause()` → | `musicPlayer.pause()` |
| Next | `onSkipToNext()` → | `musicPlayer.next()` |
| Previous | `onSkipToPrevious()` → | `musicPlayer.previous()` |
| Stop | `onStop()` → | `stopForeground(REMOVE)` + `stopSelf()` |
| Seek | `onSeekTo(pos)` → | `musicPlayer.seekTo(pos)` |

---

### 4.5. Retrofit + OkHttp — Network Layer

#### OkHttp Interceptor Chain

```
┌──────────┐     ┌────────────┐     ┌──────────┐     ┌──────────┐
│  App     │────→│  Retrofit  │────→│  OkHttp  │────→│  Server  │
│ (Call)   │     │ (Adapter)  │     │ (Client) │     │ (HTTP)   │
└──────────┘     └────────────┘     └────┬─────┘     └──────────┘
                                         │
                                   ┌─────▼─────┐
                                   │ Interceptor│
                                   │ Chain      │
                                   │ 1. Retry   │
                                   │ 2. Bridge  │
                                   │ 3. Cache   │
                                   │ 4. Connect │
                                   │ 5. Network │
                                   │ 6. Call    │
                                   │ 7. Logging │
                                   └───────────┘
```

#### Timeout Configuration

```java
OkHttpClient okHttpClient = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)   // Kết nối server
    .readTimeout(30, TimeUnit.SECONDS)      // Đọc response (cao cho streaming)
    .writeTimeout(30, TimeUnit.SECONDS)     // Ghi request
    .build();
```

- **connectTimeout:** Thời gian tối đa để thiết lập kết nối TCP với server
- **readTimeout:** Thời gian tối đa giữa 2 lần đọc dữ liệu. Cao hơn vì streaming có thể bị chậm.
- **writeTimeout:** Thời gian tối đa để gửi request lên server.

#### Logging Interceptor

```java
HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
loggingInterceptor.setLevel(
    BuildConfig.LOG_HTTP
        ? HttpLoggingInterceptor.Level.BODY     // Log full request/response body
        : HttpLoggingInterceptor.Level.NONE     // Không log gì
);
```

> ⚠️ **Bảo mật:** Bật `Level.BODY` sẽ log tất cả dữ liệu request/response, bao gồm cả access token. Chỉ bật khi debug, tắt trong release.

---

### 4.6. Content URI vs File URI (Scoped Storage)

#### Sự khác biệt

```java
// Đường dẫn file vật lý (deprecated từ API 29+)
/storage/emulated/0/Music/song.mp3
// → FileUri (file://)
// → KHÔNG được phép truy cập trên Android 10+ (Scoped Storage)

// Content URI (Android 10+)
content://media/external/audio/media/12345
// → ContentUri (content://)
// → Được phép truy cập qua ContentResolver
```

#### Tại sao Android chuyển sang Scoped Storage?

- **Bảo mật:** App không thể đọc tuỳ tiện file của app khác
- **Quyền riêng tư:** User kiểm soát được app nào đọc file nào
- **Ổn định:** File path có thể thay đổi (VD: format lại SD card) nhưng Content URI vẫn hoạt động

#### Cách MediaStoreScanner xử lý

```java
// MediaStoreScanner.java
private static Uri getAudioCollectionUri() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+: Scoped Storage
        return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
    } else {
        // Android 9: Legacy
        return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }
}

// Map từ cursor → SongEntity
long mediaStoreId = cursor.getLong(COL_ID);
Uri contentUri = ContentUris.withAppendedId(getAudioCollectionUri(), mediaStoreId);
// contentUri = "content://media/external/audio/media/12345"

// Sau đó, SongEntity.filePath = contentUri.toString()
// ExoPlayer có CacheDataSource với DefaultDataSource → xử lý content:// OK
```

---

### 4.7. LiveData + Room — Reactive Data

#### Cách LiveData hoạt động

```
SongDao.query()
    │
    ▼
Room generates SQL: "SELECT * FROM songs WHERE ..."
    │
    ▼
Room wrapper: LiveData<List<SongEntity>>
    │
    ▼
Fragment observe(getViewLifecycleOwner(), songs -> {
    adapter.submitList(songs);
});
    │
    ▼
Khi dữ liệu thay đổi (insert/update/delete):
    → Room tự động notify LiveData
    → LiveData postValue() trên main thread
    → Adapter.submitList() dùng AsyncDiffUtil
    → RecyclerView cập nhật chỉ các item thay đổi
```

#### Tại sao dùng LiveData thay vì callback?

| Tiêu chí | LiveData | Callback |
|----------|----------|----------|
| Lifecycle aware | ✅ Tự động cleanup | ❌ Phải unsubscribe thủ công |
| Thread safety | ✅ postValue() | ❌ Dễ crash "CalledFromWrongThread" |
| Realtime | ✅ Room tự động notify | ❌ Phải gọi lại query |
| Memory leak | ✅ Không leak nếu dùng getViewLifecycleOwner | ❌ Dễ leak nếu quên huỷ |

---

### 4.8. Android Permission Model

#### Các quyền cần cho streaming

```xml
<!-- Quyền Internet — bắt buộc cho streaming -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Quyền đọc nhạc local — Android 13+ (API 33+) -->
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- Quyền đọc storage — Android 9-12 (API 28-32) -->
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />

<!-- Quyền foreground service — bắt buộc cho background playback -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<!-- Quyền hiển thị notification — Android 13+ -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

#### Runtime Permission Flow

```
MainActivity.checkAndRequestPermissions()
    │
    ▼
checkSelfPermission(READ_MEDIA_AUDIO) hoặc (READ_EXTERNAL_STORAGE)
    │
    ├── GRANTED → scanMediaStore()
    │
    └── DENIED → requestPermissions()
          │
          ▼
    onRequestPermissionsResult()
          │
          ├── GRANTED → scanMediaStore()
          │
          └── DENIED → Hiển thị Snackbar
                │
                ├── Lần đầu: "Cần quyền để đọc nhạc trên thiết bị"
                │
                └── Vĩnh viễn: "Vào Settings → Ứng dụng → NgheNhac → Quyền để bật"
                      + Action "Cài đặt" → Intent(APPLICATION_DETAILS_SETTINGS)
```

---

## 5. Hướng dẫn vận hành & Debug

### 5.1. Cấu hình trước khi chạy

#### Bước 1: Cập nhật local.properties

```properties
# File: local.properties
# Kiểm tra IP máy tính:
#   Windows: ipconfig | findstr IPv4
#   macOS/Linux: ifconfig | grep inet
base.api.url=http://<IP_MÁY_TÍNH>:8080

# Emulator Android Studio:
# base.api.url=http://10.0.2.2:8080
# (10.0.2.2 là special IP trỏ đến localhost của máy host)
```

#### Bước 2: Chạy backend

```bash
cd D:\University\Nam3\NgheNhac\backend-node
npm start
# Output: 🎵 NgheNhac Music API running at http://0.0.0.0:8080
```

#### Bước 3: Build & cài app Android

```bash
cd D:\University\Nam3\NgheNhac
./gradlew assembleDebug
# Output: BUILD SUCCESSFUL in Xs
# APK: app/build/outputs/apk/debug/app-debug.apk
```

#### Bước 4: Kiểm tra kết nối

```bash
# Test API từ máy tính
curl http://localhost:8080/songs
# → JSON array 30 bài hát

curl http://localhost:8080/songs/search?q=jazz
# → JSON array bài hát jazz

# Test kết nối từ điện thoại (cùng WiFi)
# Mở browser trên điện thoại: http://192.168.1.101:8080/songs
# → OK nếu thấy JSON
```

### 5.2. Checklist khi streaming không hoạt động

```
❌ Không tìm thấy kết quả online?

[1] Backend đã chạy?
    → Mở browser: http://localhost:8080/songs
    → Nếu không thấy JSON → chạy: cd backend-node && npm start
    
[2] Điện thoại và máy tính cùng WiFi?
    → Kiểm tra kết nối mạng
    
[3] IP trong local.properties đúng?
    → Chạy: ipconfig | findstr IPv4 → so sánh với base.api.url
    
[4] Build lại app sau khi sửa local.properties?
    → local.properties chỉ đọc khi build → cần rebuild
    
[5] Android có chặn HTTP không?
    → Kiểm tra AndroidManifest.xml có android:usesCleartextTraffic="true"?

[6] Firewall Windows có chặn port 8080?
    → Tắt firewall thử, hoặc thêm rule cho phép port 8080


❌ Click bài hát nhưng không phát?

[1] Lỗi SOURCE_ERROR?
    → Kiểm tra CacheDataSourceFactory có dùng DefaultDataSource không
    
[2] URL stream có đúng không?
    → Mở URL trong browser: có tải được MP3 không?
    
[3] ExoPlayer có retry không?
    → Xem logcat: "Player error (attempt 1/3)..."
    
[4] Có đang ở chế độ airplane/flight mode?
    → Tắt airplane mode


❌ Không thấy nhạc local?

[1] Đã cấp quyền?
    → Settings → Ứng dụng → NgheNhac → Quyền → Bật "Nhạc và audio"
    
[2] Điện thoại có file nhạc hợp lệ?
    → MediaStore lọc IS_MUSIC=1 AND DURATION>0
    → File ghi âm, nhạc chuông không được tính
    
[3] ScanTriggered flag?
    → Mở lại app để scan lại
```

### 5.3. Logcat Debugging

```bash
# Lọc log của NgheNhac
adb logcat -s MusicPlayer
adb logcat -s MediaStoreScanner
adb logcat -s SearchFragment
adb logcat -s RetrofitClient
adb logcat -s CacheDataSourceFactory

# Lọc tất cả log của app (theo PID)
adb logcat | findstr "NgheNhac\|MusicPlayer\|ExoPlayer\|Retrofit\|OkHttp"

# Xem log HTTP request/response
# Bật BuildConfig.LOG_HTTP = true → build lại app
```

#### Log quan trọng cần theo dõi

```
// MediaStoreScanner — Có quét được nhạc không?
MediaStoreScanner: Found X audio files in MediaStore

// SongRepository — Đã insert được không?
SongRepository: Scanned X songs from MediaStore

// Retrofit — Có kết nối được server không?
// (khi LOG_HTTP = true)
--> GET http://192.168.1.101:8080/songs/search?q=jazz
<-- 200 OK (Xms)

// MusicPlayer — Player có hoạt động?
MusicPlayer: Player error (attempt 1/3): Source error
MusicPlayer: Max retry count reached. Pausing player

// CacheDataSource
CacheDataSource: Cache hit for URI: content://media/...
CacheDataSource: Cache miss, fetching from network
```

### 5.4. Các công cụ debug hữu ích

| Công cụ | Mục đích | Cách dùng |
|---------|----------|-----------|
| **adb logcat** | Xem log realtime | `adb logcat -s MusicPlayer` |
| **Postman** | Test API | Gửi request đến backend |
| **curl** | Test API từ terminal | `curl http://localhost:8080/songs` |
| **Android Studio Profiler** | Network, CPU, Memory | View → Tool Windows → Profiler |
| **Network Inspector** | Xem network request | View → Tool Windows → App Inspection |
| **Chrome DevTools** | Xem WebSocket (nếu có) | `chrome://inspect` |

---

## Phụ lục

### A. Sơ đồ Class Streaming

```
┌─────────────────────────────────────────────────────────────────┐
│                     RetrofitClient                              │
│  ├── init() → Tạo OkHttpClient + Retrofit + MusicApiService    │
│  └── getApiService() → MusicApiService                          │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     MusicApiService (Interface)                  │
│  ├── @GET("songs") getAllSongs() → Call<List<SongDto>>         │
│  ├── @GET("songs/search") searchSongs(@Query q) → Call<...>    │
│  ├── @GET("songs/{id}") getSongDetail(@Path id) → Call<SongDto>│
│  └── @GET("songs/{id}/stream") getStreamUrl(@Path id) → Call<> │
└─────────────────────────────────────────────────────────────────┘
                                 │ Call.execute() → Response<List<SongDto>>
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                SongDto → SongEntity Mapper                       │
│  SongDto (JSON) → @SerializedName → SongEntity (Room)          │
│  └── streamUrl → filePath (URI để ExoPlayer phát)              │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│    SearchFragment                                               │
│  ├── performSearch(query) → local + online search              │
│  └── searchOnline(query, results) → apiService.searchSongs()   │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼ (User click bài hát)
┌─────────────────────────────────────────────────────────────────┐
│    MusicPlayer (Singleton)                                      │
│  ├── play(SongEntity) → buildMediaItem() → ExoPlayer.play()   │
│  └── ExoPlayer configuration:                                  │
│        ├── DefaultMediaSourceFactory(CacheDataSourceFactory)   │
│        ├── DefaultLoadControl (15s-50s)                       │
│        └── AudioAttributes (MUSIC)                             │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│    CacheDataSourceFactory (Singleton)                           │
│  ├── SimpleCache (LRU 500MB, context.getCacheDir())            │
│  ├── CacheDataSource.Factory                                   │
│  │    └── upstream = DefaultDataSource.Factory(                │
│  │                    context,                                 │
│  │                    new DefaultHttpDataSource.Factory()       │
│  │                  )                                           │
│  └── FLAG_IGNORE_CACHE_ON_ERROR                                │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│    DefaultDataSource (Route theo URI scheme)                   │
│  ├── content:// → ContentDataSource (MediaStore)               │
│  ├── http://   → DefaultHttpDataSource (Streaming)             │
│  └── file://   → FileDataSource (File system)                  │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
                    ┌───────────────────────┐
                    │   Backend Server      │
                    │   (Node.js/Express)   │
                    │   Port 8080           │
                    │   songs array (30 bài)│
                    └───────────────────────┘
```

### B. File tham chiếu

| File | Đường dẫn | Vai trò |
|------|-----------|---------|
| **MusicApiService** | `app/.../data/remote/MusicApiService.java` | Define REST endpoints |
| **RetrofitClient** | `app/.../data/remote/RetrofitClient.java` | HTTP client config |
| **SongDto** | `app/.../data/model/SongDto.java` | JSON model |
| **SongEntity** | `app/.../data/local/entity/SongEntity.java` | Room entity |
| **SongRepository** | `app/.../data/repository/SongRepository.java` | Data access layer |
| **MusicPlayer** | `app/.../player/MusicPlayer.java` | Core player engine |
| **PlaybackQueue** | `app/.../player/PlaybackQueue.java` | Queue management |
| **CacheDataSourceFactory** | `app/.../player/CacheDataSourceFactory.java` | Cache layer |
| **PlayerEventListener** | `app/.../player/PlayerEventListener.java` | Event bus |
| **SearchFragment** | `app/.../ui/search/SearchFragment.java` | UI search |
| **MusicService** | `app/.../service/MusicService.java` | Background playback |
| **MediaSessionManager** | `app/.../service/MediaSessionManager.java` | Media session |
| **NotificationBuilder** | `app/.../service/NotificationBuilder.java` | Notification |
| **MediaStoreScanner** | `app/.../data/local/MediaStoreScanner.java` | Local music scanner |
| **PreferencesManager** | `app/.../data/local/PreferencesManager.java` | State persistence |
| **server.js (Node.js)** | `backend-node/server.js` | Backend API |
| **package.json** | `backend-node/package.json` | Backend config |
| **local.properties** | `local.properties` | API base URL |

### C. Các Dependency liên quan đến Streaming

| Dependency | Phiên bản | Vai trò trong Streaming |
|-----------|-----------|------------------------|
| `androidx.media3:media3-exoplayer` | 1.10.1 | Lõi phát nhạc |
| `androidx.media3:media3-datasource-okhttp` | 1.10.1 | HTTP stack cho ExoPlayer |
| `androidx.media3:media3-session` | 1.10.1 | MediaSession cho background playback |
| `com.squareup.retrofit2:retrofit` | 2.11.0 | REST API client |
| `com.squareup.retrofit2:converter-gson` | 2.11.0 | JSON parser |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP client |
| `com.squareup.okhttp3:logging-interceptor` | 4.12.0 | HTTP debugging |
| `com.google.code.gson:gson` | 2.14.0 | JSON serialization |
| `com.github.bumptech.glide:glide` | 4.16.0 | Album art loading |
| `androidx.room:room-runtime` | 2.8.4 | Local cache DB |

---

> **Tài liệu này mô tả chi tiết toàn bộ hệ thống streaming của NgheNhac — từ kiến trúc tổng quan, luồng dữ liệu từng component, các lỗi đã gặp và cách fix, đến kiến thức nền tảng giúp hiểu sâu về cơ chế hoạt động.**
