package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.AdminOrderAdapter;
import com.example.cuisine_finder.adapters.OrderItemAdapter;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderStatus;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.OrderRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.PermissionService;
import com.example.cuisine_finder.utils.InsetUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminOrdersActivity extends AppCompatActivity {
    private static final String DEMO_MERCHANT_EMAIL = "merchant@gmail.com";
    private static final String DEMO_RESTAURANT_ID = "place_com_ga_xoi_mo_su_su";

    private final UserRepository userRepository = new UserRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final PermissionService permissionService = new PermissionService();
    private final com.example.cuisine_finder.services.MerchantOrderActionBinder orderActionBinder =
            new com.example.cuisine_finder.services.MerchantOrderActionBinder(this, orderRepository);
    private final List<ListenerRegistration> orderListeners = new ArrayList<>();
    private final Map<String, Order> ordersById = new HashMap<>();

    private AdminOrderAdapter adminOrderAdapter;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_orders);
        InsetUtils.applySystemBars(findViewById(R.id.rootOrders), true, true);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvOrdersTitle = findViewById(R.id.tvOrdersTitle);
        TextView tvOrdersSubtitle = findViewById(R.id.tvOrdersSubtitle);
        tvOrdersTitle.setText("Đơn hàng nhà hàng");
        tvOrdersSubtitle.setText("Nhận đơn, chế biến và tự giao cho khách.");

        RecyclerView rvOrders = findViewById(R.id.rvAdminOrders);
        adminOrderAdapter = new AdminOrderAdapter(this::showOrderDetailDialog);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adminOrderAdapter);

        loadCurrentUserAndOrders();
    }

    @Override
    protected void onDestroy() {
        for (ListenerRegistration listener : orderListeners) {
            listener.remove();
        }
        super.onDestroy();
    }

    private void loadCurrentUserAndOrders() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository.getUser(userId).addOnSuccessListener(snapshot -> {
            currentUser = snapshot.toObject(User.class);
            if (currentUser == null) {
                Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentUser.setId(snapshot.getId());
            ensureDemoMerchantRestaurant(currentUser);
            if (!permissionService.canAccessMerchantDashboard(currentUser)) {
                Toast.makeText(this, "Chỉ chủ quán mới được xem màn này", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            observeMerchantOrders(currentUser);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Không tải được thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void ensureDemoMerchantRestaurant(User user) {
        if (user == null || user.getEmail() == null) return;
        if (!DEMO_MERCHANT_EMAIL.equalsIgnoreCase(user.getEmail())) return;

        List<String> managedIds = user.getManagedRestaurantIds();
        if (managedIds == null) {
            managedIds = new ArrayList<>();
            user.setManagedRestaurantIds(managedIds);
        }
        if (!managedIds.contains(DEMO_RESTAURANT_ID)) {
            managedIds.add(DEMO_RESTAURANT_ID);
        }
    }

    private void observeMerchantOrders(User user) {
        if (user.getManagedRestaurantIds() == null || user.getManagedRestaurantIds().isEmpty()) {
            Toast.makeText(this, "Tài khoản chưa gắn quán ăn", Toast.LENGTH_SHORT).show();
            return;
        }

        for (ListenerRegistration listener : orderListeners) {
            listener.remove();
        }
        orderListeners.clear();
        ordersById.clear();

        for (String restaurantId : user.getManagedRestaurantIds()) {
            ListenerRegistration listener = FirebaseFirestore.getInstance()
                    .collection("orders")
                    .whereEqualTo("restaurantId", restaurantId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null) return;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order == null) continue;
                            order.setId(doc.getId());
                            if (OrderStatus.isActive(order.getStatus())) {
                                ordersById.put(order.getId(), order);
                            } else {
                                ordersById.remove(order.getId());
                            }
                        }
                        publishOrders();
                    });
            orderListeners.add(listener);
        }
    }

    private void publishOrders() {
        List<Order> orders = new ArrayList<>(ordersById.values());
        Collections.sort(orders, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        adminOrderAdapter.setOrders(orders);
    }

    private void showOrderDetailDialog(Order order) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_admin_order_detail, null);
        TextView tvUser = view.findViewById(R.id.tvAdminDetailUser);
        View layoutRecipientInfo = view.findViewById(R.id.layoutRecipientInfo);
        TextView tvRecipientName = view.findViewById(R.id.tvRecipientName);
        TextView tvRecipientPhone = view.findViewById(R.id.tvRecipientPhone);
        TextView tvRecipientAddress = view.findViewById(R.id.tvRecipientAddress);
        TextView tvRecipientNote = view.findViewById(R.id.tvRecipientNote);
        TextView btnCallRecipient = view.findViewById(R.id.btnCallRecipient);
        TextView btnPreparing = view.findViewById(R.id.btnStatusPreparing);
        TextView btnDelivering = view.findViewById(R.id.btnStatusDelivering);
        TextView btnCompleted = view.findViewById(R.id.btnStatusCompleted);
        RecyclerView rvItems = view.findViewById(R.id.rvAdminDetailItems);

        bindDeliveryRecipientInfo(
                order,
                tvUser,
                layoutRecipientInfo,
                tvRecipientName,
                tvRecipientPhone,
                tvRecipientAddress,
                tvRecipientNote,
                btnCallRecipient
        );
        OrderItemAdapter adapter = new OrderItemAdapter();
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
        adapter.setItems(order.getItems());

        orderActionBinder.bind(order, currentUser, dialog, btnPreparing, btnDelivering, btnCompleted);
        dialog.setContentView(view);
        dialog.show();
    }

    private void bindDeliveryRecipientInfo(
            Order order,
            TextView tvUser,
            View layoutRecipientInfo,
            TextView tvRecipientName,
            TextView tvRecipientPhone,
            TextView tvRecipientAddress,
            TextView tvRecipientNote,
            TextView btnCallRecipient
    ) {
        if (!isMerchantDeliveryInProgress(order.getStatus())) {
            layoutRecipientInfo.setVisibility(View.GONE);
            btnCallRecipient.setVisibility(View.GONE);
            tvUser.setText("Khách: " + valueOrFallback(order.getCustomerPhone(), order.getCustomerId()));
            return;
        }

        String name = valueOrFallback(order.getCustomerName(), "Khách hàng");
        String phone = valueOrFallback(order.getCustomerPhone(), "Chưa có số điện thoại");
        String address = valueOrFallback(order.getDeliveryAddress(), "Chưa có địa chỉ giao hàng");
        String note = valueOrFallback(order.getDeliveryNote(), "Không có ghi chú");
        boolean hasPhone = order.getCustomerPhone() != null && !order.getCustomerPhone().trim().isEmpty();

        layoutRecipientInfo.setVisibility(View.VISIBLE);
        tvUser.setText("Đang giao đến: " + address);
        tvRecipientName.setText("Người nhận: " + name);
        tvRecipientPhone.setText("Số điện thoại: " + phone);
        tvRecipientAddress.setText("Địa chỉ: " + address);
        tvRecipientNote.setText("Ghi chú: " + note);
        btnCallRecipient.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
        btnCallRecipient.setOnClickListener(v -> {
            if (!hasPhone) return;
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + order.getCustomerPhone().trim()));
            startActivity(intent);
        });
    }

    private boolean isMerchantDeliveryInProgress(String status) {
        return OrderStatus.isDeliveringOrLater(status);
    }

    private String valueOrFallback(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

}
