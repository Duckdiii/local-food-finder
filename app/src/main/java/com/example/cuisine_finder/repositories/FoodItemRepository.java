package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.FoodItem;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class FoodItemRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference foodItemsRef = db.collection("food_items");

    public Task<QuerySnapshot> getAllFoodItems() {
        return foodItemsRef.get();
    }

    public Task<QuerySnapshot> getMenuByPlaceId(String placeId) {
        return foodItemsRef.whereEqualTo("placeId", placeId).get();
    }

    public Task<QuerySnapshot> getFeaturedFoodItems(double minRating) {
        return foodItemsRef
                .whereGreaterThanOrEqualTo("averageRating", minRating)
                .orderBy("averageRating", Query.Direction.DESCENDING)
                .limit(10)
                .get();
    }

    public Task<com.google.firebase.firestore.DocumentReference> addFoodItem(FoodItem item) {
        return foodItemsRef.add(item);
    }

    public Task<Void> updateFoodItem(String id, FoodItem item) {
        return foodItemsRef.document(id).set(item);
    }

    public Task<Void> deleteFoodItem(String id) {
        return foodItemsRef.document(id).delete();
    }
}
