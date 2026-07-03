package com.example.cuisine_finder.models;

import java.util.Arrays;
import java.util.List;

public final class OrderStatus {
    public static final String PENDING_MERCHANT_CONFIRMATION = "PENDING_MERCHANT_CONFIRMATION";
    public static final String MERCHANT_ACCEPTED = "MERCHANT_ACCEPTED";
    public static final String PREPARING = "PREPARING";
    public static final String READY_FOR_PICKUP = "READY_FOR_PICKUP";
    public static final String DELIVERY_ASSIGNED = "DELIVERY_ASSIGNED";
    // Legacy statuses from the old shipper flow. Kept to display and advance existing orders.
    public static final String SHIPPER_ACCEPTED = "SHIPPER_ACCEPTED";
    public static final String PICKED_UP = "PICKED_UP";
    public static final String SHIPPING = "SHIPPING";
    public static final String DELIVERED = "DELIVERED";
    public static final String DELIVERY_FAILED = "DELIVERY_FAILED";
    public static final String CANCELLED_BY_CUSTOMER = "CANCELLED_BY_CUSTOMER";
    public static final String REJECTED_BY_MERCHANT = "REJECTED_BY_MERCHANT";

    private static final List<String> ACTIVE_STATUSES = Arrays.asList(
            PENDING_MERCHANT_CONFIRMATION,
            MERCHANT_ACCEPTED,
            PREPARING,
            READY_FOR_PICKUP,
            DELIVERY_ASSIGNED,
            SHIPPER_ACCEPTED,
            PICKED_UP,
            SHIPPING
    );

    private OrderStatus() {
    }

    public static boolean isActive(String status) {
        return ACTIVE_STATUSES.contains(status);
    }

    public static boolean isDeliveryAssigned(String status) {
        return DELIVERY_ASSIGNED.equals(status) || SHIPPER_ACCEPTED.equals(status);
    }

    public static boolean isShippingInProgress(String status) {
        return SHIPPING.equals(status) || PICKED_UP.equals(status);
    }

    public static boolean isDeliveringOrLater(String status) {
        return isDeliveryAssigned(status)
                || isShippingInProgress(status)
                || DELIVERED.equals(status)
                || DELIVERY_FAILED.equals(status);
    }

    public static boolean isTerminal(String status) {
        return DELIVERED.equals(status)
                || DELIVERY_FAILED.equals(status)
                || CANCELLED_BY_CUSTOMER.equals(status)
                || REJECTED_BY_MERCHANT.equals(status);
    }
}
