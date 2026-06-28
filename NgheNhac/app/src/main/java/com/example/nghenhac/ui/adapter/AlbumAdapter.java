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
import com.example.nghenhac.util.ImageLoader;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * RecyclerView adapter cho danh sách album dạng Grid.
 *
 * Nguyên lý:
 * - Kế thừa ListAdapter với AlbumItem model + AlbumDiffUtil.
 * - Hiển thị album art, tên album, nghệ sĩ trong MaterialCardView.
 * - Dùng với GridLayoutManager (spanCount = 2).
 *
 * Input:
 * - List<AlbumItem> qua submitList().
 * - OnAlbumClickListener cho sự kiện click.
 *
 * Output:
 * - View binding với item_album layout.
 * - Sự kiện click được forward ra ngoài.
 */
public class AlbumAdapter extends ListAdapter<AlbumAdapter.AlbumItem, AlbumAdapter.AlbumViewHolder> {

    /** Model cho một album — chứa thông tin hiển thị trong Grid. */
    public static class AlbumItem {
        @NonNull
        private final String albumName;
        @NonNull
        private final String artist;
        @Nullable
        private final String albumArtUri;
        private final int songCount;

        public AlbumItem(@NonNull String albumName, @NonNull String artist,
                         @Nullable String albumArtUri, int songCount) {
            this.albumName = albumName;
            this.artist = artist;
            this.albumArtUri = albumArtUri;
            this.songCount = songCount;
        }

        @NonNull
        public String getAlbumName() { return albumName; }
        @NonNull
        public String getArtist() { return artist; }
        @Nullable
        public String getAlbumArtUri() { return albumArtUri; }
        public int getSongCount() { return songCount; }
    }

    /** Listener cho sự kiện click album. */
    public interface OnAlbumClickListener {
        void onAlbumClick(@NonNull AlbumItem album, int position);
    }

    @Nullable
    private OnAlbumClickListener clickListener;

    public AlbumAdapter() {
        super(new AlbumDiffUtil());
    }

    public void setOnAlbumClickListener(@Nullable OnAlbumClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener, position);
    }

    // ════════════════════════════════════════════
    //  ViewHolder
    // ════════════════════════════════════════════

    static class AlbumViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView albumArt;
        private final TextView albumTitle;
        private final TextView albumArtist;

        AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.album_art);
            albumTitle = itemView.findViewById(R.id.album_title);
            albumArtist = itemView.findViewById(R.id.album_artist);
        }

        void bind(@NonNull AlbumItem album,
                  @Nullable OnAlbumClickListener listener,
                  int position) {
            albumTitle.setText(album.getAlbumName());
            albumArtist.setText(album.getArtist());

            // Album art — load qua ImageLoader
            ImageLoader.load(albumArt.getContext(), album.getAlbumArtUri(), albumArt);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlbumClick(album, position);
                }
            });
        }
    }

    // ════════════════════════════════════════════
    //  DiffUtil
    // ════════════════════════════════════════════

    /** DiffUtil.ItemCallback cho AlbumItem. */
    static class AlbumDiffUtil extends DiffUtil.ItemCallback<AlbumItem> {

        @Override
        public boolean areItemsTheSame(@NonNull AlbumItem oldItem, @NonNull AlbumItem newItem) {
            // So sánh bằng tên album + nghệ sĩ (tránh trùng tên album)
            return oldItem.getAlbumName().equals(newItem.getAlbumName())
                    && oldItem.getArtist().equals(newItem.getArtist());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AlbumItem oldItem, @NonNull AlbumItem newItem) {
            return oldItem.getAlbumName().equals(newItem.getAlbumName())
                    && oldItem.getArtist().equals(newItem.getArtist())
                    && oldItem.getSongCount() == newItem.getSongCount();
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull AlbumItem oldItem, @NonNull AlbumItem newItem) {
            // Trả về payload khác null để RecyclerView chỉ cập nhật view thay đổi
            // thay vì rebind toàn bộ. Mặc định null = rebind cả view.
            return super.getChangePayload(oldItem, newItem);
        }
    }
}
