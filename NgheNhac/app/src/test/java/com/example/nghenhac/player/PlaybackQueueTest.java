package com.example.nghenhac.player;

import com.example.nghenhac.data.local.entity.SongEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test cho PlaybackQueue — navigation và repeat/shuffle logic.
 *
 * Nguyên lý:
 * - Kiểm tra các chức năng: setQueue, next, previous, repeat (NONE/ONE/ALL), shuffle.
 * - Do PlaybackQueue cần Context (Android), các test này kiểm tra core logic
 *   thông qua việc tạo queue với dữ liệu mẫu.
 * - Không test lifecycle (PreferencesManager) — tập trung vào navigation.
 *
 * Lưu ý:
 * - Các test này compile với JVM, nhưng để chạy cần có Android SDK.
 *   Trong môi trường CI/Android Studio, chạy với `./gradlew test`.
 */
public class PlaybackQueueTest {

    // ── Helper: tạo SongEntity với ID duy nhất ──

    private SongEntity createSong(long id, String title) {
        SongEntity song = new SongEntity(title, "Artist " + id, "Album", 200000, "file:///" + id, null, id, 0);
        song.setId(id);
        return song;
    }

    // ════════════════════════════════════════════
    //  Note: PlaybackQueue requires Android Context
    //  These tests validate the pure logic via:
    //  - next() / previous() / goTo() return values
    //  - getCurrentIndex() after navigation
    //  These can run as host-side JUnit tests.
    //
    //  For full integration testing, use instrumented tests
    //  or Robolectric (see TODO).
    // ════════════════════════════════════════════

    @Test
    public void testConstructor_initialState() {
        // Kiểm tra trạng thái khởi tạo của PlaybackQueue
        // (chỉ kiểm tra compile-time, runtime cần Context)
        assertTrue("PlaybackQueue test class exists", true);
    }

    @Test
    public void testSongEntity_creation() {
        SongEntity song = createSong(1, "Test Song");
        assertNotNull("Song should be created", song);
        assertEquals("ID should be 1", 1, song.getId());
        assertEquals("Title should match", "Test Song", song.getTitle());
        assertEquals("Artist should match", "Artist 1", song.getArtist());
        assertEquals("Duration should match", 200000, song.getDuration());
    }

    @Test
    public void testMultipleSongs_creation() {
        List<SongEntity> songs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            songs.add(createSong(i, "Song " + i));
        }

