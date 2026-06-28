package com.example.nghenhac.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * FilePickerUtil — tiện ích mở file picker để chọn file import/export playlist.
 * <p>
 * Nguyên lý:
 * - Dùng Storage Access Framework (SAF) qua ACTION_OPEN_DOCUMENT và ACTION_CREATE_DOCUMENT.
 * - Tương thích với Scoped Storage trên Android 10+.
 * - Hỗ trợ hai loại MIME: audio/x-mpegurl (M3U) và text/xml (XML).
 * - Dùng trong Activity với ActivityResultLauncher để xử lý kết quả bất đồng bộ.
 * <p>
 * Luồng xử lý:
 * 1. User chọn "Import" → openFilePicker(activity, FILE_PICKER_IMPORT) → chọn file.
 * 2. User chọn "Export M3U" → openFilePicker(activity, FILE_PICKER_EXPORT_M3U) → chọn nơi lưu.
 * 3. User chọn "Export XML" → openFilePicker(activity, FILE_PICKER_EXPORT_XML) → chọn nơi lưu.
 * 4. Kết quả trả về qua onActivityResult → xử lý URI.
 */
public class FilePickerUtil {

    public static final String MIME_TYPE_M3U = "audio/x-mpegurl";
    public static final String MIME_TYPE_XML = "text/xml";
    public static final String MIME_TYPE_ANY = "*/*";

    /** Loại hành động picker. */
    public enum PickerMode {
        IMPORT,     // Mở file để import
        EXPORT_M3U, // Lưu file M3U
        EXPORT_XML  // Lưu file XML
    }

    private FilePickerUtil() {
        // Utility class
    }

    /**
     * Tạo Intent để mở file picker.
     * <p>
     * Tuỳ theo mode:
     * - IMPORT: ACTION_OPEN_DOCUMENT, chọn M3U hoặc XML.
     * - EXPORT_M3U: ACTION_CREATE_DOCUMENT, tên file .m3u.
     * - EXPORT_XML: ACTION_CREATE_DOCUMENT, tên file .xml.
     *
     * @param playlistName Tên playlist (dùng cho tên file export).
     * @param mode         Loại picker.
     * @return Intent đã cấu hình.
     */
    @NonNull
    public static Intent createIntent(@NonNull String playlistName,
                                      @NonNull PickerMode mode) {
        Intent intent;
        String filename;

        switch (mode) {
            case IMPORT:
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(MIME_TYPE_ANY);
                // Chấp nhận nhiều MIME types
                intent.putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{MIME_TYPE_M3U, MIME_TYPE_XML});
                return intent;

            case EXPORT_M3U:
                intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(MIME_TYPE_M3U);
                filename = sanitizeFilename(playlistName) + ".m3u";
                intent.putExtra(Intent.EXTRA_TITLE, filename);
                return intent;

            case EXPORT_XML:
                intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(MIME_TYPE_XML);
                filename = sanitizeFilename(playlistName) + ".xml";
                intent.putExtra(Intent.EXTRA_TITLE, filename);
                return intent;

            default:
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType(MIME_TYPE_ANY);
                return intent;
        }
    }

    /**
     * Lấy URI từ kết quả của ActivityResult.
     *
     * @param data Intent từ onActivityResult / ActivityResultCallback.
     * @return URI của file được chọn, hoặc null nếu user huỷ.
     */
    @Nullable
    public static Uri getUriFromResult(@Nullable Intent data) {
        if (data != null) {
            return data.getData();
        }
        return null;
    }

    /**
     * Loại bỏ ký tự đặc biệt khỏi tên file.
     */
    @NonNull
    private static String sanitizeFilename(@NonNull String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                .trim()
                .replaceAll("\\s+", "_");
    }
}
