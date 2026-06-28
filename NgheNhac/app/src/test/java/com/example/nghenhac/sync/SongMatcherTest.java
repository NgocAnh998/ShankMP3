package com.example.nghenhac.sync;

import com.example.nghenhac.data.local.entity.SongEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test cho SongMatcher — matching algorithm.
 *
 * Nguyên lý:
 * - Kiểm tra các chiến lược matching: exact, normalized, contains, Levenshtein.
 * - Kiểm tra normalize() và levenshteinDistance().
 * - Test threshold 0.7 và confidence scores.
 */
public class SongMatcherTest {

    private SongMatcher matcher;
    private List<SongEntity> localSongs;

    @Before
    public void setUp() {
        matcher = new SongMatcher();
        localSongs = new ArrayList<>();

        // Thêm bài hát local mẫu
        localSongs.add(createSong(1, "Shape of You", "Ed Sheeran", "\u00f7", 233000));
        localSongs.add(createSong(2, "Perfect", "Ed Sheeran", "\u00f7", 263000));
        localSongs.add(createSong(3, "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354000));
        localSongs.add(createSong(4, "Hotel California", "Eagles", "Hotel California", 391000));
        localSongs.add(createSong(5, "Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", 482000));
    }

    // ════════════════════════════════════════════
    //  Exact Match Tests
    // ════════════════════════════════════════════

    @Test
    public void findMatch_exactMatch_returnsPerfectConfidence() {
        SongEntity firebaseSong = createSong(100, "Shape of You", "Ed Sheeran", "\u00f7", 233000);

        SongMatcher.MatchResult result = matcher.findMatch(firebaseSong, localSongs);

        assertTrue("Should find a match", result.isMatched());
        assertEquals("Should match with exact song", 1L, result.getLocalSong().getId());
        assertEquals("Confidence should be 1.0", 1.0, result.getConfidence(), 0.001);
    }

    // ════════════════════════════════════════════
    //  Normalized Match Tests
    // ════════════════════════════════════════════

    @Test
    public void findMatch_normalizedMatch_returnsHighConfidence() {
        // Khác case và có khoảng trắng thừa
        SongEntity firebaseSong = createSong(101, "  shape of you  ", "  ed sheeran  ", "\u00f7", 233000);

        SongMatcher.MatchResult result = matcher.findMatch(firebaseSong, localSongs);

        assertTrue("Should find a match with normalized strings", result.isMatched());
        assertEquals("Should match with song 1", 1L, result.getLocalSong().getId());
        assertEquals("Confidence should be 0.95", 0.95, result.getConfidence(), 0.001);
    }

    // ════════════════════════════════════════════
    //  Contains Match Tests
    // ════════════════════════════════════════════

    @Test
    public void findMatch_containsMatch_returnsMediumConfidence() {
        // Title chứa một phần
        SongEntity firebaseSong = createSong(102, "Shape", "Ed Sheeran", "\u00f7", 233000);

        SongMatcher.MatchResult result = matcher.findMatch(firebaseSong, localSongs);

        assertTrue("Should find a match with contains", result.isMatched());
        assertEquals("Confidence should be 0.85", 0.85, result.getConfidence(), 0.001);
    }

    // ════════════════════════════════════════════
    //  No Match Tests
    // ════════════════════════════════════════════

    @Test
    public void findMatch_noMatch_returnsNotMatched() {
        SongEntity firebaseSong = createSong(103, "Never Gonna Give You Up", "Rick Astley", "Whenever You Need Somebody", 212000);

        SongMatcher.MatchResult result = matcher.findMatch(firebaseSong, localSongs);

        assertFalse("Should not find a match", result.isMatched());
        assertNull("Local song should be null", result.getLocalSong());
        assertEquals("Confidence should be 0.0", 0.0, result.getConfidence(), 0.001);
    }

    // ════════════════════════════════════════════
    //  Empty List Tests
    // ════════════════════════════════════════════

    @Test
    public void findMatch_emptyLocalList_returnsNotMatched() {
        SongEntity firebaseSong = createSong(104, "Any Song", "Any Artist", "Any Album", 100000);
        List<SongEntity> emptyList = new ArrayList<>();

        SongMatcher.MatchResult result = matcher.findMatch(firebaseSong, emptyList);

        assertFalse("Should not match with empty list", result.isMatched());
        assertNull("Local song should be null", result.getLocalSong());
    }

    // ════════════════════════════════════════════
    //  Multiple Matches Tests
    // ════════════════════════════════════════════

    @Test
    public void findMatch_multipleMatches_returnsBestConfidence() {
        // Thêm bài hát có title giống nhưng artist khác
        localSongs.add(createSong(6, "Shape of You (Remix)", "Ed Sheeran ft. XXX", "\u00f7 (Deluxe)", 240000));

        SongEntity firebaseSong = createSong(105, "Shape of You", "Ed Sheeran", "\u00f7", 233000);

        SongMatcher.MatchResult result = matcher.findMatch(firebaseSong, localSongs);

        assertTrue("Should find best match", result.isMatched());
        assertEquals("Should prefer exact match over remix", 1L, result.getLocalSong().getId());
        assertEquals("Confidence should be 1.0 for exact match", 1.0, result.getConfidence(), 0.001);
    }

    // ════════════════════════════════════════════
    //  Find Multiple Matches Tests
    // ════════════════════════════════════════════

    @Test
    public void findMatches_allExact_returnsAllMatched() {
        List<SongEntity> firebaseSongs = new ArrayList<>();
        firebaseSongs.add(createSong(200, "Shape of You", "Ed Sheeran", "\u00f7", 233000));
        firebaseSongs.add(createSong(201, "Perfect", "Ed Sheeran", "\u00f7", 263000));
        firebaseSongs.add(createSong(202, "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354000));

        List<SongMatcher.MatchResult> results = matcher.findMatches(firebaseSongs, localSongs);

        assertEquals("Should have 3 results", 3, results.size());
        for (SongMatcher.MatchResult result : results) {
            assertTrue("All songs should match", result.isMatched());
            assertTrue("All should have high confidence",
                    result.getConfidence() >= 0.95);
        }
    }

    @Test
    public void findMatches_partialMatch_returnsMixedResults() {
        List<SongEntity> firebaseSongs = new ArrayList<>();
        firebaseSongs.add(createSong(203, "Shape of You", "Ed Sheeran", "\u00f7", 233000));
        firebaseSongs.add(createSong(204, "Unknown Song", "Unknown Artist", "Unknown Album", 0));

        List<SongMatcher.MatchResult> results = matcher.findMatches(firebaseSongs, localSongs);

        assertEquals("Should have 2 results", 2, results.size());
        assertTrue("First song should match", results.get(0).isMatched());
        assertFalse("Second song should not match", results.get(1).isMatched());
    }

    // ════════════════════════════════════════════
    //  Normalize Tests
    // ════════════════════════════════════════════

    @Test
    public void normalize_removesSpacesAndLowercases() {
        String result = matcher.normalize("  Hello   World  ");
        assertEquals("Should trim and collapse spaces", "hello world", result);
    }

    @Test
    public void normalize_removesSpecialCharacters() {
        String result = matcher.normalize("Hello, World!");
        assertEquals("Should remove punctuation", "hello world", result);
    }

    @Test
    public void normalize_nullInput_returnsEmpty() {
        assertEquals("Null should return empty", "", matcher.normalize(null));
    }

    @Test
    public void normalize_emptyInput_returnsEmpty() {
        assertEquals("Empty should return empty", "", matcher.normalize(""));
    }

    @Test
    public void normalize_lowercasesInput() {
        String result = matcher.normalize("SHAPE OF YOU");
        assertEquals("Should lowercase", "shape of you", result);
    }

    // ════════════════════════════════════════════
    //  Levenshtein Distance Tests
    // ════════════════════════════════════════════

    @Test
    public void levenshteinDistance_identicalStrings_returnsZero() {
        assertEquals("Identical strings should have distance 0",
                0, matcher.levenshteinDistance("hello", "hello"));
    }

    @Test
    public void levenshteinDistance_oneInsertion_returnsOne() {
        assertEquals("One character insertion should return 1",
                1, matcher.levenshteinDistance("hello", "helloo"));
    }

    @Test
    public void levenshteinDistance_oneDeletion_returnsOne() {
        assertEquals("One character deletion should return 1",
                1, matcher.levenshteinDistance("hello", "hell"));
    }

    @Test
    public void levenshteinDistance_oneSubstitution_returnsOne() {
        assertEquals("One character substitution should return 1",
                1, matcher.levenshteinDistance("hello", "halle"));
    }

    @Test
    public void levenshteinDistance_completelyDifferent_returnsMaxLength() {
        assertEquals("Completely different strings should return max length",
                5, matcher.levenshteinDistance("abcde", "vwxyz"));
    }

    @Test
    public void levenshteinDistance_emptyFirst_returnsSecondLength() {
        assertEquals("Empty first string should return length of second",
                5, matcher.levenshteinDistance("", "hello"));
    }

    @Test
    public void levenshteinDistance_emptySecond_returnsFirstLength() {
        assertEquals("Empty second string should return length of first",
                5, matcher.levenshteinDistance("hello", ""));
    }

    @Test
    public void normalizedLevenshtein_identical_returnsZero() {
        assertEquals("Identical strings should have distance 0.0",
                0.0, matcher.normalizedLevenshtein("hello", "hello"), 0.001);
    }

    @Test
    public void normalizedLevenshtein_different_returnsAboveZero() {
        double distance = matcher.normalizedLevenshtein("shape of you", "shape of u");
        assertTrue("Different strings should have distance > 0", distance > 0.0);
        assertTrue("Different strings should have distance < 1.0", distance < 1.0);
    }

    // ════════════════════════════════════════════
    //  MatchResult Tests
    // ════════════════════════════════════════════

    @Test
    public void matchResult_isMatched_belowThreshold_returnsFalse() {
        SongEntity song = createSong(300, "Test", "Test", "Test", 1000);
        // Confidence 0.5 < threshold 0.7
        SongMatcher.MatchResult result =
                new SongMatcher.MatchResult(song, song, 0.5);

        assertFalse("Should not be matched below threshold", result.isMatched());
    }

    @Test
    public void matchResult_isMatched_aboveThreshold_returnsTrue() {
        SongEntity song = createSong(301, "Test", "Test", "Test", 1000);
        // Confidence 0.8 >= threshold 0.7
        SongMatcher.MatchResult result =
                new SongMatcher.MatchResult(song, song, 0.8);

        assertTrue("Should be matched above threshold", result.isMatched());
    }

    @Test
    public void matchResult_getters_workCorrectly() {
        SongEntity firebaseSong = createSong(302, "Firebase", "Artist", "Album", 1000);
        SongEntity localSong = createSong(303, "Local", "Artist", "Album", 1000);

        SongMatcher.MatchResult result =
                new SongMatcher.MatchResult(firebaseSong, localSong, 0.9);

        assertSame("Should return firebase song", firebaseSong, result.getFirebaseSong());
        assertSame("Should return local song", localSong, result.getLocalSong());
        assertEquals("Should return confidence", 0.9, result.getConfidence(), 0.001);
    }

    // ════════════════════════════════════════════
    //  Edge Cases
    // ════════════════════════════════════════════

    @Test
    public void findMatch_sameTitleDifferentArtist_returnsLowConfidence() {
        SongEntity firebaseSong = createSong(400, "Shape of You", "Someone Else", "Different Album", 200000);

        SongMatcher.MatchResult result = matcher.findMatch(firebaseSong, localSongs);

        assertFalse("Should not match with same title but different artist",
                result.isMatched());
    }

    @Test
    public void calculateConfidence_exactMatch_returnsOne() {
        SongEntity a = createSong(1, "Test Song", "Test Artist", "Test Album", 1000);
        SongEntity b = createSong(2, "Test Song", "Test Artist", "Test Album", 1000);

        double confidence = matcher.calculateConfidence(a, b);

        assertEquals("Exact match should have confidence 1.0", 1.0, confidence, 0.001);
    }

    @Test
    public void calculateConfidence_normalizedMatch_returns95() {
        SongEntity a = createSong(1, "  TEST SONG  ", "  TEST ARTIST  ", "Test Album", 1000);
        SongEntity b = createSong(2, "test song", "test artist", "Test Album", 1000);

        double confidence = matcher.calculateConfidence(a, b);

        assertEquals("Normalized match should have confidence 0.95", 0.95, confidence, 0.001);
    }

    // ════════════════════════════════════════════
    //  Helper
    // ════════════════════════════════════════════

    private SongEntity createSong(long id, String title, String artist, String album, long duration) {
        SongEntity song = new SongEntity(title, artist, album, duration, null, null, id, 0);
        song.setId(id);
        return song;
    }
}
