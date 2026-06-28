package com.example.nghenhac.player;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;

import com.example.nghenhac.data.local.PreferencesManager;
import com.example.nghenhac.data.local.entity.SongEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quản lý danh sách phát (playback queue).
 *
 * Nguyên lý:
 * - Duy trì danh sách các MediaItem (được map từ SongEntity) để ExoPlayer phát.
 * - Quản lý repeat mode (NONE/ONE/ALL) và shuffle mode.
 * - Shuffle mode dùng shuffled indices: danh sách gốc giữ nguyên, chỉ thay đổi thứ tự duyệt.
 * - Có thể replace toàn bộ queue (khi phát album/playlist mới).
 *
 * Luồng xử lý:
 * 1. User chọn phát album/playlist → setQueue(songs, startIndex) → ExoPlayer.setMediaItems().
 * 2. User chọn phát một bài → setQueue(songs, startIndex).
 * 3. User bật/tắt shuffle → toggleShuffle() → tính toán lại shuffled indices.
 * 4. User đổi repeat → cycleRepeatMode() → lưu vào PreferencesManager.
 * 5. next()/previous() → lấy index tiếp theo dựa trên currentIndex + shuffledIndices.
 *
 * Input:
 * - List<SongEntity>: danh sách bài hát cần phát.
 * - startIndex: index bắt đầu phát trong danh sách gốc.
 *
 * Output:
 * - currentIndex: index hiện tại trong danh sách gốc.
 * - getCurrentMediaItem(): MediaItem hiện tại.
 *
 * Lưu ý:
 * - repeat/shuffle state được đồng bộ với PreferencesManager.
 * - Khi queue rỗng, next()/previous() trả về -1.
 */
public class PlaybackQueue {

    private final Context context;
    private List<MediaItem> mediaItems;     // danh sách gốc
    private List<Integer> shuffledIndices;  // shuffled indices (null nếu shuffle tắt)
    private int currentIndex;               // index hiện tại trong danh sách gốc
    private boolean isShuffleEnabled;
    private int repeatMode;                 // 0=NONE, 1=ONE, 2=ALL

    /** Sự kiện khi queue thay đổi (gửi đến listener). */
    public interface OnQueueChangedListener {
        void onQueueChanged(List<MediaItem> newQueue, int startIndex);
    }

    /**
     * @param context Context để truy cập PreferencesManager.
     */
    public PlaybackQueue(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mediaItems = new ArrayList<>();
        this.shuffledIndices = null;
        this.currentIndex = -1;
        this.isShuffleEnabled = false;

        // Đọc repeat/shuffle từ preferences
        PreferencesManager prefs = PreferencesManager.getInstance(context);
        this.isShuffleEnabled = prefs.isShuffleMode();
        PreferencesManager.RepeatMode mode = prefs.getRepeatMode();
        this.repeatMode = mode.ordinal(); // NONE=0, ONE=1, ALL=2
    }

    // ════════════════════════════════════════════
    //  Queue Management
    // ════════════════════════════════════════════

    /**
     * Set toàn bộ queue từ danh sách SongEntity.
     *
     * Nguyên lý:
     * - Map mỗi SongEntity → MediaItem.
     * - Nếu shuffle đang bật, tính toán lại shuffled indices.
     * - Chuyển đến bài hát tại startIndex.
     *
     * Input:
     * @param songs      Danh sách bài hát cần phát.
     * @param startIndex Index bắt đầu phát (trong danh sách gốc).
     */
    public void setQueue(@NonNull List<SongEntity> songs, int startIndex) {
        this.mediaItems = new ArrayList<>();
        for (SongEntity song : songs) {
            mediaItems.add(buildMediaItem(song));
        }

        this.currentIndex = Math.max(0, Math.min(startIndex, mediaItems.size() - 1));

        if (isShuffleEnabled && !mediaItems.isEmpty()) {
            buildShuffledIndices();
        } else {
            shuffledIndices = null;
        }
    }

