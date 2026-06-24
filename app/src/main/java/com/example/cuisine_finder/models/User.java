package com.example.cuisine_finder.models;

import java.util.List;

public class User {
    private String id;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String phone;

    private String password;
    private int exploredCount;
    private int reviewCount;
    private int favoriteCount;
    private String role; // CUSTOMER, MERCHANT, SHIPPER, SYSTEM_ADMIN
    private List<String> managedRestaurantIds;
    private String shipperStatus; // AVAILABLE, BUSY, OFFLINE
    private boolean active;
    private long createdAt;
    private long updatedAt;

    public User() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getExploredCount() { return exploredCount; }
    public void setExploredCount(int exploredCount) { this.exploredCount = exploredCount; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<String> getManagedRestaurantIds() { return managedRestaurantIds; }
    public void setManagedRestaurantIds(List<String> managedRestaurantIds) { this.managedRestaurantIds = managedRestaurantIds; }

    public String getShipperStatus() { return shipperStatus; }
    public void setShipperStatus(String shipperStatus) { this.shipperStatus = shipperStatus; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    private String address;
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
