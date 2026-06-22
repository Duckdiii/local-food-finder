package com.example.cuisine_finder.models;

public class Favorite {
    private String id;
    private String userId;
    private String placeId;
    private String placeName;
    private String placeImageUrl;
    private String placeAddress;
    private String foodType;
    private double latitude;
    private double longitude;
    private boolean openLate;
    private long createdAt;

    public Favorite() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public String getPlaceImageUrl() { return placeImageUrl; }
    public void setPlaceImageUrl(String placeImageUrl) { this.placeImageUrl = placeImageUrl; }

    public String getPlaceAddress() { return placeAddress; }
    public void setPlaceAddress(String placeAddress) { this.placeAddress = placeAddress; }

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isOpenLate() { return openLate; }
    public void setOpenLate(boolean openLate) { this.openLate = openLate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
