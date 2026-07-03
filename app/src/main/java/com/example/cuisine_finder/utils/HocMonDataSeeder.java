package com.example.cuisine_finder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.cuisine_finder.models.FoodItem;
import com.example.cuisine_finder.models.FoodPlace;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HocMonDataSeeder {
    private static final String TAG = "HocMonDataSeeder";
    private static final String PREF_NAME = "cuisine_finder_seeder";
    private static final String KEY_SEED_DONE = "hoc_mon_seeded_v2"; // Increment version to trigger seeding again

    public static void seedDataIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isSeeded = prefs.getBoolean(KEY_SEED_DONE, false);
        if (isSeeded) {
            Log.d(TAG, "Hoc Mon & To Ky data has already been seeded (shared preference check).");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference foodPlacesRef = db.collection("food_places");

        // Check if Bún Cá Nha Trang Tô Ký is already registered
        foodPlacesRef.whereEqualTo("name", "Bún Cá Nha Trang Tô Ký").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        Log.d(TAG, "Bún Cá Nha Trang Tô Ký found in Firestore. Skipping seed.");
                        prefs.edit().putBoolean(KEY_SEED_DONE, true).apply();
                        return;
                    }
                    performSeeding(db, prefs);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query existing food places. Retrying seeding directly...", e);
                    performSeeding(db, prefs);
                });
    }

    private static void performSeeding(FirebaseFirestore db, SharedPreferences prefs) {
        Log.d(TAG, "Starting Firestore seeding for Hoc Mon & To Ky restaurants and dishes...");

        List<FoodPlaceSeed> placesToSeed = getSeedData();

        List<Task<Void>> taskList = new ArrayList<>();

        for (FoodPlaceSeed seed : placesToSeed) {
            // Delete existing places with the same name before seeding to prevent duplicates
            Task<Void> cleanTask = db.collection("food_places")
                    .whereEqualTo("name", seed.place.getName())
                    .get()
                    .onSuccessTask(querySnapshot -> {
                        List<Task<Void>> deleteTasks = new ArrayList<>();
                        if (querySnapshot != null) {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                String oldId = doc.getId();
                                // Delete dishes
                                deleteTasks.add(db.collection("food_items")
                                        .whereEqualTo("placeId", oldId)
                                        .get()
                                        .onSuccessTask(dishSnap -> {
                                            List<Task<Void>> singleDeleteTasks = new ArrayList<>();
                                            if (dishSnap != null) {
                                                for (com.google.firebase.firestore.DocumentSnapshot dishDoc : dishSnap.getDocuments()) {
                                                    singleDeleteTasks.add(dishDoc.getReference().delete());
                                                }
                                            }
                                            return Tasks.whenAll(singleDeleteTasks);
                                        }));
                                // Delete restaurant
                                deleteTasks.add(doc.getReference().delete());
                            }
                        }
                        return Tasks.whenAll(deleteTasks);
                    })
                    .onSuccessTask(aVoidClean -> {
                        DocumentReference placeDocRef = db.collection("food_places").document();
                        String placeId = placeDocRef.getId();
                        
                        FoodPlace place = seed.place;
                        place.setId(placeId);

                        // Save FoodPlace
                        return placeDocRef.set(place)
                                .onSuccessTask(aVoidPlace -> {
                                    Log.d(TAG, "Successfully seeded restaurant: " + place.getName());
                                    List<Task<Void>> dishTasks = new ArrayList<>();
                                    
                                    // Seed all dishes for this place
                                    for (FoodItem dish : seed.dishes) {
                                        DocumentReference dishDocRef = db.collection("food_items").document();
                                        String dishId = dishDocRef.getId();
                                        
                                        dish.setId(dishId);
                                        dish.setPlaceId(placeId);
                                        
                                        dishTasks.add(dishDocRef.set(dish).addOnSuccessListener(dVoid -> {
                                            Log.d(TAG, "Successfully seeded dish '" + dish.getName() + "' for restaurant: " + place.getName());
                                        }));
                                    }
                                    return Tasks.whenAll(dishTasks);
                                });
                    });

            taskList.add(cleanTask);
        }

        Tasks.whenAll(taskList)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "All Hoc Mon & To Ky data has been seeded successfully!");
                    prefs.edit().putBoolean(KEY_SEED_DONE, true).apply();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error seeding Hoc Mon & To Ky data to Firestore", e);
                });
    }

    private static List<FoodPlaceSeed> getSeedData() {
        List<FoodPlaceSeed> seedList = new ArrayList<>();
        long now = System.currentTimeMillis();

        // 1. Lẩu Gà Hấp Hèm 34
        FoodPlace place1 = new FoodPlace();
        place1.setName("Lẩu Gà Hấp Hèm 34");
        place1.setDescription("Quán lẩu gà hấp hèm trứ danh Hóc Môn với hương vị chua ngọt đặc trưng, không gian sân vườn rộng rãi thoáng mát.");
        place1.setAddress("34/4G Nguyễn Ảnh Thủ, Bà Điểm, Hóc Môn, TP. HCM");
        place1.setFoodType("Lẩu & Gà");
        place1.setTags(Arrays.asList("lẩu gà", "hấp hèm", "đặc sản", "quán nhậu"));
        place1.setLatitude(10.852431);
        place1.setLongitude(106.612056);
        place1.setOpenTime("09:00");
        place1.setCloseTime("22:00");
        place1.setOpenLate(false);
        place1.setPriceRange("MEDIUM");
        place1.setAverageRating(4.6);
        place1.setTotalRating(4.6);
        place1.setReviewCount(1);
        place1.setFavoriteCount(0);
        place1.setExploredCount(0);
        place1.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1547058886-f3b0942d67db?auto=format&fit=crop&w=800&q=80"));
        place1.setCreatedBy("system");
        place1.setStatus("APPROVED");
        place1.setCreatedAt(now);
        place1.setUpdatedAt(now);

        List<FoodItem> dishes1 = new ArrayList<>();
        dishes1.add(createDish("Lẩu Gà Hấp Hèm", "Lẩu gà hấp hèm đặc sản nước lẩu chua thanh ngon ngọt kèm rau rừng ngon miệng.", "Món chính", "Lẩu & Gà", 250000, 4.6, now));
        dishes1.add(createDish("Gà Nướng Lu", "Gà ta thả vườn nướng lu da giòn rụm, thịt ngọt thơm dùng kèm xôi chiên phồng.", "Món chính", "Lẩu & Gà", 180000, 4.7, now));
        dishes1.add(createDish("Gà Hấp Hành", "Gà ta hấp hành lá thơm phức, giữ nguyên vị ngọt đậm đà của thịt gà.", "Món chính", "Lẩu & Gà", 160000, 4.5, now));

        seedList.add(new FoodPlaceSeed(place1, dishes1));

        // 2. Bún Bò Huế Song Anh
        FoodPlace place2 = new FoodPlace();
        place2.setName("Bún Bò Huế Song Anh");
        place2.setDescription("Bún bò Huế chuẩn vị miền Trung, nước dùng đậm đà thơm mùi sả và mắm ruốc, thịt bò mềm và chả cua siêu chất lượng.");
        place2.setAddress("45/3 Song Hành, Hóc Môn, TP. HCM");
        place2.setFoodType("Bún bò");
        place2.setTags(Arrays.asList("bún bò", "bún bò huế", "ăn sáng", "bình dân"));
        place2.setLatitude(10.870321);
        place2.setLongitude(106.589123);
        place2.setOpenTime("06:00");
        place2.setCloseTime("21:00");
        place2.setOpenLate(false);
        place2.setPriceRange("CHEAP");
        place2.setAverageRating(4.5);
        place2.setTotalRating(4.5);
        place2.setReviewCount(1);
        place2.setFavoriteCount(0);
        place2.setExploredCount(0);
        place2.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1625398407796-82650a8c135f?auto=format&fit=crop&w=800&q=80"));
        place2.setCreatedBy("system");
        place2.setStatus("APPROVED");
        place2.setCreatedAt(now);
        place2.setUpdatedAt(now);

        List<FoodItem> dishes2 = new ArrayList<>();
        dishes2.add(createDish("Bún Bò Đặc Biệt", "Tô đặc biệt đầy đủ nạm, chả cua, giò heo, gân bò và bò viên.", "Món chính", "Bún bò", 55000, 4.6, now));
        dishes2.add(createDish("Bún Bò Tái Nạm", "Thịt bò tái tươi ngon và nạm bò chín mềm thơm ngọt.", "Món chính", "Bún bò", 40000, 4.4, now));
        dishes2.add(createDish("Chả Cua Thêm", "Phần chả cua Huế dai giòn đậm vị cua tươi ngon.", "Món thêm", "Bún bò", 15000, 4.5, now));

        seedList.add(new FoodPlaceSeed(place2, dishes2));

        // 3. Bánh Mì Heo Quay Cô Chín
        FoodPlace place3 = new FoodPlace();
        place3.setName("Bánh Mì Heo Quay Cô Chín");
        place3.setDescription("Bánh mì heo quay giòn bì siêu ngon với nước sốt đậm đà gia truyền của cô Chín, phục vụ nhanh chóng nhiệt tình.");
        place3.setAddress("12 Nguyễn Hữu Cầu, Hóc Môn, TP. HCM");
        place3.setFoodType("Bánh mì");
        place3.setTags(Arrays.asList("bánh mì", "heo quay", "ăn nhanh", "đường phố"));
        place3.setLatitude(10.881452);
        place3.setLongitude(106.595674);
        place3.setOpenTime("06:00");
        place3.setCloseTime("20:00");
        place3.setOpenLate(false);
        place3.setPriceRange("CHEAP");
        place3.setAverageRating(4.7);
        place3.setTotalRating(4.7);
        place3.setReviewCount(1);
        place3.setFavoriteCount(0);
        place3.setExploredCount(0);
        place3.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1601050690597-df056fb4ce78?auto=format&fit=crop&w=800&q=80"));
        place3.setCreatedBy("system");
        place3.setStatus("APPROVED");
        place3.setCreatedAt(now);
        place3.setUpdatedAt(now);

        List<FoodItem> dishes3 = new ArrayList<>();
        dishes3.add(createDish("Bánh Mì Heo Quay Giòn Bì", "Bánh mì kẹp heo quay giòn bì nóng hổi, nước sốt cay nhẹ kèm dưa chua.", "Món chính", "Bánh mì", 25000, 4.8, now));
        dishes3.add(createDish("Bánh Mì Xá Xíu", "Thịt xá xíu đậm đà, nước sốt ngon chuẩn vị nhà làm.", "Món chính", "Bánh mì", 20000, 4.6, now));
        dishes3.add(createDish("Bánh Mì Chả Lụa", "Chả lụa thơm ngon kẹp bánh mì giòn tan, bơ và pate béo ngậy.", "Món chính", "Bánh mì", 18000, 4.5, now));

        seedList.add(new FoodPlaceSeed(place3, dishes3));

        // 4. Cơm Tấm Nguyễn Ảnh Thủ
        FoodPlace place4 = new FoodPlace();
        place4.setName("Cơm Tấm Nguyễn Ảnh Thủ");
        place4.setDescription("Cơm tấm sườn nướng mật ong thơm phức, hạt cơm dẻo thơm ăn kèm bì chả tự làm cực ngon và nước mắm chua ngọt đặc sánh.");
        place4.setAddress("182 Nguyễn Ảnh Thủ, Hóc Môn, TP. HCM");
        place4.setFoodType("Cơm tấm");
        place4.setTags(Arrays.asList("cơm tấm", "sườn bì chả", "ăn trưa", "bình dân"));
        place4.setLatitude(10.854125);
        place4.setLongitude(106.610543);
        place4.setOpenTime("07:00");
        place4.setCloseTime("21:00");
        place4.setOpenLate(false);
        place4.setPriceRange("CHEAP");
        place4.setAverageRating(4.4);
        place4.setTotalRating(4.4);
        place4.setReviewCount(1);
        place4.setFavoriteCount(0);
        place4.setExploredCount(0);
        place4.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=800&q=80"));
        place4.setCreatedBy("system");
        place4.setStatus("APPROVED");
        place4.setCreatedAt(now);
        place4.setUpdatedAt(now);

        List<FoodItem> dishes4 = new ArrayList<>();
        dishes4.add(createDish("Cơm Tấm Sườn Bì Chả", "Sườn nướng tẩm vị đậm đà kèm bì thơm, chả chưng trứng ngon ngậy.", "Món chính", "Cơm tấm", 45000, 4.5, now));
        dishes4.add(createDish("Cơm Tấm Đùi Gà Nướng", "Đùi gà nướng mật ong vàng óng, da giòn thịt mọng nước.", "Món chính", "Cơm tấm", 50000, 4.3, now));
        dishes4.add(createDish("Canh Khổ Qua Nhồi Thịt", "Canh khổ qua giải nhiệt mát lành nhồi thịt băm đậm vị.", "Món phụ", "Cơm tấm", 15000, 4.4, now));

        seedList.add(new FoodPlaceSeed(place4, dishes4));

        // 5. Bún Đậu Mắm Tôm Phố Cổ
        FoodPlace place5 = new FoodPlace();
        place5.setName("Bún Đậu Mắm Tôm Phố Cổ");
        place5.setDescription("Bún đậu mắm tôm chuẩn vị Hà Nội giữa lòng Hóc Môn. Đậu hũ chiên giòn, mắm tôm pha chế thơm ngon, nem chua rán nóng hổi.");
        place5.setAddress("78 Lê Thị Hà, Hóc Môn, TP. HCM");
        place5.setFoodType("Bún đậu");
        place5.setTags(Arrays.asList("bún đậu", "mắm tôm", "ăn tối", "đặc sản hà nội"));
        place5.setLatitude(10.880153);
        place5.setLongitude(106.594876);
        place5.setOpenTime("10:00");
        place5.setCloseTime("22:00");
        place5.setOpenLate(false);
        place5.setPriceRange("CHEAP");
        place5.setAverageRating(4.5);
        place5.setTotalRating(4.5);
        place5.setReviewCount(1);
        place5.setFavoriteCount(0);
        place5.setExploredCount(0);
        place5.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=800&q=80"));
        place5.setCreatedBy("system");
        place5.setStatus("APPROVED");
        place5.setCreatedAt(now);
        place5.setUpdatedAt(now);

        List<FoodItem> dishes5 = new ArrayList<>();
        dishes5.add(createDish("Mẹt Bún Đậu Đầy Đủ", "Mẹt bún đậu đầy đủ bún lá, đậu rán giòn, thịt chân giò luộc, chả cốm, nem chua rán.", "Món chính", "Bún đậu", 60000, 4.6, now));
        dishes5.add(createDish("Nem Chua Rán", "Đĩa nem chua chiên giòn rụm bên ngoài, dai mềm ngọt bên trong.", "Ăn kèm", "Bún đậu", 35000, 4.4, now));
        dishes5.add(createDish("Chả Cốm Chiên", "Chả cốm dẻo thơm mùi nếp cốm non chiên vàng giòn.", "Ăn kèm", "Bún đậu", 30000, 4.5, now));

        seedList.add(new FoodPlaceSeed(place5, dishes5));

        // 6. Bún Cá Nha Trang Tô Ký
        FoodPlace place6 = new FoodPlace();
        place6.setName("Bún Cá Nha Trang Tô Ký");
        place6.setDescription("Bún cá Nha Trang chuẩn vị với nước lèo trong veo ngọt thanh, sứa tươi giòn sần sật, chả cá chiên và chả cá hấp dẻo dai ngon miệng.");
        place6.setAddress("142 Tô Ký, Thới Tam Thôn, Hóc Môn, TP. HCM");
        place6.setFoodType("Bún cá");
        place6.setTags(Arrays.asList("bún cá", "đặc sản nha trang", "ăn sáng", "bình dân"));
        place6.setLatitude(10.871200);
        place6.setLongitude(106.615400);
        place6.setOpenTime("06:00");
        place6.setCloseTime("21:30");
        place6.setOpenLate(false);
        place6.setPriceRange("CHEAP");
        place6.setAverageRating(4.5);
        place6.setTotalRating(4.5);
        place6.setReviewCount(1);
        place6.setFavoriteCount(0);
        place6.setExploredCount(0);
        place6.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=80"));
        place6.setCreatedBy("system");
        place6.setStatus("APPROVED");
        place6.setCreatedAt(now);
        place6.setUpdatedAt(now);

        List<FoodItem> dishes6 = new ArrayList<>();
        dishes6.add(createDish("Bún Cá Sứa Đặc Biệt", "Tô bún đầy đủ với sứa tươi giòn, chả cá hấp, chả cá chiên và cá dầm ngọt thịt.", "Món chính", "Bún cá", 45000, 4.6, now));
        dishes6.add(createDish("Bún Chả Cá", "Bún chả cá chiên và hấp nóng hổi, nước dùng ngọt thanh thanh vị cá biển.", "Món chính", "Bún cá", 35000, 4.4, now));
        dishes6.add(createDish("Sứa Thêm", "Phần sứa Nha Trang tươi giòn giòn ăn kèm cho đã thèm.", "Món thêm", "Bún cá", 15000, 4.5, now));

        seedList.add(new FoodPlaceSeed(place6, dishes6));

        // 7. Quán Lẩu Bò Tô Ký
        FoodPlace place7 = new FoodPlace();
        place7.setName("Quán Lẩu Bò Tô Ký");
        place7.setDescription("Quán lẩu bò bình dân nổi tiếng khu Tô Ký với nước dùng đậm vị xương ống bò ninh nhừ, thịt bò tơ Củ Chi mềm ngọt và các món nướng hấp dẫn.");
        place7.setAddress("205 Tô Ký, Trung Mỹ Tây, Hóc Môn, TP. HCM");
        place7.setFoodType("Lẩu bò");
        place7.setTags(Arrays.asList("lẩu bò", "phá lấu", "quán nhậu", "lẩu đuôi bò"));
        place7.setLatitude(10.865400);
        place7.setLongitude(106.616200);
        place7.setOpenTime("11:00");
        place7.setCloseTime("23:00");
        place7.setOpenLate(true);
        place7.setPriceRange("MEDIUM");
        place7.setAverageRating(4.6);
        place7.setTotalRating(4.6);
        place7.setReviewCount(1);
        place7.setFavoriteCount(0);
        place7.setExploredCount(0);
        place7.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1555126634-323283e090fa?auto=format&fit=crop&w=800&q=80"));
        place7.setCreatedBy("system");
        place7.setStatus("APPROVED");
        place7.setCreatedAt(now);
        place7.setUpdatedAt(now);

        List<FoodItem> dishes7 = new ArrayList<>();
        dishes7.add(createDish("Lẩu Bò Thập Cẩm", "Nồi lẩu bò thơm phức gồm nạm, gân, đuôi bò, lòng bò và các loại rau nấm ăn kèm.", "Món chính", "Lẩu bò", 200000, 4.7, now));
        dishes7.add(createDish("Bò Tơ Nướng Y", "Thịt bò tơ cắt lát mỏng nướng trực tiếp tại bàn ăn kèm muối ớt xanh.", "Món chính", "Lẩu bò", 150000, 4.6, now));
        dishes7.add(createDish("Phá Lấu Bò Kèm Bánh Mì", "Phá lấu bò béo ngậy nước cốt dừa kèm ổ bánh mì đặc ruột giòn rụm.", "Món phụ", "Lẩu bò", 40000, 4.5, now));

        seedList.add(new FoodPlaceSeed(place7, dishes7));

        // 8. Cơm Gà Xối Mỡ 142 Tô Ký
        FoodPlace place8 = new FoodPlace();
        place8.setName("Cơm Gà Xối Mỡ 142 Tô Ký");
        place8.setDescription("Cơm gà xối mỡ giòn rụm với hạt cơm chiên vàng giòn thơm dẻo, đùi gà góc tư siêu to chiên ráo dầu thịt mềm mọng nước dùng kèm kim chi chua ngọt.");
        place8.setAddress("185 Tô Ký, Thới Tam Thôn, Hóc Môn, TP. HCM");
        place8.setFoodType("Cơm gà");
        place8.setTags(Arrays.asList("cơm gà", "gà xối mỡ", "ăn trưa", "bình dân"));
        place8.setLatitude(10.868200);
        place8.setLongitude(106.615800);
        place8.setOpenTime("09:30");
        place8.setCloseTime("21:30");
        place8.setOpenLate(false);
        place8.setPriceRange("CHEAP");
        place8.setAverageRating(4.4);
        place8.setTotalRating(4.4);
        place8.setReviewCount(1);
        place8.setFavoriteCount(0);
        place8.setExploredCount(0);
        place8.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=800&q=80"));
        place8.setCreatedBy("system");
        place8.setStatus("APPROVED");
        place8.setCreatedAt(now);
        place8.setUpdatedAt(now);

        List<FoodItem> dishes8 = new ArrayList<>();
        dishes8.add(createDish("Cơm Đùi Gà Xối Mỡ", "Phần cơm chiên giòn thơm kèm đùi gà góc tư da giòn thịt ngọt dưa leo cà chua.", "Món chính", "Cơm gà", 42000, 4.5, now));
        dishes8.add(createDish("Cơm Cánh Gà Xối Mỡ", "Phần cơm kèm cánh gà chiên xối mỡ mắm tỏi thơm giòn đậm đà.", "Món chính", "Cơm gà", 38000, 4.3, now));
        dishes8.add(createDish("Canh Rong Biển Thịt Bằm", "Canh rong biển nấu thịt băm thanh mát giải ngấy khi ăn đồ chiên.", "Món thêm", "Cơm gà", 10000, 4.4, now));

        seedList.add(new FoodPlaceSeed(place8, dishes8));

        // 9. Hủ Tiếu Nam Vang Tài Anh Tô Ký
        FoodPlace place9 = new FoodPlace();
        place9.setName("Hủ Tiếu Nam Vang Tài Anh Tô Ký");
        place9.setDescription("Hủ tiếu Nam Vang chuẩn vị với sợi hủ tiếu dai ngon trộn sốt đậm đà, nước dùng hầm xương ngọt lịm, kèm tôm tươi, thịt băm, gan và trứng cút.");
        place9.setAddress("95 Tô Ký, Thới Tam Thôn, Hóc Môn, TP. HCM");
        place9.setFoodType("Hủ tiếu");
        place9.setTags(Arrays.asList("hủ tiếu nam vang", "hủ tiếu khô", "ăn sáng", "ăn tối"));
        place9.setLatitude(10.873500);
        place9.setLongitude(106.615100);
        place9.setOpenTime("06:00");
        place9.setCloseTime("22:30");
        place9.setOpenLate(false);
        place9.setPriceRange("CHEAP");
        place9.setAverageRating(4.5);
        place9.setTotalRating(4.5);
        place9.setReviewCount(1);
        place9.setFavoriteCount(0);
        place9.setExploredCount(0);
        place9.setImageUrls(Collections.singletonList("https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=80"));
        place9.setCreatedBy("system");
        place9.setStatus("APPROVED");
        place9.setCreatedAt(now);
        place9.setUpdatedAt(now);

        List<FoodItem> dishes9 = new ArrayList<>();
        dishes9.add(createDish("Hủ Tiếu Khô Đặc Biệt", "Hủ tiếu trộn nước sốt sệt cay chua ngọt cùng tôm tươi lột vỏ, tim gan bong bóng và trứng cút.", "Món chính", "Hủ tiếu", 50000, 4.6, now));
        dishes9.add(createDish("Hủ Tiếu Nước", "Tô hủ tiếu truyền thống ngập tràn nước lèo thanh ngọt rắc hành lá hẹ tươi tôm tươi xắt mỏng.", "Món chính", "Hủ tiếu", 45000, 4.4, now));
        dishes9.add(createDish("Xương Ống Thêm", "Một chén xương ống hầm tủy ngọt béo đầy ắp nhiều thịt.", "Món thêm", "Hủ tiếu", 20000, 4.5, now));

        seedList.add(new FoodPlaceSeed(place9, dishes9));

        return seedList;
    }

    private static FoodItem createDish(String name, String desc, String cat, String foodType, double price, double rating, long ts) {
        FoodItem item = new FoodItem();
        item.setName(name);
        item.setDescription(desc);
        item.setCategoryName(cat);
        item.setFoodType(foodType);
        item.setPrice(price);
        item.setAverageRating(rating);
        item.setImageUrls(new ArrayList<>());
        item.setCreatedAt(ts);
        item.setUpdatedAt(ts);
        return item;
    }

    private static class FoodPlaceSeed {
        final FoodPlace place;
        final List<FoodItem> dishes;

        FoodPlaceSeed(FoodPlace place, List<FoodItem> dishes) {
            this.place = place;
            this.dishes = dishes;
        }
    }
}
