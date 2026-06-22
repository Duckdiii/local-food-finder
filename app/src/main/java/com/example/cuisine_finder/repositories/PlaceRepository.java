package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.FoodPlace;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class PlaceRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference placesRef = db.collection("food_places");

    // In-memory cache — shared across all PlaceRepository instances (app lifetime)
    private static List<FoodPlace> cachedPlaces = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 phút

    public interface OnPlacesLoadedCallback {
        void onLoaded(List<FoodPlace> places);
        void onError();
    }

    public Query getPlacesByUser(String userId) {
        return placesRef.whereEqualTo("createdBy", userId);
    }

    public Query getApprovedPlaces() {
        return placesRef.whereEqualTo("status", "APPROVED").limit(100);
    }

    public Query getAllPlaces() {
        return placesRef.limit(100);
    }

    public Task<QuerySnapshot> getPlacesByFoodType(String foodType) {
        return placesRef
                .whereEqualTo("status", "APPROVED")
                .whereEqualTo("foodType", foodType)
                .get();
    }

    /**
     * Trả về danh sách quán đã được duyệt từ cache (nếu còn hạn) hoặc fetch mới từ Firestore.
     * Giảm số lần gọi mạng: chỉ fetch lại sau mỗi 5 phút.
     */
    public void getCachedApprovedPlaces(OnPlacesLoadedCallback callback) {
        long now = System.currentTimeMillis();
        if (cachedPlaces != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            callback.onLoaded(cachedPlaces);
            return;
        }
        getApprovedPlaces().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<FoodPlace> places = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    FoodPlace place = doc.toObject(FoodPlace.class);
                    if (place != null) {
                        place.setId(doc.getId());
                        places.add(place);
                    }
                }
                cachedPlaces = places;
                cacheTimestamp = System.currentTimeMillis();
                callback.onLoaded(places);
            } else {
                callback.onError();
            }
        });
    }

    /** Xóa cache thủ công — gọi khi có thay đổi dữ liệu (thêm/sửa quán). */
    public static void invalidateCache() {
        cachedPlaces = null;
    }
}
