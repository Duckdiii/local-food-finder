package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("users");

    public Task<Void> saveUser(User user) {
        return usersRef.document(user.getId()).set(user);
    }

    public Task<DocumentSnapshot> getUser(String userId) {
        return usersRef.document(userId).get();
    }

    public Task<Void> updateUser(User user) {
        user.setUpdatedAt(System.currentTimeMillis());
        return usersRef.document(user.getId()).set(user);
    }

    public Task<QuerySnapshot> searchUsersByName(String query) {
        String q = query.trim();
        return usersRef
                .orderBy("fullName")
                .startAt(q)
                .endAt(q + "")
                .limit(30)
                .get();
    }

    public Task<QuerySnapshot> getAllUsers() {
        return usersRef.limit(50).get();
    }
}
