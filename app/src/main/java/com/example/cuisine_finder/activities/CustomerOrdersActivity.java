package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.AdminOrderAdapter;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.utils.InsetUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomerOrdersActivity extends AppCompatActivity {
    private AdminOrderAdapter orderAdapter;
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        InsetUtils.applySystemBars(findViewById(R.id.rootOrders), false, true);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvOrdersTitle = findViewById(R.id.tvOrdersTitle);
        TextView tvOrdersSubtitle = findViewById(R.id.tvOrdersSubtitle);
        tvOrdersTitle.setText("Don hang cua toi");
        tvOrdersSubtitle.setText("Bam vao don de xem trang thai moi nhat.");

        RecyclerView rvOrders = findViewById(R.id.rvAdminOrders);
        orderAdapter = new AdminOrderAdapter(this::openOrderTracking);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);

        observeCustomerOrders();
    }

    @Override
    protected void onDestroy() {
        if (ordersListener != null) {
            ordersListener.remove();
        }
        super.onDestroy();
    }

    private void observeCustomerOrders() {
        String customerId = FirebaseAuth.getInstance().getUid();
        if (customerId == null) {
            Toast.makeText(this, "Vui long dang nhap", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ordersListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("customerId", customerId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Khong tai duoc don hang", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null) return;

                    List<Order> orders = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order == null) continue;
                        order.setId(doc.getId());
                        orders.add(order);
                    }
                    Collections.sort(orders, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    orderAdapter.setOrders(orders);
                });
    }

    private void openOrderTracking(Order order) {
        if (order == null || order.getId() == null) return;
        Intent intent = new Intent(this, ActiveOrderActivity.class);
        intent.putExtra(ActiveOrderActivity.EXTRA_ORDER_ID, order.getId());
        startActivity(intent);
    }
}
