package com.example.nghenhac.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nghenhac.R;

/**
 * Placeholder fragment tạm thời cho mỗi tab trong Library.
 *
 * Hiển thị tên tab và lời nhắc "Sắp ra mắt".
 * Sẽ được thay thế bằng các Fragment chuyên biệt sau này.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TAB_INDEX = "tab_index";

    public static PlaceholderFragment newInstance(int tabIndex) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_INDEX, tabIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_placeholder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView label = view.findViewById(R.id.placeholder_label);
        int tabIndex = getArguments() != null ? getArguments().getInt(ARG_TAB_INDEX, 0) : 0;

        String[] tabNames = {"Bài hát", "Album", "Nghệ sĩ", "Playlist"};
        String name = (tabIndex >= 0 && tabIndex < tabNames.length) ? tabNames[tabIndex] : "Tab";
        label.setText(name);
    }
}
