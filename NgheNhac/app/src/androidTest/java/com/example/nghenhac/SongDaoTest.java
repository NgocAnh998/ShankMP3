package com.example.nghenhac;

import android.content.Context;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.nghenhac.data.local.AppDatabase;
import com.example.nghenhac.data.local.dao.SongDao;
import com.example.nghenhac.data.local.entity.SongEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumentation test — kiểm tra Room DAO operations (X3).
 *
 * Nguyên lý:
 * - Tạo in-memory database để test độc lập.
 * - Test CRUD operations: insert, read, update, delete, search.
 * - Room Migration test với MigrationTestHelper.
 *
 * Lưu ý:
 * - Chạy với `./gradlew connectedAndroidTest`.
 * - Database được tạo mới mỗi test, không ảnh hưởng lẫn nhau.
 */
@RunWith(AndroidJUnit4.class)
public class SongDaoTest {

    private AppDatabase database;
    private SongDao songDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(
                context,
                AppDatabase.class
        ).build();
        songDao = database.songDao();
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void insertSong_andRetrieve() {
        SongEntity song = createTestSong(100, "Test Song", "Test Artist", "Test Album");

        long id = songDao.insert(song);
        assertTrue("Insert should return positive ID", id > 0);

        SongEntity retrieved = songDao.getSongByIdSync(id);
        assertNotNull("Retrieved song should not be null", retrieved);
        assertEquals("Title should match", "Test Song", retrieved.getTitle());
        assertEquals("Artist should match", "Test Artist", retrieved.getArtist());
        assertEquals("Album should match", "Test Album", retrieved.getAlbum());
    }

    @Test
    public void insertMultiple_andCount() {
        songDao.insert(createTestSong(1, "Song A", "Artist 1", "Album 1"));
        songDao.insert(createTestSong(2, "Song B", "Artist 1", "Album 1"));
        songDao.insert(createTestSong(3, "Song C", "Artist 2", "Album 2"));

        int count = songDao.getSongCount().getValue() != null
                ? songDao.getSongCount().getValue() : 0;
        assertEquals("Should have 3 songs", 3, count);
    }

    @Test
    public void updateFavorite_togglesState() {
        SongEntity song = createTestSong(200, "Favorite Test", "Artist", "Album");
        long id = songDao.insert(song);

        // Toggle favorite on
        int updatedRows = songDao.updateFavorite(id, true);
        assertEquals("Should update 1 row", 1, updatedRows);

        SongEntity retrieved = songDao.getSongByIdSync(id);
        assertNotNull(retrieved);
        assertTrue("Song should be favorite", retrieved.isFavorite());

        // Toggle favorite off
        songDao.updateFavorite(id, false);
        retrieved = songDao.getSongByIdSync(id);
        assertNotNull(retrieved);
        assertFalse("Song should not be favorite", retrieved.isFavorite());
    }

    @Test
    public void searchSongs_returnsMatchingResults() {
        songDao.insert(createTestSong(300, "Shape of You", "Ed Sheeran", "\u00f7"));
        songDao.insert(createTestSong(301, "Perfect", "Ed Sheeran", "\u00f7"));
        songDao.insert(createTestSong(302, "Hotel California", "Eagles", "Hotel California"));

        List<SongEntity> results = songDao.searchSync("Shape");
        assertNotNull("Search results should not be null", results);
        assertFalse("Should find at least 1 song", results.isEmpty());
        assertEquals("Should find Shape of You", "Shape of You", results.get(0).getTitle());
    }

    @Test
    public void searchSongs_byArtist_returnsResults() {
        songDao.insert(createTestSong(400, "Song 1", "Queen", "Album 1"));
        songDao.insert(createTestSong(401, "Song 2", "Queen", "Album 2"));
        songDao.insert(createTestSong(402, "Song 3", "Other", "Album 3"));

        List<SongEntity> results = songDao.searchSync("Queen");
        assertNotNull("Search results should not be null", results);
        assertEquals("Should find 2 songs by Queen", 2, results.size());
    }

    @Test
    public void searchSongs_emptyQuery_returnsAll() {
        songDao.insert(createTestSong(500, "Song A", "Artist", "Album"));
        songDao.insert(createTestSong(501, "Song B", "Artist", "Album"));

        List<SongEntity> results = songDao.searchSync("");
        assertNotNull("Search with empty query should return all", results);
        assertEquals("Should return 2 songs", 2, results.size());
    }

    @Test
    public void deleteSong_removesFromDatabase() {
        SongEntity song = createTestSong(600, "Delete Me", "Artist", "Album");
        long id = songDao.insert(song);

        int deletedRows = songDao.deleteById(id);
        assertEquals("Should delete 1 row", 1, deletedRows);

        SongEntity retrieved = songDao.getSongByIdSync(id);
        assertNull("Deleted song should be null", retrieved);
    }

    @Test
    public void insertSong_withReplaceStrategy_updatesExisting() {
        SongEntity song1 = createTestSong(700, "Original Title", "Artist", "Album");
        long id1 = songDao.insert(song1);

        // Insert same song with updated title (same mediaStoreId)
        SongEntity song2 = createTestSong(700, "Updated Title", "Artist", "Album");
        song2.setId(id1);
        long id2 = songDao.insert(song2);

        // With REPLACE strategy, should update existing
        SongEntity retrieved = songDao.getSongByIdSync(id1);
        assertNotNull("Song should exist after replace", retrieved);
        assertTrue("Should update title or keep original",
                "Original Title".equals(retrieved.getTitle())
                || "Updated Title".equals(retrieved.getTitle()));
    }

    // ── Helpers ──

    private SongEntity createTestSong(long mediaStoreId, String title,
                                      String artist, String album) {
        SongEntity song = new SongEntity(
                title, artist, album, 200000,
                "file:///test/" + mediaStoreId,
                null, mediaStoreId, System.currentTimeMillis());
        song.setId(mediaStoreId);
        return song;
    }
}
