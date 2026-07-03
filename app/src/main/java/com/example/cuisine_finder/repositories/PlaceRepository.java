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
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();// Tham chiếu đến collection "food_places" trong Firestore
    private final CollectionReference placesRef = db.collection("food_places");

    // In-memory cache — shared across all PlaceRepository instances (app lifetime)
    private static List<FoodPlace> cachedPlaces = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 phút

    public interface OnPlacesLoadedCallback {// Callback interface để trả về danh sách quán ăn đã được duyệt hoặc thông báo lỗi
        void onLoaded(List<FoodPlace> places);
        void onError();
    }

    public Query getPlacesByUser(String userId) {// Lấy danh sách các quán ăn được tạo bởi một người dùng cụ thể dựa trên ID người dùng.
        return placesRef.whereEqualTo("createdBy", userId);
    }

    public Query getApprovedPlaces() {// Lấy danh sách tối đa 100 quán ăn đã được phê duyệt (status là "APPROVED").
        return placesRef.whereEqualTo("status", "APPROVED").limit(100);
    }

    public Query getAllPlaces() {// Lấy danh sách tối đa 100 quán ăn bất kể trạng thái phê duyệt.
        return placesRef.limit(100);
    }

    public Task<QuerySnapshot> getPlacesByFoodType(String foodType) {// Tìm kiếm các quán ăn đã phê duyệt dựa trên loại hình ẩm thực (foodType).
        return placesRef
                .whereEqualTo("status", "APPROVED")
                .whereEqualTo("foodType", foodType)
                .get();
    }

    public Task<QuerySnapshot> getTopRatedPlaces(double minRating) {// Lấy danh sách các quán ăn có điểm đánh giá trung bình cao hơn mức tối thiểu, sắp xếp giảm dần.
        return placesRef
                .whereEqualTo("status", "APPROVED")
                .whereGreaterThanOrEqualTo("averageRating", minRating)
                .orderBy("averageRating", Query.Direction.DESCENDING)
                .limit(50)
                .get();
    }

    public void getCachedApprovedPlaces(OnPlacesLoadedCallback callback) {// Lấy danh sách quán ăn đã phê duyệt từ bộ nhớ đệm (cache) nếu còn hiệu lực, nếu không sẽ tải mới từ Firestore và cập nhật cache. Giúp giảm thiểu số lượng truy vấn mạng.
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
