package com.example.nghenhac.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.repository.SongRepository;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * PlaylistImporter — nhập danh sách phát từ file M3U hoặc XML.
 * <p>
 * Nguyên lý:
 * - Đọc toàn bộ file vào String (tránh mở InputStream nhiều lần cho các URI chỉ đọc một lần).
 * - Phát hiện định dạng: M3U bắt đầu bằng #EXTM3U, còn lại là XML.
 * - Parse metadata, sau đó match với database qua SongRepository.searchSync().
 * - Trả về ImportResult chứa danh sách song IDs có thể thêm vào playlist mới.
 * <p>
 * Luồng xử lý:
 * 1. User chọn file → FilePickerUtil → URI.
 * 2. PlaylistImporter.readFile(context, uri) → đọc file → parse → trả về ImportResult.
 * 3. UI dùng matchedSongIds để addSongsToPlaylist() vào playlist mới.
 */
public class PlaylistImporter {

    private PlaylistImporter() {
        // Utility class
    }

    /** Model cho metadata bài hát từ file import. */
    public static class SongEntry {
        @NonNull public final String title;
        @NonNull public final String artist;
        @NonNull public final String album;
        public final long duration;
        @Nullable public final String filePath;

        public SongEntry(@NonNull String title, @NonNull String artist,
                         @NonNull String album, long duration,
                         @Nullable String filePath) {
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.duration = duration;
            this.filePath = filePath;
        }
    }

    /** Kết quả import gồm danh sách bài hát parse được và số bài match thành công. */
    public static class ImportResult {
        @NonNull public final String playlistName;
        @NonNull public final List<SongEntry> entries;
        @NonNull public final List<Long> matchedSongIds;
        public final int totalCount;
        public final int matchedCount;

        public ImportResult(@NonNull String playlistName, @NonNull List<SongEntry> entries,
                            @NonNull List<Long> matchedSongIds) {
            this.playlistName = playlistName;
            this.entries = entries;
            this.matchedSongIds = matchedSongIds;
            this.totalCount = entries.size();
            this.matchedCount = matchedSongIds.size();
        }
    }

    /**
     * Đọc file và parse playlist.
     * <p>
     * Tự động phát hiện định dạng: M3U (bắt đầu bằng #EXTM3U) hoặc XML.
     *
     * @param context Context.
     * @param uri     URI file từ file picker.
     * @return ImportResult chứa danh sách bài hát và kết quả match.
     * @throws Exception nếu đọc file thất bại.
     */
    @NonNull
    public static ImportResult readFile(@NonNull Context context, @NonNull Uri uri)
            throws Exception {
        // Đọc toàn bộ file vào String
        String content;
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            if (is == null) {
                throw new Exception("Cannot open file");
            }
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            content = sb.toString();
        }

        String playlistName;
        List<SongEntry> entries;

        if (content.startsWith("#EXTM3U")) {
            playlistName = extractM3UPlaylistName(content);
            entries = parseM3U(content);
        } else {
            playlistName = extractXMLPlaylistName(content);
            entries = parseXML(content);
        }

        List<Long> matchedIds = matchSongs(context, entries);

        return new ImportResult(
                playlistName != null ? playlistName : "Imported Playlist",
                entries, matchedIds);
    }

    // ════════════════════════════════════════════
    //  M3U Parser (String-based)
    // ════════════════════════════════════════════

    @Nullable
    private static String extractM3UPlaylistName(@NonNull String content) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#PLAYLIST: ")) {
                return trimmed.substring("#PLAYLIST: ".length()).trim();
            }
        }
        return null;
    }

    @NonNull
    private static List<SongEntry> parseM3U(@NonNull String content) {
        List<SongEntry> entries = new ArrayList<>();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("#EXTINF:")) {
                String meta = line.substring("#EXTINF:".length());
                int commaIndex = meta.indexOf(',');
                long duration = 0;
                String title = "";
                String artist = "";

                if (commaIndex > 0) {
                    try {
                        duration = Long.parseLong(meta.substring(0, commaIndex).trim()) * 1000;
                    } catch (NumberFormatException ignored) {}

                    String artistTitle = meta.substring(commaIndex + 1).trim();
                    int dashIndex = artistTitle.lastIndexOf(" - ");
                    if (dashIndex > 0) {
                        artist = artistTitle.substring(0, dashIndex).trim();
                        title = artistTitle.substring(dashIndex + 3).trim();
                    } else {
                        title = artistTitle;
                    }
                }

                // Next non-empty line is the file path
                String filePath = null;
                while (i + 1 < lines.length) {
                    String next = lines[++i].trim();
                    if (!next.isEmpty() && !next.startsWith("#")) {
                        filePath = next;
                        break;
                    }
                }

                entries.add(new SongEntry(
                        title.isEmpty() ? "Unknown" : title,
                        artist.isEmpty() ? "Unknown" : artist,
                        "", duration, filePath));
            }
        }
        return entries;
    }

    // ════════════════════════════════════════════
    //  XML Parser (String-based)
    // ════════════════════════════════════════════

    @Nullable
    private static String extractXMLPlaylistName(@NonNull String content) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(content));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG
                        && "playlist".equals(parser.getName())) {
                    return parser.getAttributeValue(null, "name");
                }
                eventType = parser.next();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @NonNull
    private static List<SongEntry> parseXML(@NonNull String content) throws Exception {
        List<SongEntry> entries = new ArrayList<>();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(content));

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "song".equals(parser.getName())) {
                String title = parser.getAttributeValue(null, "title");
                String artist = parser.getAttributeValue(null, "artist");
                String album = parser.getAttributeValue(null, "album");
                String durationStr = parser.getAttributeValue(null, "duration");
                String filePath = parser.getAttributeValue(null, "filePath");

                long duration = 0;
                if (durationStr != null) {
                    try { duration = Long.parseLong(durationStr); } catch (NumberFormatException ignored) {}
                }

                entries.add(new SongEntry(
                        title != null ? title : "Unknown",
                        artist != null ? artist : "Unknown",
                        album != null ? album : "",
                        duration, filePath));
            }
            eventType = parser.next();
        }
        return entries;
    }

    // ════════════════════════════════════════════
    //  Match songs với database
    // ════════════════════════════════════════════

    @NonNull
    private static List<Long> matchSongs(@NonNull Context context,
                                         @NonNull List<SongEntry> entries) {
        List<Long> matchedIds = new ArrayList<>();
        SongRepository repository = SongRepository.getInstance(context);

        for (SongEntry entry : entries) {
            List<SongEntity> results = repository.searchSync(entry.title);
            for (SongEntity song : results) {
                if (!matchedIds.contains(song.getId())) {
                    boolean artistMatch = entry.artist.equalsIgnoreCase(song.getArtist())
                            || "Unknown".equals(entry.artist);
                    boolean durationMatch = Math.abs(song.getDuration() - entry.duration) < 2000;
                    if (artistMatch || durationMatch) {
                        matchedIds.add(song.getId());
                    }
                }
            }
        }
        return matchedIds;
    }
}
