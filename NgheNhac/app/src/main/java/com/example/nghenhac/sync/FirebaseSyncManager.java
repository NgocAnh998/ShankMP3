package com.example.nghenhac.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.local.entity.PlaylistSongCrossRef;
import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.repository.PlaylistRepository;
import com.example.nghenhac.data.repository.SongRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Đồng bộ playlist và yêu thích lên Firebase Realtime Database.
 *
 * Nguyên lý:
 * - Cấu trúc Firebase: /users/{uid}/playlists/{playlistId}/songs/{songId}: true
 * - Cấu trúc Firebase: /users/{uid}/favorites/{songId}: true
 * - Đồng bộ hai chiều: local → Firebase (upload) và Firebase → local (download/merge).
 * - Sử dụng SongMatcher để match bài hát giữa các thiết bị.
 * - Tất cả thao tác blocking (dùng CountDownLatch) — phù hợp chạy trong background thread.
 *
 * Luồng xử lý:
 * 1. Upload: đọc dữ liệu local → ghi lên Firebase → overwrite.
 * 2. Download: đọc từ Firebase → match với local database → insert/update.
 * 3. Merge: tải về → match → merge vào local → re-upload nếu cần.
 *
 * Input:
 * - Context: truy cập database local.
 * - User phải đăng nhập trước khi gọi sync.
 *
 * Output:
 * - Dữ liệu playlist và yêu thích được đồng bộ giữa local và Firebase.
 *
 * Lưu ý:
 * - Tất cả phương thức blocking — gọi từ background thread.
 * - Timeout mặc định: 30 giây cho mỗi thao tác Firebase.
 * - Không đồng bộ file nhạc, chỉ đồng bộ metadata (playlist structure + favorites).
 */
public class FirebaseSyncManager {

    private static final String TAG = "FirebaseSyncManager";
    private static final long SYNC_TIMEOUT_SECONDS = 30;

    private static volatile FirebaseSyncManager instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabase database;
    private final SongMatcher songMatcher;
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;

    // ── Singleton ──

    private FirebaseSyncManager(@NonNull Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.database = FirebaseDatabase.getInstance();
        this.songMatcher = new SongMatcher();
        this.songRepository = SongRepository.getInstance(context);
        this.playlistRepository = PlaylistRepository.getInstance(context);
    }

