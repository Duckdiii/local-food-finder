package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.ExploredPlace;
import com.example.cuisine_finder.models.Favorite;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class InteractionRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference favoritesRef = db.collection("favorites");
    private final CollectionReference exploredRef = db.collection("explored_places");

    // --- FAVORITES ---
    public Task<Void> addFavorite(Favorite favorite) {
        String id = favorite.getUserId() + "_" + favorite.getPlaceId();
        favorite.setId(id);
        favorite.setCreatedAt(System.currentTimeMillis());
        return favoritesRef.document(id).set(favorite);
    }

    public Task<Void> removeFavorite(String userId, String placeId) {
        String id = userId + "_" + placeId;
        return favoritesRef.document(id).delete();
    }

    public Query getFavoritesByUser(String userId) {
        return favoritesRef.whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    public Task<DocumentSnapshot> getFavoriteStatus(String userId, String placeId) {
        String id = userId + "_" + placeId;
        return favoritesRef.document(id).get();
    }

    // --- EXPLORED PLACES ---
    public Task<Void> markAsExplored(ExploredPlace exploredPlace) {
        String id = exploredPlace.getUserId() + "_" + exploredPlace.getPlaceId();
        exploredPlace.setId(id);
        if (exploredPlace.getFirstVisitedAt() == 0) {
            exploredPlace.setFirstVisitedAt(System.currentTimeMillis());
        }
        exploredPlace.setLastVisitedAt(System.currentTimeMillis());
        return exploredRef.document(id).set(exploredPlace);
    }

    public Query getExploredByUser(String userId) {
        return exploredRef.whereEqualTo("userId", userId)
                .orderBy("lastVisitedAt", Query.Direction.DESCENDING);
    }

    public Task<DocumentSnapshot> getExploredStatus(String userId, String placeId) {
        String id = userId + "_" + placeId;
        return exploredRef.document(id).get();
    }
}
