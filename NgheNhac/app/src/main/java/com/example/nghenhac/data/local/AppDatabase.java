package com.example.nghenhac.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.nghenhac.data.local.dao.CachedSongDao;
import com.example.nghenhac.data.local.dao.PlaylistDao;
import com.example.nghenhac.data.local.dao.SongDao;
import com.example.nghenhac.data.local.entity.CachedSongEntity;
import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.local.entity.PlaylistSongCrossRef;
import com.example.nghenhac.data.local.entity.SongEntity;

/**
 * Room Database cho NgheNhac — trung tâm lưu trữ dữ liệu local.
 *
 * Nguyên lý:
 * - Singleton pattern với double-checked locking, thread-safe.
 * - Quản lý 4 entities: Song (bài hát), Playlist (danh sách phát),
 *   PlaylistSongCrossRef (bảng trung gian N-N), CachedSong (cache offline).
 * - Room tự động sinh code implementation dựa trên annotation @Database.
 *
 * Luồng xử lý:
 * 1. App khởi động → getInstance() lần đầu → Room tạo/mở database.
 * 2. Các Repository gọi db.songDao() / db.playlistDao() / db.cachedSongDao().
 * 3. Khi schema thay đổi → cần viết Migration và tăng version.
 *
 * Phiên bản hiện tại: 1
 * exportSchema = false (không lưu schema file cho CI).
 *
 * Lưu ý:
 * - Chỉ tạo một instance duy nhất trong toàn bộ vòng đời app.
 * - Application context được dùng để tránh memory leak.
 * - Khi nâng version database, phải viết Migration — nếu không app crash.
 */
@Database(
        entities = {
                SongEntity.class,
                PlaylistEntity.class,
                PlaylistSongCrossRef.class,
                CachedSongEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "nghenhac_database";
    private static volatile AppDatabase instance;

    // ── DAOs ──

    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();
    public abstract CachedSongDao cachedSongDao();

    // ── Singleton ──

    /**
     * Lấy instance duy nhất của AppDatabase.
     *
     * Nguyên lý:
     * - Dùng double-checked locking: kiểm tra instance 2 lần (trước và sau synchronized).
     * - volatile đảm bảo visibility giữa các thread.
     * - build() với addCallback để seed data khi database được tạo lần đầu.
     *
     * Input:
     * @param context Context (application context được dùng bên trong).
     *
     * Output: AppDatabase instance duy nhất.
     */
    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DB_NAME
                    )
                    .addCallback(sRoomCallback)
                    .build();
                }
            }
        }
        return instance;
    }

    // ── Callback (seed data khi database được tạo lần đầu) ──

    /**
     * RoomDatabase.Callback — chạy khi database được tạo lần đầu.
     *
     * Nguyên lý:
     * - onCreate chạy trong background thread.
     * - Có thể dùng để seed dữ liệu mẫu (VD: thêm playlist mặc định).
     *
     * Lưu ý:
     * - Chỉ chạy một lần duy nhất khi database chưa tồn tại.
     * - Không chạy khi database đã có (kể cả xoá hết dữ liệu).
     */
    private static final RoomDatabase.Callback sRoomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Có thể thêm seed data tại đây nếu cần
            // Chạy trong background thread
        }
    };

    // ── Migration (dùng sau này khi nâng version) ──

    // Ví dụ: Migration từ version 1 → 2
    // static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    //     @Override
    //     public void migrate(@NonNull SupportSQLiteDatabase database) {
    //         database.execSQL("ALTER TABLE songs ADD COLUMN new_column INTEGER DEFAULT 0");
    //     }
    // };
}
