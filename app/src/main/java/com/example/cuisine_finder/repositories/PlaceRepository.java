package com.example.cuisine_finder.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class PlaceRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference placesRef = db.collection("food_places");

    public Query getPlacesByUser(String userId) {
        return placesRef.whereEqualTo("createdBy", userId);
    }
}
