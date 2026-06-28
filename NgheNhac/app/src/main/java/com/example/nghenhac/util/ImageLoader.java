package com.example.nghenhac.util;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.nghenhac.R;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * ImageLoader — Glide wrapper tải và hiển thị album art.
 * <p>
 * Nguyên lý:
 * - Tập trung tất cả cấu hình Glide vào một chỗ (placeholder, error, disk cache, transform).
 * - Hỗ trợ cả URI content:// (từ MediaStore) và URL http(s) (từ streaming).
 * - Tự động xử lý fallback: nếu URI null/rỗng → dùng placeholder drawable.
 * - Transform optional: rounded corners cho ShapeableImageView (dùng tính năng của view thay vì Glide).
 * <p>
 * Lưu ý:
 * - Không dùng Glide transform cho corner vì ShapeableImageView đã xử lý.
 * - DiskCacheStrategy.AUTOMATIC — Glide tự quyết định cache strategy.
 * - override(512, 512) — giới hạn kích thước để tránh OOM với ảnh lớn.
 */
public class ImageLoader {

    private static final int MAX_SIZE = 512;
    private static final int THUMB_SIZE = 128;

    @DrawableRes
    private static final int DEFAULT_PLACEHOLDER = R.drawable.ic_library;
    @DrawableRes
    private static final int DEFAULT_ERROR = R.drawable.ic_library;

    private ImageLoader() {
        // Utility class — no instances
    }

    /**
     * Load album art vào ShapeableImageView với kích thước đầy đủ.
     * <p>
     * Dùng cho PlayerActivity (album art lớn) và AlbumAdapter (ảnh grid).
     *
     * @param context  Context.
     * @param uri      URI ảnh (content:// hoặc https://), có thể null.
     * @param imageView View đích.
     */
    public static void load(@NonNull Context context,
                            @Nullable String uri,
                            @NonNull ShapeableImageView imageView) {
        if (uri == null || uri.isEmpty()) {
            // Không có URI → hiển thị placeholder tĩnh
            imageView.setImageResource(DEFAULT_PLACEHOLDER);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(DEFAULT_PLACEHOLDER)
                .error(DEFAULT_ERROR)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(MAX_SIZE);

        Glide.with(context)
                .load(Uri.parse(uri))
                .apply(options)
                .into(imageView);
    }

    /**
     * Load album art với kích thước nhỏ (thumbnail).
     * <p>
     * Dùng cho SongAdapter (danh sách bài hát) và MiniPlayerFragment.
     *
     * @param context  Context.
     * @param uri      URI ảnh, có thể null.
     * @param imageView View đích.
     */
    public static void loadThumb(@NonNull Context context,
                                 @Nullable String uri,
                                 @NonNull ShapeableImageView imageView) {
        if (uri == null || uri.isEmpty()) {
            imageView.setImageResource(DEFAULT_PLACEHOLDER);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(DEFAULT_PLACEHOLDER)
                .error(DEFAULT_ERROR)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(THUMB_SIZE);

        Glide.with(context)
                .load(Uri.parse(uri))
                .apply(options)
                .into(imageView);
    }

    /**
     * Clear bộ nhớ cache của Glide cho một View cụ thể.
     * Dùng trong ViewHolder.recycle() nếu cần.
     */
    public static void clear(@NonNull Context context,
                             @NonNull ShapeableImageView imageView) {
        Glide.with(context).clear(imageView);
    }
}
