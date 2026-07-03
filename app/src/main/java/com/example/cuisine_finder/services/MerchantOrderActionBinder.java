package com.example.cuisine_finder.services;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderStatus;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.OrderRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Shared status-transition logic for the merchant order detail bottom sheet
 * (dialog_admin_order_detail.xml), used by both MerchantDashboardActivity and
 * AdminOrdersActivity so the two screens can't drift out of sync again.
 */
public class MerchantOrderActionBinder {
    private final Context context;
    private final OrderRepository orderRepository;

    public MerchantOrderActionBinder(Context context, OrderRepository orderRepository) {
        this.context = context;
        this.orderRepository = orderRepository;
    }

    public void bind(
            Order order,
            User actor,
            BottomSheetDialog dialog,
            TextView btnPreparing,
            TextView btnDelivering,
            TextView btnCompleted
    ) {
        btnPreparing.setVisibility(View.GONE);
        btnDelivering.setVisibility(View.GONE);
        btnCompleted.setVisibility(View.GONE);

        String status = order.getStatus();

        if (OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(status)) {
            showAction(btnPreparing, "Nhận đơn", () -> updateStatus(order, actor, OrderStatus.MERCHANT_ACCEPTED, "Merchant accepted order", dialog));
            showAction(btnDelivering, "Từ chối", () -> promptRejectReason(order, actor, dialog));
            return;
        }
        if (OrderStatus.MERCHANT_ACCEPTED.equals(status)) {
            showAction(btnPreparing, "Chế biến", () -> updateStatus(order, actor, OrderStatus.PREPARING, "Merchant started preparing", dialog));
            return;
        }
        if (OrderStatus.PREPARING.equals(status)) {
            showAction(btnCompleted, "Sẵn sàng giao", () -> updateStatus(order, actor, OrderStatus.READY_FOR_PICKUP, "Order ready for pickup", dialog));
            return;
        }
        if (OrderStatus.READY_FOR_PICKUP.equals(status)) {
            showAction(btnPreparing, "Nhận giao", () -> updateStatus(order, actor, OrderStatus.DELIVERY_ASSIGNED, "Merchant accepted delivery", dialog));
            return;
        }
        if (OrderStatus.DELIVERY_ASSIGNED.equals(status)
                || OrderStatus.SHIPPER_ACCEPTED.equals(status)
                || OrderStatus.PICKED_UP.equals(status)) {
            showAction(btnDelivering, "Đang giao", () -> updateStatus(order, actor, OrderStatus.SHIPPING, "Merchant started delivery", dialog));
            return;
        }
        if (OrderStatus.SHIPPING.equals(status)) {
            showAction(btnCompleted, "Giao thành công", () -> updateStatus(order, actor, OrderStatus.DELIVERED, "Delivered", dialog));
            showAction(btnDelivering, "Thất bại", () -> updateStatus(order, actor, OrderStatus.DELIVERY_FAILED, "Delivery failed", dialog));
        }
    }

    private void showAction(TextView button, String label, Runnable action) {
        button.setText(label);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(v -> action.run());
    }

    private void promptRejectReason(Order order, User actor, BottomSheetDialog detailDialog) {
        EditText etReason = new EditText(context);
        etReason.setHint("Nhập lý do từ chối (ví dụ: Hết nguyên liệu, Quán quá tải...)");

        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        params.topMargin = 20;
        params.bottomMargin = 20;
        etReason.setLayoutParams(params);
        container.addView(etReason);

        new AlertDialog.Builder(context)
                .setTitle("Từ chối đơn hàng")
                .setView(container)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        reason = "Quán từ chối đơn hàng";
                    }
                    updateStatus(order, actor, OrderStatus.REJECTED_BY_MERCHANT, "Lý do: " + reason, detailDialog);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateStatus(Order order, User actor, String nextStatus, String note, BottomSheetDialog dialog) {
        if (actor == null) return;
        orderRepository.updateStatus(order.getId(), actor, nextStatus, note)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Đã cập nhật đơn hàng", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Không cập nhật được: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