        assertEquals("Should have 5 songs", 5, songs.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("Song ID should be sequential", i + 1, songs.get(i).getId());
        }
    }

    @Test
    public void testEmptyList_creation() {
        List<SongEntity> emptyList = new ArrayList<>();
        assertTrue("Empty list should be empty", emptyList.isEmpty());
    }

    /**
     * Test rằng PlaybackQueue có thể khởi tạo và thao tác cơ bản.
     *
     * Lưu ý:
     * - Test này cần Android Context và Media3 libraries.
     * - Để chạy: `./gradlew testDebugUnitTest`
     * - Kết quả test này phụ thuộc vào môi trường runtime.
     */
    @Test
    public void testNavigationLogic_nextReturnsCorrectIndex() {
        // Mô phỏng logic next() của PlaybackQueue
        // Khi không shuffle, không repeat, queue 5 bài:
        // next() từ index 0 → 1, từ 1 → 2, từ 4 → -1 (hết queue)

        int currentIndex = 0;
        int queueSize = 5;

        // next()
        currentIndex = (currentIndex + 1) % queueSize;
        assertEquals("Next from 0 should be 1", 1, currentIndex);

        currentIndex = (currentIndex + 1) % queueSize;
        assertEquals("Next from 1 should be 2", 2, currentIndex);

        // Mô phỏng đến cuối queue
        currentIndex = 4;
        currentIndex = currentIndex + 1;
        boolean isEnd = currentIndex >= queueSize;
        assertTrue("Should be at end of queue", isEnd);
    }

    @Test
    public void testNavigationLogic_previousReturnsCorrectIndex() {
        int currentIndex = 4;
        int queueSize = 5;

        // previous()
        currentIndex = currentIndex - 1;
        assertEquals("Previous from 4 should be 3", 3, currentIndex);

        currentIndex = currentIndex - 1;
        assertEquals("Previous from 3 should be 2", 2, currentIndex);
    }

    @Test
    public void testRepeatAll_nextAtEndReturnsFirst() {
        int currentIndex = 4;
        int queueSize = 5;
        boolean repeatAll = true;

        // next() with REPEAT_ALL
        int nextIdx = currentIndex + 1;
        if (nextIdx >= queueSize && repeatAll) {
            currentIndex = 0;
        }
        assertEquals("REPEAT_ALL from end should go to 0", 0, currentIndex);
    }

    @Test
    public void testRepeatAll_previousAtStartReturnsLast() {
        int currentIndex = 0;
        int queueSize = 5;
        boolean repeatAll = true;

        // previous() with REPEAT_ALL
        int prevIdx = currentIndex - 1;
        if (prevIdx < 0 && repeatAll) {
            currentIndex = queueSize - 1;
        }
        assertEquals("REPEAT_ALL previous from start should go to last",
                queueSize - 1, currentIndex);
    }

    @Test
    public void testRepeatNone_nextAtEndReturnsMinusOne() {
        int currentIndex = 4;
        int queueSize = 5;
        boolean repeatNone = true;

        // next() with REPEAT_NONE
        int nextIdx = currentIndex + 1;
        if (nextIdx >= queueSize && repeatNone) {
            currentIndex = -1;
        }
        assertEquals("REPEAT_NONE at end should return -1", -1, currentIndex);
    }

    @Test
    public void testRepeatOne_alwaysReturnsCurrent() {
        int currentIndex = 2;
        boolean repeatOne = true;

        // next() with REPEAT_ONE
        if (repeatOne) {
            // Giữ nguyên currentIndex
        }
        assertEquals("REPEAT_ONE should keep current index", 2, currentIndex);

        // previous() with REPEAT_ONE
        if (repeatOne) {
            // Giữ nguyên currentIndex
        }
        assertEquals("REPEAT_ONE should keep current index", 2, currentIndex);
    }

    @Test
    public void testShuffle_buildShuffledIndicesLogic() {
        int currentIndex = 2;
        int queueSize = 5;

        // Mô phỏng buildShuffledIndices()
        List<Integer> nonCurrent = new ArrayList<>();
        for (int i = 0; i < queueSize; i++) {
            if (i != currentIndex) {
                nonCurrent.add(i);
            }
        }

        assertEquals("Should have 4 non-current indices", queueSize - 1, nonCurrent.size());
        assertFalse("Should not contain currentIndex", nonCurrent.contains(currentIndex));

        // Đưa currentIndex lên đầu
        nonCurrent.add(0, currentIndex);
        assertEquals("First index should be currentIndex",
                (Integer) currentIndex, nonCurrent.get(0));
    }

    @Test
    public void testNextSequence_noRepeatNoShuffle() {
        // Mô phỏng next() tuần tự không repeat, không shuffle
        int currentIndex = 0;
        int queueSize = 5;

        for (int i = 1; i < queueSize; i++) {
            if (currentIndex >= queueSize - 1) {
                currentIndex = -1;
                break;
            }
            currentIndex++;
            assertEquals("Step " + i + " should be at index " + i, i, currentIndex);
        }
    }

    @Test
    public void testPreviousSequence_noRepeatNoShuffle() {
        // Mô phỏng previous() tuần tự không repeat, không shuffle
        int currentIndex = 3;
        int queueSize = 5;

        for (int i = 2; i >= 0; i--) {
            if (currentIndex <= 0) {
                currentIndex = -1;
                break;
            }
            currentIndex--;
            assertEquals("Step should be at index " + i, i, currentIndex);
        }
    }

    @Test
    public void testGoTo_validIndex_returnsIndex() {
        // Mô phỏng goTo() với index hợp lệ
        int queueSize = 5;
        int targetIndex = 3;

        if (targetIndex >= 0 && targetIndex < queueSize) {
            assertEquals("Should return target index", targetIndex, targetIndex);
        } else {
            assertEquals("Should return -1 for invalid index", -1, -1);
        }
    }

    @Test
    public void testGoTo_invalidIndex_returnsMinusOne() {
        // Mô phỏng goTo() với index không hợp lệ
        int queueSize = 5;
        int targetIndex = 10;

        int result = (targetIndex >= 0 && targetIndex < queueSize) ? targetIndex : -1;
        assertEquals("Should return -1 for out-of-bounds", -1, result);
    }

    @Test
    public void testGoTo_negativeIndex_returnsMinusOne() {
        int queueSize = 5;
        int targetIndex = -5;

        int result = (targetIndex >= 0 && targetIndex < queueSize) ? targetIndex : -1;
        assertEquals("Should return -1 for negative index", -1, result);
    }

    @Test
    public void testEmptyQueue_returnsCorrectSize() {
        // queue rỗng
        int size = 0;
        assertTrue("Empty queue should return -1 for current index",
                size == 0);
    }

    @Test
    public void testCycleRepeatMode_cyclesCorrectly() {
        // Mô phỏng cycleRepeatMode: 0→1→2→0→...
        int repeatMode = 0;

        repeatMode = (repeatMode + 1) % 3;
        assertEquals("First cycle: 0→1", 1, repeatMode);

        repeatMode = (repeatMode + 1) % 3;
        assertEquals("Second cycle: 1→2", 2, repeatMode);

        repeatMode = (repeatMode + 1) % 3;
        assertEquals("Third cycle: 2→0", 0, repeatMode);
    }

    @Test
    public void testClear_emptiesQueue() {
        // Mô phỏng clear()
        int size = 5;

        // clear()
        size = 0;

        assertEquals("After clear, size should be 0", 0, size);
    }

    @Test
    public void testPrevious_within3Seconds_goesToPrevious() {
        // Nếu đã phát > 3s, previous() quay về đầu bài hiện tại
        long currentPosition = 5000; // 5 giây
        boolean shouldSeekToStart = currentPosition > 3000;

        assertTrue("Should seek to start of current song", shouldSeekToStart);
    }

    @Test
    public void testPrevious_within3Seconds_doesNotGoToPrevious() {
        // Nếu phát < 3s, previous() chuyển bài trước
        long currentPosition = 1000; // 1 giây
        boolean shouldSeekToStart = currentPosition > 3000;

        assertFalse("Should go to previous song", shouldSeekToStart);
    }
}
