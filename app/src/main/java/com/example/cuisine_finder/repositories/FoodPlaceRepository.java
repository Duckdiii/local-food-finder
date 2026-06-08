package com.example.cuisine_finder.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class FoodPlaceRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference foodPlacesRef = db.collection("food_places");

    public Task<QuerySnapshot> getApprovedPlaces() {
        return foodPlacesRef
                .whereEqualTo("status", "APPROVED")
                .get();
    }

    public Task<QuerySnapshot> searchPlaces(String query) {
        // Simple search by name (requires exact match or startAt/endAt for partial)
        return foodPlacesRef.orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get();
    }

    public Query getFilteredPlaces(String query, String foodType, String priceRange, double minRating) {
        Query q = foodPlacesRef.orderBy("name").startAt(query).endAt(query + "\uf8ff");
        
        if (foodType != null && !foodType.isEmpty() && !foodType.equals("Tất cả")) {
            q = q.whereEqualTo("foodType", foodType);
        }
        if (priceRange != null && !priceRange.isEmpty() && !priceRange.equals("Tất cả")) {
            q = q.whereEqualTo("priceRange", priceRange);
        }
        if (minRating > 0) {
            q = q.whereGreaterThanOrEqualTo("averageRating", minRating);
        }
        
        return q;
    }
}
