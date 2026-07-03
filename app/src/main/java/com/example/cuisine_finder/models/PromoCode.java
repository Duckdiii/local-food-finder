package com.example.cuisine_finder.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PromoCode {
    public static final String TYPE_PERCENT = "PERCENT";
    public static final String TYPE_FIXED = "FIXED";

    private String id;
    private String code;
    private String discountType; // PERCENT, FIXED
    private double discountValue;
    private double minOrderAmount;
    private double maxDiscountAmount; // 0 = no cap
    private boolean active;
    private long expiresAt; // 0 = never expires

    public PromoCode() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }

    public double computeDiscount(double subtotal) {
        if (subtotal <= 0) return 0;
        double discount;
        if (TYPE_PERCENT.equals(discountType)) {
            discount = subtotal * (discountValue / 100.0);
        } else {
            discount = discountValue;
        }
        if (maxDiscountAmount > 0) {
            discount = Math.min(discount, maxDiscountAmount);
        }
        return Math.min(discount, subtotal);
    }
}