    /**
     * Set queue từ danh sách MediaItem (dùng cho streaming hoặc playlist có sẵn).
     */
    public void setMediaItems(@NonNull List<MediaItem> items, int startIndex) {
        this.mediaItems = new ArrayList<>(items);
        this.currentIndex = Math.max(0, Math.min(startIndex, mediaItems.size() - 1));

        if (isShuffleEnabled && !mediaItems.isEmpty()) {
            buildShuffledIndices();
        } else {
            shuffledIndices = null;
        }
    }

    /**
     * Map SongEntity thành MediaItem với metadata.
     *
     * Nguyên lý:
     * - Dùng MediaItem.Builder với URI từ filePath.
     * - Set title, artist, album qua MediaMetadata.
     * - Media3 tự động parse các trường này cho UI.
     */
    @NonNull
    private MediaItem buildMediaItem(@NonNull SongEntity song) {
        return new MediaItem.Builder()
                .setMediaId(String.valueOf(song.getId()))
                .setUri(song.getFilePath())
                .setMediaMetadata(
                        new androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(song.getTitle())
                                .setArtist(song.getArtist())
                                .setAlbumTitle(song.getAlbum())
                                .setArtworkUri(
                                        song.getAlbumArtUri() != null
                                                ? android.net.Uri.parse(song.getAlbumArtUri())
                                                : null)
                                .build()
                )
                .build();
    }

    // ════════════════════════════════════════════
    //  Navigation
    // ════════════════════════════════════════════

    /**
     * Chuyển đến bài hát tiếp theo.
     *
     * Nguyên lý:
     * - Nếu repeat ONE → giữ nguyên currentIndex (phát lại bài hiện tại).
     * - Nếu repeat NONE + đang ở bài cuối → trả về -1 (dừng phát).
     * - Nếu repeat ALL + đang ở bài cuối → quay lại bài đầu tiên.
     * - Nếu shuffle bật → lấy index từ shuffledIndices.
     *
     * Output:
     * @return Index tiếp theo, hoặc -1 nếu hết queue.
     */
    public int next() {
        if (mediaItems.isEmpty()) return -1;

        if (repeatMode == 1) { // REPEAT ONE
            return currentIndex;
        }

        if (isShuffleEnabled && shuffledIndices != null) {
            int shuffledPos = shuffledIndices.indexOf(currentIndex);
            int nextPos = shuffledPos + 1;
            if (nextPos >= shuffledIndices.size()) {
                if (repeatMode == 2) { // REPEAT ALL
                    currentIndex = shuffledIndices.get(0);
                    return currentIndex;
                } else {
                    return -1; // hết queue
                }
            }
            currentIndex = shuffledIndices.get(nextPos);
        } else {
            int nextIdx = currentIndex + 1;
            if (nextIdx >= mediaItems.size()) {
                if (repeatMode == 2) { // REPEAT ALL
                    currentIndex = 0;
                    return currentIndex;
                } else {
                    return -1;
                }
            }
            currentIndex = nextIdx;
        }
        return currentIndex;
    }

    /**
     * Chuyển đến bài hát trước đó.
     *
     * Nguyên lý:
     * - Nếu repeat ONE → giữ nguyên currentIndex.
     * - Nếu đang ở bài đầu tiên + repeat ALL → quay lại bài cuối.
     * - Nếu shuffle bật → lấy index từ shuffledIndices.
     *
     * Output:
     * @return Index trước đó, hoặc -1 nếu không có.
     */
    public int previous() {
        if (mediaItems.isEmpty()) return -1;

        if (repeatMode == 1) { // REPEAT ONE
            return currentIndex;
        }

        if (isShuffleEnabled && shuffledIndices != null) {
            int shuffledPos = shuffledIndices.indexOf(currentIndex);
            int prevPos = shuffledPos - 1;
            if (prevPos < 0) {
                prevPos = shuffledIndices.size() - 1;
            }
            currentIndex = shuffledIndices.get(prevPos);
        } else {
            int prevIdx = currentIndex - 1;
            if (prevIdx < 0) {
                if (repeatMode == 2) { // REPEAT ALL
                    currentIndex = mediaItems.size() - 1;
                    return currentIndex;
                }
                return -1;
            }
            currentIndex = prevIdx;
        }
        return currentIndex;
    }

