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
import com.example.cuisine_finder.models.FoodItem;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    private List<FoodItem> items = new ArrayList<>();

    public void setItems(List<FoodItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu_item, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        FoodItem item = items.get(position);

        holder.tvName.setText(item.getName() != null ? item.getName() : "Món ăn");

        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            holder.tvDescription.setText(item.getDescription());
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        if (item.getAverageRating() > 0) {
            holder.tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f", item.getAverageRating()));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }

        if (item.getCategoryName() != null && !item.getCategoryName().isEmpty()) {
            holder.tvCategory.setText(item.getCategoryName());
            holder.tvCategory.setVisibility(View.VISIBLE);
        } else {
            holder.tvCategory.setVisibility(View.GONE);
        }

        if (item.getPrice() > 0) {
            holder.tvPrice.setText(formatPrice(item.getPrice()));
        } else {
            holder.tvPrice.setText("Liên hệ");
        }

        String imageUrl = (item.getImageUrls() != null && !item.getImageUrls().isEmpty())
                ? item.getImageUrls().get(0) : null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.ivImage.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.bg_image_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format((long) price) + "đ";
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvDescription, tvRating, tvCategory, tvPrice;

        MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivMenuItemImage);
            tvName = itemView.findViewById(R.id.tvMenuItemName);
            tvDescription = itemView.findViewById(R.id.tvMenuItemDescription);
            tvRating = itemView.findViewById(R.id.tvMenuItemRating);
            tvCategory = itemView.findViewById(R.id.tvMenuItemCategory);
            tvPrice = itemView.findViewById(R.id.tvMenuItemPrice);
        }
    }
}
