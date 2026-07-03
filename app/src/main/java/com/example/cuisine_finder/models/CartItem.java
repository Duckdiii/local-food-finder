package com.example.cuisine_finder.models;

public class CartItem {
    private String foodItemId;
    private String name;
    private double price;
    private int quantity;
    private String imageUrl;

    public CartItem() {
    }

    public CartItem(String foodItemId, String name, double price, int quantity) {
        this.foodItemId = foodItemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public CartItem(FoodItem foodItem) {
        this.foodItemId = foodItem.getId();
        this.name = foodItem.getName();
        this.price = foodItem.getPrice();
        this.quantity = 1;
        if (foodItem.getImageUrls() != null && !foodItem.getImageUrls().isEmpty()) {
            this.imageUrl = foodItem.getImageUrls().get(0);
        }
    }

    public String getFoodItemId() { return foodItemId; }
    public void setFoodItemId(String foodItemId) { this.foodItemId = foodItemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getSubtotal() {
        return price * quantity;
    }

    public double getTotalPrice() {
        return getSubtotal();
    }
}
