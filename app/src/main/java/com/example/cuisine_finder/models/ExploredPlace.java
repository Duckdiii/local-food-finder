package com.example.cuisine_finder.models;

public class ExploredPlace {
    private String id;
    private String userId;
    private String placeId;
    private String placeName;
    private String placeImageUrl;
    private String foodType;
    private String address;
    private double latitude;
    private double longitude;
    private int visitCount;
    private long firstVisitedAt;
    private long lastVisitedAt;

    public ExploredPlace() {
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

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }

    public long getFirstVisitedAt() { return firstVisitedAt; }
    public void setFirstVisitedAt(long firstVisitedAt) { this.firstVisitedAt = firstVisitedAt; }

    public long getLastVisitedAt() { return lastVisitedAt; }
    public void setLastVisitedAt(long lastVisitedAt) { this.lastVisitedAt = lastVisitedAt; }
}
