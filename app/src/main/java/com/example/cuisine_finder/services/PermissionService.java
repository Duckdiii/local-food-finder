package com.example.cuisine_finder.services;

import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderStatus;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.models.UserRole;
import java.util.List;

public class PermissionService {

    public boolean canViewOrder(User user, Order order) {
        if (user == null || order == null || user.getId() == null) return false;

        String role = user.getRole();
        if (UserRole.SYSTEM_ADMIN.equals(role)) return true;

        // Cho phép xem nếu là đơn hàng của mình
        if (user.getId().equals(order.getCustomerId())) {
            return true;
        }

        // Cho phép Merchant xem nếu quản lý nhà hàng đó
        if (UserRole.isMerchant(role)) {
            return managesRestaurant(user, order.getRestaurantId());
        }

        // Cho phép Shipper xem nếu đơn được giao cho họ (giả sử có trường shipperId)
        // if (UserRole.isShipper(role) && user.getId().equals(order.getShipperId())) return true;

        return false;
    }

    public boolean canCustomerCancelOrder(User user, Order order) {
        return user != null
                && order != null
                && UserRole.isCustomer(user.getRole())
                && user.getId() != null
                && user.getId().equals(order.getCustomerId())
                && OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(order.getStatus());
    }

    public boolean canMerchantUpdateStatus(User user, Order order, String nextStatus) {
        if (user == null || order == null || !UserRole.isMerchant(user.getRole())) return false;
        if (!managesRestaurant(user, order.getRestaurantId())) return false;

        String currentStatus = order.getStatus();
        if (OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(currentStatus)) {
            return OrderStatus.MERCHANT_ACCEPTED.equals(nextStatus)
                    || OrderStatus.REJECTED_BY_MERCHANT.equals(nextStatus);
        }
        if (OrderStatus.MERCHANT_ACCEPTED.equals(currentStatus)) {
            return OrderStatus.PREPARING.equals(nextStatus);
        }
        if (OrderStatus.PREPARING.equals(currentStatus)) {
            return OrderStatus.READY_FOR_PICKUP.equals(nextStatus);
        }
        if (OrderStatus.READY_FOR_PICKUP.equals(currentStatus)) {
            return OrderStatus.DELIVERY_ASSIGNED.equals(nextStatus);
        }
        if (OrderStatus.DELIVERY_ASSIGNED.equals(currentStatus)
                || OrderStatus.SHIPPER_ACCEPTED.equals(currentStatus)
                || OrderStatus.PICKED_UP.equals(currentStatus)) {
            return OrderStatus.SHIPPING.equals(nextStatus);
        }
        if (OrderStatus.SHIPPING.equals(currentStatus)) {
            return OrderStatus.DELIVERED.equals(nextStatus)
                    || OrderStatus.DELIVERY_FAILED.equals(nextStatus);
        }
        return false;
    }

    public boolean canUpdateStatus(User user, Order order, String nextStatus) {
        return canMerchantUpdateStatus(user, order, nextStatus)
                || (OrderStatus.CANCELLED_BY_CUSTOMER.equals(nextStatus) && canCustomerCancelOrder(user, order));
    }

    public boolean canAccessMerchantDashboard(User user) {
        return user != null && UserRole.isMerchant(user.getRole());
    }

    public boolean canAccessShipperDashboard(User user) {
        return user != null && UserRole.isShipper(user.getRole());
    }

    private boolean managesRestaurant(User user, String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty()) return false;
        List<String> ids = user.getManagedRestaurantIds();
        return ids != null && ids.contains(restaurantId);
    }
}
