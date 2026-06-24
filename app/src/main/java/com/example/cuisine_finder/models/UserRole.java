package com.example.cuisine_finder.models;

public final class UserRole {
    public static final String CUSTOMER = "CUSTOMER";
    public static final String MERCHANT = "MERCHANT";
    public static final String SHIPPER = "SHIPPER";
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    private UserRole() {
    }

    public static boolean isMerchant(String role) {
        return MERCHANT.equals(role);
    }

    public static boolean isShipper(String role) {
        return SHIPPER.equals(role);
    }

    public static boolean isCustomer(String role) {
        return role == null || role.isEmpty() || CUSTOMER.equals(role) || "USER".equals(role);
    }
}
