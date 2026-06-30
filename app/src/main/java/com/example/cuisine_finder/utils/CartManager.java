package com.example.cuisine_finder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.cuisine_finder.models.CartItem;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static final String PREF_NAME = "cuisine_finder_cart";
    private static final String KEY_CART_ITEMS = "cart_items";
    private static final String KEY_RESTAURANT_ID = "restaurant_id";
    private static final String KEY_RESTAURANT_NAME = "restaurant_name";

    private static CartManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private List<CartItem> cartItems;
    private String currentRestaurantId;
    private String currentRestaurantName;

    private CartManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCart();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }

    private void loadCart() {
        String json = prefs.getString(KEY_CART_ITEMS, null);
        Type type = new com.google.gson.reflect.TypeToken<ArrayList<CartItem>>() {}.getType();
        cartItems = json != null ? gson.fromJson(json, type) : new ArrayList<>();
        currentRestaurantId = prefs.getString(KEY_RESTAURANT_ID, null);
        currentRestaurantName = prefs.getString(KEY_RESTAURANT_NAME, null);
    }

    private void saveCart() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_CART_ITEMS, gson.toJson(cartItems));
        editor.putString(KEY_RESTAURANT_ID, currentRestaurantId);
        editor.putString(KEY_RESTAURANT_NAME, currentRestaurantName);
        editor.apply();
    }

    public boolean addItem(String restaurantId, String restaurantName, CartItem newItem) {
        // Fix #13: Price validation
        if (newItem.getPrice() <= 0) return false;

        // Fix: One-restaurant rule
        if (currentRestaurantId != null && !currentRestaurantId.equals(restaurantId) && !cartItems.isEmpty()) {
            return false; // Caller should handle showing "Clear cart?" dialog
        }

        currentRestaurantId = restaurantId;
        currentRestaurantName = restaurantName;

        for (CartItem item : cartItems) {
            if (item.getFoodItemId().equals(newItem.getFoodItemId())) {
                item.setQuantity(item.getQuantity() + newItem.getQuantity());
                saveCart();
                return true;
            }
        }

        cartItems.add(newItem);
        saveCart();
        return true;
    }

    public void updateQuantity(String foodItemId, int newQuantity) {
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).getFoodItemId().equals(foodItemId)) {
                // Fix #6: Automatic removal if quantity <= 0
                if (newQuantity <= 0) {
                    cartItems.remove(i);
                } else {
                    cartItems.get(i).setQuantity(newQuantity);
                }
                break;
            }
        }
        if (cartItems.isEmpty()) {
            clearCart();
        } else {
            saveCart();
        }
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    public String getCurrentRestaurantId() {
        return currentRestaurantId;
    }

    public String getCurrentRestaurantName() {
        return currentRestaurantName;
    }

    // Fix #2: Use double for financial precision
    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public int getTotalQuantity() {
        int total = 0;
        for (CartItem item : cartItems) {
            total += item.getQuantity();
        }
        return total;
    }

    public void clearCart() {
        cartItems.clear();
        currentRestaurantId = null;
        currentRestaurantName = null;
        saveCart();
    }
}
