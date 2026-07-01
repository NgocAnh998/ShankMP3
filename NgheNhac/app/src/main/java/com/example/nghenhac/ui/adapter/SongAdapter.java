package com.example.nghenhac.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.entity.SongEntity;
import com.example.nghenhac.data.repository.SongRepository;
import com.example.nghenhac.util.ImageLoader;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import java.util.Locale;

/**
 * RecyclerView.Adapter cho danh sách bài hát.
 *
 * Nguyên lý:
 * - Kế thừa ListAdapter (có sẵn DiffUtil) thay vì RecyclerView.Adapter — tự động tính toán
 *   sự khác biệt khi submitList(), chỉ cập nhật item thay đổi.
 * - ViewHolder pattern: giữ references đến các view để tránh findViewById() liên tục.
 * - Click listeners: onItemClick (mở PlayerActivity) + onMoreClick (menu context).
 * - Hỗ trợ: favorite button, duration hiển thị, now playing indicator.
 *
 * Luồng xử lý:
 * 1. Activity/Fragment tạo adapter → set adapter lên RecyclerView.
 * 2. Dữ liệu thay đổi → adapter.submitList(newList) → DiffUtil tự động cập nhật.
 * 3. User click item → onItemClickListener.onItemClick(song).
 * 4. User click "more" button → onItemClickListener.onMoreClick(song, view).
 * 5. User click favorite → gọi SongRepository.updateFavorite() và cập nhật icon.
 * 6. setNowPlayingId() được gọi từ fragment để highlight bài đang phát.
 *
 * Input:
 * - List<SongEntity> qua submitList().
 * - OnSongClickListener để xử lý sự kiện click.
 *
 * Output:
 * - View binding với item_song layout.
 * - Sự kiện click được forward ra ngoài qua listener.
 */
public class SongAdapter extends ListAdapter<SongEntity, SongAdapter.SongViewHolder> {

    /** Listener cho sự kiện click trên item. */
    public interface OnSongClickListener {
        /**
         * Click vào bài hát — thường mở PlayerActivity và phát nhạc.
         *
         * @param song     Bài hát được click.
         * @param position Vị trí trong danh sách.
         * @param itemView View của item (itemView) — dùng cho shared element transition.
         */
        void onItemClick(@NonNull SongEntity song, int position, @NonNull View itemView);
        /** Click vào nút "more" — mở menu context (thêm vào playlist, yêu thích...). */
        void onMoreClick(@NonNull SongEntity song, @NonNull View anchor);
        /** Click vào nút favorite — toggle yêu thích. */
        void onFavoriteClick(@NonNull SongEntity song, boolean isFavorite, int position);
    }

    @Nullable
    private OnSongClickListener clickListener;
    private long nowPlayingId = -1;

    public SongAdapter() {
        super(new SongDiffUtil());
    }

