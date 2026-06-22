package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Favorite;
import java.util.ArrayList;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    private List<Favorite> favorites = new ArrayList<>();

    public interface OnItemClickListener {
        void onItemClick(Favorite favorite);
    }

    public interface OnRemoveListener {
        void onRemove(Favorite favorite);
    }

    private OnItemClickListener clickListener;
    private OnRemoveListener removeListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.removeListener = listener;
    }

    public void setFavorites(List<Favorite> favorites) {
        this.favorites = new ArrayList<>(favorites);
        notifyDataSetChanged();
    }

    public void removeItem(Favorite favorite) {
        int index = favorites.indexOf(favorite);
        if (index >= 0) {
            favorites.remove(index);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, favorites.size());
        }
    }

    public List<Favorite> getFavorites() {
        return favorites;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_place, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Favorite favorite = favorites.get(position);
        holder.bind(favorite, clickListener, removeListener);
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvFoodType, tvAddress, tvSavedTime, btnRemove;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivFavoriteImage);
            tvName = itemView.findViewById(R.id.tvFavoriteName);
            tvFoodType = itemView.findViewById(R.id.tvFavoriteFoodType);
            tvAddress = itemView.findViewById(R.id.tvFavoriteAddress);
            tvSavedTime = itemView.findViewById(R.id.tvFavoriteSavedTime);
            btnRemove = itemView.findViewById(R.id.btnRemoveFavorite);
        }

        void bind(Favorite favorite, OnItemClickListener clickListener, OnRemoveListener removeListener) {
            tvName.setText(favorite.getPlaceName() != null ? favorite.getPlaceName() : "Quán ăn");

            // Food type badge
            if (favorite.getFoodType() != null && !favorite.getFoodType().isEmpty()) {
                tvFoodType.setText(favorite.getFoodType());
                tvFoodType.setVisibility(View.VISIBLE);
            } else {
                tvFoodType.setVisibility(View.GONE);
            }

            // Address with pin icon
            if (favorite.getPlaceAddress() != null && !favorite.getPlaceAddress().isEmpty()) {
                tvAddress.setText("📍 " + favorite.getPlaceAddress());
                tvAddress.setVisibility(View.VISIBLE);
            } else {
                tvAddress.setVisibility(View.GONE);
            }

            // Relative save time
            tvSavedTime.setText(getRelativeTime(favorite.getCreatedAt()));

            // Place image via Glide
            if (favorite.getPlaceImageUrl() != null && !favorite.getPlaceImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(favorite.getPlaceImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(ivImage);
            } else {
                ivImage.setImageResource(R.drawable.bg_image_placeholder);
            }

            // Card click → view detail
            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onItemClick(favorite);
            });

            // Remove button
            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onRemove(favorite);
            });
        }

        private String getRelativeTime(long createdAt) {
            if (createdAt == 0) return "Đã lưu";
            long diffMs = System.currentTimeMillis() - createdAt;
            long days = diffMs / (1000L * 60 * 60 * 24);
            if (days == 0) return "Lưu hôm nay";
            if (days == 1) return "Lưu hôm qua";
            if (days < 30) return "Lưu " + days + " ngày trước";
            long months = days / 30;
            if (months < 12) return "Lưu " + months + " tháng trước";
            return "Lưu " + (months / 12) + " năm trước";
        }
    }
}
