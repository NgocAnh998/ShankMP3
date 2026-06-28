package com.example.nghenhac.ui.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.example.nghenhac.data.local.entity.SongEntity;

/**
 * DiffUtil.ItemCallback cho RecyclerView adapter của danh sách bài hát.
 *
 * Nguyên lý:
 * - DiffUtil tính toán sự khác biệt giữa 2 danh sách (cũ và mới) để chỉ cập nhật item thay đổi.
 * - Tránh gọi notifyDataSetChanged() — vốn làm refresh toàn bộ danh sách, gây giật lag.
 * - areItemsTheSame(): so sánh id — xác định 2 item có cùng bài hát không.
 * - areContentsTheSame(): so sánh title, artist, album, isFavorite, duration.
 * - getChangePayload(): trả về payload cho biết trường nào thay đổi để cập nhật từng phần.
 *
 * Input:
 * - oldItem, newItem: SongEntity cũ và mới.
 *
 * Output:
 * - true/false cho DiffUtil quyết định có cập nhật item hay không.
 */
public class SongDiffUtil extends DiffUtil.ItemCallback<SongEntity> {

    @Override
    public boolean areItemsTheSame(@NonNull SongEntity oldItem, @NonNull SongEntity newItem) {
        // So sánh ID — đây là định danh duy nhất của bài hát
        return oldItem.getId() == newItem.getId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull SongEntity oldItem, @NonNull SongEntity newItem) {
        // So sánh các trường có thể thay đổi (title, artist, favorite, album, duration)
        return oldItem.getTitle().equals(newItem.getTitle())
                && oldItem.getArtist().equals(newItem.getArtist())
                && oldItem.isFavorite() == newItem.isFavorite()
                && oldItem.getAlbum().equals(newItem.getAlbum())
                && oldItem.getDuration() == newItem.getDuration();
    }

    @Nullable
    @Override
    public Object getChangePayload(@NonNull SongEntity oldItem, @NonNull SongEntity newItem) {
        // Trả về payload để cập nhật từng phần (không cần rebind toàn bộ)
        // Nếu chỉ có isFavorite thay đổi → trả về "favorite"
        if (oldItem.isFavorite() != newItem.isFavorite()) {
            return "favorite";
        }
        // Các trường khác thay đổi → rebind toàn bộ
        return null;
    }
}