    /**
     * Set listener xử lý sự kiện click.
     *
     * @param listener Listener (có thể null để huỷ).
     */
    public void setOnSongClickListener(@Nullable OnSongClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Cập nhật ID bài hát đang phát để hiển thị now playing indicator.
     *
     * Nguyên lý:
     * - Gọi từ fragment/activity khi bài hát thay đổi (MusicPlayer onMediaItemTransition).
     * - So sánh với ID cũ, chỉ notify các item thay đổi để tránh refresh toàn bộ.
     *
     * Input:
     * @param songId ID bài hát đang phát, -1 nếu không có.
     */
    public void setNowPlayingId(long songId) {
        long oldId = this.nowPlayingId;
        this.nowPlayingId = songId;

        // Notify item cũ và mới để cập nhật indicator
        if (oldId != -1) {
            int oldPos = findPositionById(oldId);
            if (oldPos != -1) notifyItemChanged(oldPos, "now_playing");
        }
        if (songId != -1) {
            int newPos = findPositionById(songId);
            if (newPos != -1) notifyItemChanged(newPos, "now_playing");
        }
    }

    /**
     * Lấy ID bài hát đang phát hiện tại.
     */
    public long getNowPlayingId() {
        return nowPlayingId;
    }

    /**
     * Tìm vị trí của bài hát theo ID trong danh sách hiện tại.
     *
     * Nguyên lý:
     * - Duyệt getCurrentList() để tìm index.
     * - Trả về -1 nếu không tìm thấy.
     *
     * Input:
     * @param songId ID bài hát cần tìm.
     *
     * Output:
     * @return Vị trí trong danh sách, -1 nếu không có.
     */
    private int findPositionById(long songId) {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i).getId() == songId) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        SongEntity song = getItem(position);
        holder.bind(song, clickListener, position, nowPlayingId);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        SongEntity song = getItem(position);
        for (Object payload : payloads) {
            if ("now_playing".equals(payload)) {
                // Chỉ cập nhật now playing indicator, không rebind toàn bộ
                holder.updateNowPlayingIndicator(song.getId() == nowPlayingId);
            } else if ("favorite".equals(payload)) {
                // Chỉ cập nhật favorite icon
                holder.updateFavoriteIcon(song.isFavorite());
            }
        }
    }

    // ════════════════════════════════════════════
    //  ViewHolder
    // ════════════════════════════════════════════

    /**
     * ViewHolder cho item_song layout.
     *
     * Giữ references đến các view: album art thumbnail, title, artist, duration,
     * favorite button, more button, now playing indicator.
     * Load album art qua Glide (placeholder nếu chưa có).
     */
    static class SongViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardView;
        private final ShapeableImageView albumArt;
        private final TextView songTitle;
        private final TextView songArtist;
        private final TextView songDuration;
        private final ImageButton favoriteButton;
        private final ImageButton moreButton;
        private final View nowPlayingDot;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.song_card);
            albumArt = itemView.findViewById(R.id.song_art);
            songTitle = itemView.findViewById(R.id.song_title);
            songArtist = itemView.findViewById(R.id.song_artist);
            songDuration = itemView.findViewById(R.id.song_duration);
            favoriteButton = itemView.findViewById(R.id.song_favorite);
            moreButton = itemView.findViewById(R.id.song_more);
            nowPlayingDot = itemView.findViewById(R.id.song_now_playing_dot);
        }

        /**
         * Gán dữ liệu từ SongEntity vào các view.
         *
         * Nguyên lý:
         * - Set title, artist, duration text.
         * - Load album art via ImageLoader.
         * - Set favorite icon (filled/outline) dựa trên isFavorite.
         * - Hiển thị now playing dot nếu bài này đang phát.
         * - Gắn click listeners cho item, favorite, more button.
         *
         * Input:
         * @param song          Entity bài hát cần hiển thị.
         * @param listener      Listener xử lý click (có thể null).
         * @param position      Vị trí của item trong danh sách.
         * @param nowPlayingId  ID bài hát đang phát (-1 nếu không có).
         */
        void bind(@NonNull SongEntity song,
                  @Nullable OnSongClickListener listener,
                  int position,
                  long nowPlayingId) {
            // Title & Artist
            songTitle.setText(song.getTitle());
            String artistText = song.getArtist() != null && !song.getArtist().isEmpty()
                    ? song.getArtist()
                    : songTitle.getContext().getString(R.string.unknown_artist);
            songArtist.setText(artistText);

            // Duration
            songDuration.setText(formatDuration(song.getDuration()));

            // Album art — load thumbnail qua ImageLoader
            ImageLoader.loadThumb(albumArt.getContext(), song.getAlbumArtUri(), albumArt);

            // Favorite icon
            updateFavoriteIcon(song.isFavorite());

            // Now playing indicator
            updateNowPlayingIndicator(song.getId() == nowPlayingId);

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(song, position, itemView);
                }
            });

            favoriteButton.setOnClickListener(v -> {
                boolean newState = !song.isFavorite();
                song.setFavorite(newState);
                updateFavoriteIcon(newState);
                if (listener != null) {
                    listener.onFavoriteClick(song, newState, position);
                }
            });

            moreButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMoreClick(song, v);
                }
            });
        }

        /**
         * Cập nhật favorite icon dựa trên trạng thái yêu thích.
         *
         * Nguyên lý:
         * - isFavorite = true → hiển thị icon filled (ic_favorite), tint primary.
         * - isFavorite = false → hiển thị icon border (ic_favorite_border), tint onSurfaceVariant.
         *
         * Input:
         * @param isFavorite true nếu bài hát được yêu thích.
         */
        void updateFavoriteIcon(boolean isFavorite) {
            if (isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_favorite);
                favoriteButton.setColorFilter(
                        favoriteButton.getContext().getColor(R.color.primary));
            } else {
                favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                favoriteButton.setColorFilter(
                        favoriteButton.getContext().getColor(R.color.on_surface_variant));
            }
        }

        /**
         * Cập nhật now playing indicator.
         *
         * Nguyên lý:
         * - isPlaying = true → hiển thị chấm xanh bên trái + highlight card background.
         * - isPlaying = false → ẩn chấm, restore background.
         *
         * Input:
         * @param isPlaying true nếu bài này đang được phát.
         */
        void updateNowPlayingIndicator(boolean isPlaying) {
            if (isPlaying) {
                nowPlayingDot.setVisibility(View.VISIBLE);
                cardView.setCardBackgroundColor(
                        cardView.getContext().getColor(R.color.primary_container));
                songTitle.setTextColor(
                        cardView.getContext().getColor(R.color.on_primary_container));
            } else {
                nowPlayingDot.setVisibility(View.GONE);
                cardView.setCardBackgroundColor(
                        cardView.getContext().getColor(R.color.surface));
                songTitle.setTextColor(
                        cardView.getContext().getColor(R.color.on_surface));
            }
        }
    }

    // ════════════════════════════════════════════
    //  Utility
    // ════════════════════════════════════════════

    /**
     * Định dạng thời lượng từ milliseconds sang mm:ss hoặc h:mm:ss.
     *
     * Nguyên lý:
     * - Hỗ trợ bài hát dài > 1 tiếng: hiển thị "1:23:45" thay vì "83:45".
     * - ms <= 0: hiển thị "--:--" (chưa có thông tin thời lượng).
     *
     * Input:
     * @param ms Thời lượng (milliseconds).
     *
     * Output:
     * @return Chuỗi định dạng h:mm:ss, mm:ss, hoặc "--:--".
     */
    public static String formatDuration(long ms) {
        if (ms <= 0) return "--:--";
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }
}
