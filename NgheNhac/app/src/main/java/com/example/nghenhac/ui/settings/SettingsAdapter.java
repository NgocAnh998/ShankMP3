package com.example.nghenhac.ui.settings;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nghenhac.BuildConfig;
import com.example.nghenhac.R;
import com.example.nghenhac.player.CacheDataSourceFactory;
import com.example.nghenhac.sync.FirebaseAuthManager;
import com.example.nghenhac.sync.FirebaseSyncManager;
import com.example.nghenhac.sync.SyncWorker;
import com.example.nghenhac.ui.auth.LoginActivity;
import com.example.nghenhac.ui.auth.ProfileActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adapter cho RecyclerView trong SettingsFragment.
 *
 * Nguyên lý:
 * - Hiển thị danh sách các mục cài đặt dạng list.
 * - Mỗi mục có: title, subtitle, hành động khi click.
 * - Hỗ trợ các loại: header, item (click).
 * - Các mục động: Tài khoản, Theme, Xoá cache, Giới thiệu.
 *
 * Luồng xử lý:
 * 1. Fragment khởi tạo → adapter.setItems().
 * 2. User click item → handleItemClick() → xử lý tương ứng.
 * 3. Cache: xoá cache + hiển thị dung lượng cache hiện tại.
 * 4. About: hiển thị dialog thông tin app đầy đủ.
 */
