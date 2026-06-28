package com.example.nghenhac.ui.library;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter cho ViewPager2 trong LibraryFragment.
 * Quản lý 4 tab: Songs, Albums, Artists, Playlists.
 *
 * Tab 0 (Bài hát) dùng SongListFragment — kết nối SongRepository + SongAdapter.
 * Các tab còn lại dùng PlaceholderFragment tạm thời.
 */
public class LibraryPagerAdapter extends FragmentStateAdapter {

    private static final int PAGE_COUNT = 4;

    public LibraryPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // Tab Bài hát — danh sách từ database
                return new SongListFragment();
            case 1:
                // Tab Album — grid từ database
                return new AlbumGridFragment();
            case 2:
                // Tab Nghệ sĩ — danh sách từ database
                return new ArtistListFragment();
            case 3:
                // Tab Playlist — danh sách từ database
                return new PlaylistListFragment();
            default:
                // Không còn tab nào dùng placeholder
                return new PlaceholderFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
