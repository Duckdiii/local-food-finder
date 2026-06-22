package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.OrderItemAdapter;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderStatus;
import com.example.cuisine_finder.utils.InsetUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ActiveOrderActivity extends AppCompatActivity {
    public static final String EXTRA_ORDER_ID = "orderId";

    private TextView tvOrderStatus, tvOrderRestaurantName, tvOrderTotalPrice;
    private View indicatorPending, indicatorPreparing, indicatorDelivering;
    private OrderItemAdapter orderItemAdapter;
    private ListenerRegistration orderListener;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_order);
        InsetUtils.applySystemBars(findViewById(R.id.rootActiveOrder), true, true);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        initViews();
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Khong tim thay ma don hang", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        observeOrder(orderId);
    }

    @Override
    protected void onDestroy() {
        if (orderListener != null) {
            orderListener.remove();
        }
        super.onDestroy();
    }

    private void initViews() {
        tvOrderStatus = findViewById(R.id.tvOrderStatus);
        tvOrderRestaurantName = findViewById(R.id.tvOrderRestaurantName);
        tvOrderTotalPrice = findViewById(R.id.tvOrderTotalPrice);
        indicatorPending = findViewById(R.id.indicatorPending);
        indicatorPreparing = findViewById(R.id.indicatorPreparing);
        indicatorDelivering = findViewById(R.id.indicatorDelivering);

        RecyclerView rvOrderItems = findViewById(R.id.rvOrderItems);
        orderItemAdapter = new OrderItemAdapter();
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        rvOrderItems.setAdapter(orderItemAdapter);
    }

    private void observeOrder(String orderId) {
        orderListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .document(orderId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Khong tai duoc don hang", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "Don hang khong ton tai", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Order order = snapshot.toObject(Order.class);
                    if (order == null) return;
                    order.setId(snapshot.getId());
                    bindOrder(order);
                });
    }

    private void bindOrder(Order order) {
        tvOrderStatus.setText(getStatusLabel(order.getStatus()));
        tvOrderRestaurantName.setText(order.getRestaurantName() != null ? order.getRestaurantName() : "Quan an");
        tvOrderTotalPrice.setText(formatPrice(order.getTotalAmount()));
        orderItemAdapter.setItems(order.getItems());
        updateProgress(order.getStatus());
    }

    private void updateProgress(String status) {
        int active = getColor(R.color.orange_main);
        int inactive = getColor(R.color.border_light);

        indicatorPending.setBackgroundColor(active);
        indicatorPreparing.setBackgroundColor(isPreparingOrLater(status) ? active : inactive);
        indicatorDelivering.setBackgroundColor(isDeliveringOrLater(status) ? active : inactive);
    }

    private boolean isPreparingOrLater(String status) {
        return OrderStatus.MERCHANT_ACCEPTED.equals(status)
                || OrderStatus.PREPARING.equals(status)
                || OrderStatus.READY_FOR_PICKUP.equals(status)
                || isDeliveringOrLater(status);
    }

    private boolean isDeliveringOrLater(String status) {
        return OrderStatus.SHIPPER_ACCEPTED.equals(status)
                || OrderStatus.PICKED_UP.equals(status)
                || OrderStatus.SHIPPING.equals(status)
                || OrderStatus.DELIVERED.equals(status)
                || OrderStatus.DELIVERY_FAILED.equals(status);
    }

    private String getStatusLabel(String status) {
        if (OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(status)) return "Cho quan nhan don";
        if (OrderStatus.MERCHANT_ACCEPTED.equals(status)) return "Quan da nhan don";
        if (OrderStatus.PREPARING.equals(status)) return "Quan dang lam mon";
        if (OrderStatus.READY_FOR_PICKUP.equals(status)) return "Mon da san sang";
        if (OrderStatus.SHIPPER_ACCEPTED.equals(status)) return "Shipper da nhan don";
        if (OrderStatus.PICKED_UP.equals(status)) return "Shipper da lay mon";
        if (OrderStatus.SHIPPING.equals(status)) return "Dang giao hang";
        if (OrderStatus.DELIVERED.equals(status)) return "Giao hang thanh cong";
        if (OrderStatus.DELIVERY_FAILED.equals(status)) return "Giao hang that bai";
        if (OrderStatus.CANCELLED_BY_CUSTOMER.equals(status)) return "Don da huy";
        if (OrderStatus.REJECTED_BY_MERCHANT.equals(status)) return "Quan tu choi don";
        return "Dang cap nhat";
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format((long) price) + "d";
    }
}
