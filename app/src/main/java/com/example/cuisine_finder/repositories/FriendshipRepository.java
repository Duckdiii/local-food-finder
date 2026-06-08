package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.Friendship;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class FriendshipRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference friendshipsRef = db.collection("friend_ship");

    public Task<QuerySnapshot> getFriendsByRequester(String userId) {
        return friendshipsRef.whereEqualTo("requesterId", userId).get();
    }
}
