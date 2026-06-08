package com.example.cuisine_finder.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class CategoryRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference categoriesRef = db.collection("food_categories");

    public Task<QuerySnapshot> getAllCategories() {
        return categoriesRef.whereEqualTo("active", true).get();
    }
}
