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
import com.example.cuisine_finder.models.FoodPlace;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlacesByCategoryAdapter extends RecyclerView.Adapter<PlacesByCategoryAdapter.ViewHolder> {

    private List<FoodPlace> places = new ArrayList<>();
    private OnPlaceClickListener listener;

    public interface OnPlaceClickListener {
        void onPlaceClick(FoodPlace place);
    }

    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    public void setPlaces(List<FoodPlace> places) {
        this.places = places;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodPlace place = places.get(position);

        holder.tvName.setText(place.getName() != null ? place.getName() : "Quán ăn");

        String info = buildInfoText(place);
        holder.tvInfo.setText(info);

        String stats = buildStatsText(place);
        holder.tvStats.setText(stats);

        if (place.getImageUrls() != null && !place.getImageUrls().isEmpty()) {
            Glide.with(holder.ivImage.getContext())
                    .load(place.getImageUrls().get(0))
                    .placeholder(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.bg_image_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlaceClick(place);
        });
    }

    private String buildInfoText(FoodPlace place) {
        StringBuilder sb = new StringBuilder();
        if (place.getFoodType() != null && !place.getFoodType().isEmpty()) {
            sb.append(place.getFoodType());
        }
        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(place.getAddress());
        }
        return sb.length() > 0 ? sb.toString() : "Quán ăn";
    }

    private String buildStatsText(FoodPlace place) {
        String rating = String.format(Locale.getDefault(), "⭐ %.1f", place.getAverageRating());
        String reviews = place.getReviewCount() > 0
                ? " • " + place.getReviewCount() + " đánh giá"
                : "";
        String status = isCurrentlyOpen(place) ? " • Mở cửa" : " • Đóng cửa";
        return rating + reviews + status;
    }

    private boolean isCurrentlyOpen(FoodPlace place) {
        if (place.getOpenTime() == null || place.getCloseTime() == null) return true;
        try {
            java.util.Calendar now = java.util.Calendar.getInstance();
            int currentHour = now.get(java.util.Calendar.HOUR_OF_DAY);
            int currentMin = now.get(java.util.Calendar.MINUTE);
            int currentTotal = currentHour * 60 + currentMin;

            String[] openParts = place.getOpenTime().split(":");
            String[] closeParts = place.getCloseTime().split(":");
            int openTotal = Integer.parseInt(openParts[0]) * 60 + Integer.parseInt(openParts[1]);
            int closeTotal = Integer.parseInt(closeParts[0]) * 60 + Integer.parseInt(closeParts[1]);

            return currentTotal >= openTotal && currentTotal <= closeTotal;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvInfo, tvStats;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivPlaceImage);
            tvName = itemView.findViewById(R.id.tvPlaceName);
            tvInfo = itemView.findViewById(R.id.tvPlaceInfo);
            tvStats = itemView.findViewById(R.id.tvPlaceStats);
        }
    }
}
