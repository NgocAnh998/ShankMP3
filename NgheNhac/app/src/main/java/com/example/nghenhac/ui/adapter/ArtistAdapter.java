package com.example.nghenhac.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nghenhac.R;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * RecyclerView adapter cho danh sách nghệ sĩ.
 * <p>
 * Nguyên lý:
 * - Hiển thị tên nghệ sĩ và số lượng bài hát.
 * - ListAdapter với ArtistDiffUtil cho hiệu suất tốt.
 * - Click nghệ sĩ → phát tất cả bài hát của nghệ sĩ đó.
 */
public class ArtistAdapter extends ListAdapter<ArtistAdapter.ArtistItem, ArtistAdapter.ArtistViewHolder> {

    /** Model cho một nghệ sĩ. */
    public static class ArtistItem {
        @NonNull
        private final String name;
        private final int songCount;

        public ArtistItem(@NonNull String name, int songCount) {
            this.name = name;
            this.songCount = songCount;
        }

        @NonNull
        public String getName() { return name; }
        public int getSongCount() { return songCount; }
    }

    /** Listener cho sự kiện click nghệ sĩ. */
    public interface OnArtistClickListener {
        void onArtistClick(@NonNull ArtistItem artist, int position);
    }

    @Nullable
    private OnArtistClickListener clickListener;

    public ArtistAdapter() {
        super(new ArtistDiffUtil());
    }

    public void setOnArtistClickListener(@Nullable OnArtistClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener, position);
    }

    // ════════════════════════════════════════════
    //  ViewHolder
    // ════════════════════════════════════════════

    static class ArtistViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView avatar;
        private final TextView nameView;
        private final TextView songCountView;

        ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.artist_avatar);
            nameView = itemView.findViewById(R.id.artist_name);
            songCountView = itemView.findViewById(R.id.artist_song_count);
        }

        void bind(@NonNull ArtistItem artist,
                  @Nullable OnArtistClickListener listener,
                  int position) {
            nameView.setText(artist.getName());

            // Format song count
            String countText = songCountView.getContext()
                    .getString(R.string.playlist_song_count_format, artist.getSongCount());
            songCountView.setText(countText);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onArtistClick(artist, position);
                }
            });
        }
    }

    // ════════════════════════════════════════════
    //  DiffUtil
    // ════════════════════════════════════════════

    static class ArtistDiffUtil extends DiffUtil.ItemCallback<ArtistItem> {
        @Override
        public boolean areItemsTheSame(@NonNull ArtistItem oldItem, @NonNull ArtistItem newItem) {
            return oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ArtistItem oldItem, @NonNull ArtistItem newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getSongCount() == newItem.getSongCount();
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull ArtistItem oldItem, @NonNull ArtistItem newItem) {
            // Cho phép RecyclerView cập nhật từng phần thay vì rebind toàn bộ
            return super.getChangePayload(oldItem, newItem);
        }
    }
}
