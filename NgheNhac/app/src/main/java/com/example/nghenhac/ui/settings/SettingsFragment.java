package com.example.nghenhac.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nghenhac.R;
import com.example.nghenhac.sync.FirebaseAuthManager;
import com.example.nghenhac.sync.SyncWorker;

/**
 * Fragment cài đặt ứng dụng.
 *
 * Nguyên lý:
 * - Hiển thị danh sách các tuỳ chọn cài đặt dạng RecyclerView.
 * - Các mục: Tài khoản (đăng nhập/profile), Giao diện, Xoá cache, Giới thiệu.
 * - Tự động refresh danh sách khi trạng thái đăng nhập thay đổi.
 *
 * Luồng xử lý:
 * 1. Fragment được tạo → gắn SettingsAdapter với danh sách items.
 * 2. User chọn "Tài khoản" → mở LoginActivity hoặc ProfileActivity.
 * 3. User quay lại từ LoginActivity → refresh danh sách + lên lịch sync.
 *
 * Lưu ý:
 * - REQUEST_LOGIN dùng để refresh adapter sau khi login thành công.
 * - SyncWorker.scheduleSync() được gọi sau lần đăng nhập đầu tiên.
 */
public class SettingsFragment extends Fragment {

    static final int REQUEST_LOGIN = 9001;

    private SettingsAdapter adapter;
    private RecyclerView settingsList;

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

        refreshSettingsItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh khi quay lại từ LoginActivity hoặc ProfileActivity
        refreshSettingsItems();
    }

    /**
     * Xử lý kết quả từ LoginActivity.
     *
     * Nguyên lý:
     * - Khi login thành công → refresh danh sách + schedule sync background.
     *
     * Input:
     * @param requestCode REQUEST_LOGIN.
     * @param resultCode  RESULT_OK nếu login thành công.
     * @param data        Intent (không dùng).
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOGIN && resultCode == getActivity().RESULT_OK) {
            refreshSettingsItems();
            // Lên lịch đồng bộ background sau khi đăng nhập
            if (FirebaseAuthManager.getInstance().isLoggedIn()) {
                SyncWorker.scheduleSync(requireContext());
            }
        }
    }

    /**
     * Refresh danh sách mục cài đặt.
     *
     * Nguyên lý:
     * - Tạo lại danh sách items từ SettingsAdapter.createDefaultItems().
     * - Items động: Account item thay đổi theo trạng thái đăng nhập.
     */
    private void refreshSettingsItems() {
        if (adapter != null) {
            adapter.setItems(SettingsAdapter.createDefaultItems(this));
        }
    }
}