public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.ViewHolder> {

    private List<SettingsItem> items = new ArrayList<>();
    private final SettingsFragment fragment;
    private final ExecutorService executor;

    public SettingsAdapter(SettingsFragment fragment) {
        this.fragment = fragment;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Cập nhật danh sách items và refresh RecyclerView.
     */
    public void setItems(List<SettingsItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settings, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SettingsItem item = items.get(position);
        holder.titleText.setText(item.title);
        holder.subtitleText.setText(item.subtitle);
        holder.subtitleText.setVisibility(item.subtitle != null ? View.VISIBLE : View.GONE);

        if (item.type == SettingsItem.TYPE_HEADER) {
            holder.itemView.setEnabled(false);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.itemView.setEnabled(true);
            holder.itemView.setOnClickListener(v -> handleItemClick(item));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Xử lý click trên từng mục cài đặt.
     */
    private void handleItemClick(SettingsItem item) {
        if (fragment == null || fragment.getActivity() == null) return;

        switch (item.id) {
            case "account":
                if (FirebaseAuthManager.getInstance().isLoggedIn()) {
                    Intent intent = new Intent(fragment.getActivity(), ProfileActivity.class);
                    fragment.startActivity(intent);
                } else {
                    Intent intent = new Intent(fragment.getActivity(), LoginActivity.class);
                    fragment.startActivityForResult(intent, SettingsFragment.REQUEST_LOGIN);
                }
                break;
            case "theme":
                showThemeDialog();
                break;
            case "clear_cache":
                showClearCacheDialog();
                break;
            case "about":
                showAboutDialog();
                break;
        }
    }

    /**
     * Hiển thị dialog chọn theme.
     */
    private void showThemeDialog() {
        String[] themes = {
                fragment.getString(R.string.settings_theme_system),
                fragment.getString(R.string.settings_theme_light),
                fragment.getString(R.string.settings_theme_dark)
        };

        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.settings_theme)
                .setSingleChoiceItems(themes, 0, (dialog, which) -> {
                    // TODO: Lưu theme preference và recreate activity
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * Xoá cache MediaStore và hiển thị thông báo.
     *
     * Nguyên lý:
     * - Lấy CacheDataSourceFactory instance.
     * - Gọi clearCache() trên executor background.
     * - Snackbar thông báo kết quả.
     */
    private void clearCache() {
        executor.execute(() -> {
            try {
                CacheDataSourceFactory cacheFactory =
                        CacheDataSourceFactory.getInstance(fragment.requireContext());
                cacheFactory.clearCache();

                if (fragment.getActivity() != null) {
                    fragment.getActivity().runOnUiThread(() -> {
                        Snackbar.make(
                                fragment.requireView(),
                                "Đã xoá bộ nhớ đệm",
                                Snackbar.LENGTH_SHORT).show();
                        // Refresh để cập nhật dung lượng cache
                        refreshCacheSize();
                    });
                }
            } catch (Exception e) {
                if (fragment.getActivity() != null) {
                    fragment.getActivity().runOnUiThread(() -> {
                        Snackbar.make(
                                fragment.requireView(),
                                "Lỗi xoá cache: " + e.getMessage(),
                                Snackbar.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Cập nhật dung lượng cache hiển thị trong settings.
     */
    private void refreshCacheSize() {
        executor.execute(() -> {
            try {
                CacheDataSourceFactory cacheFactory =
                        CacheDataSourceFactory.getInstance(fragment.requireContext());
                long cacheSize = cacheFactory.getCurrentCacheSize();
                String sizeText = formatFileSize(cacheSize);

                if (fragment.getActivity() != null) {
                    fragment.getActivity().runOnUiThread(() -> {
                        // Cập nhật subtitle của cache item
                        List<SettingsItem> newItems = new ArrayList<>(items);
                        for (int i = 0; i < newItems.size(); i++) {
                            if ("clear_cache".equals(newItems.get(i).id)) {
                                newItems.set(i, new SettingsItem(
                                        "clear_cache", SettingsItem.TYPE_ITEM,
                                        fragment.getString(R.string.settings_cache),
                                        "Đã dùng " + sizeText));
                                break;
                            }
                        }
                        setItems(newItems);
                    });
                }
            } catch (Exception e) {
                // Bỏ qua lỗi
            }
        });
    }

    /**
     * Định dạng dung lượng file (bytes → KB/MB/GB).
     *
     * Input:
     * @param bytes Dung lượng (bytes).
     *
     * Output:
     * @return Chuỗi định dạng: "1.5 MB", "500 KB", ...
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(size) + " " + units[unitIndex];
    }

    /**
     * Hiển thị dialog xác nhận xoá cache.
     */
    private void showClearCacheDialog() {
        executor.execute(() -> {
            String cacheSizeText = "0 B";
            try {
                long cacheSize = CacheDataSourceFactory
                        .getInstance(fragment.requireContext()).getCurrentCacheSize();
                cacheSizeText = formatFileSize(cacheSize);
            } catch (Exception ignored) {}

            final String finalCacheSizeText = cacheSizeText;
            if (fragment.getActivity() != null) {
                fragment.getActivity().runOnUiThread(() -> {
                    new MaterialAlertDialogBuilder(fragment.requireContext())
                            .setTitle(R.string.settings_clear_cache)
                            .setMessage("Dung lượng cache hiện tại: " + finalCacheSizeText
                                    + "\n\nBạn có chắc muốn xoá toàn bộ bộ nhớ đệm? "
                                    + "Các file nhạc đã tải xuống sẽ bị xoá.")
                            .setPositiveButton("Xoá", (dialog, which) -> clearCache())
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                });
            }
        });
    }

    /**
     * Hiển thị dialog giới thiệu về ứng dụng (nâng cấp).
     *
     * Nguyên lý:
     * - Hiển thị thông tin app, phiên bản, mô tả tính năng.
     * - Có nút để đánh giá hoặc chia sẻ app.
     */
    private void showAboutDialog() {
        String appName = fragment.getString(R.string.app_name);
        String version = BuildConfig.VERSION_NAME;

        String aboutMessage = appName + " - Music Player\n\n"
                + "\uD83C\uDFB5 Phiên bản: " + version + "\n"
                + "\uD83D\uDCC5 Ngày phát hành: 2026\n\n"
                + "\u2728 Tính năng chính:\n"
                + "\u2022 Phát nhạc local từ thiết bị\n"
                + "\u2022 Quản lý playlist, album, nghệ sĩ\n"
                + "\u2022 Đồng bộ yêu thích qua Firebase\n"
                + "\u2022 Phát nhạc nền (Foreground Service)\n"
                + "\u2022 Chế độ tối (Dark Mode)\n"
                + "\u2022 Import/Export playlist (M3U/XML)\n"
                + "\u2022 Offline cache\n\n"
                + "\uD83D\uDEE1\uFE0F Bảo mật: Mã hoá dữ liệu (EncryptedSharedPreferences)\n\n"
                + "\u00A9 2026 NgheNhac Team";

        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.settings_about)
                .setMessage(aboutMessage)
                .setPositiveButton(R.string.action_ok, null)
                .setNeutralButton("Đóng góp", (dialog, which) -> {
                    Snackbar.make(fragment.requireView(),
                            "Cảm ơn bạn! Hãy gửi feedback qua email.",
                            Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    // ── ViewHolder ──

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView subtitleText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.settings_item_title);
            subtitleText = itemView.findViewById(R.id.settings_item_subtitle);
        }
    }

    // ── SettingsItem Model ──

    public static class SettingsItem {
        static final int TYPE_HEADER = 0;
        static final int TYPE_ITEM = 1;

        final String id;
        final int type;
        final String title;
        final String subtitle;

        public SettingsItem(String id, int type, String title, String subtitle) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    /**
     * Tạo danh sách items mặc định cho Settings.
     */
    public static List<SettingsItem> createDefaultItems(SettingsFragment fragment) {
        List<SettingsItem> items = new ArrayList<>();
        FirebaseAuthManager auth = FirebaseAuthManager.getInstance();

        // ── Tài khoản ──
        if (auth.isLoggedIn()) {
            items.add(new SettingsItem("account", SettingsItem.TYPE_ITEM,
                    fragment.getString(R.string.settings_account),
                    auth.getUserEmail()));
        } else {
            items.add(new SettingsItem("account", SettingsItem.TYPE_ITEM,
                    fragment.getString(R.string.settings_account),
                    fragment.getString(R.string.settings_account_not_logged_in)));
        }

        // ── Giao diện ──
        items.add(new SettingsItem("theme_header", SettingsItem.TYPE_HEADER,
                fragment.getString(R.string.settings_general), null));
        items.add(new SettingsItem("theme", SettingsItem.TYPE_ITEM,
                fragment.getString(R.string.settings_theme),
                fragment.getString(R.string.settings_theme_system)));

        // ── Bộ nhớ đệm ──
        items.add(new SettingsItem("clear_cache", SettingsItem.TYPE_ITEM,
                fragment.getString(R.string.settings_cache),
                fragment.getString(R.string.settings_clear_cache)));

        // ── Giới thiệu ──
        items.add(new SettingsItem("about", SettingsItem.TYPE_ITEM,
                fragment.getString(R.string.settings_about),
                fragment.getString(R.string.settings_version) + " " + BuildConfig.VERSION_NAME));

        return items;
    }
}
