package com.example.cuisine_finder.adapters;

import android.text.TextUtils;
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
import com.example.cuisine_finder.utils.SearchTextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExistingPlacePickerAdapter extends RecyclerView.Adapter<ExistingPlacePickerAdapter.PlaceViewHolder> {
    public interface OnPlaceClickListener {
        void onPlaceClick(FoodPlace place);
    }

    private final List<FoodPlace> allPlaces = new ArrayList<>();
    private final List<FoodPlace> visiblePlaces = new ArrayList<>();
    private final OnPlaceClickListener listener;

    public ExistingPlacePickerAdapter(List<FoodPlace> places, OnPlaceClickListener listener) {
        allPlaces.addAll(places);
        visiblePlaces.addAll(places);
        this.listener = listener;
    }

    public int filter(String query) {
        visiblePlaces.clear();
        for (FoodPlace place : allPlaces) {
            if (SearchTextUtils.matchesAllTerms(searchableText(place), query)) {
                visiblePlaces.add(place);
            }
        }
        notifyDataSetChanged();
        return visiblePlaces.size();
    }

    private String searchableText(FoodPlace place) {
        StringBuilder text = new StringBuilder();
        append(text, place.getName());
        append(text, place.getAddress());
        append(text, place.getFoodType());
        if (place.getTags() != null) {
            for (String tag : place.getTags()) append(text, tag);
        }
        return text.toString();
    }

    private void append(StringBuilder text, String value) {
        if (!TextUtils.isEmpty(value)) text.append(' ').append(value);
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_existing_place_picker, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        FoodPlace place = visiblePlaces.get(position);
        holder.name.setText(TextUtils.isEmpty(place.getName()) ? "Quán chưa đặt tên" : place.getName());
        holder.address.setText(TextUtils.isEmpty(place.getAddress()) ? "Chưa có địa chỉ" : place.getAddress());
        holder.rating.setText(String.format(Locale.getDefault(), "★ %.1f", place.getAverageRating()));
        holder.type.setText(TextUtils.isEmpty(place.getFoodType()) ? "Quán ăn" : place.getFoodType());

        String imageUrl = place.getImageUrls() == null || place.getImageUrls().isEmpty()
                ? null
                : place.getImageUrls().get(0);
        Glide.with(holder.image)
                .load(imageUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .error(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(holder.image);
        holder.itemView.setOnClickListener(view -> listener.onPlaceClick(place));
    }

    @Override
    public int getItemCount() {
        return visiblePlaces.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView address;
        final TextView rating;
        final TextView type;

        PlaceViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivPickerPlaceImage);
            name = itemView.findViewById(R.id.tvPickerPlaceName);
            address = itemView.findViewById(R.id.tvPickerPlaceAddress);
            rating = itemView.findViewById(R.id.tvPickerPlaceRating);
            type = itemView.findViewById(R.id.tvPickerPlaceType);
        }
    }
}
