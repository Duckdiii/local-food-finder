package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.Friendship;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;

public class FriendshipRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference friendshipsRef = db.collection("friend_ship");

    // Keep for backward compatibility (HomeFragment uses this)
    public Task<QuerySnapshot> getFriendsByRequester(String userId) {
        return friendshipsRef.whereEqualTo("requesterId", userId).get();
    }

    public Task<QuerySnapshot> getFriendsByReceiver(String userId) {
        return friendshipsRef.whereEqualTo("receiverId", userId).get();
    }

    public Task<DocumentReference> sendFriendRequest(String requesterId, String receiverId) {
        Map<String, Object> data = new HashMap<>();
        data.put("requesterId", requesterId);
        data.put("receiverId", receiverId);
        data.put("status", Friendship.STATUS_PENDING);
        data.put("createdAt", System.currentTimeMillis());
        return friendshipsRef.add(data);
    }

    public Task<Void> acceptFriendRequest(String friendshipId) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", Friendship.STATUS_ACCEPTED);
        return friendshipsRef.document(friendshipId).update(update);
    }

    public Task<Void> deleteFriendship(String friendshipId) {
        return friendshipsRef.document(friendshipId).delete();
    }
}