    public static FirebaseSyncManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (FirebaseSyncManager.class) {
                if (instance == null) {
                    instance = new FirebaseSyncManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ════════════════════════════════════════════
    //  Sync Methods
    // ════════════════════════════════════════════

    /**
     * Upload dữ liệu local lên Firebase.
     *
     * Nguyên lý:
     * - Đọc playlist + yêu thích từ local database.
     * - Ghi toàn bộ lên Firebase (overwrite).
     * - Sử dụng CountDownLatch để blocking chờ kết quả.
     *
     * Luồng xử lý:
     * 1. Kiểm tra user đã đăng nhập chưa.
     * 2. Lấy tất cả playlist + yêu thích từ local.
     * 3. Tạo Map<playlistId, Map<songId, true>> và Map<favoriteId, true>.
     * 4. setValue() lên Firebase.
     * 5. CountDownLatch.await(timeout).
     * 6. Trả về SyncResult.
     *
     * Output:
     * @return SyncResult chứa thông tin kết quả đồng bộ.
     */
    @NonNull
    public SyncResult uploadToFirebase(@NonNull Context context) throws SyncException {
        String uid = getUserIdOrThrow();
        DatabaseReference userRef = database.getReference("users").child(uid);

        // Lấy dữ liệu local
        List<PlaylistEntity> playlists = getLocalPlaylistsSync();
        List<SongEntity> favorites = getLocalFavoritesSync();

        // Tạo data structure cho Firebase
        Map<String, Object> syncData = new HashMap<>();

        // Playlists
        Map<String, Object> playlistsData = new HashMap<>();
        for (PlaylistEntity playlist : playlists) {
            List<SongEntity> songs = getSongsInPlaylistSync(playlist.getId());
            Map<String, Object> songsMap = new HashMap<>();
            for (int i = 0; i < songs.size(); i++) {
                SongEntity song = songs.get(i);
                Map<String, Object> songData = new HashMap<>();
                songData.put("title", song.getTitle());
                songData.put("artist", song.getArtist());
                songData.put("album", song.getAlbum());
                songData.put("order", i);
                songsMap.put(String.valueOf(song.getId()), songData);
            }

            Map<String, Object> playlistData = new HashMap<>();
            playlistData.put("name", playlist.getName());
            playlistData.put("description", playlist.getDescription() != null ? playlist.getDescription() : "");
            playlistData.put("songs", songsMap);
            playlistsData.put(String.valueOf(playlist.getId()), playlistData);
        }
        syncData.put("playlists", playlistsData);

        // Favorites
        Map<String, Object> favoritesData = new HashMap<>();
        for (SongEntity song : favorites) {
            Map<String, Object> songData = new HashMap<>();
            songData.put("title", song.getTitle());
            songData.put("artist", song.getArtist());
            songData.put("album", song.getAlbum());
            favoritesData.put(String.valueOf(song.getId()), songData);
        }
        syncData.put("favorites", favoritesData);

        // Write to Firebase
        final CountDownLatch latch = new CountDownLatch(1);
        final SyncResult result = new SyncResult();

        userRef.setValue(syncData, (error, ref) -> {
            if (error != null) {
                result.setError(new SyncException("Lỗi upload: " + error.getMessage()));
            } else {
                result.setPlaylistsUploaded(playlists.size());
                result.setFavoritesUploaded(favorites.size());
                result.setSuccess(true);
            }
            latch.countDown();
        });

        try {
            boolean completed = latch.await(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                throw new SyncException("Upload timeout sau " + SYNC_TIMEOUT_SECONDS + " giây");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SyncException("Upload bị gián đoạn");
        }

        if (!result.isSuccess()) {
            SyncException error = result.getError();
            throw error != null ? error : new SyncException("Upload thất bại");
        }

        Log.d(TAG, "Upload success: " + result.getPlaylistsUploaded() + " playlists, "
                + result.getFavoritesUploaded() + " favorites");
        return result;
    }

    /**
     * Download dữ liệu từ Firebase và merge vào local database.
     *
     * Nguyên lý:
     * - Đọc toàn bộ dữ liệu user từ Firebase.
     * - Với mỗi playlist Firebase: match songs → merge vào local.
     * - Với favorites: match songs → cập nhật is_favorite trong local.
     *
     * Luồng xử lý:
     * 1. Kiểm tra user đã đăng nhập chưa.
     * 2. Đọc snapshot từ Firebase (addListenerForSingleValueEvent).
     * 3. Parse playlists → match songs → create playlist + add songs.
     * 4. Parse favorites → match songs → updateFavorite().
     * 5. Trả về SyncResult.
     *
     * Output:
     * @return SyncResult chứa thông tin kết quả đồng bộ.
     */
    @NonNull
    public SyncResult downloadFromFirebase(@NonNull Context context) throws SyncException {
        String uid = getUserIdOrThrow();
        DatabaseReference userRef = database.getReference("users").child(uid);

        final CountDownLatch latch = new CountDownLatch(1);
        final SyncResult result = new SyncResult();

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (!snapshot.exists()) {
                        result.setSuccess(true);
                        latch.countDown();
                        return;
                    }

                    // Lấy danh sách bài hát local để matching
                    List<SongEntity> localSongs = getLocalAllSongsSync();

                    // Parse playlists
                    DataSnapshot playlistsSnapshot = snapshot.child("playlists");
                    int playlistsProcessed = 0;
                    for (DataSnapshot playlistSnap : playlistsSnapshot.getChildren()) {
                        String firebasePlaylistId = playlistSnap.getKey();
                        String name = playlistSnap.child("name").getValue(String.class);
                        String description = playlistSnap.child("description").getValue(String.class);

                        if (name != null && !name.isEmpty()) {
                            // Create playlist locally
                            long localPlaylistId = playlistRepository.createPlaylistSync(name, description);

                            // Parse songs
                            DataSnapshot songsSnapshot = playlistSnap.child("songs");
                            List<Long> matchedSongIds = new ArrayList<>();

                            for (DataSnapshot songSnap : songsSnapshot.getChildren()) {
                                String title = songSnap.child("title").getValue(String.class);
                                String artist = songSnap.child("artist").getValue(String.class);
                                String album = songSnap.child("album").getValue(String.class);
                                Long order = songSnap.child("order").getValue(Long.class);

                                if (title != null && artist != null) {
                                    SongEntity firebaseSong = createTempSong(title, artist, album);
                                    SongMatcher.MatchResult matchResult = songMatcher.findMatch(firebaseSong, localSongs);

                                    if (matchResult.isMatched() && matchResult.getLocalSong() != null) {
                                        matchedSongIds.add(matchResult.getLocalSong().getId());
                                    }
                                }
                            }

                            // Add matched songs to playlist
                            if (!matchedSongIds.isEmpty()) {
                                playlistRepository.addSongsToPlaylist(localPlaylistId, matchedSongIds);
                            }
                            playlistsProcessed++;
                        }
                    }
                    result.setPlaylistsDownloaded(playlistsProcessed);

                    // Parse favorites
                    DataSnapshot favoritesSnapshot = snapshot.child("favorites");
                    int favoritesProcessed = 0;
                    for (DataSnapshot favSnap : favoritesSnapshot.getChildren()) {
                        String title = favSnap.child("title").getValue(String.class);
                        String artist = favSnap.child("artist").getValue(String.class);
                        String album = favSnap.child("album").getValue(String.class);

                        if (title != null && artist != null) {
                            SongEntity firebaseSong = createTempSong(title, artist, album);
                            SongMatcher.MatchResult matchResult = songMatcher.findMatch(firebaseSong, localSongs);

                            if (matchResult.isMatched() && matchResult.getLocalSong() != null) {
                                songRepository.updateFavorite(matchResult.getLocalSong().getId(), true);
                                favoritesProcessed++;
                            }
                        }
                    }
                    result.setFavoritesDownloaded(favoritesProcessed);
                    result.setSuccess(true);

                } catch (Exception e) {
                    result.setError(new SyncException("Lỗi xử lý dữ liệu: " + e.getMessage()));
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                result.setError(new SyncException("Lỗi download: " + error.getMessage()));
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                throw new SyncException("Download timeout sau " + SYNC_TIMEOUT_SECONDS + " giây");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SyncException("Download bị gián đoạn");
        }

        if (!result.isSuccess()) {
            SyncException error = result.getError();
            throw error != null ? error : new SyncException("Download thất bại");
        }

        Log.d(TAG, "Download success: " + result.getPlaylistsDownloaded() + " playlists, "
                + result.getFavoritesDownloaded() + " favorites");
        return result;
    }

    /**
     * Đồng bộ hai chiều: upload → download (full sync).
     *
     * Nguyên lý:
     * - Upload local data lên Firebase trước.
     * - Sau đó download dữ liệu từ Firebase merge vào local.
     * - Kết quả cuối: cả hai nơi đều có cùng dữ liệu.
     *
     * Output:
     * @return SyncResult tổng hợp của upload + download.
     */
    @NonNull
    public SyncResult fullSync(@NonNull Context context) throws SyncException {
        SyncResult uploadResult = uploadToFirebase(context);
        SyncResult downloadResult = downloadFromFirebase(context);
        return SyncResult.merge(uploadResult, downloadResult);
    }

    // ════════════════════════════════════════════
    //  Private Helpers
    // ════════════════════════════════════════════

    /**
     * Lấy UID của user hiện tại, throw exception nếu chưa đăng nhập.
     *
     * Output: UID của user.
     */
    @NonNull
    private String getUserIdOrThrow() throws SyncException {
        String uid = firebaseAuth.getUid();
        if (uid == null) {
            throw new SyncException("Bạn cần đăng nhập để đồng bộ");
        }
        return uid;
    }

    /**
     * Lấy danh sách playlist từ local database (phiên bản đồng bộ).
     *
     * Nguyên lý:
     * - Dùng getValue() trên LiveData — dữ liệu đã được load khi Repository khởi tạo.
     * - Nếu chưa có giá trị, trả về danh sách rỗng thay vì spin-wait.
     * - Caller nên gọi từ background thread sau khi dữ liệu đã được load.
     *
     * Output: Danh sách playlist local (không null, có thể rỗng).
     */
    @NonNull
    private List<PlaylistEntity> getLocalPlaylistsSync() {
        try {
            List<PlaylistEntity> playlists = playlistRepository.getAllPlaylists().getValue();
            return playlists != null ? playlists : new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get local playlists", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách bài hát trong playlist (phiên bản đồng bộ).
     */
    @NonNull
    private List<SongEntity> getSongsInPlaylistSync(long playlistId) {
        try {
            List<SongEntity> songs = playlistRepository.getSongsInPlaylist(playlistId).getValue();
            return songs != null ? songs : new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get songs for playlist " + playlistId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách bài hát yêu thích từ local (phiên bản đồng bộ).
     */
    @NonNull
    private List<SongEntity> getLocalFavoritesSync() {
        try {
            List<SongEntity> favorites = songRepository.getFavorites().getValue();
            return favorites != null ? favorites : new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get local favorites", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tất cả bài hát local (phiên bản đồng bộ).
     */
    @NonNull
    private List<SongEntity> getLocalAllSongsSync() {
        try {
            List<SongEntity> songs = songRepository.getAllSongs().getValue();
            return songs != null ? songs : new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get all local songs", e);
            return new ArrayList<>();
        }
    }

    /**
     * Tạo SongEntity tạm thời từ metadata để matching.
     *
     * Nguyên lý:
     * - SongMatcher cần SongEntity để tính confidence.
     * - Tạo entity tạm chỉ với title, artist, album (các trường còn lại mặc định).
     *
     * Input:
     * @param title  Tên bài hát.
     * @param artist Tên nghệ sĩ.
     * @param album  Tên album.
     *
     * Output:
     * @return SongEntity với metadata đã cho.
     */
    @NonNull
    private SongEntity createTempSong(@Nullable String title, @Nullable String artist, @Nullable String album) {
        return new SongEntity(
                title != null ? title : "",
                artist != null ? artist : "",
                album != null ? album : "",
                0L, null, null, null, System.currentTimeMillis()
        );
    }

    // ════════════════════════════════════════════
    //  SyncResult — inner class
    // ════════════════════════════════════════════

    /**
     * Kết quả của một lần đồng bộ.
     *
     * Nguyên lý:
     * - Chứa thông tin chi tiết về số lượng dữ liệu đã đồng bộ.
     * - Upload: playlistsUploaded, favoritesUploaded.
     * - Download: playlistsDownloaded, favoritesDownloaded.
     * - Full sync: merge cả upload và download.
     */
    public static class SyncResult {
        private boolean success;
        private int playlistsUploaded;
        private int favoritesUploaded;
        private int playlistsDownloaded;
        private int favoritesDownloaded;
        private SyncException error;

        SyncResult() {}

        public boolean isSuccess() { return success; }
        void setSuccess(boolean success) { this.success = success; }

        public int getPlaylistsUploaded() { return playlistsUploaded; }
        void setPlaylistsUploaded(int count) { this.playlistsUploaded = count; }

        public int getFavoritesUploaded() { return favoritesUploaded; }
        void setFavoritesUploaded(int count) { this.favoritesUploaded = count; }

        public int getPlaylistsDownloaded() { return playlistsDownloaded; }
        void setPlaylistsDownloaded(int count) { this.playlistsDownloaded = count; }

        public int getFavoritesDownloaded() { return favoritesDownloaded; }
        void setFavoritesDownloaded(int count) { this.favoritesDownloaded = count; }

        @Nullable
        public SyncException getError() { return error; }
        void setError(SyncException error) { this.error = error; }

        /**
         * Gộp kết quả upload + download thành một SyncResult.
         *
         * Input:
         * @param upload   Kết quả upload.
         * @param download Kết quả download.
         *
         * Output:
         * @return SyncResult tổng hợp.
         */
        @NonNull
        static SyncResult merge(@NonNull SyncResult upload, @NonNull SyncResult download) {
            SyncResult merged = new SyncResult();
            merged.success = upload.success && download.success;
            merged.playlistsUploaded = upload.playlistsUploaded;
            merged.favoritesUploaded = upload.favoritesUploaded;
            merged.playlistsDownloaded = download.playlistsDownloaded;
            merged.favoritesDownloaded = download.favoritesDownloaded;
            if (!merged.success) {
                merged.error = upload.error != null ? upload.error : download.error;
            }
            return merged;
        }
    }

    // ════════════════════════════════════════════
    //  SyncException
    // ════════════════════════════════════════════

    /**
     * Exception cho lỗi đồng bộ.
     * Kế thừa Exception (checked) để caller phải xử lý.
     */
    public static class SyncException extends Exception {
        public SyncException(String message) {
            super(message);
        }
    }
}
