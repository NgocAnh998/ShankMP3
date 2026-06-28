package com.example.nghenhac.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nghenhac.R;
import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.util.ImageLoader;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter cho danh sách playlist.
 *
 * Nguyên lý:
 * - Kế thừa ListAdapter với PlaylistEntity + PlaylistDiffUtil.
 * - Hiển thị playlist icon, tên playlist, số lượng bài hát.
 * - Click item để mở PlaylistDetailActivity, click more để edit/xoá.
 *
 * Input:
 * - List<PlaylistEntity> qua submitList().
 * - OnPlaylistClickListener cho sự kiện click.
 *
 * Output:
 * - View binding với item_playlist layout.
 * - Sự kiện click được forward ra ngoài.
 */
public class PlaylistAdapter extends ListAdapter<PlaylistEntity, PlaylistAdapter.PlaylistViewHolder> {

    /** Listener cho sự kiện click playlist. */
    public interface OnPlaylistClickListener {
        /** Click vào playlist — mở danh sách bài hát trong playlist. */
        void onPlaylistClick(@NonNull PlaylistEntity playlist, int position);
        /** Click vào nút "more" — menu edit/xoá playlist. */
        void onPlaylistMoreClick(@NonNull PlaylistEntity playlist, @NonNull View anchor);
    }

    // Map lưu album art URI cho mỗi playlist
    private final Map<Long, String> playlistArtMap = new HashMap<>();

    @Nullable
    private OnPlaylistClickListener clickListener;

    public PlaylistAdapter() {
        super(new PlaylistDiffUtil());
    }

    public void setOnPlaylistClickListener(@Nullable OnPlaylistClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Cập nhật ảnh bìa cho một playlist.
     *
     * Nguyên lý:
     * - Lưu URI ảnh bìa vào map, sau đó notify item đã thay đổi.
     * - RecyclerView tự động rebind ViewHolder với ảnh mới.
     *
     * Input:
     * @param playlistId  ID playlist.
     * @param albumArtUri URI ảnh bìa (có thể null).
     */
    public void setPlaylistArt(long playlistId, @Nullable String albumArtUri) {
        playlistArtMap.put(playlistId, albumArtUri);
        // Tìm vị trí của playlist trong danh sách và notify
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i).getId() == playlistId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener, position, playlistArtMap);
    }

    // ════════════════════════════════════════════
    //  ViewHolder
    // ════════════════════════════════════════════

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView playlistArt;
        private final TextView playlistName;
        private final TextView playlistSongCount;
        private final ImageButton moreButton;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            playlistArt = itemView.findViewById(R.id.playlist_art);
            playlistName = itemView.findViewById(R.id.playlist_name);
            playlistSongCount = itemView.findViewById(R.id.playlist_song_count);
            moreButton = itemView.findViewById(R.id.playlist_more);
        }

        /**
         * Gán dữ liệu từ PlaylistEntity vào các view.
         *
         * Input:
         * @param playlist  Entity playlist cần hiển thị.
         * @param listener  Listener xử lý click.
         * @param position  Vị trí item trong danh sách.
         */
        void bind(@NonNull PlaylistEntity playlist,
                  @Nullable OnPlaylistClickListener listener,
                  int position,
                  @NonNull Map<Long, String> artMap) {
            // Tên playlist
            playlistName.setText(playlist.getName());

            // Số bài hát
            String countText = String.format(Locale.getDefault(),
                    "%d bài hát", playlist.getSongCount());
            playlistSongCount.setText(countText);

            // Load ảnh bìa playlist (từ bài hát đầu tiên) nếu có
            String albumArtUri = artMap.get(playlist.getId());
            if (albumArtUri != null && !albumArtUri.isEmpty()) {
                ImageLoader.loadThumb(playlistArt.getContext(), albumArtUri, playlistArt);
            } else {
                playlistArt.setImageResource(R.drawable.ic_library);
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist, position);
                }
            });

            moreButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistMoreClick(playlist, v);
                }
            });
        }
    }

    // ════════════════════════════════════════════
    //  DiffUtil
    // ════════════════════════════════════════════

    /** DiffUtil.ItemCallback cho PlaylistEntity. */
    static class PlaylistDiffUtil extends DiffUtil.ItemCallback<PlaylistEntity> {

        @Override
        public boolean areItemsTheSame(@NonNull PlaylistEntity oldItem, @NonNull PlaylistEntity newItem) {
            // So sánh bằng ID (khoá chính)
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PlaylistEntity oldItem, @NonNull PlaylistEntity newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getSongCount() == newItem.getSongCount()
                    && oldItem.getDescription().equals(newItem.getDescription());
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull PlaylistEntity oldItem, @NonNull PlaylistEntity newItem) {
            // Cho phép RecyclerView cập nhật từng phần thay vì rebind toàn bộ
            return super.getChangePayload(oldItem, newItem);
        }
    }
}
