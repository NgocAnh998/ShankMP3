# NgheNhac — Tài liệu Luồng Xử Lý (Flows)

> Phiên bản: 1.0  
> Ngày cập nhật: 2026-06-13  
> Tổng số file Java: 51  
> Ngôn ngữ: Java (Android, API 28-30)

---

## Mục lục

1. [App Startup Flow](#1-app-startup-flow)
2. [MediaStore Scan Flow](#2-mediastore-scan-flow)
3. [MusicContentObserver Flow](#3-musiccontentobserver-flow)
4. [Library Tab Flow](#4-library-tab-flow)
5. [Song Playback Flow](#5-song-playback-flow)
6. [MusicPlayer Core Flow](#6-musicplayer-core-flow)
7. [Playback Queue Flow](#7-playback-queue-flow)
8. [PlayerActivity UI Flow](#8-playeractivity-ui-flow)
9. [MiniPlayer Flow](#9-miniplayer-flow)
10. [Background Service Flow](#10-background-service-flow)
11. [Notification Flow](#11-notification-flow)
12. [Playlist CRUD Flow](#12-playlist-crud-flow)
13. [Playlist Detail & Export Flow](#13-playlist-detail--export-flow)
14. [Playlist Import Flow](#14-playlist-import-flow)
15. [Album Detail Flow](#15-album-detail-flow)
16. [Artist Playback Flow](#16-artist-playback-flow)
17. [Search Flow](#17-search-flow)
18. [Offline Download Flow](#18-offline-download-flow)
19. [Cache Layer Flow](#19-cache-layer-flow)
20. [Preferences & State Persistence Flow](#20-preferences--state-persistence-flow)
21. [Error Handling Flow](#21-error-handling-flow)
22. [Permission Flow](#22-permission-flow)

---

## 1. App Startup Flow

### Sơ đồ

```
Android OS → NgheNhacApp.onCreate()
  → set instance (singleton)
  → setupUncaughtExceptionHandler()
  → RetrofitClient.init() [lazy]
  → (các Activity/Fragment tự do sử dụng)
```

### File chính: `NgheNhacApp.java`

### Mô tả chi tiết

| Bước | Hành động | Thành phần | Ghi chú |
|------|-----------|-----------|---------|
| 1 | OS tạo process | Android System | - |
| 2 | Gọi `Application.onCreate()` | `NgheNhacApp` | Lưu instance singleton |
| 3 | Thiết lập `UncaughtExceptionHandler` | `NgheNhacApp` | Ghi log tất cả crash |
| 4 | Khởi tạo Retrofit | `RetrofitClient.init()` | Lazy init, chưa gọi API ngay |
| 5 | Glide memory management | `onLowMemory()` / `onTrimMemory()` | Gọi khi cần |

### Luồng chi tiết

1. **Android OS** tạo process → gọi `NgheNhacApp.onCreate()`
2. Lưu `static instance` → các component gọi `NgheNhacApp.getInstance()` khi cần context
3. `setupUncaughtExceptionHandler()`: bắt toàn bộ exception không được try-catch → ghi log chi tiết (thread name, stack trace, cause chain) → chuyển cho default handler
4. `RetrofitClient.init()`: khởi tạo OkHttpClient + Retrofit instance (lazy — chưa gọi network)
5. Khi bộ nhớ thấp: Glide tự động clear cache qua `onLowMemory()` và `onTrimMemory()`

**Lưu ý:** Room, MediaStoreScanner, MusicPlayer đều **lazy init** — không khởi tạo tại Application.

---

## 2. MediaStore Scan Flow

### Sơ đồ

```
MainActivity.onCreate()
  → checkAndRequestPermissions()
    → PERMISSION_GRANTED?
      → Yes: scanMediaStore()
      → No: requestPermissions(READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE)
        → Granted: scanMediaStore()
        → Denied: Snackbar + nút mở Settings
  
scanMediaStore():
  → SongRepository.refreshFromMediaStore(context, callback)
    → executor.execute() [background thread]
      → MediaStoreScanner.scanAllSongs(context)
        → ContentResolver.query(MediaStore URI, PROJECTION, SELECTION, ...)
        → Duyệt cursor → cursorToSongEntity() → List<SongEntity>
      → songDao.insertAll(scannedSongs) [REPLACE strategy]
      → callback.onComplete(songCount)
    → LiveData tự động notify → UI cập nhật
  → contentObserver.register() [theo dõi thay đổi]
```

### File chính: `MainActivity.java`, `SongRepository.java`, `MediaStoreScanner.java`, `MusicContentObserver.java`, `SongDao.java`

### Chi tiết các bước

| Bước | Mô tả | Code | Thread |
|------|-------|------|--------|
| 1 | Kiểm tra permission | `checkAndRequestPermissions()` | Main |
| 2 | Xin permission runtime | `requestPermissions()` | Main |
| 3 | Xử lý kết quả | `onRequestPermissionsResult()` | Main |
| 4 | Gọi scan | `scanMediaStore()` | Main |
| 5 | Repository async refresh | `refreshFromMediaStore()` | Background (Executor) |
| 6 | Query MediaStore | `MediaStoreScanner.scanAllSongs()` | Background |
| 7 | Map cursor → Entity | `cursorToSongEntity()` | Background |
| 8 | Bulk insert Room | `songDao.insertAll()` (REPLACE) | Background |
| 9 | Callback hoàn thành | `onComplete(songCount)` | Background |
| 10 | LiveData notify → UI | Room tự động | Main |
| 11 | Đăng ký ContentObserver | `contentObserver.register()` | Main |

### MediaStoreScanner: Chi tiết query

- **Collection URI:**
  - API 29+ (Android 10+): `MediaStore.Audio.Media.getContentUri(VOLUME_EXTERNAL)` — Scoped Storage
  - API 28: `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` — Legacy
- **Projection:** 10 cột tối ưu: `_ID, TITLE, ARTIST, ALBUM, ALBUM_ID, DURATION, TRACK, DATE_ADDED, MIME_TYPE, SIZE`
- **Selection:** `IS_MUSIC = 1 AND DURATION > 0` — chỉ lấy nhạc thật, loại ringtone/podcast
- **Sort Order:** `TITLE ASC`
- **Album Art URI:** `content://media/external/audio/albumart/<albumId>`
- **File Path:** Content URI (`content://media/external/audio/media/<id>`) — không dùng `_DATA` (deprecated)

### scanTriggered flag

- `scanTriggered` là instance field boolean, ngăn scan trùng lặp
- Set `true` khi `scanMediaStore()` bắt đầu
- Reset khi activity recreate (instance mới)

---

## 3. MusicContentObserver Flow

### Sơ đồ

```
MediaStore thay đổi (thêm/xoá/sửa file nhạc)
  → ContentObserver.onChange() [main thread]
    → handleChange(uri)
      → Ghi nhận lastChangeTime
      → Nếu < 1.5s từ lần cuối → reset debounce timer
      → Post debounceRunnable sau 1.5s
        → Hết 1.5s im lặng → listener.onMusicChanged()
          → MainActivity kích hoạt scanMediaStore() lại
```

### File chính: `MusicContentObserver.java`

### Tham số

| Tham số | Giá trị |
|---------|---------|
| DEBOUNCE_MS | 1500ms (1.5 giây) |
| URI theo dõi | `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` |
| notifyForDescendants | true (theo dõi cả URI con) |
| Handler | Main Looper |

### Lưu ý

- Bắt buộc gọi `unregister()` trong `onDestroy()` để tránh memory leak
- `destroy()` gọi `unregister()` + xoá tất cả callback pending
- Thread-safe nhờ `synchronized` trên `handleChange()`

---

## 4. Library Tab Flow

### Sơ đồ

```
MainActivity → FragmentContainerView
  → LibraryFragment
    → TabLayout + ViewPager2
      → Tab 0: SongListFragment (Bài hát)
      → Tab 1: AlbumGridFragment (Album)
      → Tab 2: ArtistListFragment (Nghệ sĩ)
      → Tab 3: PlaylistListFragment (Playlist)
  → MiniPlayerFragment [ở đáy màn hình]
```

### File chính: `LibraryFragment.java`, `LibraryPagerAdapter.java`

### 4.1 SongListFragment

| Thành phần | Chi tiết |
|-----------|---------|
| Layout | `R.layout.fragment_song_list` |
| RecyclerView | `R.id.song_list`, LinearLayoutManager |
| Adapter | `SongAdapter` (ListAdapter + SongDiffUtil) |
| Data source | `SongRepository.getAllSongs()` (LiveData) |
| Click | → `MusicPlayer.play()` → `PlayerActivity` |
| More click | TODO: BottomSheetDialog |

### 4.2 AlbumGridFragment

| Thành phần | Chi tiết |
|-----------|---------|
| Layout | `R.layout.fragment_album_grid` |
| RecyclerView | `R.id.album_grid`, GridLayoutManager (span=2) |
| Adapter | `AlbumAdapter` (ListAdapter + AlbumDiffUtil) |
| Data source | `SongRepository.getAllSongs()` → `groupSongsByAlbum()` |
| Group logic | LinkedHashMap → nhóm theo album name |
| Click | → `AlbumDetailActivity` |

### 4.3 ArtistListFragment

| Thành phần | Chi tiết |
|-----------|---------|
| Layout | `R.layout.fragment_artist_list` |
| RecyclerView | `R.id.artist_list`, LinearLayoutManager |
| Adapter | `ArtistAdapter` (ListAdapter + ArtistDiffUtil) |
| Data source | `SongRepository.getAllSongs()` → `groupSongsByArtist()` |
| Click | Lọc allSongs → `MusicPlayer.play()` → `PlayerActivity` |

### 4.4 PlaylistListFragment

| Thành phần | Chi tiết |
|-----------|---------|
| Layout | `R.layout.fragment_playlist_list` |
| RecyclerView | `R.id.playlist_list`, LinearLayoutManager |
| Adapter | `PlaylistAdapter` (ListAdapter + PlaylistDiffUtil) |
| Data source | `PlaylistRepository.getAllPlaylists()` (LiveData) |
| FAB click | → `CreatePlaylistDialog` |
| FAB long-click | → Import playlist (SAF file picker) |
| Click playlist | → `PlaylistDetailActivity` |
| More click | PopupMenu: Rename / Delete |

---

## 5. Song Playback Flow

### Sơ đồ

```
User click bài hát
  → SongListFragment/AlbumDetailActivity/PlaylistDetailActivity/ArtistListFragment
  → MusicPlayer.play(songs, startIndex) [hoặc .play(song) đơn lẻ]
    → MusicPlayer.initialize() (nếu chưa có ExoPlayer)
    → PlaybackQueue.setQueue(songs, startIndex)
    → exoPlayer.setMediaItems(items, index, TIME_UNSET)
    → exoPlayer.prepare()
    → exoPlayer.play()
    → ExoPlayer mở URI → đệm → phát
  → Intent mở PlayerActivity
    → PlayerActivity.onStart()
      → setListener(eventListener)
      → Cập nhật UI: title, artist, album art, seekbar
      → startSeekbarUpdates() [Handler postDelayed 500ms]
```

### File chính: `SongListFragment.java`, `MusicPlayer.java`, `PlayerActivity.java`, `MiniPlayerFragment.java`

### Các entry point phát nhạc

| Màn hình | Method gọi | Queue |
|---------|-----------|-------|
| SongListFragment | `playSong(song, position)` | All songs list |
| AlbumDetailActivity | `onItemClick(song, position)` | Songs in album |
| PlaylistDetailActivity | `onItemClick(song, position)` | Songs in playlist |
| ArtistListFragment | `playArtistSongs(artistName)` | Songs by artist |
| MiniPlayerFragment | `musicPlayer.play()` (resume) | Existing queue |
| PlayerActivity | `musicPlayer.play()` (resume) | Existing queue |

---

## 6. MusicPlayer Core Flow

### Sơ đồ

```
MusicPlayer (Singleton)
  ├── initialize()
  │   → Tạo ExoPlayer với:
  │     - DefaultLoadControl (15s-50s buffer)
  │     - AudioAttributes (USAGE_MEDIA + CONTENT_TYPE_MUSIC)
  │     - DefaultMediaSourceFactory
  │     - setAudioAttributes(audio, true) [ExoPlayer tự quản lý audio focus]
  │     - PlayerEventListener (forward sự kiện ra UI)
  │     - Player.Listener riêng (xử lý STATE_ENDED + onPlayerError)
  │     - Khôi phục volume từ PreferencesManager
  │
  ├── play(List<SongEntity>, startIndex)
  │   → initialize()
  │   → resetRetryCount()
  │   → playbackQueue.setQueue(songs, startIndex)
  │   → exoPlayer.setMediaItems() + prepare() + play()
  │
  ├── play(SongEntity) [đơn lẻ]
  │   → initialize()
  │   → buildMediaItem(song)
  │   → playbackQueue.clear()
  │   → exoPlayer.setMediaItem() + prepare() + play()
  │
  ├── play() [resume]
  │   → resetRetryCount()
  │   → Nếu STATE_IDLE → prepare() trước
  │   → exoPlayer.play()
  │
  ├── pause() → exoPlayer.pause()
  ├── stop() → exoPlayer.stop()
  ├── next() → playbackQueue.next() → seekTo + prepare + play
  ├── previous() → playbackQueue.previous() → seekTo + prepare + play
  ├── seekTo(positionMs) → exoPlayer.seekTo()
  ├── cycleRepeatMode() → playbackQueue.cycleRepeatMode()
  ├── setShuffleEnabled() → playbackQueue.setShuffleEnabled()
  ├── release() → exoPlayer.stop() + exoPlayer.release() + clear queue
  │
  └── handlePlayerError(error)
      → retryCount++
      → Nếu <= MAX_RETRY_COUNT (3): prepare() + play() sau 1s delay
      → Nếu hết retry: exoPlayer.pause() [giữ queue, không stop()]
```

### File chính: `MusicPlayer.java` (380+ dòng)

### Tham số ExoPlayer

| Tham số | Giá trị |
|---------|---------|
| Min buffer | 15,000ms (15s) |
| Max buffer | 50,000ms (50s) |
| Buffer để play | 2,500ms (2.5s) |
| Buffer để rebuffer | 5,000ms (5s) |
| PrioritizeTimeOverSize | true |
| Audio focus | ExoPlayer tự quản lý (`setAudioAttributes(audio, true)`) |
| Max retry count | 3 lần |
| Retry delay | 1,000ms |

### PlayerEventListener

```
Player.Listener
  └── PlayerEventListener
      └── OnEventListener (interface)
          ├── onPlaybackStateChanged(int)
          ├── onIsPlayingChanged(boolean)
          ├── onMediaItemTransition(MediaItem, int)
          ├── onPlayerError(PlaybackException)
          ├── onPlayerErrorRecovered()
          ├── onPlaybackEnded()
          └── onVolumeChanged(float)
```

- `PlayerEventListener` implements `Player.Listener` → forward sự kiện đến `OnEventListener`
- UI set listener qua `musicPlayer.getEventListener().setListener(listener)`
- Huỷ listener = `setListener(null)` trong `onPause()`/`onStop()`

---

## 7. Playback Queue Flow

### Sơ đồ

```
PlaybackQueue
  ├── mediaItems: List<MediaItem> [danh sách gốc]
  ├── shuffledIndices: List<Integer> [null khi shuffle tắt]
  ├── currentIndex: int [-1 khi rỗng]
  ├── isShuffleEnabled: boolean
  ├── repeatMode: int [0=NONE, 1=ONE, 2=ALL]
  │
  ├── setQueue(songs, startIndex)
  │   → Map SongEntity → MediaItem
  │   → Nếu shuffle bật → buildShuffledIndices()
  │
  ├── next()
  │   → REPEAT_ONE → giữ nguyên currentIndex
  │   → Shuffle → lấy index tiếp theo từ shuffledIndices
  │   → Hết queue + REPEAT_ALL → quay đầu
  │   → Hết queue + REPEAT_NONE → -1 (dừng)
  │
  ├── previous()
  │   → REPEAT_ONE → giữ nguyên
  │   → Nếu > 3s từ đầu bài → seek về 0
  │   → Shuffle → lấy index trước từ shuffledIndices
  │
  ├── cycleRepeatMode() → NONE → ONE → ALL → NONE
  ├── setShuffleEnabled(bool)
  │   → Bật: buildShuffledIndices() [currentIndex lên đầu]
  │   → Tắt: clear shuffledIndices
  │
  └── buildShuffledIndices()
      → Tạo [0, 1, 2, ...]
      → Shuffle ngẫu nhiên (Collections.shuffle)
      → Đưa currentIndex lên đầu
```

### File chính: `PlaybackQueue.java`

### Repeat/Shuffle state

- Đồng bộ với `PreferencesManager` khi thay đổi
- Repeat: lưu `RepeatMode` enum (NONE=0, ONE=1, ALL=2)
- Shuffle: lưu boolean

### MediaItem mapping

```java
SongEntity → MediaItem.Builder
  .setMediaId(song.id)
  .setUri(song.filePath) // content:// URI
  .setMediaMetadata(title, artist, albumTitle, artworkUri)
```

---

## 8. PlayerActivity UI Flow

### Sơ đồ

```
PlayerActivity.onCreate()
  → initViews(): albumArt, title, artist, seekbar, play/pause FAB, shuffle, repeat
  → musicPlayer.initialize()
  → setupClickListeners()
  → Tạo eventListener (onPlaybackStateChanged, onIsPlayingChanged, onMediaItemTransition, onPlayerError)
  → Cập nhật UI lần đầu

PlayerActivity.onStart()
  → setListener(eventListener)
  → Nếu đang phát → startSeekbarUpdates()
  → Cập nhật UI

onStop()
  → stopSeekbarUpdates()
  → Lưu lastPosition vào PreferencesManager
  → setListener(null)

Seekbar updates:
  → Handler.postDelayed(seekRunnable, 500ms)
    → updateSeekbarPosition()
      → seekbar.setValueTo(duration)
      → seekbar.setValue(position)
      → currentTime.setText(formatDuration(position))
```

### File chính: `PlayerActivity.java`

### Controls

| Control | Handler | MusicPlayer method |
|---------|---------|-------------------|
| Play/Pause FAB | `isPlaying() ? pause() : play()` | `pause()` / `play()` |
| Next button | `musicPlayer.next()` | `next()` |
| Previous button | `musicPlayer.previous()` | `previous()` |
| Shuffle button | `toggleShuffle()` | `setShuffleEnabled()` |
| Repeat button | `cycleRepeatMode()` | `cycleRepeatMode()` |
| Seekbar slider | `seekTo(value)` | `seekTo()` |
| Back button | `finish()` | - |

### Album Art + Palette

- Load album art qua `ImageLoader.load()` (Glide)
- Gọi `PaletteHelper.loadPaletteAsync()` để phân tích màu sắc từ ảnh bìa
- (TODO: apply palette colors vào background)

### Error handling

```java
onPlayerError(PlaybackException error) {
  → Snackbar: "Lỗi phát nhạc: <errorMsg>" (truncated 100 ký tự)
  → Action "Thử lại": gọi musicPlayer.play()
}
```

---

## 9. MiniPlayer Flow

### Sơ đồ

```
MiniPlayerFragment.onResume()
  → setListener(eventListener)
  → updateSongInfo()
  → updatePlayPauseIcon()
  → updateVisibility()
  
onPause() → setListener(null)

Listener:
  onMediaItemTransition → updateSongInfo() + updateVisibility()
  onIsPlayingChanged → updatePlayPauseIcon() + updateVisibility()
  onPlaybackStateChanged → updatePlayPauseIcon() + updateVisibility()

Click rootView → mở PlayerActivity
Click play/pause → musicPlayer.play() / musicPlayer.pause()
Click prev → musicPlayer.previous()
Click next → musicPlayer.next()
```

### File chính: `MiniPlayerFragment.java`

### Visibility logic

```java
updateVisibility() {
  boolean hasSong = musicPlayer.getCurrentMediaItem() != null
      || !musicPlayer.getPlaybackQueue().isEmpty();
  rootView.setVisibility(hasSong ? VISIBLE : GONE);
}
```

---

## 10. Background Service Flow

### Sơ đồ

```
MusicService (MediaSessionService)
  ├── onCreate()
  │   → musicPlayer = MusicPlayer.getInstance(this)
  │   → musicPlayer.initialize()
  │   → mediaSession = MediaSession.Builder(this, exoPlayer).build()
  │   → setListener để theo dõi playback state
  │
  ├── onGetSession(ControllerInfo)
  │   → return mediaSession [cho notification/Bluetooth/lock screen kết nối]
  │
  ├── onStartCommand(intent)
  │   → MediaSessionService tự động start foreground khi playback
  │
  ├── onTaskRemoved()
  │   → Nếu không đang phát → stopSelf()
  │
  └── onDestroy()
      → mediaSession.release()
      → musicPlayer.release() [giải phóng ExoPlayer]
```

### File chính: `MusicService.java`, `MediaSessionManager.java`

### Media3 Integration

- Kế thừa `MediaSessionService` (không phải Service thường)
- `MediaSession.Builder(this, exoPlayer)` — Media3 tự xử lý play/pause/next/prev
- Default MediaNotificationProvider tự động tạo notification
- Foreground service type: `mediaPlayback` (AndroidManifest)

### MediaSessionManager (wrapper tùy chỉnh)

- Constructor: `MediaSessionManager(service, player, callback)`
- Callback tùy chỉnh cho các lệnh: play, pause, stop, seekTo, skipToNext, skipToPrevious

---

## 11. Notification Flow

### Sơ đồ

```
MusicService.onCreate()
  → NotificationBuilder.createNotificationChannel() [một lần]

Khi playback thay đổi:
  → NotificationBuilder.build(isPlaying, title, artist, albumArtUri)
    → NotificationCompat.Builder với MediaStyle
    → 3 actions: prev, play/pause, next
    → setOngoing(isPlaying)
    → setVisibility(VISIBILITY_PUBLIC)

Intent từ notification action:
  → MusicService (action = ACTION_PLAY / ACTION_PAUSE / ...)
```

### File chính: `NotificationBuilder.java`

### Tham số Notification

| Tham số | Giá trị |
|---------|---------|
| Channel ID | `MUSIC_CHANNEL_ID` |
| Notification ID | 1 |
| Importance | `IMPORTANCE_LOW` (không popup, không âm thanh) |
| Visibility | `VISIBILITY_PUBLIC` (hiển thị trên lock screen) |
| Ongoing | true khi đang phát, false khi pause |
| Compact view | 3 buttons: prev, play/pause, next |

---

## 12. Playlist CRUD Flow

### Sơ đồ

```
CreatePlaylistDialog.show(context, anchorView)
  → MaterialAlertDialog với EditText
  → User nhập tên → nút "Tạo" enabled
  → User ấn "Tạo"
    → PlaylistRepository.createPlaylist(name, desc, callback)
      → executor.execute() [background]
        → PlaylistEntity(name, description)
        → playlistDao.insert(playlist) [REPLACE]
        → callback.onCreated(playlistId)
          → mainHandler.post: dismiss dialog + Snackbar
```

### File chính: `CreatePlaylistDialog.java`, `PlaylistRepository.java`, `PlaylistDao.java`

### PlaylistRepository methods

| Method | Mô tả | Thread |
|--------|-------|--------|
| `getAllPlaylists()` | LiveData tất cả playlist | Main (LiveData) |
| `getPlaylistById(id)` | LiveData chi tiết playlist | Main (LiveData) |
| `getSongsInPlaylist(id)` | LiveData bài hát (JOIN query) | Main (LiveData) |
| `createPlaylist(name, desc, callback)` | Tạo playlist mới (async) | Background |
| `createPlaylistSync(name, desc)` | Tạo playlist (sync) | Background |
| `updatePlaylist(playlist)` | Cập nhật thông tin | Background |
| `deletePlaylist(playlist)` | Xoá playlist (CASCADE) | Background |
| `deletePlaylistById(id)` | Xoá theo ID | Background |
| `addSongToPlaylist(playlistId, songId)` | Thêm bài hát | Background |
| `addSongsToPlaylist(playlistId, songIds)` | Thêm nhiều bài | Background |
| `removeSongFromPlaylist(playlistId, songId)` | Xoá bài khỏi playlist | Background |
| `clearPlaylist(playlistId)` | Xoá tất cả bài (giữ playlist) | Background |
| `isSongInPlaylist(playlistId, songId)` | Kiểm tra tồn tại (sync) | Any |

### Database schema

```
playlists (id, name, description, created_at, song_count)
songs (id, title, artist, album, ...)
playlist_songs (playlist_id, song_id, order_index) — FK CASCADE
```

---

## 13. Playlist Detail & Export Flow

### Sơ đồ

```
PlaylistDetailActivity.onCreate()
  → initViews(): name, description, song count, cover, FAB
  → setupRecyclerView(): SongAdapter
  → setupClickListeners()
  → observePlaylist() [LiveData playlist info]
  → observeSongs() [LiveData songs in playlist]

Export:
  → User click menu → PopupMenu (M3U / XML)
    → exportLauncher.launch([MIME_TYPE])
      → User chọn nơi lưu file (SAF)
      → doExport(uri)
        → PlaylistExporter.exportToM3U() / exportToXML()
          → ContentResolver.openOutputStream(uri)
          → PrintWriter ghi file
```

### File chính: `PlaylistDetailActivity.java`, `PlaylistExporter.java`

### Định dạng M3U

```
#EXTM3U
#PLAYLIST: <name>
#EXTINF:<duration_sec>,<artist> - <title>
<file_path>
```

### Định dạng XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<playlist name="..." created="..." songCount="...">
  <song title="..." artist="..." album="..." duration="..." filePath="..." />
</playlist>
```

### Tính năng

- Export qua Storage Access Framework (SAF) — tương thích Scoped Storage
- Escape XML characters (&, ", ', <, >)
- Callback run trên UI thread sau khi export hoàn thành

---

## 14. Playlist Import Flow

### Sơ đồ

```
User long-click FAB (PlaylistListFragment)
  → importLauncher.launch([M3U, XML MIME types])
    → User chọn file → URI
    → doImport(uri) [background thread]
      → PlaylistImporter.readFile(context, uri)
        → Đọc file → String
        → Phát hiện format: #EXTM3U? → M3U parser : XML parser
        → Parse metadata → List<SongEntry>
        → matchSongs() → searchSync(title) → filter by artist/duration
        → return ImportResult(entries, matchedSongIds)
      → repository.createPlaylist() → addSongsToPlaylist(matchedSongIds)
      → Snackbar: "Matched X/Y songs"
```

### File chính: `PlaylistImporter.java`, `PlaylistListFragment.java`

### Match logic

```java
matchSongs(context, entries):
  for each entry:
    results = repository.searchSync(entry.title)  // LIKE search
    for each result:
      if (artistMatch || durationMatch) → add to matchedSongIds

artistMatch = entry.artist.equalsIgnoreCase(song.artist)
            || entry.artist == "Unknown"
durationMatch = abs(song.duration - entry.duration) < 2000ms
```

---

## 15. Album Detail Flow

### Sơ đồ

```
AlbumGridFragment click album
  → AlbumDetailActivity.createIntent(context, albumName, artist)
  → AlbumDetailActivity.onCreate()
    → initViews: album art, name, artist, song count
    → setupRecyclerView: SongAdapter
    → observeSongs(): repository.getByAlbum(albumName)
      → LiveData → adapter.submitList()
      → updateSongCount()
    → Click song → MusicPlayer.play(currentSongs, position) → PlayerActivity
```

### File chính: `AlbumDetailActivity.java`

### Intent extras

| Extra | Type | Mô tả |
|-------|------|-------|
| `album_name` | String | Tên album |
| `artist` | String | Tên nghệ sĩ |

---

## 16. Artist Playback Flow

### Sơ đồ

```
ArtistListFragment click artist
  → playArtistSongs(artistName)
    → Filter allSongs (đã load sẵn) theo artist name
    → MusicPlayer.play(filteredList, 0)
    → startActivity(PlayerActivity)
```

### File chính: `ArtistListFragment.java`

### Đặc điểm

- `allSongs` được lưu từ LiveData observation — không tạo query mới
- Filter dùng cùng fallback `UNKNOWN_ARTIST` như `groupSongsByArtist()`
- Artist grouping dùng `LinkedHashMap` để giữ thứ tự

---

## 17. Search Flow

### Sơ đồ

```
SearchFragment.onViewCreated()
  → searchInput.addTextChangedListener()
    → onTextChanged() → handler.removeCallbacks()
      → postDelayed(performSearch(), 300ms) [debounce]
  → searchInput.setOnEditorActionListener() [IME_ACTION_SEARCH]
  
performSearch(query):
  → Nếu query rỗng → clear results + ẩn empty state
  → TODO: kết nối với SongRepository.searchSongs(query)
  → Hiện tại: hiển thị empty state
```

### File chính: `SearchFragment.java`

### Tham số

| Tham số | Giá trị |
|---------|---------|
| DEBOUNCE_MS | 300ms |
| Handler | Main Looper |

### Lưu ý

- TODO: Cần kết nối với `SongRepository.search()` khi có SearchAdapter
- `SongDao.search()` dùng `LIKE '%' || :query || '%'` trên title, artist, album

---

## 18. Offline Download Flow

### Sơ đồ

```
OfflineDownloadManager.downloadSong(song)
  → Kiểm tra activeDownloads (tránh tải trùng)
  → Kiểm tra CachedSongDao (tránh tải lại)
  → executor.submit(downloadFile(song))
  
downloadFile(song) [background]:
  → Nếu URI content:// hoặc file:// → copyLocalFile()
    → ContentResolver.openInputStream() → FileOutputStream
  → Nếu HTTP URL → downloadFromHttp()
    → HttpURLConnection → InputStream → FileOutputStream
  → rename .tmp → tên thật
  → CachedSongDao.insert(cacheEntity)
  → notify listener

cancelDownload(songId) → future.cancel(true)
clearAllCache() → cancelAll() → xoá file → xoá database
```

### File chính: `OfflineDownloadManager.java`

### Tham số

| Tham số | Giá trị |
|---------|---------|
| Executor | Single thread |
| Cache dir | `context.getExternalCacheDir()/offline_songs` |
| Buffer size | 8KB (8192) |
| Connect timeout | 15s |
| Read timeout | 30s |

### File extension mapping

| MIME Type | Extension |
|-----------|-----------|
| audio/mpeg, audio/mp3 | .mp3 |
| audio/flac | .flac |
| audio/ogg, application/ogg | .ogg |
| audio/m4a, audio/mp4, audio/aac | .m4a |
| audio/wav, audio/wave | .wav |
| audio/x-ms-wma | .wma |
| audio/opus | .opus |
| Khác | .mp3 (fallback) |

---

## 19. Cache Layer Flow

### Sơ đồ

```
CacheDataSourceFactory (Singleton)
  → SimpleCache
    → cacheDir: context.getCacheDir()/media_cache
    → Evictor: LeastRecentlyUsedCacheEvictor (500MB max)
    → Database: StandaloneDatabaseProvider
  → CacheDataSource.Factory
    → upstream: DefaultHttpDataSource.Factory
    → flags: FLAG_IGNORE_CACHE_ON_ERROR

ExoPlayer sử dụng CacheDataSourceFactory:
  → Khi stream: CacheDataSource kiểm tra cache trước
    → Có cache → đọc từ cache
    → Không cache → tải từ mạng → tự động cache chunk
  → Khi lỗi cache → fallback về stream trực tiếp (FLAG_IGNORE_CACHE_ON_ERROR)
```

### File chính: `CacheDataSourceFactory.java`

### Cache management

| Method | Mô tả |
|--------|-------|
| `getCacheDataSourceFactory()` | CacheDataSource.Factory cho ExoPlayer |
| `getSimpleCache()` | SimpleCache instance cho OfflineDownloadManager |
| `getCurrentCacheSize()` | Dung lượng đã dùng (bytes) |
| `getMaxCacheSize()` | Dung lượng tối đa (500MB mặc định) |
| `clearCache()` | Xoá toàn bộ cache |
| `release()` | Giải phóng SimpleCache |

---

## 20. Preferences & State Persistence Flow

### Sơ đồ

```
PreferencesManager (Singleton)
  → SharedPreferences (nghenhac_preferences)
  
Save:
  → setLastSongId(songId)
  → setLastPosition(positionMs)
  → setLastPlaylistId(playlistId)
  → setRepeatMode(mode)
  → setShuffleMode(enabled)
  → setVolume(volume)
  → setThemeMode(mode)
  → savePlaybackState(songId, positionMs, playlistId) [batch]

Restore:
  → getLastSongId() → -1 nếu chưa có
  → getLastPosition() → 0 mặc định
  → getRepeatMode() → NONE mặc định
  → isShuffleMode() → false mặc định
  → getVolume() → 0.8f mặc định
  → getThemeMode() → SYSTEM mặc định
  → isFirstLaunch() → true + set false [one-time check]
```

### File chính: `PreferencesManager.java`

### State được lưu

| Key | Type | Mô tả |
|-----|------|-------|
| `last_song_id` | long | Bài hát cuối cùng đã phát |
| `last_position` | long | Vị trí phát (ms) |
| `repeat_mode` | int | 0=NONE, 1=ONE, 2=ALL |
| `shuffle_mode` | boolean | Xáo trộn bật/tắt |
| `volume` | float | Âm lượng (0.0 - 1.0) |
| `last_playlist_id` | long | Playlist cuối cùng |
| `first_launch` | boolean | Lần đầu chạy app |
| `theme_mode` | int | 0=SYSTEM, 1=LIGHT, 2=DARK |

---

## 21. Error Handling Flow

### 21.1 Uncaught Exception Handler

```
NgheNhacApp.setupUncaughtExceptionHandler()
  → Bắt tất cả exception không được try-catch
  → Log: thread name, exception class, message
  → Log: full stack trace
  → Log: cause chain
  → Chuyển cho default handler (Android kết thúc process)
```

### 21.2 MusicPlayer Error Handling

```
onPlayerError(PlaybackException)
  → retryCount++
  → Log: "Player error (attempt X/3): <error>"
  → Nếu <= 3 lần: postDelayed(prepare() + play(), 1000ms)
  → Nếu hết 3 lần:
    → Log: "Max retry count reached. Pausing player"
    → exoPlayer.pause() [giữ queue, không stop()]
    → retryCount = 0
  → UI nhận onPlayerError → Snackbar + nút "Thử lại"
```

### 21.3 Content Observer Error

```
MusicContentObserver
  → catch (Exception) khi unregister — tránh crash
  → Thread-safe với synchronized
```

### 21.4 MediaStore Scanner Error

```
MediaStoreScanner.scanAllSongs()
  → SecurityException → log + return empty list
  → Exception (chung) → log + return empty list
  
cursorToSongEntity()
  → Exception (từng dòng) → log + return null (bỏ qua dòng lỗi)
```

---

## 22. Permission Flow

### Sơ đồ

```
MainActivity.checkAndRequestPermissions()
  → getStoragePermission():
    → API 33+ (TIRAMISU): READ_MEDIA_AUDIO
    → API 28-32: READ_EXTERNAL_STORAGE
  → ContextCompat.checkSelfPermission()
    → Đã cấp → scanMediaStore()
    → Chưa cấp:
      → shouldShowRequestPermissionRationale()?
        → Yes: AlertDialog giải thích → nếu OK → requestPermissions()
        → No: requestPermissions() trực tiếp

onRequestPermissionsResult():
  → Granted → scanMediaStore()
  → Denied:
    → shouldShowRequestPermissionRationale()?
      → true (từ chối lần đầu): Snackbar "Cần quyền để đọc nhạc"
      → false (từ chối vĩnh viễn): Snackbar "Vào Settings để bật quyền"
        + Action "Cài đặt" → Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
```

### File chính: `MainActivity.java`

### Quyền yêu cầu

| API Level | Permission | Ghi chú |
|-----------|-----------|---------|
| API 28-32 (Android 9-12) | `READ_EXTERNAL_STORAGE` | `maxSdkVersion="32"` |
| API 33+ (Android 13+) | `READ_MEDIA_AUDIO` | Không cần maxSdkVersion |

---

## Phụ lục: Kiến trúc Tổng Thể

```
NgheNhacApp (Application)
  │
  ├── Data Layer
  │   ├── Room Database (AppDatabase)
  │   │   ├── SongDao → songs table
  │   │   ├── PlaylistDao → playlists + playlist_songs
  │   │   └── CachedSongDao → cached_songs
  │   ├── MediaStoreScanner (ContentResolver query)
  │   ├── MusicContentObserver (ContentObserver)
  │   ├── PreferencesManager (SharedPreferences)
  │   ├── Repository
  │   │   ├── SongRepository
  │   │   └── PlaylistRepository
  │   └── Remote
  │       ├── RetrofitClient
  │       └── MusicApiService
  │
  ├── Player Layer
  │   ├── MusicPlayer (ExoPlayer wrapper, singleton)
  │   ├── PlaybackQueue (queue management)
  │   ├── PlayerEventListener (event forwarding)
  │   ├── CacheDataSourceFactory (SimpleCache)
  │   └── OfflineDownloadManager (full file download)
  │
  ├── Service Layer
  │   ├── MusicService (MediaSessionService)
  │   ├── MediaSessionManager
  │   └── NotificationBuilder
  │
  ├── UI Layer
  │   ├── MainActivity (permission + scan + navigation)
  │   ├── Library
  │   │   ├── LibraryFragment (TabLayout + ViewPager2)
  │   │   ├── SongListFragment
  │   │   ├── AlbumGridFragment
  │   │   ├── ArtistListFragment
  │   │   ├── PlaylistListFragment
  │   │   ├── AlbumDetailActivity
  │   │   ├── PlaylistDetailActivity
  │   │   └── CreatePlaylistDialog
  │   ├── Player
  │   │   ├── PlayerActivity (full screen)
  │   │   └── MiniPlayerFragment (bottom bar)
  │   ├── SearchFragment
  │   └── SettingsFragment
  │
  └── Util Layer
      ├── ImageLoader (Glide)
      ├── PaletteHelper (color extraction)
      ├── FilePickerUtil
      ├── PlaylistExporter (M3U/XML)
      ├── PlaylistImporter (M3U/XML)
      └── SecurePreferences
```

---

> Tài liệu này được tạo từ mã nguồn NgheNhac (51 file Java).  
> Mọi thông tin đều được trích xuất trực tiếp từ code và JavaDoc.
