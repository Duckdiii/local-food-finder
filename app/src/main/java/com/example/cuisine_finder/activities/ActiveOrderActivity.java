package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.OrderItemAdapter;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderStatus;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.OrderRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.utils.InsetUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ActiveOrderActivity extends AppCompatActivity {
    public static final String EXTRA_ORDER_ID = "orderId";

    private TextView tvOrderStatus, tvOrderRestaurantName, tvOrderTotalPrice, btnCancelOrder, tvActiveOrderId;
    private View indicatorSubmitted, indicatorAccepted, indicatorPreparing, indicatorReady, indicatorDelivering;
    private OrderItemAdapter orderItemAdapter;
    private ListenerRegistration orderListener;
    private String orderId;
    private Order currentOrder;
    private boolean cancelling;

    private final OrderRepository orderRepository = new OrderRepository();
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_order);
        InsetUtils.applySystemBars(findViewById(R.id.rootActiveOrder), true, true);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        initViews();
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
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
        tvActiveOrderId = findViewById(R.id.tvActiveOrderId);
        indicatorSubmitted = findViewById(R.id.indicatorSubmitted);
        indicatorAccepted = findViewById(R.id.indicatorAccepted);
        indicatorPreparing = findViewById(R.id.indicatorPreparing);
        indicatorReady = findViewById(R.id.indicatorReady);
        indicatorDelivering = findViewById(R.id.indicatorDelivering);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        btnCancelOrder.setOnClickListener(v -> confirmCancelOrder());

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
                        Toast.makeText(this, "Không tải được đơn hàng", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "Đơn hàng không tồn tại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Order order = snapshot.toObject(Order.class);
                    if (order == null) return;
                    order.setId(snapshot.getId());
                    bindOrder(order);
                });
    }

    private void bindOrder(Order order) {
        if (orderId != null) {
            tvActiveOrderId.setText("Mã đơn: #" + orderId);
        }
        currentOrder = order;
        tvOrderStatus.setText(getStatusLabel(order.getStatus()));
        tvOrderRestaurantName.setText(order.getRestaurantName() != null ? order.getRestaurantName() : "Quán ăn");
        tvOrderTotalPrice.setText(formatPrice(order.getTotalAmount()));
        orderItemAdapter.setItems(order.getItems());
        updateProgress(order.getStatus());

        boolean cancellable = OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(order.getStatus());
        btnCancelOrder.setVisibility(cancellable ? View.VISIBLE : View.GONE);
        btnCancelOrder.setEnabled(!cancelling);
    }

    private void confirmCancelOrder() {
        if (cancelling || currentOrder == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> cancelOrder())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void cancelOrder() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        cancelling = true;
        btnCancelOrder.setEnabled(false);
        userRepository.getUser(currentUserId).addOnSuccessListener(documentSnapshot -> {
            User actor = documentSnapshot.toObject(User.class);
            if (actor == null) {
                actor = new User();
            }
            actor.setId(currentUserId);
            orderRepository.updateStatus(orderId, actor, OrderStatus.CANCELLED_BY_CUSTOMER, "Khách hàng đã hủy đơn")
                    .addOnSuccessListener(aVoid -> {
                        cancelling = false;
                        Toast.makeText(this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        cancelling = false;
                        btnCancelOrder.setEnabled(true);
                        Toast.makeText(this, "Không hủy được đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            cancelling = false;
            btnCancelOrder.setEnabled(true);
            Toast.makeText(this, "Không thể xác thực người dùng", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateProgress(String status) {
        int active = getColor(R.color.orange_main);
        int inactive = getColor(R.color.border_light);

        if (OrderStatus.CANCELLED_BY_CUSTOMER.equals(status) || OrderStatus.REJECTED_BY_MERCHANT.equals(status)) {
            indicatorSubmitted.setBackgroundColor(inactive);
            indicatorAccepted.setBackgroundColor(inactive);
            indicatorPreparing.setBackgroundColor(inactive);
            indicatorReady.setBackgroundColor(inactive);
            indicatorDelivering.setBackgroundColor(inactive);
            return;
        }

        indicatorSubmitted.setBackgroundColor(active);
        indicatorAccepted.setBackgroundColor(isAcceptedOrLater(status) ? active : inactive);
        indicatorPreparing.setBackgroundColor(isPreparingOrLater(status) ? active : inactive);
        indicatorReady.setBackgroundColor(isReadyOrLater(status) ? active : inactive);
        indicatorDelivering.setBackgroundColor(isDeliveringOrLater(status) ? active : inactive);
    }

    private boolean isAcceptedOrLater(String status) {
        return !OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(status);
    }

    private boolean isPreparingOrLater(String status) {
        return isAcceptedOrLater(status) && !OrderStatus.MERCHANT_ACCEPTED.equals(status);
    }

    private boolean isReadyOrLater(String status) {
        return isPreparingOrLater(status) && !OrderStatus.PREPARING.equals(status);
    }

    private boolean isDeliveringOrLater(String status) {
        return OrderStatus.PICKED_UP.equals(status)
                || OrderStatus.SHIPPING.equals(status)
                || OrderStatus.DELIVERED.equals(status)
                || OrderStatus.DELIVERY_FAILED.equals(status);
    }

    private String getStatusLabel(String status) {
        if (OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(status)) return "Chờ quán nhận đơn";
        if (OrderStatus.MERCHANT_ACCEPTED.equals(status)) return "Quán đã nhận đơn";
        if (OrderStatus.PREPARING.equals(status)) return "Quán đang làm món";
        if (OrderStatus.READY_FOR_PICKUP.equals(status)) return "Quán sẵn sàng giao";
        if (OrderStatus.SHIPPER_ACCEPTED.equals(status)) return "Quán đã nhận giao";
        if (OrderStatus.PICKED_UP.equals(status)) return "Quán đang đưa đơn đi giao";
        if (OrderStatus.SHIPPING.equals(status)) return "Quán đang giao hàng";
        if (OrderStatus.DELIVERED.equals(status)) return "Giao hàng thành công";
        if (OrderStatus.DELIVERY_FAILED.equals(status)) return "Giao hàng thất bại";
        if (OrderStatus.CANCELLED_BY_CUSTOMER.equals(status)) return "Đơn đã hủy";
        if (OrderStatus.REJECTED_BY_MERCHANT.equals(status)) return "Quán từ chối đơn";
        return "Đang cập nhật";
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(price) + "đ";
    }
}
