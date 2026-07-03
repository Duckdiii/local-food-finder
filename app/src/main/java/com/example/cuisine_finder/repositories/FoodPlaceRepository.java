package com.example.cuisine_finder.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class FoodPlaceRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference foodPlacesRef = db.collection("food_places");

    public Task<QuerySnapshot> getApprovedPlaces() {//  Lấy danh sách các quán ăn đã được phê duyệt (status là "APPROVED").
        return foodPlacesRef
                .whereEqualTo("status", "APPROVED")
                .get();
    }

    public Task<QuerySnapshot> searchPlaces(String query) {//  Tìm kiếm các quán ăn theo tên sử dụng phương pháp so khớp chuỗi bắt đầu bằng từ khóa.
        // Simple search by name (requires exact match or startAt/endAt for partial)
        return foodPlacesRef.orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get();
    }

    public Query getFilteredPlaces(String query, String foodType, String priceRange, double minRating) {
        //  Tạo truy vấn lọc danh sách quán ăn dựa trên từ khóa tìm kiếm, loại hình ẩm thực, mức giá và điểm đánh giá tối thiểu.
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
