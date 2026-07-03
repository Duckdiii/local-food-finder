package com.example.cuisine_finder.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class PromoCodeRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference promoCodesRef = db.collection("promo_codes");

    public Task<QuerySnapshot> findByCode(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        return promoCodesRef
                .whereEqualTo("code", normalized)
                .whereEqualTo("active", true)
                .limit(1)
                .get();
    }
}
