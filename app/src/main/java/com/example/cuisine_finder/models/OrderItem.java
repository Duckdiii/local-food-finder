package com.example.cuisine_finder.models;

public class OrderItem {
    private String foodItemId;
    private String name;
    private double price;
    private int quantity;
    private double subtotal;
    private String imageUrl;
    private String note;

    public OrderItem() {
    }

    public OrderItem(CartItem cartItem) {
        this.foodItemId = cartItem.getFoodItemId();
        this.name = cartItem.getName();
        this.price = cartItem.getPrice();
        this.quantity = cartItem.getQuantity();
        this.subtotal = cartItem.getSubtotal();
        this.imageUrl = cartItem.getImageUrl();
        this.note = cartItem.getNote();
    }

    public String getFoodItemId() { return foodItemId; }
    public void setFoodItemId(String foodItemId) { this.foodItemId = foodItemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
