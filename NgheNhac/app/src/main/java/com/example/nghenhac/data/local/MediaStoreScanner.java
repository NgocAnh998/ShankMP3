package com.example.nghenhac.data.local;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.nghenhac.data.local.entity.SongEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Quét MediaStore để lấy danh sách bài hát từ bộ nhớ thiết bị.
 *
 * Nguyên lý:
 * - Dùng ContentResolver.query() để đọc dữ liệu từ MediaStore (CSDL hệ thống Android).
 * - Chỉ lấy file audio (IS_MUSIC = 1) có thời lượng > 0 (loại bỏ ringtone, notification, podcast).
 * - Hỗ trợ Scoped Storage (API 29+): dùng content:// URI thay vì đường dẫn file.
 * - Không dùng _DATA (deprecated trên API 29+), thay bằng content:// URI.
 *
 * Luồng xử lý:
 * 1. Xác định collection URI phù hợp API level (API 28: EXTERNAL_CONTENT_URI, API 29+: getContentUri).
 * 2. Query MediaStore với projection (các cột cần lấy) và selection (IS_MUSIC=1 AND DURATION>0).
 * 3. Duyệt cursor → map từng dòng thành SongEntity.
 * 4. Xây dựng album art URI (content://media/external/audio/albumart/<albumId>).
 * 5. Trả về danh sách SongEntity.
 *
 * Input:
 * - Context (để lấy ContentResolver).
 *
 * Output:
 * - List<SongEntity> chứa metadata của tất cả bài hát trên thiết bị.
 *
 * Lưu ý:
 * - Phải chạy trên background thread (WorkerThread).
 * - Cần permission READ_EXTERNAL_STORAGE (API 28-32) hoặc READ_MEDIA_AUDIO (API 33+).
 * - Cursor được tự động đóng nhờ try-with-resources (Cursor implements AutoCloseable).
 * - Null/exception được xử lý an toàn, trả về danh sách rỗng nếu có lỗi.
 */
public final class MediaStoreScanner {

    private static final String TAG = "MediaStoreScanner";

    /**
     * Các cột cần query từ MediaStore.Audio.Media.
     *
     * Nguyên lý:
     * - Chỉ query đúng các cột cần thiết để tối ưu hiệu năng.
     * - Không dùng DATA / _DATA vì deprecated trên API 29+.
     * - Thay vào đó dùng content:// URI xây dựng từ _ID.
     */
    private static final String[] PROJECTION = {
            MediaStore.Audio.AudioColumns._ID,           // 0
            MediaStore.Audio.AudioColumns.TITLE,          // 1
            MediaStore.Audio.AudioColumns.ARTIST,         // 2
            MediaStore.Audio.AudioColumns.ALBUM,          // 3
            MediaStore.Audio.AudioColumns.ALBUM_ID,       // 4
            MediaStore.Audio.AudioColumns.DURATION,       // 5
            MediaStore.Audio.AudioColumns.TRACK,          // 6
            MediaStore.Audio.AudioColumns.DATE_ADDED,     // 7
            MediaStore.Audio.AudioColumns.MIME_TYPE,      // 8
            MediaStore.Audio.AudioColumns.SIZE,           // 9
    };

    // Index constants for PROJECTION
    private static final int COL_ID = 0;
    private static final int COL_TITLE = 1;
    private static final int COL_ARTIST = 2;
    private static final int COL_ALBUM = 3;
    private static final int COL_ALBUM_ID = 4;
    private static final int COL_DURATION = 5;
    private static final int COL_TRACK = 6;
    private static final int COL_DATE_ADDED = 7;
    private static final int COL_MIME_TYPE = 8;
    private static final int COL_SIZE = 9;

    /**
     * Điều kiện lọc: chỉ lấy file nhạc, loại bỏ file không phải nhạc.
     *
     * - IS_MUSIC = 1: MediaStore tự động phân loại, loại bỏ ringtone, notification, podcast.
     * - DURATION > 0: loại bỏ file audio ngắn (VD: sound effect).
     */
    private static final String SELECTION =
            MediaStore.Audio.AudioColumns.IS_MUSIC + " = 1 AND " +
            MediaStore.Audio.AudioColumns.DURATION + " > 0";

    private static final String SORT_ORDER =
            MediaStore.Audio.AudioColumns.TITLE + " ASC";

    /**
     * Utility class — không cho instantiate.
     */
    private MediaStoreScanner() {}

    /**
     * Quét tất cả bài hát từ MediaStore.
     *
     * Nguyên lý:
     * - Query ContentResolver với projection, selection, sort order.
     * - Xử lý cursor bằng try-with-resources để đảm bảo đóng cursor.
     * - SecurityException được bắt riêng để xử lý lỗi permission.
     *
     * Luồng xử lý:
     * 1. Lấy ContentResolver từ context.
     * 2. Xác định URI phù hợp API level.
     * 3. Query MediaStore → nhận Cursor.
     * 4. Duyệt cursor → map từng dòng thành SongEntity.
     * 5. Trả về danh sách.
     *
     * Input:
     * @param context Context (Application context đủ dùng).
     *
     * Output:
     * @return Danh sách SongEntity, không bao giờ null (rỗng nếu có lỗi hoặc không có nhạc).
     */
    @WorkerThread
    @NonNull
    public static List<SongEntity> scanAllSongs(@NonNull Context context) {
        List<SongEntity> songs = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        Uri collectionUri = getAudioCollectionUri();

        try (Cursor cursor = resolver.query(
                collectionUri,
                PROJECTION,
                SELECTION,
                null,
                SORT_ORDER
        )) {
            if (cursor == null) {
                Log.w(TAG, "MediaStore query returned null cursor");
                return songs;
            }

            int count = cursor.getCount();
            Log.d(TAG, "Found " + count + " audio files in MediaStore");

            while (cursor.moveToNext()) {
                SongEntity song = cursorToSongEntity(cursor, context);
                if (song != null) {
                    songs.add(song);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied when scanning MediaStore", e);
        } catch (Exception e) {
            Log.e(TAG, "Error scanning MediaStore", e);
        }

        return songs;
    }

    /**
     * Lấy URI collection phù hợp với API level.
     *
     * Nguyên lý:
     * - API 28 (Android 9): dùng EXTERNAL_CONTENT_URI (legacy).
     * - API 29+ (Android 10+): dùng getContentUri(VOLUME_EXTERNAL) hỗ trợ Scoped Storage.
     * - Khác biệt này rất quan trọng để tương thích với Scoped Storage.
     *
     * Output: Uri để query MediaStore.
     */
    @NonNull
    private static Uri getAudioCollectionUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
    }

    /**
     * Chuyển đổi một dòng cursor từ MediaStore thành SongEntity.
     *
     * Nguyên lý:
     * - Map từng cột trong PROJECTION vào trường tương ứng của SongEntity.
     * - Fallback title/artist/album thành "Unknown" nếu null.
     * - Xây dựng content:// URI cho file_path (thay vì đường dẫn file vật lý).
     * - Xây dựng album art URI từ albumId.
     *
     * Luồng xử lý:
     * 1. Đọc mediaStoreId → tạo contentUri từ ID + collectionUri.
     * 2. Đọc metadata strings → fallback nếu null/empty.
     * 3. Đọc các trường số (duration, dateAdded, trackNumber, fileSize).
     * 4. Đọc albumId → xây dựng albumArtUri.
     * 5. Tạo SongEntity → set các trường còn lại → return.
     *
     * Input:
     * @param cursor  Cursor đang ở dòng cần xử lý.
     * @param context Context (dùng sau nếu cần open InputStream).
     *
     * Output:
     * @return SongEntity hoặc null nếu có lỗi đọc dòng.
     *
     * Lưu ý:
     * - Exception được bắt riêng từng dòng để không làm gián đoạn quá trình quét.
     * - Các trường không bắt buộc (mimeType, fileSize) để mặc định nếu null.
     */
    @WorkerThread
    @Nullable
    private static SongEntity cursorToSongEntity(@NonNull Cursor cursor, @NonNull Context context) {
        try {
            long mediaStoreId = cursor.getLong(COL_ID);
            Uri contentUri = ContentUris.withAppendedId(getAudioCollectionUri(), mediaStoreId);

            String title = cursor.getString(COL_TITLE);
            String artist = cursor.getString(COL_ARTIST);
            String album = cursor.getString(COL_ALBUM);

            if (title == null || title.isEmpty()) {
                title = "Unknown";
            }
            if (artist == null || artist.isEmpty()) {
                artist = "Unknown Artist";
            }
            if (album == null || album.isEmpty()) {
                album = "Unknown Album";
            }

            long duration = cursor.getLong(COL_DURATION);
            long dateAdded = cursor.getLong(COL_DATE_ADDED);
            int trackNumber = cursor.getInt(COL_TRACK);
            String mimeType = cursor.getString(COL_MIME_TYPE);
            long fileSize = cursor.getLong(COL_SIZE);

            long albumId = cursor.getLong(COL_ALBUM_ID);
            String albumArtUri = buildAlbumArtUri(albumId);

            SongEntity song = new SongEntity(title, artist, album, duration,
                    contentUri.toString(), albumArtUri, mediaStoreId, dateAdded);
            song.setTrackNumber(trackNumber);
            song.setMimeType(mimeType != null ? mimeType : "audio/*");
            song.setFileSize(fileSize);

            return song;

        } catch (Exception e) {
            Log.w(TAG, "Error reading cursor row", e);
            return null;
        }
    }

    /**
     * Xây dựng URI cho album art từ albumId.
     *
     * Nguyên lý:
     * - Album art được truy cập qua URI content://media/external/audio/albumart/<albumId>.
     * - URI này có thể dùng trực tiếp với Glide để load ảnh bìa album.
     * - albumId <= 0 (không có album art) → trả về null.
     *
     * Input:
     * @param albumId ID của album từ MediaStore.
     *
     * Output:
     * @return URI string của album art, hoặc null nếu không có.
     */
    @Nullable
    private static String buildAlbumArtUri(long albumId) {
        if (albumId <= 0) return null;
        Uri uri = Uri.parse("content://media/external/audio/albumart/" + albumId);
        return uri.toString();
    }
}
