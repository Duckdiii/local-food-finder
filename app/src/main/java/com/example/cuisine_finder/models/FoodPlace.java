package com.example.cuisine_finder.models;

import java.util.List;

public class FoodPlace {
    private String id;
    private String name;
    private String description;
    private String address;
    private String foodType;
    private List<String> tags;
    private double latitude;
    private double longitude;
    private String geohash;
    private String openTime;
    private String closeTime;
    private boolean openLate;
    private String priceRange; // CHEAP, MEDIUM, EXPENSIVE
    private double averageRating;
    private int reviewCount;
    private int favoriteCount;
    private int exploredCount;
    private List<String> imageUrls;
    private String createdBy;
    private String status; // APPROVED, PENDING, REJECTED
    private long createdAt;
    private long updatedAt;

    public FoodPlace() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }

    public boolean isOpenLate() { return openLate; }
    public void setOpenLate(boolean openLate) { this.openLate = openLate; }

    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    public int getExploredCount() { return exploredCount; }
    public void setExploredCount(int exploredCount) { this.exploredCount = exploredCount; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
