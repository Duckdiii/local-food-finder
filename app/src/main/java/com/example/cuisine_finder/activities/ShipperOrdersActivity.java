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

public class ShipperOrdersActivity extends AppCompatActivity {
    private final UserRepository userRepository = new UserRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final PermissionService permissionService = new PermissionService();
    private final List<ListenerRegistration> orderListeners = new ArrayList<>();
    private final Map<String, Order> ordersById = new HashMap<>();

    private AdminOrderAdapter orderAdapter;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        InsetUtils.applySystemBars(findViewById(R.id.rootOrders), false, true);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvOrdersTitle = findViewById(R.id.tvOrdersTitle);
        TextView tvOrdersSubtitle = findViewById(R.id.tvOrdersSubtitle);
        tvOrdersTitle.setText("Don giao hang");
        tvOrdersSubtitle.setText("Nhan don san sang giao va cap nhat hanh trinh giao hang.");

        RecyclerView rvOrders = findViewById(R.id.rvAdminOrders);
        orderAdapter = new AdminOrderAdapter(this::showOrderDetailDialog);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);

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
            if (!permissionService.canAccessShipperDashboard(currentUser)) {
                Toast.makeText(this, "Chi shipper moi duoc xem man nay", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            observeShipperOrders(currentUser.getId());
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Khong tai duoc thong tin nguoi dung", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void observeShipperOrders(String shipperId) {
        for (ListenerRegistration listener : orderListeners) {
            listener.remove();
        }
        orderListeners.clear();
        ordersById.clear();

        ListenerRegistration availableListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("status", OrderStatus.READY_FOR_PICKUP)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order == null) continue;
                        order.setId(doc.getId());
                        ordersById.put(order.getId(), order);
                    }
                    publishOrders();
                });

        ListenerRegistration assignedListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("shipperId", shipperId)
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

        orderListeners.add(availableListener);
        orderListeners.add(assignedListener);
    }

    private void publishOrders() {
        List<Order> orders = new ArrayList<>(ordersById.values());
        Collections.sort(orders, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        orderAdapter.setOrders(orders);
    }

    private void showOrderDetailDialog(Order order) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_admin_order_detail, null);
        TextView tvUser = view.findViewById(R.id.tvAdminDetailUser);
        TextView btnPreparing = view.findViewById(R.id.btnStatusPreparing);
        TextView btnDelivering = view.findViewById(R.id.btnStatusDelivering);
        TextView btnCompleted = view.findViewById(R.id.btnStatusCompleted);
        RecyclerView rvItems = view.findViewById(R.id.rvAdminDetailItems);

        tvUser.setText("Giao den: " + (order.getDeliveryAddress() != null ? order.getDeliveryAddress() : order.getCustomerPhone()));
        OrderItemAdapter adapter = new OrderItemAdapter();
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
        adapter.setItems(order.getItems());

        bindShipperActions(order, dialog, btnPreparing, btnDelivering, btnCompleted);
        dialog.setContentView(view);
        dialog.show();
    }

    private void bindShipperActions(
            Order order,
            BottomSheetDialog dialog,
            TextView btnPreparing,
            TextView btnDelivering,
            TextView btnCompleted
    ) {
        btnPreparing.setVisibility(View.GONE);
        btnDelivering.setVisibility(View.GONE);
        btnCompleted.setVisibility(View.GONE);

        if (OrderStatus.READY_FOR_PICKUP.equals(order.getStatus())) {
            showAction(btnPreparing, "Nhan giao", () -> updateStatus(order, OrderStatus.SHIPPER_ACCEPTED, "Shipper accepted order", dialog));
            return;
        }
        if (OrderStatus.SHIPPER_ACCEPTED.equals(order.getStatus())) {
            showAction(btnPreparing, "Da lay mon", () -> updateStatus(order, OrderStatus.PICKED_UP, "Shipper picked up order", dialog));
            return;
        }
        if (OrderStatus.PICKED_UP.equals(order.getStatus())) {
            showAction(btnDelivering, "Dang giao", () -> updateStatus(order, OrderStatus.SHIPPING, "Shipper started delivery", dialog));
            return;
        }
        if (OrderStatus.SHIPPING.equals(order.getStatus())) {
            showAction(btnCompleted, "Giao thanh cong", () -> updateStatus(order, OrderStatus.DELIVERED, "Delivered", dialog));
            showAction(btnDelivering, "That bai", () -> updateStatus(order, OrderStatus.DELIVERY_FAILED, "Delivery failed", dialog));
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
