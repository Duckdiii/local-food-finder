package com.example.cuisine_finder.models;

import java.util.List;

public class FoodItem {
    private String id;
    private String placeId;
    private String name;
    private String description;
    private String categoryName;
    private String foodType;
    private double price;
    private double averageRating;
    private List<String> imageUrls;
    private long createdAt;
    private long updatedAt;

    public FoodItem() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
