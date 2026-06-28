package com.example.nghenhac.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.model.SongDto;
import com.example.nghenhac.data.remote.MusicApiService;
import com.example.nghenhac.data.remote.RetrofitClient;
import com.example.nghenhac.data.repository.SongRepository;
import com.example.nghenhac.player.MusicPlayer;
import com.example.nghenhac.ui.adapter.SongAdapter;
import com.example.nghenhac.ui.library.SongBottomSheetDialog;
import com.example.nghenhac.ui.player.PlayerActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Fragment tìm kiếm bài hát, nghệ sĩ, album.
 *
 * Nguyên lý:
 * - TextInputEditText với debounce 300ms để tránh gọi search quá nhiều.
 * - Kết quả hiển thị qua RecyclerView + SongAdapter.
 * - Empty state khi không có kết quả.
 * - Observe SongRepository.search(query) LiveData để cập nhật kết quả realtime.
 *
 * Luồng xử lý:
 * 1. User nhập text → TextWatcher.onTextChanged() → postDelayed 300ms.
 * 2. Hết debounce → performSearch() → gọi songRepository.search(query).
 * 3. LiveData trả về → observe → adapter.submitList() hoặc empty state.
 * 4. User click kết quả → mở PlayerActivity tương tự SongListFragment.
 */
public class SearchFragment extends Fragment {

    private static final long SEARCH_DEBOUNCE_MS = 300;

    private TextInputEditText searchInput;
    private RecyclerView searchResults;
    private View emptyState;
    private View onlineLoadingIndicator;
    private Handler searchHandler;
    private SongRepository songRepository;
    private SongAdapter adapter;
    private LiveData<List<SongEntity>> currentSearchLiveData;
    private ExecutorService executor;
    private MusicApiService apiService;
    private String currentQuery = "";  // Tránh search online trùng lặp

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        songRepository = SongRepository.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        apiService = RetrofitClient.getApiService();

        searchInput = view.findViewById(R.id.search_input);
        searchResults = view.findViewById(R.id.search_results);
        emptyState = view.findViewById(R.id.empty_state);
        onlineLoadingIndicator = view.findViewById(R.id.online_loading);
        searchHandler = new Handler(Looper.getMainLooper());

        // Setup RecyclerView + adapter
        searchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchResults.setHasFixedSize(true);

        adapter = new SongAdapter();
        adapter.setOnSongClickListener(new SongAdapter.OnSongClickListener() {
            @Override
            public void onItemClick(@NonNull SongEntity song, int position, @NonNull View itemView) {
                MusicPlayer.getInstance(requireContext()).play(song);
                Intent intent = new Intent(requireContext(), PlayerActivity.class);
                startActivityWithSharedElement(intent, itemView);
            }

            @Override
            public void onMoreClick(@NonNull SongEntity song, @NonNull View anchor) {
                SongBottomSheetDialog.show(requireContext(), song);
            }

            @Override
            public void onFavoriteClick(@NonNull SongEntity song, boolean isFavorite, int position) {
                SongRepository.getInstance(requireContext()).updateFavorite(song.getId(), isFavorite);
            }
        });
        searchResults.setAdapter(adapter);

