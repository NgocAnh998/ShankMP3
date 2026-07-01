package com.example.nghenhac.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nghenhac.data.local.entity.SongEntity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Matching algorithm — tìm bài hát khớp nhất trong database local với dữ liệu từ Firebase.
 *
 * Nguyên lý:
 * - Khi đồng bộ playlist giữa các thiết bị, songId trên Firebase khác với local.
 * - SongMatcher dùng metadata (title, artist, album) để matching.
 * - Sử dụng nhiều chiến lược: exact match → fuzzy match → confidence score.
 * - Giảm thiểu false positive bằng cách tính normalized confidence score.
 *
 * Luồng xử lý:
 * 1. Nhận danh sách SongEntity từ Firebase (có title, artist, album).
 * 2. Với mỗi bài hát từ Firebase, tìm trong danh sách local:
 *    a. Exact match (title + artist) → confidence = 1.0 (khớp tuyệt đối).
 *    b. Normalized match (trim, lowerCase) → confidence = 0.95.
 *    c. Fuzzy match (contains, Levenshtein) → confidence = 0.7 ~ 0.9.
 * 3. Trả về kết quả matched với confidence cao nhất.
 *
 * Input:
 * - firebaseSongs: danh sách bài hát từ Firebase (title, artist, album).
 * - localSongs: danh sách bài hát trong database local.
 *
 * Output:
 * - Danh sách MatchResult: firebaseSong ↔ localSong (null nếu không tìm thấy).
 *
 * Lưu ý:
 * - Không matching dựa trên duration (có thể khác nhau giữa các nguồn).
 * - Normalize tất cả string trước khi so sánh (trim, lowerCase, remove accent optional).
 * - Threshold tối thiểu: 0.7 (dưới ngưỡng này coi như không match).
 */
public class SongMatcher {

    private static final double CONFIDENCE_THRESHOLD = 0.7;
    private static final double EXACT_MATCH_CONFIDENCE = 1.0;
    private static final double NORMALIZED_MATCH_CONFIDENCE = 0.95;
    private static final double CONTAINS_MATCH_CONFIDENCE = 0.85;
    private static final double LEVENSHTEIN_MATCH_CONFIDENCE = 0.75;

    /**
     * Kết quả matching giữa một bài hát từ Firebase và bài hát trong local database.
     */
    public static class MatchResult {
        @NonNull
        private final SongEntity firebaseSong;
        @Nullable
        private final SongEntity localSong;
        private final double confidence;

        MatchResult(@NonNull SongEntity firebaseSong, @Nullable SongEntity localSong, double confidence) {
            this.firebaseSong = firebaseSong;
            this.localSong = localSong;
            this.confidence = confidence;
        }

        /**
         * Kiểm tra có tìm thấy match không.
         *
         * Output: true nếu confidence >= threshold (0.7).
         */
        public boolean isMatched() {
            return localSong != null && confidence >= CONFIDENCE_THRESHOLD;
        }

        @NonNull
        public SongEntity getFirebaseSong() { return firebaseSong; }

        @Nullable
        public SongEntity getLocalSong() { return localSong; }

        public double getConfidence() { return confidence; }
    }

    // ════════════════════════════════════════════
    //  Public API
    // ════════════════════════════════════════════

    /**
     * Match một bài hát từ Firebase với danh sách bài hát local.
     *
     * Nguyên lý:
     * - Duyệt toàn bộ danh sách local, tính confidence score cho mỗi cặp.
     * - Chọn cặp có confidence cao nhất.
     * - Nếu confidence < threshold, trả về null localSong.
     *
     * Luồng xử lý:
     * 1. Kiểm tra null/empty.
     * 2. Duyệt localSongs, gọi matchSong() cho mỗi cặp.
     * 3. Giữ lại kết quả có confidence cao nhất.
     * 4. Trả về MatchResult.
     *
     * Input:
     * @param firebaseSong Bài hát từ Firebase.
     * @param localSongs   Danh sách bài hát local.
     *
     * Output:
     * @return MatchResult chứa kết quả matching.
     */
    @NonNull
    public MatchResult findMatch(@NonNull SongEntity firebaseSong,
                                 @NonNull List<SongEntity> localSongs) {
        if (localSongs.isEmpty()) {
            return new MatchResult(firebaseSong, null, 0.0);
        }

        SongEntity bestMatch = null;
        double bestConfidence = 0.0;

        for (SongEntity localSong : localSongs) {
            double confidence = calculateConfidence(firebaseSong, localSong);
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                bestMatch = localSong;
            }
        }

