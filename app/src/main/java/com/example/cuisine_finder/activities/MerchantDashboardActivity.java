package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.SignInFragment;
import com.example.cuisine_finder.adapters.AdminOrderAdapter;
import com.example.cuisine_finder.adapters.MenuAdapter;
import com.example.cuisine_finder.adapters.OrderItemAdapter;
import com.example.cuisine_finder.adapters.ReviewAdapter;
import com.example.cuisine_finder.models.FoodItem;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderItem;
import com.example.cuisine_finder.models.OrderStatus;
import com.example.cuisine_finder.models.Review;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.OrderRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.AuthService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MerchantDashboardActivity extends AppCompatActivity {

    private TextView tvRestaurantName, tvRestaurantInfo;
    private TextView btnSignOut;
    private MaterialCardView tabMenu, tabOrders, tabReviews;
    private TextView tvTabMenu, tvTabOrders, tvTabReviews;
    private View layoutMenuTab, layoutOrdersTab, layoutReviewsTab;
    private View btnAddDish;

    private RecyclerView rvMenu, rvOrders, rvReviews;
    private TextView tvMenuEmpty, tvOrdersEmpty, tvReviewsEmpty;
    private TextView btnFilterActive, btnFilterHistory;
    private TextView tvAverageRatingText, tvTotalReviewsText;
    private android.widget.ProgressBar pbDashboardLoading;
    private TextView btnEditRestaurant;
    private TextView tvStatsRevenue, tvStatsTotalOrders, tvStatsBestSeller;
    private com.google.android.material.chip.Chip chipAllReviews, chip5Star, chip4Star, chip3Star, chip2Star, chip1Star;

    private String currentUserId;
    private String restaurantId;
    private User currentUser;
    private FoodPlace currentRestaurant;

    private MenuAdapter menuAdapter;
    private AdminOrderAdapter orderAdapter;
    private ReviewAdapter reviewAdapter;

    private final UserRepository userRepository = new UserRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AuthService authService = new AuthService();
    private final com.example.cuisine_finder.services.MerchantOrderActionBinder orderActionBinder =
            new com.example.cuisine_finder.services.MerchantOrderActionBinder(this, orderRepository);

    private ListenerRegistration dishListener;
    private ListenerRegistration orderListener;
    private ListenerRegistration reviewListener;

    private boolean showActiveOrders = true;
    private boolean isFirstOrderLoad = true;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Review> allReviews = new ArrayList<>();
    private final List<FoodPlace> managedRestaurants = new ArrayList<>();
    private int filterRating = 0;
    private boolean dishesLoaded = false;
    private boolean ordersLoaded = false;
    private boolean reviewsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_dashboard);

        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            navigateToSignIn();
            return;
        }

        initViews();
        setupAdapters();
        setupTabs();
        loadMerchantProfile();
    }

    private void initViews() {
        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        tvRestaurantInfo = findViewById(R.id.tvRestaurantInfo);
        btnSignOut = findViewById(R.id.btnSignOut);
        tabMenu = findViewById(R.id.tabMenu);
        tabOrders = findViewById(R.id.tabOrders);
        tabReviews = findViewById(R.id.tabReviews);
        tvTabMenu = findViewById(R.id.tvTabMenu);
        tvTabOrders = findViewById(R.id.tvTabOrders);
        tvTabReviews = findViewById(R.id.tvTabReviews);
        layoutMenuTab = findViewById(R.id.layoutMenuTab);
        layoutOrdersTab = findViewById(R.id.layoutOrdersTab);
        layoutReviewsTab = findViewById(R.id.layoutReviewsTab);
        btnAddDish = findViewById(R.id.btnAddDish);

        rvMenu = findViewById(R.id.rvMenu);
        rvOrders = findViewById(R.id.rvOrders);
        rvReviews = findViewById(R.id.rvReviews);

        tvMenuEmpty = findViewById(R.id.tvMenuEmpty);
        tvOrdersEmpty = findViewById(R.id.tvOrdersEmpty);
        tvReviewsEmpty = findViewById(R.id.tvReviewsEmpty);
        pbDashboardLoading = findViewById(R.id.pbDashboardLoading);

        btnFilterActive = findViewById(R.id.btnFilterActive);
        btnFilterHistory = findViewById(R.id.btnFilterHistory);

        tvStatsRevenue = findViewById(R.id.tvStatsRevenue);
        tvStatsTotalOrders = findViewById(R.id.tvStatsTotalOrders);
        tvStatsBestSeller = findViewById(R.id.tvStatsBestSeller);
        btnEditRestaurant = findViewById(R.id.btnEditRestaurant);

        tvAverageRatingText = findViewById(R.id.tvAverageRatingText);
        tvTotalReviewsText = findViewById(R.id.tvTotalReviewsText);
        chipAllReviews = findViewById(R.id.chipAllReviews);
        chip5Star = findViewById(R.id.chip5Star);
        chip4Star = findViewById(R.id.chip4Star);
        chip3Star = findViewById(R.id.chip3Star);
        chip2Star = findViewById(R.id.chip2Star);
        chip1Star = findViewById(R.id.chip1Star);

        btnFilterActive.setOnClickListener(v -> {
            showActiveOrders = true;
            filterAndDisplayOrders();
        });

        btnFilterHistory.setOnClickListener(v -> {
            showActiveOrders = false;
            filterAndDisplayOrders();
        });

        chipAllReviews.setOnClickListener(v -> setReviewFilter(0));
        chip5Star.setOnClickListener(v -> setReviewFilter(5));
        chip4Star.setOnClickListener(v -> setReviewFilter(4));
        chip3Star.setOnClickListener(v -> setReviewFilter(3));
        chip2Star.setOnClickListener(v -> setReviewFilter(2));
        chip1Star.setOnClickListener(v -> setReviewFilter(1));

        btnSignOut.setOnClickListener(v -> {
            authService.signOut();
            navigateToSignIn();
        });

        btnEditRestaurant.setOnClickListener(v -> {
            if (restaurantId != null) {
                Intent intent = new Intent(this, CreateRestaurantActivity.class);
                intent.putExtra("restaurantId", restaurantId);
                startActivity(intent);
            }
        });

        btnAddDish.setOnClickListener(v -> {
            if (restaurantId != null) {
                Intent intent = new Intent(this, AddDishActivity.class);
                intent.putExtra("placeId", restaurantId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không tìm thấy thông tin quán ăn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapters() {
        // Menu Adapter
        menuAdapter = new MenuAdapter();
        menuAdapter.setShowAddButton(false); // Hide add button for merchants
        menuAdapter.setOnItemClickListener(item -> {
            if (restaurantId != null) {
                Intent intent = new Intent(this, AddDishActivity.class);
                intent.putExtra("placeId", restaurantId);
                intent.putExtra("dishId", item.getId());
                startActivity(intent);
            }
        });
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        rvMenu.setAdapter(menuAdapter);

        // Orders Adapter
        orderAdapter = new AdminOrderAdapter(this::showOrderDetailDialog);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(orderAdapter);

        // Reviews Adapter
        reviewAdapter = new ReviewAdapter();
        reviewAdapter.setShowReplyButton(true);
        reviewAdapter.setOnReplyClickListener(review -> showReviewReplyDialog(review));
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupTabs() {
        tabMenu.setOnClickListener(v -> selectTab("MENU"));
        tabOrders.setOnClickListener(v -> selectTab("ORDERS"));
        tabReviews.setOnClickListener(v -> selectTab("REVIEWS"));

        // Default tab selection
        selectTab("MENU");
    }

    private void selectTab(String tab) {
        int activeBgColor = getColor(R.color.orange_main);
        int inactiveBgColor = getColor(R.color.white);
        int activeTextColor = getColor(R.color.white);
        int inactiveTextColor = getColor(R.color.text_dark);

        // Reset all tabs
        tabMenu.setCardBackgroundColor(ColorStateList.valueOf(inactiveBgColor));
        tvTabMenu.setTextColor(inactiveTextColor);
        tabOrders.setCardBackgroundColor(ColorStateList.valueOf(inactiveBgColor));
        tvTabOrders.setTextColor(inactiveTextColor);
        tabReviews.setCardBackgroundColor(ColorStateList.valueOf(inactiveBgColor));
        tvTabReviews.setTextColor(inactiveTextColor);

        layoutMenuTab.setVisibility(View.GONE);
        layoutOrdersTab.setVisibility(View.GONE);
        layoutReviewsTab.setVisibility(View.GONE);

        switch (tab) {
            case "MENU":
                tabMenu.setCardBackgroundColor(ColorStateList.valueOf(activeBgColor));
                tvTabMenu.setTextColor(activeTextColor);
                layoutMenuTab.setVisibility(View.VISIBLE);
                break;
            case "ORDERS":
                tabOrders.setCardBackgroundColor(ColorStateList.valueOf(activeBgColor));
                tvTabOrders.setTextColor(activeTextColor);
                layoutOrdersTab.setVisibility(View.VISIBLE);
                break;
            case "REVIEWS":
                tabReviews.setCardBackgroundColor(ColorStateList.valueOf(activeBgColor));
                tvTabReviews.setTextColor(activeTextColor);
                layoutReviewsTab.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void loadMerchantProfile() {
        userRepository.getUser(currentUserId).addOnSuccessListener(doc -> {
            currentUser = doc.toObject(User.class);
            if (currentUser == null) {
                Toast.makeText(this, "Không tìm thấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
                navigateToSignIn();
                return;
            }
            currentUser.setId(doc.getId());

            // 4. Verify permission
            if (!new com.example.cuisine_finder.services.PermissionService().canAccessMerchantDashboard(currentUser)) {
                Toast.makeText(this, "Bạn không có quyền truy cập vào màn hình này", Toast.LENGTH_SHORT).show();
                navigateToSignIn();
                return;
            }

            List<String> managedIds = currentUser.getManagedRestaurantIds();
            if (managedIds == null || managedIds.isEmpty()) {
                // If merchant has no restaurant, force them to onboarding (Create Restaurant)
                Intent intent = new Intent(this, CreateRestaurantActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // Bind the first restaurant by default
            if (restaurantId == null) {
                restaurantId = managedIds.get(0);
            }
            
            loadRestaurantDetails();
            observeRestaurantData();

            // Load all managed restaurants to enable switching
            loadManagedRestaurants(managedIds);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải thông tin tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadRestaurantDetails() {
        dishesLoaded = false;
        ordersLoaded = false;
        reviewsLoaded = false;
        if (pbDashboardLoading != null) {
            pbDashboardLoading.setVisibility(View.VISIBLE);
        }
        db.collection("food_places").document(restaurantId).get().addOnSuccessListener(doc -> {
            currentRestaurant = doc.toObject(FoodPlace.class);
            if (currentRestaurant != null) {
                currentRestaurant.setId(doc.getId());
                setupRestaurantSelector();
                String info = String.format(Locale.getDefault(), "%s • ⭐ %.1f (%d đánh giá)",
                        currentRestaurant.getFoodType() != null ? currentRestaurant.getFoodType() : "Quán ăn",
                        currentRestaurant.getAverageRating(),
                        currentRestaurant.getReviewCount());
                tvRestaurantInfo.setText(info);
            }
        });
    }

    private void observeRestaurantData() {
        // 1. Observe Dishes (Menu)
        dishListener = db.collection("food_items")
                .whereEqualTo("placeId", restaurantId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        dishesLoaded = true;
                        checkInitialLoadComplete();
                        return;
                    }
                    List<FoodItem> items = new ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            FoodItem item = doc.toObject(FoodItem.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                items.add(item);
                            }
                        }
                    }
                    menuAdapter.setItems(items);
                    tvMenuEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    dishesLoaded = true;
                    checkInitialLoadComplete();
                });

        // 2. Observe Orders
        orderListener = db.collection("orders")
                .whereEqualTo("restaurantId", restaurantId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        ordersLoaded = true;
                        checkInitialLoadComplete();
                        return;
                    }
                    
                    boolean hasNewIncomingOrder = false;
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                Order order = dc.getDocument().toObject(Order.class);
                                if (!isFirstOrderLoad 
                                        && OrderStatus.PENDING_MERCHANT_CONFIRMATION.equals(order.getStatus())
                                        && (System.currentTimeMillis() - order.getCreatedAt() < 300000)) {
                                    hasNewIncomingOrder = true;
                                }
                            }
                        }

                        allOrders.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                order.setId(doc.getId());
                                allOrders.add(order);
                            }
                        }
                    }
                    
                    isFirstOrderLoad = false;
                    filterAndDisplayOrders();

                    // Calculate business metrics
                    double revenue = 0.0;
                    int completedOrdersCount = 0;
                    java.util.Map<String, Integer> itemQuantities = new java.util.HashMap<>();

                    for (Order order : allOrders) {
                        if (OrderStatus.DELIVERED.equals(order.getStatus())) {
                            revenue += order.getTotalAmount();
                            completedOrdersCount++;
                            if (order.getItems() != null) {
                                for (OrderItem item : order.getItems()) {
                                    String name = item.getName();
                                    int q = item.getQuantity();
                                    if (name != null) {
                                        itemQuantities.put(name, itemQuantities.getOrDefault(name, 0) + q);
                                    }
                                }
                            }
                        }
                    }

                    // Find best seller
                    String bestSeller = "Chưa có";
                    int maxQty = 0;
                    for (java.util.Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
                        if (entry.getValue() > maxQty) {
                            maxQty = entry.getValue();
                            bestSeller = entry.getKey();
                        }
                    }

                    tvStatsRevenue.setText(String.format(Locale.getDefault(), "%,.0fđ", revenue));
                    tvStatsTotalOrders.setText(completedOrdersCount + " đơn");
                    tvStatsBestSeller.setText(bestSeller);

                    ordersLoaded = true;
                    checkInitialLoadComplete();

                    if (hasNewIncomingOrder) {
                        playNewOrderSound();
                        showNewOrderNotificationDialog();
                    }
                });

        // 3. Observe Reviews
        reviewListener = db.collection("reviews")
                .whereEqualTo("placeId", restaurantId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        reviewsLoaded = true;
                        checkInitialLoadComplete();
                        return;
                    }
                    
                    allReviews.clear();
                    int count5 = 0, count4 = 0, count3 = 0, count2 = 0, count1 = 0;
                    double totalRatingSum = 0;

                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Review review = doc.toObject(Review.class);
                            if (review != null) {
                                review.setId(doc.getId());
                                allReviews.add(review);

                                int r = (int) review.getRating();
                                totalRatingSum += review.getRating();
                                if (r == 5) count5++;
                                else if (r == 4) count4++;
                                else if (r == 3) count3++;
                                else if (r == 2) count2++;
                                else if (r == 1) count1++;
                            }
                        }
                    }

                    int totalCount = allReviews.size();
                    double avg = totalCount > 0 ? (totalRatingSum / totalCount) : 0.0;

                    tvAverageRatingText.setText(String.format(Locale.getDefault(), "%.1f", avg));
                    tvTotalReviewsText.setText(totalCount + " nhận xét");
                    
                    chipAllReviews.setText("Tất cả (" + totalCount + ")");
                    chip5Star.setText("5 ⭐ (" + count5 + ")");
                    chip4Star.setText("4 ⭐ (" + count4 + ")");
                    chip3Star.setText("3 ⭐ (" + count3 + ")");
                    chip2Star.setText("2 ⭐ (" + count2 + ")");
                    chip1Star.setText("1 ⭐ (" + count1 + ")");

                    reviewsLoaded = true;
                    checkInitialLoadComplete();
                    filterAndDisplayReviews();
                });
    }

    private void showOrderDetailDialog(Order order) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_admin_order_detail, null);
        TextView tvUser = view.findViewById(R.id.tvAdminDetailUser);
        TextView btnPreparing = view.findViewById(R.id.btnStatusPreparing);
        TextView btnDelivering = view.findViewById(R.id.btnStatusDelivering);
        TextView btnCompleted = view.findViewById(R.id.btnStatusCompleted);
        RecyclerView rvItems = view.findViewById(R.id.rvAdminDetailItems);

        tvUser.setText("Khách: " + (order.getCustomerPhone() != null ? order.getCustomerPhone() : order.getCustomerId()));
        OrderItemAdapter adapter = new OrderItemAdapter();
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
        adapter.setItems(order.getItems());

        orderActionBinder.bind(order, currentUser, dialog, btnPreparing, btnDelivering, btnCompleted);
        dialog.setContentView(view);
        dialog.show();
    }

    private void filterAndDisplayOrders() {
        List<Order> filteredList = new ArrayList<>();
        for (Order order : allOrders) {
            String status = order.getStatus();
            boolean isTerminal = OrderStatus.DELIVERED.equals(status)
                    || OrderStatus.DELIVERY_FAILED.equals(status)
                    || OrderStatus.CANCELLED_BY_CUSTOMER.equals(status)
                    || OrderStatus.REJECTED_BY_MERCHANT.equals(status);
            
            if (showActiveOrders) {
                if (!isTerminal) filteredList.add(order);
            } else {
                if (isTerminal) filteredList.add(order);
            }
        }
        
        Collections.sort(filteredList, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
        orderAdapter.setOrders(filteredList);
        tvOrdersEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        
        if (showActiveOrders) {
            btnFilterActive.setBackgroundResource(R.drawable.bg_chip_orange);
            btnFilterActive.setTextColor(getColor(R.color.white));
            btnFilterHistory.setBackgroundResource(R.drawable.bg_search);
            btnFilterHistory.setTextColor(getColor(R.color.text_dark));
        } else {
            btnFilterActive.setBackgroundResource(R.drawable.bg_search);
            btnFilterActive.setTextColor(getColor(R.color.text_dark));
            btnFilterHistory.setBackgroundResource(R.drawable.bg_chip_orange);
            btnFilterHistory.setTextColor(getColor(R.color.white));
        }
    }

    private void playNewOrderSound() {
        try {
            android.net.Uri notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone r = android.media.RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNewOrderNotificationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔔 Có Đơn Hàng Mới!")
                .setMessage("Bạn vừa nhận được một đơn hàng mới từ khách hàng. Vui lòng kiểm tra và xử lý ngay.")
                .setPositiveButton("Xem ngay", (dialog, which) -> {
                    selectTab("ORDERS");
                    showActiveOrders = true;
                    filterAndDisplayOrders();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void setReviewFilter(int rating) {
        this.filterRating = rating;
        filterAndDisplayReviews();
    }

    private void filterAndDisplayReviews() {
        List<Review> filteredList = new ArrayList<>();
        for (Review review : allReviews) {
            if (filterRating == 0 || (int) review.getRating() == filterRating) {
                filteredList.add(review);
            }
        }

        Collections.sort(filteredList, (r1, r2) -> Long.compare(r2.getCreatedAt(), r1.getCreatedAt()));
        reviewAdapter.setReviews(filteredList);
        tvReviewsEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);

        chipAllReviews.setChecked(filterRating == 0);
        chip5Star.setChecked(filterRating == 5);
        chip4Star.setChecked(filterRating == 4);
        chip3Star.setChecked(filterRating == 3);
        chip2Star.setChecked(filterRating == 2);
        chip1Star.setChecked(filterRating == 1);
    }

    private void showReviewReplyDialog(Review review) {
        android.widget.EditText etReply = new android.widget.EditText(this);
        etReply.setHint("Nhập nội dung phản hồi của bạn...");

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        params.topMargin = 20;
        params.bottomMargin = 20;
        etReply.setLayoutParams(params);
        container.addView(etReply);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Phản hồi đánh giá")
                .setView(container)
                .setPositiveButton("Gửi phản hồi", (dialog, which) -> {
                    String reply = etReply.getText().toString().trim();
                    if (!reply.isEmpty()) {
                        review.setMerchantReply(reply);
                        review.setRepliedAt(System.currentTimeMillis());
                        db.collection("reviews").document(review.getId()).set(review)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã gửi phản hồi thành công!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lỗi khi gửi phản hồi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Vui lòng nhập nội dung phản hồi", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadManagedRestaurants(List<String> managedIds) {
        managedRestaurants.clear();
        final int total = managedIds.size();
        final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

        for (String id : managedIds) {
            db.collection("food_places").document(id).get()
                    .addOnSuccessListener(doc -> {
                        FoodPlace place = doc.toObject(FoodPlace.class);
                        if (place != null) {
                            place.setId(doc.getId());
                            managedRestaurants.add(place);
                        }
                        if (count.incrementAndGet() == total) {
                            setupRestaurantSelector();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (count.incrementAndGet() == total) {
                            setupRestaurantSelector();
                        }
                    });
        }
    }

    private void setupRestaurantSelector() {
        if (managedRestaurants.size() > 1) {
            tvRestaurantName.setText(currentRestaurant != null ? currentRestaurant.getName() + " ▾" : "Chọn quán ăn ▾");
            tvRestaurantName.setOnClickListener(v -> showRestaurantSwitchDialog());
        } else {
            tvRestaurantName.setOnClickListener(null);
            if (currentRestaurant != null) {
                tvRestaurantName.setText(currentRestaurant.getName());
            }
        }
    }

    private void showRestaurantSwitchDialog() {
        String[] names = new String[managedRestaurants.size()];
        for (int i = 0; i < managedRestaurants.size(); i++) {
            names[i] = managedRestaurants.get(i).getName();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chọn quán ăn quản lý")
                .setItems(names, (dialog, which) -> {
                    FoodPlace selected = managedRestaurants.get(which);
                    restaurantId = selected.getId();
                    currentRestaurant = selected;
                    
                    if (dishListener != null) dishListener.remove();
                    if (orderListener != null) orderListener.remove();
                    if (reviewListener != null) reviewListener.remove();

                    loadRestaurantDetails();
                    observeRestaurantData();
                    setupRestaurantSelector();
                })
                .show();
    }

    private void checkInitialLoadComplete() {
        if (dishesLoaded && ordersLoaded && reviewsLoaded) {
            if (pbDashboardLoading != null) {
                pbDashboardLoading.setVisibility(View.GONE);
            }
        }
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (dishListener != null) dishListener.remove();
        if (orderListener != null) orderListener.remove();
        if (reviewListener != null) reviewListener.remove();
        super.onDestroy();
    }
}
