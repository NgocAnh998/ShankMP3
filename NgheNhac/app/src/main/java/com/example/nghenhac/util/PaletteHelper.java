package com.example.nghenhac.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

/**
 * PaletteHelper — trích xuất màu sắc từ ảnh bìa album.
 * <p>
 * Nguyên lý:
 * - Dùng Palette từ thư viện androidx.palette để lấy màu chủ đạo từ ảnh.
 * - Hỗ trợ fallback: nếu ảnh null hoặc Palette trả về null, dùng màu mặc định.
 * - Các callback trả về màu primary (nổi bật) và màu nền (nhẹ hơn).
 * <p>
 * Luồng xử lý:
 * 1. loadPaletteAsync(context, uri, callback) → Glide tải Bitmap → Palette.generate() → callback.
 * 2. Callback nhận vibrantColor (màu nổi) và mutedColor (màu dịu) từ Palette.
 * 3. Nếu Palette không có màu phù hợp, fallback về màu mặc định.
 * <p>
 * Lưu ý:
 * - Palette yêu cầu Bitmap, nên phải tải ảnh trước qua Glide.
 * - Chỉ gọi khi có URI ảnh hợp lệ (không gọi khi null).
 * - Các màu trả về có thể dùng để tô nền PlayerActivity header.
 */
public class PaletteHelper {

    private PaletteHelper() {
        // Utility class — no instances
    }

    /** Callback nhận kết quả phân tích màu từ Palette. */
    public interface PaletteCallback {
        /**
         * Gọi khi phân tích màu hoàn tất.
         *
         * @param vibrantColor Màu nổi bật (fallback: defaultColor).
         * @param mutedColor   Màu dịu hơn (fallback: defaultColor).
         * @param defaultColor Màu mặc định (truyền từ ngoài vào).
         */
        void onColorsExtracted(int vibrantColor, int mutedColor, int defaultColor);

        /** Gọi khi có lỗi tải ảnh. */
        void onError(Exception e);
    }

    /**
     * Tải ảnh bìa album và phân tích màu sắc bất đồng bộ.
     * <p>
     * Nguyên lý:
     * - Dùng Glide để tải Bitmap từ URI.
     * - Khi có Bitmap, gọi Palette.from(bitmap).generate().
     * - Trích xuất vibrantColor (màu nổi) và mutedColor (màu dịu).
     * - Nếu không có, fallback về defaultColor.
     *
     * @param context      Context.
     * @param albumArtUri  URI ảnh bìa (content:// hoặc https://).
     * @param defaultColor Màu mặc định nếu không trích xuất được.
     * @param callback     Callback xử lý kết quả.
     */
    public static void loadPaletteAsync(@NonNull Context context,
                                        @Nullable String albumArtUri,
                                        int defaultColor,
                                        @NonNull PaletteCallback callback) {
        if (albumArtUri == null || albumArtUri.isEmpty()) {
            callback.onColorsExtracted(defaultColor, defaultColor, defaultColor);
            return;
        }

        Glide.with(context)
                .asBitmap()
                .load(albumArtUri)
                .override(256, 256) // Chỉ cần ảnh nhỏ để phân tích palette
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {
                    // Chạy Palette.generate() trên background thread
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    new Thread(() -> {
                        try {
                            Palette palette = Palette.from(resource).generate();

                            int vibrant = palette.getVibrantColor(defaultColor);
                            int vibrantLight = palette.getLightVibrantColor(defaultColor);
                            int muted = palette.getMutedColor(defaultColor);
                            int darkMuted = palette.getDarkMutedColor(defaultColor);

                            int primaryColor = vibrant != defaultColor ? vibrant : muted;
                            int bgColor = muted != defaultColor ? muted : darkMuted;
                            if (primaryColor == defaultColor && vibrantLight != defaultColor) {
                                primaryColor = vibrantLight;
                            }

                            final int finalPrimary = primaryColor;
                            final int finalBg = bgColor;
                            mainHandler.post(() ->
                                    callback.onColorsExtracted(finalPrimary, finalBg, defaultColor));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    }).start();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Không cần xử lý
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        callback.onColorsExtracted(defaultColor, defaultColor, defaultColor);
                    }
                });
    }
}