        // Search debounce
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacksAndMessages(null);
                searchHandler.postDelayed(() -> performSearch(s.toString().trim()), SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Xử lý nút Search trên bàn phím
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchInput.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    /**
     * Thực hiện tìm kiếm và cập nhật kết quả.
     *
     * Nguyên lý:
     * - Query rỗng → clear adapter, ẩn empty state.
     * - Query có nội dung → observe SongRepository.search() LiveData.
     * - Xoá observer cũ trước khi tạo mới để tránh memory leak.
     * - LiveData tự động notify adapter khi có kết quả.
     *
     * Input:
     * @param query Từ khoá tìm kiếm (đã trim).
     */
    /**
     * Thực hiện tìm kiếm kết hợp local + online.
     *
     * Nguyên lý:
     * - Query rỗng → clear adapter, ẩn empty state.
     * - Tìm local trước qua SongRepository.search() LiveData.
     * - Song song tìm online qua MusicApiService.searchSongs().
     * - Kết quả local + online được merge và hiển thị cùng nhau.
     * - Online search chạy trên background thread (ExecutorService).
     *
     * Input:
     * @param query Từ khoá tìm kiếm (đã trim).
     */
    private void performSearch(String query) {
        this.currentQuery = query;

        if (query.isEmpty()) {
            if (currentSearchLiveData != null) {
                currentSearchLiveData.removeObservers(getViewLifecycleOwner());
                currentSearchLiveData = null;
            }
            adapter.submitList(null);
            emptyState.setVisibility(View.GONE);
            if (onlineLoadingIndicator != null) {
                onlineLoadingIndicator.setVisibility(View.GONE);
            }
            return;
        }

        // Xoá observer cũ
        if (currentSearchLiveData != null) {
            currentSearchLiveData.removeObservers(getViewLifecycleOwner());
        }

        // Hiển thị loading indicator
        if (onlineLoadingIndicator != null) {
            onlineLoadingIndicator.setVisibility(View.VISIBLE);
        }

        // Tìm kiếm local
        currentSearchLiveData = songRepository.search(query);
        currentSearchLiveData.observe(getViewLifecycleOwner(), localSongs -> {
            // Luôn gọi online search song song, dù local có kết quả hay không
            final List<SongEntity> allResults = new ArrayList<>();
            if (localSongs != null) {
                allResults.addAll(localSongs);
            }

            // Hiển thị local results trước, rồi mới append online
            if (!allResults.isEmpty()) {
                adapter.submitList(new ArrayList<>(allResults));
                emptyState.setVisibility(View.GONE);
            }

            // Gọi online search bất kể local có kết quả hay không
            searchOnline(query, allResults);
        });
    }

    /**
     * Tìm kiếm online qua MusicApiService.
     *
     * Nguyên lý:
     * - Gọi API searchSongs(query) trên background thread.
     * - Map kết quả SongDto → SongEntity.
     * - Merge vào danh sách kết quả và cập nhật adapter.
     *
     * Input:
     * @param query      Từ khoá tìm kiếm.
     * @param allResults Danh sách kết quả đã có (local) để merge thêm.
     */
    private void searchOnline(String query, List<SongEntity> allResults) {
        executor.execute(() -> {
            try {
                Call<List<SongDto>> call = apiService.searchSongs(query);
                Response<List<SongDto>> response = call.execute();

                // Ẩn loading indicator trên main thread
                runOnUiThreadSafe(() -> {
                    if (onlineLoadingIndicator != null) {
                        onlineLoadingIndicator.setVisibility(View.GONE);
                    }
                });

                if (response.isSuccessful() && response.body() != null) {
                    List<SongDto> onlineSongs = response.body();
                    if (!onlineSongs.isEmpty()) {
                        for (SongDto dto : onlineSongs) {
                            SongEntity entity = new SongEntity(
                                    dto.getTitle() != null ? dto.getTitle() : "Không rõ",
                                    dto.getArtist() != null ? dto.getArtist() : "Không rõ",
                                    dto.getAlbum() != null ? dto.getAlbum() : "Không rõ",
                                    dto.getDuration(),
                                    dto.getStreamUrl(),           // filePath = stream URL
                                    dto.getAlbumArtUrl(),          // albumArtUri
                                    null,                         // mediaStoreId (online)
                                    System.currentTimeMillis()
                            );
                            allResults.add(entity);
                        }
                        // Cập nhật UI trên main thread
                        runOnUiThreadSafe(() -> {
                            adapter.submitList(new ArrayList<>(allResults));
                            emptyState.setVisibility(View.GONE);
                        });
                    }
                } else {
                    // Server trả về lỗi (VD: 404, 500)
                    runOnUiThreadSafe(() -> {
                        if (allResults.isEmpty()) {
                            com.google.android.material.snackbar.Snackbar.make(
                                    searchInput,
                                    "Không thể kết nối server: " + response.code(),
                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                            ).show();
                        }
                    });
                }
            } catch (java.net.ConnectException e) {
                // Không kết nối được server
                runOnUiThreadSafe(() -> {
                    if (onlineLoadingIndicator != null) {
                        onlineLoadingIndicator.setVisibility(View.GONE);
                    }
                    if (allResults.isEmpty()) {
                        adapter.submitList(null);
                        emptyState.setVisibility(View.VISIBLE);
                        com.google.android.material.snackbar.Snackbar.make(
                                searchInput,
                                "Không thể kết nối server. Kiểm tra IP trong local.properties",
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                        ).show();
                    }
                });
            } catch (java.net.UnknownHostException e) {
                runOnUiThreadSafe(() -> {
                    if (onlineLoadingIndicator != null) {
                        onlineLoadingIndicator.setVisibility(View.GONE);
                    }
                    if (allResults.isEmpty()) {
                        adapter.submitList(null);
                        emptyState.setVisibility(View.VISIBLE);
                        com.google.android.material.snackbar.Snackbar.make(
                                searchInput,
                                "Không có kết nối Internet",
                                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).show();
                    }
                });                } catch (Exception e) {
                runOnUiThreadSafe(() -> {
                    if (onlineLoadingIndicator != null) {
                        onlineLoadingIndicator.setVisibility(View.GONE);
                    }
                    if (allResults.isEmpty()) {
                        adapter.submitList(null);
                        emptyState.setVisibility(View.VISIBLE);
                        String errMsg = e.getMessage();
                        if (errMsg == null || errMsg.isEmpty()) {
                            errMsg = "Lỗi kết nối server. Kiểm tra IP và firewall";
                        }
                        com.google.android.material.snackbar.Snackbar.make(
                                searchResults != null ? searchResults : requireView(),
                                errMsg,
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Chạy một Runnable trên UI thread an toàn — kiểm tra fragment còn attached không.
     * Tránh crash IllegalStateException khi fragment đã detach.
     */
    private void runOnUiThreadSafe(@NonNull Runnable action) {
        if (isAdded() && getActivity() != null) {
            requireActivity().runOnUiThread(action);
        }
    }

    /**
     * Start activity với shared element transition (album art).
     *
     * Nguyên lý:
     * - Tìm album art View trong itemView.
     * - Tạo ActivityOptions.makeSceneTransitionAnimation với shared element.
     * - Dùng requireActivity() vì Fragment cần Activity context cho transition.
     *
     * Input:
     * @param intent   Intent đến PlayerActivity.
     * @param itemView View của item được click (chứa song_art).
     */
    private void startActivityWithSharedElement(@NonNull Intent intent, @NonNull View itemView) {
        View albumArt = itemView.findViewById(R.id.song_art);
        if (albumArt != null && albumArt.getTransitionName() != null) {
            android.app.ActivityOptions options = android.app.ActivityOptions
                    .makeSceneTransitionAnimation(requireActivity(), albumArt, albumArt.getTransitionName());
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }
}
