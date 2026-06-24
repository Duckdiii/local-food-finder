package com.example.cuisine_finder.activities;

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
import com.example.cuisine_finder.models.UserRole;
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
        tvOrdersTitle.setText("Don hang nha hang");
        tvOrdersSubtitle.setText("Nhan don, che bien va chuyen sang trang thai san sang giao.");

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
            Toast.makeText(this, "Vui long dang nhap", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository.getUser(userId).addOnSuccessListener(snapshot -> {
            currentUser = snapshot.toObject(User.class);
            if (currentUser == null) {
                Toast.makeText(this, "Khong tim thay nguoi dung", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentUser.setId(snapshot.getId());
            ensureDemoMerchantRestaurant(currentUser);
            if (!permissionService.canAccessMerchantDashboard(currentUser)) {
                Toast.makeText(this, "Chi chu quan moi duoc xem man nay", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            observeMerchantOrders(currentUser);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Khong tai duoc thong tin nguoi dung", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Tai khoan chua gan quan an", Toast.LENGTH_SHORT).show();
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
        TextView btnPreparing = view.findViewById(R.id.btnStatusPreparing);
        TextView btnDelivering = view.findViewById(R.id.btnStatusDelivering);
        TextView btnCompleted = view.findViewById(R.id.btnStatusCompleted);
        RecyclerView rvItems = view.findViewById(R.id.rvAdminDetailItems);

        tvUser.setText("Khach: " + (order.getCustomerPhone() != null ? order.getCustomerPhone() : order.getCustomerId()));
        OrderItemAdapter adapter = new OrderItemAdapter();
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
        adapter.setItems(order.getItems());

        bindMerchantActions(order, dialog, btnPreparing, btnDelivering, btnCompleted);
        dialog.setContentView(view);
        dialog.show();
    }

    private void bindMerchantActions(
            Order order,
            BottomSheetDialog dialog,
            TextView btnPreparing,
            TextView btnDelivering,
            TextView btnCompleted
    ) {
        btnPreparing.setVisibility(View.GONE);
        btnDelivering.setVisibility(View.GONE);
        btnCompleted.setVisibility(View.GONE);

        if (OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(order.getStatus())) {
            showAction(btnPreparing, "Nhan don", () -> updateStatus(order, OrderStatus.MERCHANT_ACCEPTED, "Merchant accepted order", dialog));
            showAction(btnDelivering, "Tu choi", () -> updateStatus(order, OrderStatus.REJECTED_BY_MERCHANT, "Merchant rejected order", dialog));
            return;
        }
        if (OrderStatus.MERCHANT_ACCEPTED.equals(order.getStatus())) {
            showAction(btnPreparing, "Che bien", () -> updateStatus(order, OrderStatus.PREPARING, "Merchant started preparing", dialog));
            return;
        }
        if (OrderStatus.PREPARING.equals(order.getStatus())) {
            showAction(btnCompleted, "San sang giao", () -> updateStatus(order, OrderStatus.READY_FOR_PICKUP, "Order ready for pickup", dialog));
        }
    }

    private void showAction(TextView button, String label, Runnable action) {
        button.setText(label);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(v -> action.run());
    }

    private void updateStatus(Order order, String nextStatus, String note, BottomSheetDialog dialog) {
        if (currentUser == null) return;
        orderRepository.updateStatus(order.getId(), currentUser, nextStatus, note)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Da cap nhat don", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Khong cap nhat duoc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
