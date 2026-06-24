package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderItem;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder> {
    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final OnOrderClickListener listener;
    private List<Order> orders = new ArrayList<>();

    public AdminOrderAdapter(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_order, parent, false);
        return new AdminOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminOrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.tvUser.setText(order.getCustomerPhone() != null ? order.getCustomerPhone() : "Khach hang");
        holder.tvStatus.setText(order.getStatus() != null ? order.getStatus() : "UNKNOWN");
        holder.tvItemsSummary.setText(buildItemsSummary(order));
        holder.tvTime.setText(formatTime(order.getCreatedAt()));
        holder.tvTotal.setText(formatPrice(order.getTotalAmount()));
        holder.itemView.setOnClickListener(v -> listener.onOrderClick(order));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private String buildItemsSummary(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) return "Khong co mon";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < order.getItems().size(); i++) {
            OrderItem item = order.getItems().get(i);
            if (i > 0) builder.append(", ");
            builder.append(item.getQuantity()).append("x ");
            builder.append(item.getName() != null ? item.getName() : "Mon an");
        }
        return builder.toString();
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "Moi tao";
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
        return df.format(new java.util.Date(timestamp));
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format((long) price) + "d";
    }

    static class AdminOrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvStatus, tvItemsSummary, tvTime, tvTotal;

        AdminOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvAdminOrderUser);
            tvStatus = itemView.findViewById(R.id.tvAdminOrderStatus);
            tvItemsSummary = itemView.findViewById(R.id.tvAdminOrderItemsSummary);
            tvTime = itemView.findViewById(R.id.tvAdminOrderTime);
            tvTotal = itemView.findViewById(R.id.tvAdminOrderTotal);
        }
    }
}
