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
 * 🔍 SearchFragment — Tìm kiếm bài hát (local + online) với debounce.
 *
 * ============================================================
 *  GIẢI THÍCH CHI TIẾT — DÀNH CHO BÁO CÁO ĐỒ ÁN
 * ============================================================
 *
 * ─── 1. VAI TRÒ ───
 * Cho phép người dùng tìm kiếm bài hát NHANH CHÓNG từ:
 *   - 🏠 LOCAL: nhạc trong máy (Room database)
 *   - ☁️ ONLINE: nhạc từ server (Retrofit API)
 *
 * ─── 2. KIẾN TRÚC ───
 *
 *   ┌─────────────────────────────────────────────────────────┐
 *   │                    SearchFragment                       │
 *   │                                                         │
 *   │  TextInputEditText (ô nhập)                             │
 *   │       │                                                 │
 *   │       ├── TextWatcher → Debounce 300ms                 │
 *   │       ├── setOnEditorActionListener (nút Search)       │
 *   │       │                                                 │
 *   │       ▼                                                 │
 *   │  performSearch(query)                                   │
 *   │       │                                                 │
 *   │       ├── Query rỗng? → Clear + Ẩn empty state         │
 *   │       └── Query có nội dung →                          │
 *   │             │                                           │
 *   │             ├── 1. TÌM LOCAL (Room)                    │
 *   │             │    songRepository.search(query)           │
 *   │             │    → LiveData → observe → adapter        │
 *   │             │                                           │
 *   │             └── 2. TÌM ONLINE (Retrofit)               │
 *   │                  apiService.searchSongs(query)          │
 *   │                  → Executor (BG thread)                │
 *   │                  → Map SongDto → SongEntity            │
 *   │                  → Merge + adapter.submitList()        │
 *   │                                                         │
 *   │  RecyclerView (kết quả)                                 │
 *   │       │                                                 │
 *   │       ├── Click → MusicPlayer.play(song)               │
 *   │       ├── More → SongBottomSheetDialog                 │
 *   │       └── Favorite → SongRepository.updateFavorite()   │
 *   │                                                         │
 *   │  Empty State (khi không có kết quả)                     │
 *   │  Online Loading (ProgressBar khi chờ API)              │
 *   └─────────────────────────────────────────────────────────┘
 *
 * ─── 3. DEBOUNCE 300MS (QUAN TRỌNG) ───
 *
 *   User gõ "jazz" trong 0.5 giây → gõ 4 ký tự
 *       │
 *       ├── KHÔNG debounce: search 4 lần (lãng phí)
 *       │   "j" → search "j"
 *       │   "ja" → search "ja"
 *       │   "jaz" → search "jaz"
 *       │   "jazz" → search "jazz"
 *       │
 *       └── CÓ debounce 300ms: CHỈ search 1 lần
 *           Gõ "j" → đặt timer 300ms
 *           Gõ "ja" → reset timer 300ms
 *           Gõ "jaz" → reset timer 300ms
 *           Gõ "jazz" → reset timer 300ms
 *           Hết 300ms không gõ → performSearch("jazz") ✅
 *
 *   GIỐNG NHƯ: Gõ cửa. Gõ liên tục? Người trong nhà đợi
 *   bạn dừng 0.3s mới ra mở.
 *
 * ─── 4. LUỒNG CHI TIẾT ───
 *
 *   Bước 1: Fragment khởi tạo → onViewCreated()
 *   Bước 2: Setup RecyclerView + SongAdapter
 *   Bước 3: Gắn TextWatcher vào ô tìm kiếm
 *   Bước 4: User gõ "jazz" → TextWatcher.onTextChanged()
 *   Bước 5: Debounce 300ms → performSearch("jazz")
 *   Bước 6: Song song:
 *           - LOCAL: songRepository.search("jazz") → LiveData
 *             → observe → adapter.submitList(localResults)
 *           - ONLINE: searchOnline("jazz", localResults)
 *             → Executor → Retrofit → Server
 *             → Map DTO → Entity → Merge → adapter.submitList()
 *   Bước 7: User click kết quả → MusicPlayer.play()
 *   Bước 8: ActivityOptions shared element → PlayerActivity
 *
 * ─── 5. XỬ LÝ LỖI ───
 *   - ConnectException: Server không chạy → Snackbar báo
 *   - UnknownHostException: Mất Internet → Snackbar báo
 *   - Exception khác: Fallback message → Snackbar
 *   - Fragment detached: runOnUiThreadSafe() → tránh crash
 *
 * ─── 6. THREAD SAFETY ───
 *   - Online search chạy trên BACKGROUND thread (ExecutorService)
 *   - Cập nhật UI qua runOnUiThreadSafe() (kiểm tra isAdded())
 *   - Dùng bản sao List<SongEntity> để tránh concurrent modification
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
     * ─── THỰC HIỆN TÌM KIẾM ───
     *
     * Đây là phương thức CHÍNH. Được gọi SAU KHI debounce 300ms kết thúc.
     *
     * LUỒNG CHI TIẾT:
     *
     *  Bước 1: Kiểm tra query có rỗng không?
     *          - Rỗng → clear adapter, ẩn empty state, bỏ qua
     *          - Có nội dung → tiếp tục
     *
     *  Bước 2: Xoá observer cũ (nếu có)
     *          → Tránh memory leak: LiveData giữ reference đến Fragment
     *
     *  Bước 3: Hiển thị loading indicator (ProgressBar)
     *          → User biết đang chờ kết quả online
     *
     *  Bước 4: Gọi LOCAL search → SongRepository.search(query)
     *          → Trả về LiveData (reactive)
     *          → observe: khi có kết quả, hiển thị ngay
     *
     *  Bước 5: Trong callback của local, gọi ONLINE search
     *          → searchOnline(queryCopy, localResults)
     *          → Chạy trên BACKGROUND thread (ExecutorService)
     *          → Không block UI
     *
     *  Bước 6: Kết quả online → merge vào local → adapter.submitList()
     *          → Cập nhật UI trên MAIN thread qua runOnUiThreadSafe()
     *
     * @param query Từ khoá tìm kiếm (đã trim khoảng trắng)
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

        // Tạo bản sao query để tránh race condition
        final String queryCopy = query;

        // Tìm kiếm local
        currentSearchLiveData = songRepository.search(queryCopy);
        currentSearchLiveData.observe(getViewLifecycleOwner(), localSongs -> {
            // Tạo bản sao list để tránh concurrent modification với online search
            final List<SongEntity> localResults = new ArrayList<>();
            if (localSongs != null) {
                localResults.addAll(localSongs);
            }

            // Hiển thị local results trước
            if (!localResults.isEmpty()) {
                adapter.submitList(new ArrayList<>(localResults));
                emptyState.setVisibility(View.GONE);
            }

            // Gọi online search với bản sao local results (không dùng chung tham chiếu)
            searchOnline(queryCopy, localResults);
        });
    }

    /**
     * Tìm kiếm online qua MusicApiService.
     *
     * Nguyên lý:
     * - Gọi API searchSongs(query) trên background thread.
     * - Map kết quả SongDto → SongEntity.
     * - Merge vào danh sách kết quả và cập nhật adapter.
     * - Dùng bản sao localResults để tránh concurrent modification.
     *
     * Input:
     * @param query       Từ khoá tìm kiếm.
     * @param localResults Danh sách kết quả local (bản sao, an toàn để thêm online).
     */
    private void searchOnline(String query, List<SongEntity> localResults) {
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
                        // Tạo kết quả tổng hợp: local + online (trên thread này, ko dùng chung với main)
                        final List<SongEntity> mergedResults = new ArrayList<>(localResults);
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
                            mergedResults.add(entity);
                        }
                        // Cập nhật UI trên main thread — submit bản sao mới
                        final List<SongEntity> finalResults = new ArrayList<>(mergedResults);
                        runOnUiThreadSafe(() -> {
                            adapter.submitList(finalResults);
                            emptyState.setVisibility(View.GONE);
                        });
                    }
                } else {
                    // Server trả về lỗi (VD: 404, 500)
                    runOnUiThreadSafe(() -> {
                        if (localResults.isEmpty()) {
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
                    if (localResults.isEmpty()) {
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
                    if (localResults.isEmpty()) {
                        adapter.submitList(null);
                        emptyState.setVisibility(View.VISIBLE);
                        com.google.android.material.snackbar.Snackbar.make(
                                searchInput,
                                "Không có kết nối Internet",
                                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThreadSafe(() -> {
                    if (onlineLoadingIndicator != null) {
                        onlineLoadingIndicator.setVisibility(View.GONE);
                    }
                    if (localResults.isEmpty()) {
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
