package com.example.nghenhac.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.local.entity.SongEntity;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PlaylistExporter — xuất danh sách phát ra file M3U hoặc XML.
 * <p>
 * Nguyên lý:
 * - Hỗ trợ hai định dạng: M3U (Extended M3U, tương thích với hầu hết media player)
 *   và XML (tùy chỉnh, tối ưu cho import lại vào app).
 * - Dùng ContentResolver + OutputStream để ghi file qua Storage Access Framework (SAF),
 *   tương thích với Scoped Storage trên Android 10+.
 * <p>
 * Định dạng M3U:
 * #EXTM3U
 * #EXTINF:duration,Artist - Title
 * file_path
 * <p>
 * Định dạng XML:
 * {@code
 * <playlist name="..." created="...">
 *   <song title="..." artist="..." album="..." duration="..." filePath="..." />
 * </playlist>
 * }
 */
public class PlaylistExporter {

    private static final String M3U_HEADER = "#EXTM3U";
    private static final String M3U_ENTRY = "#EXTINF:%d,%s - %s";

    private PlaylistExporter() {
        // Utility class
    }

    /**
     * Xuất playlist ra file M3U qua URI (SAF).
     *
     * @param context  Context.
     * @param uri      URI từ Intent ACTION_CREATE_DOCUMENT.
     * @param playlist Thông tin playlist.
     * @param songs    Danh sách bài hát trong playlist.
     * @return true nếu xuất thành công.
     */
    public static boolean exportToM3U(@NonNull Context context,
                                      @NonNull Uri uri,
                                      @NonNull PlaylistEntity playlist,
                                      @NonNull List<SongEntity> songs) {
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream == null) return false;

            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            writer.println(M3U_HEADER);
            writer.println("#PLAYLIST: " + playlist.getName());

            for (SongEntity song : songs) {
                long durationSec = song.getDuration() / 1000;
                String entry = String.format(Locale.US, M3U_ENTRY,
                        durationSec, song.getArtist(), song.getTitle());
                writer.println(entry);

                String filePath = song.getFilePath() != null ? song.getFilePath() : "";
                writer.println(filePath);
            }

            writer.flush();
            writer.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Xuất playlist ra file XML qua URI (SAF).
     *
     * @param context  Context.
     * @param uri      URI từ Intent ACTION_CREATE_DOCUMENT.
     * @param playlist Thông tin playlist.
     * @param songs    Danh sách bài hát trong playlist.
     * @return true nếu xuất thành công.
     */
    public static boolean exportToXML(@NonNull Context context,
                                      @NonNull Uri uri,
                                      @NonNull PlaylistEntity playlist,
                                      @NonNull List<SongEntity> songs) {
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream == null) return false;

            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            String dateStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                    Locale.US).format(new Date());

            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.printf("<playlist name=\"%s\" created=\"%s\" songCount=\"%d\">%n",
                    escapeXml(playlist.getName()), dateStr, songs.size());

            for (SongEntity song : songs) {
                writer.printf("  <song title=\"%s\" artist=\"%s\" album=\"%s\" "
                                + "duration=\"%d\" filePath=\"%s\" />%n",
                        escapeXml(song.getTitle()),
                        escapeXml(song.getArtist()),
                        escapeXml(song.getAlbum()),
                        song.getDuration(),
                        escapeXml(song.getFilePath() != null ? song.getFilePath() : ""));
            }

            writer.println("</playlist>");
            writer.flush();
            writer.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Escape các ký tự đặc biệt trong XML.
     */
    @NonNull
    private static String escapeXml(@Nullable String input) {
        if (input == null || input.isEmpty()) return "";
        return input
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
