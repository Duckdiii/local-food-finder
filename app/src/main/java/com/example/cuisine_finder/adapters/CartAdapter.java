package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.CartItem;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    public interface CartItemActionListener {
        void onIncrease(CartItem item);
        void onDecrease(CartItem item);
        void onRemove(CartItem item);
    }

    private final CartItemActionListener listener;
    private List<CartItem> items = new ArrayList<>();

    public CartAdapter(CartItemActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvName.setText(item.getName() != null ? item.getName() : "Mon an");
        holder.tvPrice.setText(formatPrice(item.getSubtotal()));
        holder.btnIncrease.setOnClickListener(v -> listener.onIncrease(item));
        holder.btnDecrease.setOnClickListener(v -> listener.onDecrease(item));
        holder.btnRemove.setOnClickListener(v -> listener.onRemove(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format((long) price) + "d";
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView btnDecrease, tvQuantity, btnIncrease, tvName, tvPrice;
        ImageView btnRemove;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            btnDecrease = itemView.findViewById(R.id.btnDecreaseCartItem);
            tvQuantity = itemView.findViewById(R.id.tvCartItemQuantity);
            btnIncrease = itemView.findViewById(R.id.btnIncreaseCartItem);
            tvName = itemView.findViewById(R.id.tvCartItemName);
            tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
            btnRemove = itemView.findViewById(R.id.btnRemoveCartItem);
        }
    }
}
