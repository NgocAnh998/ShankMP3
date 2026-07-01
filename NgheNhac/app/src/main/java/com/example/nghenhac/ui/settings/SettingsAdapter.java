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
 * ⚙️ SettingsAdapter — Hiển thị danh sách các mục cài đặt.
 *
 * ============================================================
 *  GIẢI THÍCH CHI TIẾT — DÀNH CHO BÁO CÁO ĐỒ ÁN
 * ============================================================
 *
 * ─── 1. VAI TRÒ ───
 * Quản lý danh sách các mục cài đặt dạng RecyclerView.
 * Mỗi mục gồm: id (định danh), title, subtitle, type (header/item).
 * Khi user click → xử lý hành động tương ứng.
 *
 * ─── 2. CÁC LOẠI ITEM ───
 *
 *   TYPE_HEADER: Chỉ hiển thị tiêu đề, không click được
 *   TYPE_ITEM:    Có thể click, mỗi item có id riêng để xử lý
 *
 * ─── 3. CÁC MỤC CÀI ĐẶT ───
 *
 *   ┌──────────────────────────────────────────────────────┐
 *   │  🔑 Tài khoản (id: "account")                       │
 *   │  ├── Chưa đăng nhập                                  │
 *   │  │    → Click → LoginActivity (đăng nhập)           │
 *   │  └── Đã đăng nhập (hiển thị email)                   │
 *   │       → Click → ProfileActivity (xem profile)       │
 *   │                                                      │
 *   │  ─── CHUNG (header) ───                              │
 *   │                                                      │
 *   │  🎨 Giao diện (id: "theme")                        │
 *   │  ├── Click → Dialog chọn theme                      │
 *   │  │    ● Theo hệ thống                                │
 *   │  │    ○ Sáng                                         │
 *   │  │    ○ Tối                                          │
 *   │  └── (TODO: lưu và áp dụng theme)                    │
 *   │                                                      │
 *   │  💾 Bộ nhớ đệm (id: "clear_cache")                 │
 *   │  ├── Click → Dialog xác nhận                        │
 *   │  ├── Hiển thị: "Đã dùng 15.3 MB"                   │
 *   │  └── Xoá → CacheDataSourceFactory.clearCache()      │
 *   │                                                      │
 *   │  ℹ️ Giới thiệu (id: "about")                       │
 *   │     ├── Click → Dialog thông tin app                 │
 *   │     └── Hiển thị: "Phiên bản 1.0"                  │
 *   └──────────────────────────────────────────────────────┘
 *
 * ─── 4. LUỒNG XỬ LÝ CLICK ───
 *
 *   User click item → handleItemClick(item)
 *       │
 *       ├── "account"     → LoginActivity / ProfileActivity
 *       ├── "theme"       → showThemeDialog()
 *       ├── "clear_cache" → showClearCacheDialog() + clearCache()
 *       └── "about"       → showAboutDialog()
 *
 * ─── 5. TÍNH NĂNG NỔI BẬT ───
 *   - Cache tự động tính dung lượng và hiển thị (formatFileSize)
 *   - Cache xoá trên BACKGROUND thread (ExecutorService)
 *   - Cập nhật subtitle cache sau khi xoá
 *   - MaterialAlertDialog cho tất cả dialog (Material Design 3)
 *   - Snackbar thông báo kết quả
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
     * ─── XỬ LÝ CLICK TRÊN TỪNG MỤC ───
     *
     * Dựa vào item.id để quyết định hành động:
     *
     *   "account"     → Kiểm tra đăng nhập → Login/Profile
     *   "theme"       → Dialog chọn theme
     *   "clear_cache" → Dialog xác nhận → Clear cache
     *   "about"       → Dialog thông tin app
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
                    // Dùng loginLauncher từ SettingsFragment (ActivityResultLauncher)
                    if (fragment.getLoginLauncher() != null) {
                        fragment.getLoginLauncher().launch(intent);
                    } else {
                        // Fallback: start thường (không nhận kết quả)
                        fragment.startActivity(intent);
                    }
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
     * ─── DIALOG CHỌN THEME ───
     *
     * Hiển thị 3 lựa chọn:
     *   - Theo hệ thống: tự động theo cài đặt hệ thống Android
     *   - Sáng: luôn ở chế độ sáng
     *   - Tối: luôn ở chế độ tối
     *
     * TODO: Lưu preference và gọi AppCompatDelegate.setDefaultNightMode()
     *       để áp dụng ngay lập tức.
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
     * ─── XOÁ CACHE ───
     *
     * Xoá toàn bộ file cache đã tải khi stream nhạc.
     * Chạy trên BACKGROUND thread (ExecutorService) để không block UI.
     *
     * LUỒNG:
     *   1. Lấy CacheDataSourceFactory instance (Singleton)
     *   2. Gọi cacheFactory.clearCache() — xoá file trong cache dir
     *   3. Hiển thị Snackbar "Đã xoá bộ nhớ đệm"
     *   4. Cập nhật lại dung lượng cache = 0 B
     *
     * LƯU Ý:
     *   - Chỉ xoá CACHE (các chunk nhạc đã tải tạm thời)
     *   - KHÔNG ảnh hưởng đến: bài hát yêu thích, playlist, tài khoản
     *   - Chạy background để không làm treo UI
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
     * ─── CẬP NHẬT DUNG LƯỢNG CACHE ───
     *
     * Tính dung lượng cache hiện tại và cập nhật vào subtitle của item.
     * Chạy trên BACKGROUND thread.
     *
     * LUỒNG:
     *   1. Lấy CacheDataSourceFactory → cacheFactory.getCurrentCacheSize()
     *   2. Định dạng bytes → "15.3 MB" (formatFileSize)
     *   3. Cập nhật item "clear_cache" với subtitle mới
     *   4. Gọi setItems() → notifyDataSetChanged() → UI cập nhật
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
     * ─── ĐỊNH DẠNG DUNG LƯỢNG ───
     *
     * Chuyển đổi bytes → định dạng dễ đọc:
     *   1024 B       → "1.0 KB"
     *   15728640 B   → "15.0 MB"
     *   1073741824 B → "1.0 GB"
     *
     * @param bytes Dung lượng (bytes)
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
     * ─── DIALOG XÁC NHẬN XOÁ CACHE ───
     *
     * Hiển thị dung lượng cache hiện tại + xác nhận.
     * Nếu user đồng ý → gọi clearCache() (background thread).
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
     * ─── DIALOG GIỚI THIỆU ───
     *
     * Hiển thị thông tin ứng dụng:
     *   - Tên app + version (BuildConfig.VERSION_NAME)
     *   - Danh sách tính năng chính
     *   - Thông tin bảo mật
     *   - Nút "Đóng góp" (feedback)
     *
     * BuildConfig.VERSION_NAME được tự động sinh từ build.gradle.kts.
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
     * ─── TẠO DANH SÁCH ITEMS MẶC ĐỊNH ───
     *
     * Tạo danh sách các mục cài đặt dựa trên trạng thái hiện tại:
     *
     *   1. TÀI KHOẢN (động)
     *      - Đã đăng nhập: hiển thị email user
     *      - Chưa đăng nhập: "Chưa đăng nhập"
     *
     *   2. HEADER "Chung"
     *   3. GIAO DIỆN: subtitle = "Theo hệ thống"
     *   4. BỘ NHỚ ĐỆM: subtitle = "Xoá bộ nhớ đệm"
     *   5. GIỚI THIỆU: subtitle = version
     *
     * @param fragment SettingsFragment để lấy context + string resources
     * @return Danh sách SettingsItem
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
