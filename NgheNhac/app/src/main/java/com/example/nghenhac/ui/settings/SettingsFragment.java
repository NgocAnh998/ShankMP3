package com.example.nghenhac.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nghenhac.R;
import com.example.nghenhac.sync.FirebaseAuthManager;
import com.example.nghenhac.sync.SyncWorker;

/**
 * ⚙️ SettingsFragment — Màn hình cài đặt ứng dụng.
 *
 * ============================================================
 *  GIẢI THÍCH CHI TIẾT — DÀNH CHO BÁO CÁO ĐỒ ÁN
 * ============================================================
 *
 * ─── 1. VAI TRÒ ───
 * Cung cấp giao diện để người dùng quản lý:
 *   - 🔑 Tài khoản: Đăng nhập/Đăng xuất/Xem profile
 *   - 🎨 Giao diện: Theme sáng/tối/theo hệ thống
 *   - 💾 Bộ nhớ đệm: Xem dung lượng + xoá cache
 *   - ℹ️ Giới thiệu: Thông tin app, phiên bản
 *
 * ─── 2. KIẾN TRÚC ───
 *
 *   ┌─────────────────────────────────────────────────────┐
 *   │                 SettingsFragment                    │
 *   │                                                     │
 *   │  RecyclerView (settings_list)                       │
 *   │       │                                             │
 *   │       ▼                                             │
 *   │  SettingsAdapter                                    │
 *   │       │                                             │
 *   │       ├── Item 0: "Tài khoản" (động)              │
 *   │       │   ├── Chưa đăng nhập → LoginActivity       │
 *   │       │   └── Đã đăng nhập → ProfileActivity      │
 *   │       │                                             │
 *   │       ├── Item 1: Header "Chung"                  │
 *   │       ├── Item 2: "Giao diện" → Dialog theme     │
 *   │       ├── Item 3: "Bộ nhớ đệm" → Xoá cache       │
 *   │       └── Item 4: "Giới thiệu" → About dialog    │
 *   │                                                     │
 *   │  ActivityResultLauncher (loginLauncher)             │
 *   │       └── Nhận kết quả từ LoginActivity            │
 *   │           → Refresh danh sách + schedule sync       │
 *   └─────────────────────────────────────────────────────┘
 *
 * ─── 3. LUỒNG CHI TIẾT ───
 *
 *   Bước 1: Fragment tạo → onViewCreated()
 *   Bước 2: Setup RecyclerView + LinearLayoutManager
 *   Bước 3: Tạo SettingsAdapter with this (Fragment)
 *   Bước 4: Đăng ký loginLauncher (ActivityResultLauncher)
 *           → Thay thế onActivityResult (deprecated từ Android X)
 *   Bước 5: refreshSettingsItems() → tạo danh sách items
 *   Bước 6: User click item → SettingsAdapter.handleItemClick()
 *   Bước 7: User quay lại từ LoginActivity → onResume()
 *           → refreshSettingsItems() → cập nhật trạng thái
 *           → Nếu đã đăng nhập → lên lịch đồng bộ SyncWorker
 *
 * ─── 4. ACTIVITY RESULT LAUNCHER ───
 *   - registerForActivityResult() thay thế onActivityResult() cũ
 *   - An toàn hơn, không bị ảnh hưởng bởi lifecycle
 *   - Khi LoginActivity trả về RESULT_OK:
 *     1. Refresh danh sách (hiển thị email thay vì "Chưa đăng nhập")
 *     2. Lên lịch đồng bộ Firebase (SyncWorker.scheduleSync())
 */
public class SettingsFragment extends Fragment {

    private SettingsAdapter adapter;
    private RecyclerView settingsList;
    private ActivityResultLauncher<Intent> loginLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsList = view.findViewById(R.id.settings_list);
        settingsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        settingsList.setHasFixedSize(true);

        adapter = new SettingsAdapter(this);
        settingsList.setAdapter(adapter);

        // Đăng ký ActivityResultLauncher — thay thế onActivityResult deprecated
        loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        refreshSettingsItems();
                        // Lên lịch đồng bộ background sau khi đăng nhập
                        if (FirebaseAuthManager.getInstance().isLoggedIn()) {
                            SyncWorker.scheduleSync(requireContext());
                        }
                    }
                });

        refreshSettingsItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh khi quay lại từ LoginActivity hoặc ProfileActivity
        refreshSettingsItems();
    }

    /**
     * Lấy loginLauncher để SettingsAdapter dùng mở LoginActivity.
     * SettingsAdapter không có Activity context, nên cần lấy từ Fragment.
     *
     * @return ActivityResultLauncher để start activity và nhận kết quả
     */
    public ActivityResultLauncher<Intent> getLoginLauncher() {
        return loginLauncher;
    }

    /**
     * ─── LÀM MỚI DANH SÁCH CÀI ĐẶT ───
     *
     * Gọi lại mỗi khi:
     *   - Fragment vừa tạo (lần đầu)
     *   - User quay lại từ LoginActivity/ProfileActivity
     *
     * Tạo danh sách items MỚI → adapter.setItems() → notifyDataSetChanged()
     * Items THAY ĐỔI theo trạng thái đăng nhập:
     *   - Chưa đăng nhập: subtitle = "Chưa đăng nhập"
     *   - Đã đăng nhập:   subtitle = email user
     */
    private void refreshSettingsItems() {
        if (adapter != null) {
            adapter.setItems(SettingsAdapter.createDefaultItems(this));
        }
    }
}