        return new MatchResult(firebaseSong, bestMatch, bestConfidence);
    }

    /**
     * Match nhiều bài hát từ Firebase với danh sách local.
     *
     * Nguyên lý:
     * - Gọi findMatch() cho từng bài hát Firebase.
     * - Kết quả là danh sách MatchResult với cùng thứ tự như input.
     *
     * Input:
     * @param firebaseSongs Danh sách bài hát từ Firebase.
     * @param localSongs    Danh sách bài hát local.
     *
     * Output:
     * @return Danh sách MatchResult cho từng firebaseSong.
     */
    @NonNull
    public List<MatchResult> findMatches(@NonNull List<SongEntity> firebaseSongs,
                                         @NonNull List<SongEntity> localSongs) {
        List<MatchResult> results = new ArrayList<>(firebaseSongs.size());
        for (SongEntity firebaseSong : firebaseSongs) {
            results.add(findMatch(firebaseSong, localSongs));
        }
        return results;
    }

    // ════════════════════════════════════════════
    //  Confidence Calculation
    // ════════════════════════════════════════════

    /**
     * Tính confidence score giữa hai bài hát.
     *
     * Nguyên lý:
     * Chiến lược matching theo thứ tự ưu tiên:
     * 1. Exact match: title + artist (cả hai khớp chính xác) → 1.0
     * 2. Normalized match: trim + lowerCase + khớp → 0.95
     * 3. Title contains + artist contains → 0.85
     * 4. Levenshtein distance thấp → 0.75
     * 5. Chỉ match title → 0.5 (dưới threshold)
     *
     * Input:
     * @param a Bài hát thứ nhất (từ Firebase).
     * @param b Bài hát thứ hai (từ local).
     *
     * Output:
     * @return Confidence score từ 0.0 đến 1.0.
     */
    double calculateConfidence(@NonNull SongEntity a, @NonNull SongEntity b) {
        // 1. Exact match: title + artist khớp chính xác
        if (a.getTitle().equals(b.getTitle()) && a.getArtist().equals(b.getArtist())) {
            return EXACT_MATCH_CONFIDENCE;
        }

        // Normalize strings
        String normTitleA = normalize(a.getTitle());
        String normTitleB = normalize(b.getTitle());
        String normArtistA = normalize(a.getArtist());
        String normArtistB = normalize(b.getArtist());

        // 2. Normalized match: trim + lowerCase
        if (normTitleA.equals(normTitleB) && normArtistA.equals(normArtistB)) {
            return NORMALIZED_MATCH_CONFIDENCE;
        }

        // 3. Contains match: title chứa nhau, artist chứa nhau
        boolean titleContains = normTitleA.contains(normTitleB) || normTitleB.contains(normTitleA);
        boolean artistContains = normArtistA.contains(normArtistB) || normArtistB.contains(normArtistA);
        if (titleContains && artistContains) {
            return CONTAINS_MATCH_CONFIDENCE;
        }

        // 4. Levenshtein distance thấp (title + artist)
        double titleDistance = normalizedLevenshtein(normTitleA, normTitleB);
        double artistDistance = normalizedLevenshtein(normArtistA, normArtistB);
        double avgDistance = (titleDistance + artistDistance) / 2.0;
        if (avgDistance <= 0.2) {
            return LEVENSHTEIN_MATCH_CONFIDENCE;
        }

        // 5. Album match bonus (nếu title match nhưng artist khác)
        if (normTitleA.equals(normTitleB)) {
            String normAlbumA = normalize(a.getAlbum());
            String normAlbumB = normalize(b.getAlbum());
            if (!normAlbumA.isEmpty() && normAlbumA.equals(normAlbumB)) {
                return 0.6; // Dưới threshold, không dùng
            }
        }

        return 0.0;
    }

    // ════════════════════════════════════════════
    //  Helper Methods
    // ════════════════════════════════════════════

    /**
     * Chuẩn hoá string để so sánh (hỗ trợ tiếng Việt).
     *
     * Nguyên lý:
     * - Trim khoảng trắng.
     * - Chuyển về lowerCase.
     * - Thay thế nhiều khoảng trắng bằng một.
     * - Dùng Normalizer (NFD) để tách dấu khỏi chữ cái, sau đó xoá dấu (\\p{M}).
     *   Cách này giữ nguyên chữ cái gốc, CHỈ xoá dấu (ê → e, à → a, đ → đ).
     * - Xoá ký tự đặc biệt (dấu câu) nhưng GIỮ chữ cái tiếng Việt.
     *
     * Input:
     * @param input String cần chuẩn hoá.
     *
     * Output:
     * @return String đã chuẩn hoá, bỏ dấu nhưng giữ chữ (không null).
     */
    @NonNull
    String normalize(@Nullable String input) {
        if (input == null) return "";
        String normalized = input.trim().toLowerCase();
        // Thay thế nhiều khoảng trắng bằng một
        normalized = normalized.replaceAll("\\s+", " ");
        // Tách dấu và xoá dấu, giữ chữ cái gốc
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")      // xoá dấu (combining marks)
                .replaceAll("[^a-zA-Z0-9\\s]", ""); // xoá ký tự đặc biệt (giữ a-z, A-Z, 0-9, space)
        return normalized.trim();
    }

    /**
     * Tính Levenshtein distance chuẩn hoá (0.0 = giống nhau, 1.0 = khác nhau hoàn toàn).
     *
     * Nguyên lý:
     * - Levenshtein distance: số edit operations (insert, delete, replace) để biến a thành b.
     * - Chuẩn hoá: distance / max(lenA, lenB).
     * - Kết quả: 0.0 (giống nhau) đến 1.0 (khác nhau).
     *
     * Input:
     * @param a String thứ nhất.
     * @param b String thứ hai.
     *
     * Output:
     * @return Normalized distance từ 0.0 đến 1.0.
     */
    double normalizedLevenshtein(@NonNull String a, @NonNull String b) {
        if (a.isEmpty()) return b.isEmpty() ? 0.0 : 1.0;
        if (b.isEmpty()) return 1.0;

        int distance = levenshteinDistance(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return (double) distance / maxLen;
    }

    /**
     * Tính Levenshtein distance (hỗ trợ int array để tối ưu bộ nhớ).
     *
     * Nguyên lý:
     * - Dùng hai hàng (prev + curr) thay vì ma trận đầy đủ.
     * - Phức tạp: O(n*m) time, O(min(n,m)) space.
     *
     * Input:
     * @param a String thứ nhất.
     * @param b String thứ hai.
     *
     * Output:
     * @return Levenshtein distance (int).
     */
    int levenshteinDistance(@NonNull String a, @NonNull String b) {
        // Đảm bảo a là ngắn hơn để tối ưu bộ nhớ
        if (a.length() > b.length()) {
            String temp = a;
            a = b;
            b = temp;
        }

        int lenA = a.length();
        int lenB = b.length();

        // Chỉ dùng 2 hàng
        int[] prev = new int[lenA + 1];
        int[] curr = new int[lenA + 1];

        // Khởi tạo hàng đầu tiên
        for (int j = 0; j <= lenA; j++) {
            prev[j] = j;
        }

        // Duyệt từng ký tự của b
        for (int i = 1; i <= lenB; i++) {
            curr[0] = i;
            for (int j = 1; j <= lenA; j++) {
                int cost = (a.charAt(j - 1) == b.charAt(i - 1)) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            // Swap rows
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[lenA];
    }
}
