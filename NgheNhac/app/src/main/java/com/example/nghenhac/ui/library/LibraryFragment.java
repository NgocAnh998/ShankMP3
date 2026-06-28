package com.example.nghenhac.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.nghenhac.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Fragment chính của tab Thư viện — hiển thị danh sách bài hát, album, nghệ sĩ, playlist.
 *
 * Nguyên lý:
 * - Sử dụng TabLayout + ViewPager2 để chuyển đổi giữa 4 tab.
 * - LibraryPagerAdapter quản lý 4 Fragment con.
 * - Mỗi tab hiển thị danh sách tương ứng dạng RecyclerView.
 *
 * Luồng xử lý:
 * 1. Fragment được tạo → onCreateView() inflate layout.
 * 2. onViewCreated() → gắn ViewPager2 với TabLayout qua TabLayoutMediator.
 * 3. User vuốt / chọn tab → ViewPager2 chuyển fragment.
 */
public class LibraryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager);

        // Gắn Adapter
        LibraryPagerAdapter adapter = new LibraryPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Kết nối TabLayout với ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText(R.string.tab_songs);
                            break;
                        case 1:
                            tab.setText(R.string.tab_albums);
                            break;
                        case 2:
                            tab.setText(R.string.tab_artists);
                            break;
                        case 3:
                            tab.setText(R.string.tab_playlists);
                            break;
                    }
                }
        ).attach();
    }
}
