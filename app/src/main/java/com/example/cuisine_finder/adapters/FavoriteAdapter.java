package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Favorite;
import java.util.ArrayList;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    private List<Favorite> favorites = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Favorite favorite);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setFavorites(List<Favorite> favorites) {
        this.favorites = favorites;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_restaurant, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Favorite favorite = favorites.get(position);
        holder.bind(favorite, listener);
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlaceImage;
        TextView tvName, tvInfo, tvStats;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlaceImage = itemView.findViewById(R.id.ivPlaceImage);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvInfo = itemView.findViewById(R.id.tvPlaceInfo);
            tvStats = itemView.findViewById(R.id.tvPlaceStats);
        }

        public void bind(Favorite favorite, OnItemClickListener listener) {
            tvName.setText(favorite.getPlaceName());
            tvInfo.setText(favorite.getFoodType() != null ? favorite.getFoodType() : "Quán ăn");
            tvStats.setText("Đã lưu vào yêu thích");

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(favorite);
                }
            });
        }
    }
}
