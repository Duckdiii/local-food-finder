package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderStatus;
import com.example.cuisine_finder.models.OrderStatusHistoryEntry;
import com.example.cuisine_finder.models.PaymentMethod;
import com.example.cuisine_finder.models.PaymentStatus;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.services.PermissionService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OrderRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference ordersRef = db.collection("orders");
    private final PermissionService permissionService = new PermissionService();

    public Task<DocumentReference> createOrder(Order order) {
        if (order == null) {
            return Tasks.forException(new IllegalArgumentException("Order is required"));
        }
        if (order.getCustomerId() == null || order.getCustomerId().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Customer is required"));
        }
        if (order.getRestaurantId() == null || order.getRestaurantId().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Restaurant is required"));
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Order must contain at least one item"));
        }

        long now = System.currentTimeMillis();
        order.setStatus(OrderStatus.PENDING_MERCHANT_CONFIRMATION);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        if (order.getPaymentMethod() == null || order.getPaymentMethod().isEmpty()) {
            order.setPaymentMethod(PaymentMethod.COD);
        }
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        ArrayList<OrderStatusHistoryEntry> history = new ArrayList<>();
        history.add(new OrderStatusHistoryEntry(
                OrderStatus.PENDING_MERCHANT_CONFIRMATION,
                order.getCustomerId(),
                "CUSTOMER",
                "Customer placed order"
        ));
        order.setStatusHistory(history);

        DocumentReference orderRef = ordersRef.document();
        order.setId(orderRef.getId());
        return orderRef.set(order).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null
                        ? task.getException()
                        : new IllegalStateException("Cannot create order");
            }
            return orderRef;
        });
    }

    public Task<DocumentSnapshot> getOrder(String orderId) {
        return ordersRef.document(orderId).get();
    }

    public Task<QuerySnapshot> getCustomerOrders(String customerId) {
        return ordersRef
                .whereEqualTo("customerId", customerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> getRestaurantOrders(String restaurantId) {
        return ordersRef
                .whereEqualTo("restaurantId", restaurantId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> getAvailableShippingOrders() {
        return ordersRef
                .whereEqualTo("status", OrderStatus.READY_FOR_PICKUP)
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .get();
    }

    public Task<QuerySnapshot> getAssignedShipperOrders(String shipperId) {
        return ordersRef
                .whereEqualTo("shipperId", shipperId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<Void> updateStatus(String orderId, User actor, String nextStatus, String note) {
        if (orderId == null || orderId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Order id is required"));
        }
        if (actor == null || actor.getId() == null || actor.getId().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Actor is required"));
        }
        if (nextStatus == null || nextStatus.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Next status is required"));
        }

        DocumentReference orderRef = ordersRef.document(orderId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(orderRef);
            Order order = snapshot.toObject(Order.class);
            if (order == null) {
                throw new IllegalStateException("Order not found");
            }
            order.setId(snapshot.getId());

            if (!permissionService.canUpdateStatus(actor, order, nextStatus)) {
                throw new SecurityException("Actor cannot update order to " + nextStatus);
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", nextStatus);
            updates.put("updatedAt", System.currentTimeMillis());
            updates.put("statusHistory", FieldValue.arrayUnion(
                    new OrderStatusHistoryEntry(nextStatus, actor.getId(), actor.getRole(), note)
            ));

            if (OrderStatus.SHIPPER_ACCEPTED.equals(nextStatus)) {
                updates.put("shipperId", actor.getId());
            }
            if (OrderStatus.MERCHANT_ACCEPTED.equals(nextStatus)) {
                updates.put("merchantId", actor.getId());
            }
            if (OrderStatus.DELIVERED.equals(nextStatus)
                    && PaymentMethod.COD.equals(order.getPaymentMethod())) {
                updates.put("paymentStatus", PaymentStatus.PAID);
            }
            if (OrderStatus.CANCELLED_BY_CUSTOMER.equals(nextStatus)
                    || OrderStatus.REJECTED_BY_MERCHANT.equals(nextStatus)
                    || OrderStatus.DELIVERY_FAILED.equals(nextStatus)) {
                updates.put("cancelReason", note);
            }

            transaction.update(orderRef, updates);
            return null;
        });
    }
}