    /**
     * Chuyển đến index cụ thể trong danh sách gốc.
     *
     * Input:
     * @param index Index trong danh sách gốc.
     * @return Index đã set, hoặc -1 nếu không hợp lệ.
     */
    public int goTo(int index) {
        if (index >= 0 && index < mediaItems.size()) {
            currentIndex = index;
            return currentIndex;
        }
        return -1;
    }

    // ════════════════════════════════════════════
    //  Repeat & Shuffle
    // ════════════════════════════════════════════

    /**
     * Chuyển đổi repeat mode theo vòng: NONE → ONE → ALL → NONE → ...
     * Lưu thay đổi vào PreferencesManager.
     */
    public void cycleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
        PreferencesManager.RepeatMode mode =
                repeatMode == 0 ? PreferencesManager.RepeatMode.NONE :
                repeatMode == 1 ? PreferencesManager.RepeatMode.ONE :
                                  PreferencesManager.RepeatMode.ALL;
        PreferencesManager.getInstance(context).setRepeatMode(mode);
    }

    /** Lấy repeat mode hiện tại (0=NONE, 1=ONE, 2=ALL). */
    public int getRepeatMode() { return repeatMode; }

    /**
     * Bật/tắt shuffle mode.
     *
     * Nguyên lý:
     * - Khi bật: tạo shuffled indices (đảm bảo index hiện tại ở vị trí đầu tiên).
     * - Khi tắt: xoá shuffled indices.
     * - Lưu vào PreferencesManager.
     *
     * Input:
     * @param enabled true = bật shuffle, false = tắt.
     */
    public void setShuffleEnabled(boolean enabled) {
        this.isShuffleEnabled = enabled;
        PreferencesManager.getInstance(context).setShuffleMode(enabled);

        if (enabled && !mediaItems.isEmpty()) {
            buildShuffledIndices();
        } else {
            shuffledIndices = null;
        }
    }

    /** Kiểm tra shuffle có đang bật không. */
    public boolean isShuffleEnabled() { return isShuffleEnabled; }

    /**
     * Tạo danh sách shuffled indices.
     *
     * Nguyên lý:
     * - Tạo danh sách index [0, 1, 2, ..., size-1].
     * - Shuffle ngẫu nhiên.
     * - Đưa currentIndex lên đầu danh sách để bài hiện tại không thay đổi.
     */
    private void buildShuffledIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < mediaItems.size(); i++) {
            if (i != currentIndex) {
                indices.add(i);
            }
        }
        Collections.shuffle(indices);
        // Đưa currentIndex lên đầu
        indices.add(0, currentIndex);
        this.shuffledIndices = indices;
    }

    // ════════════════════════════════════════════
    //  Getters
    // ════════════════════════════════════════════

    /** Lấy index hiện tại trong danh sách gốc. Trả về -1 nếu queue rỗng. */
    public int getCurrentIndex() { return currentIndex; }

    /** Lấy MediaItem hiện tại. Trả về null nếu queue rỗng. */
    public MediaItem getCurrentMediaItem() {
        if (currentIndex >= 0 && currentIndex < mediaItems.size()) {
            return mediaItems.get(currentIndex);
        }
        return null;
    }

    /** Lấy toàn bộ danh sách MediaItem. */
    @NonNull
    public List<MediaItem> getMediaItems() { return mediaItems; }

    /** Lấy kích thước queue. */
    public int size() { return mediaItems.size(); }

    /** Kiểm tra queue có rỗng không. */
    public boolean isEmpty() { return mediaItems.isEmpty(); }

    /**
     * Xoá toàn bộ queue.
     */
    public void clear() {
        mediaItems.clear();
        shuffledIndices = null;
        currentIndex = -1;
    }
}
