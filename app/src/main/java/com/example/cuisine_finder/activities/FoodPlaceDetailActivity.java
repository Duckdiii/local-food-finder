package com.example.cuisine_finder.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.CartAdapter;
import com.example.cuisine_finder.adapters.MenuAdapter;
import com.example.cuisine_finder.adapters.ReviewAdapter;
import com.example.cuisine_finder.models.CartItem;
import com.example.cuisine_finder.models.ExploredPlace;
import com.example.cuisine_finder.models.Favorite;
import com.example.cuisine_finder.models.FoodItem;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.models.Review;
import com.example.cuisine_finder.repositories.FoodItemRepository;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.example.cuisine_finder.repositories.ReviewRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FoodPlaceDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PLACE_ID = "placeId";

    private ImageView ivPlaceImage;
    private TextView tvPlaceName, tvStatus, tvRating, tvReviewCount, tvFoodType;
    private TextView tvAddress, tvDescription, tvOpenTime, tvCloseTime;
    private TextView btnFavorite, btnShare, btnCall, btnOrder, btnExplored, btnWriteReview;
    private RecyclerView rvReviews;
    private RecyclerView rvMenu;
    private LinearLayout layoutMenuTab, layoutReviewsTab, layoutMenuEmpty;
    private com.google.android.material.card.MaterialCardView tabMenu, tabReviews;
    private com.google.android.material.card.MaterialCardView layoutViewCart;
    private TextView tvTabMenuLabel, tvTabReviewsLabel;
    private TextView tvCartCount, tvCartTotal;
    private ReviewAdapter reviewAdapter;
    private MenuAdapter menuAdapter;
    private final Map<String, CartItem> cartItems = new LinkedHashMap<>();

    private FoodPlace currentPlace;
    private ReviewRepository reviewRepository;
    private FoodItemRepository foodItemRepository;
    private InteractionRepository interactionRepository;
    private String currentUserId;
    private boolean isFavorited = false;
    private boolean isExplored = false;

    // Image upload variables
    private List<android.net.Uri> selectedImageUris = new ArrayList<>();
    private LinearLayout llImagePreview;
    private TextView tvImageCount;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable Edge-to-Edge for Notch/Punch hole support
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_food_place_detail);

        reviewRepository = new ReviewRepository();
        foodItemRepository = new FoodItemRepository();
        interactionRepository = new InteractionRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupTabs();
        setupImagePicker();
        handleWindowInsets();

        // Fetch real data from Firestore
        String placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        if (placeId == null) placeId = getIntent().getStringExtra("PLACE_ID");
        if (placeId == null) placeId = "place_banh_mi_chao_hcmute";
        loadPlaceFromFirestore(placeId);

        setupClickListeners();
        setupChatShare();
    }

    private void handleWindowInsets() {
        View appBarLayout = findViewById(R.id.appBarLayout);
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            
            // Add top margin to toolbar and icons to avoid notch
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                lp.topMargin = statusBarHeight;
                toolbar.setLayoutParams(lp);
            }
            
            // We also need to move the right icons (Favorite, Share, etc.)
            View iconContainer = (View) btnFavorite.getParent();
            if (iconContainer != null && iconContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams iconLp = (ViewGroup.MarginLayoutParams) iconContainer.getLayoutParams();
                iconLp.topMargin = statusBarHeight + (int)(12 * getResources().getDisplayMetrics().density);
                iconContainer.setLayoutParams(iconLp);
            }
            
            return insets;
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        if (selectedImageUris.size() < 3) {
                            selectedImageUris.add(uri);
                            updateImagePreview();
                        } else {
                            Toast.makeText(this, "Bạn chỉ được chọn tối đa 3 ảnh", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void updateImagePreview() {
        if (llImagePreview == null) return;
        llImagePreview.removeAllViews();
        for (android.net.Uri uri : selectedImageUris) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
            params.setMargins(0, 0, 16, 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(uri);
            llImagePreview.addView(imageView);
        }
        tvImageCount.setText(selectedImageUris.size() + "/3 ảnh");
    }

    private void loadPlaceFromFirestore(String placeId) {
        FirebaseFirestore.getInstance().collection("food_places").document(placeId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        currentPlace = documentSnapshot.toObject(FoodPlace.class);
                        if (currentPlace != null) {
                            currentPlace.setId(documentSnapshot.getId());
                            loadPlaceDetails(currentPlace);
                            loadReviews();
                            loadMenu(currentPlace.getId());
                            checkStatus();
                        }
                    }
                });
    }

    private void initViews() {
        ivPlaceImage = findViewById(R.id.ivPlaceImage);
        tvPlaceName = findViewById(R.id.tvPlaceName);
        tvStatus = findViewById(R.id.tvStatus);
        tvRating = findViewById(R.id.tvRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        tvFoodType = findViewById(R.id.tvFoodType);
        tvAddress = findViewById(R.id.tvAddress);
        tvDescription = findViewById(R.id.tvDescription);
        tvOpenTime = findViewById(R.id.tvOpenTime);
        tvCloseTime = findViewById(R.id.tvCloseTime);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnShare = findViewById(R.id.btnShare);
        btnCall = findViewById(R.id.btnCall);
        btnOrder = findViewById(R.id.btnOrder);
        btnExplored = findViewById(R.id.btnExplored);
        btnWriteReview = findViewById(R.id.btnWriteReview);
        rvReviews = findViewById(R.id.rvReviews);
        rvMenu = findViewById(R.id.rvMenu);
        layoutMenuTab = findViewById(R.id.layoutMenuTab);
        layoutReviewsTab = findViewById(R.id.layoutReviewsTab);
        layoutMenuEmpty = findViewById(R.id.layoutMenuEmpty);
        tabMenu = findViewById(R.id.tabMenu);
        tabReviews = findViewById(R.id.tabReviews);
        tvTabMenuLabel = findViewById(R.id.tvTabMenuLabel);
        tvTabReviewsLabel = findViewById(R.id.tvTabReviewsLabel);
        layoutViewCart = findViewById(R.id.layoutViewCart);
        tvCartCount = findViewById(R.id.tvCartCount);
        tvCartTotal = findViewById(R.id.tvCartTotal);
    }

    private void setupRecyclerViews() {
        reviewAdapter = new ReviewAdapter();
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

        menuAdapter = new MenuAdapter();
        menuAdapter.setOnAddToCartClickListener(this::addToCart);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        rvMenu.setAdapter(menuAdapter);
    }

    private void setupTabs() {
        tabMenu.setOnClickListener(v -> switchTab(true));
        tabReviews.setOnClickListener(v -> switchTab(false));
    }

    private void switchTab(boolean showMenu) {
        if (showMenu) {
            layoutMenuTab.setVisibility(View.VISIBLE);
            layoutReviewsTab.setVisibility(View.GONE);
            tabMenu.setCardBackgroundColor(getResources().getColor(R.color.orange_main));
            tabReviews.setCardBackgroundColor(getResources().getColor(R.color.white));
            tvTabMenuLabel.setTextColor(getResources().getColor(R.color.white));
            tvTabReviewsLabel.setTextColor(getResources().getColor(R.color.text_gray));
        } else {
            layoutMenuTab.setVisibility(View.GONE);
            layoutReviewsTab.setVisibility(View.VISIBLE);
            tabMenu.setCardBackgroundColor(getResources().getColor(R.color.white));
            tabReviews.setCardBackgroundColor(getResources().getColor(R.color.orange_main));
            tvTabMenuLabel.setTextColor(getResources().getColor(R.color.text_gray));
            tvTabReviewsLabel.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void loadMenu(String placeId) {
        foodItemRepository.getMenuByPlaceId(placeId).addOnSuccessListener(querySnapshot -> {
            if (querySnapshot == null || querySnapshot.isEmpty()) {
                layoutMenuEmpty.setVisibility(View.VISIBLE);
                rvMenu.setVisibility(View.GONE);
                return;
            }
            List<FoodItem> items = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                FoodItem item = doc.toObject(FoodItem.class);
                if (item != null) {
                    item.setId(doc.getId());
                    items.add(item);
                }
            }
            items.sort((a, b) -> a.getName() != null && b.getName() != null
                    ? a.getName().compareTo(b.getName()) : 0);
            menuAdapter.setItems(items);
            layoutMenuEmpty.setVisibility(View.GONE);
            rvMenu.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e -> {
            layoutMenuEmpty.setVisibility(View.VISIBLE);
            rvMenu.setVisibility(View.GONE);
        });
    }

    private void checkStatus() {
        if (currentUserId == null || currentPlace == null) return;

        interactionRepository.getFavoriteStatus(currentUserId, currentPlace.getId())
                .addOnSuccessListener(doc -> {
                    isFavorited = doc.exists();
                    updateFavoriteUI();
                });

        interactionRepository.getExploredStatus(currentUserId, currentPlace.getId())
                .addOnSuccessListener(doc -> {
                    isExplored = doc.exists();
                    updateExploredUI();
                });
    }

    private void updateFavoriteUI() {
        btnFavorite.setText(isFavorited ? "❤" : "♡");
        btnFavorite.setTextColor(getResources().getColor(isFavorited ? R.color.red_close : R.color.text_dark));
    }

    private void updateExploredUI() {
        btnExplored.setBackgroundResource(isExplored ? R.drawable.bg_chip_orange : R.drawable.bg_icon_soft);
    }

    private void loadReviews() {
        if (currentPlace == null) return;
        reviewRepository.getReviewsByPlace(currentPlace.getId())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("ReviewError", "Lỗi tải review: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        reviewAdapter.setReviews(value.toObjects(Review.class));
                    }
                });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadPlaceDetails(FoodPlace place) {
        if (place == null) return;

        tvPlaceName.setText(place.getName());
        tvFoodType.setText(place.getFoodType());
        tvAddress.setText(place.getAddress());
        tvDescription.setText(place.getDescription());
        tvRating.setText(String.format("%.1f", place.getAverageRating()));
        tvReviewCount.setText("(" + place.getReviewCount() + " reviews)");
        tvOpenTime.setText("Mở: " + place.getOpenTime());
        tvCloseTime.setText("Đóng: " + place.getCloseTime());
        
        // Kiểm tra trạng thái thực tế dựa trên giờ mở/đóng cửa
        boolean isOpen = isCurrentlyOpen(place);
        if (!"APPROVED".equals(place.getStatus())) {
            tvStatus.setText("Tạm đóng");
            tvStatus.setTextColor(getResources().getColor(R.color.red_close));
            tvStatus.setBackgroundResource(R.drawable.bg_chip_red);
        } else if (isOpen) {
            tvStatus.setText("Đang mở");
            tvStatus.setTextColor(getResources().getColor(R.color.green_open));
            tvStatus.setBackgroundResource(R.drawable.bg_chip_green);
        } else {
            tvStatus.setText("Đã đóng cửa");
            tvStatus.setTextColor(getResources().getColor(R.color.red_close));
            tvStatus.setBackgroundResource(R.drawable.bg_chip_red);
        }
    }

    private boolean isCurrentlyOpen(FoodPlace place) {
        if (place.getOpenTime() == null || place.getCloseTime() == null) return true;
        try {
            java.util.Calendar now = java.util.Calendar.getInstance();
            int current = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE);
            String[] o = place.getOpenTime().split(":");
            String[] c = place.getCloseTime().split(":");
            int open = Integer.parseInt(o[0]) * 60 + Integer.parseInt(o[1]);
            int close = Integer.parseInt(c[0]) * 60 + Integer.parseInt(c[1]);
            if (open <= close) return current >= open && current <= close;
            return current >= open || current <= close; // crosses midnight
        } catch (Exception e) {
            return true;
        }
    }

    private void setupClickListeners() {
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnExplored.setOnClickListener(v -> toggleExplored());
        btnWriteReview.setOnClickListener(v -> showReviewDialog());
        
        btnShare.setOnClickListener(v -> Toast.makeText(this, "Chia sẻ địa điểm này", Toast.LENGTH_SHORT).show());
        btnCall.setOnClickListener(v -> Toast.makeText(this, "Đang gọi hotline quán...", Toast.LENGTH_SHORT).show());
        btnOrder.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                switchTab(true);
                Toast.makeText(this, "Vui lòng chọn món ăn từ thực đơn", Toast.LENGTH_SHORT).show();
            } else {
                showCartDialog();
            }
        });
        layoutViewCart.setOnClickListener(v -> showCartDialog());
    }

    private void addToCart(FoodItem foodItem) {
        if (foodItem == null || foodItem.getId() == null) return;
        if (foodItem.getPrice() <= 0) {
            Toast.makeText(this, "Mon nay chua co gia de dat hang", Toast.LENGTH_SHORT).show();
            return;
        }

        CartItem existing = cartItems.get(foodItem.getId());
        if (existing == null) {
            cartItems.put(foodItem.getId(), new CartItem(foodItem));
        } else {
            existing.setQuantity(existing.getQuantity() + 1);
        }
        updateCartBar();
        Toast.makeText(this, "Da them vao gio hang", Toast.LENGTH_SHORT).show();
    }

    private void increaseCartItem(CartItem item) {
        if (item == null || item.getFoodItemId() == null) return;
        CartItem existing = cartItems.get(item.getFoodItemId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + 1);
        }
    }

    private void decreaseCartItem(CartItem item) {
        if (item == null || item.getFoodItemId() == null) return;
        CartItem existing = cartItems.get(item.getFoodItemId());
        if (existing == null) return;
        int nextQuantity = existing.getQuantity() - 1;
        if (nextQuantity <= 0) {
            cartItems.remove(item.getFoodItemId());
        } else {
            existing.setQuantity(nextQuantity);
        }
    }

    private void removeCartItem(CartItem item) {
        if (item == null || item.getFoodItemId() == null) return;
        cartItems.remove(item.getFoodItemId());
    }

    private void updateCartBar() {
        int count = getCartItemCount();
        if (count <= 0) {
            layoutViewCart.setVisibility(View.GONE);
            return;
        }
        layoutViewCart.setVisibility(View.VISIBLE);
        tvCartCount.setText(String.valueOf(count));
        tvCartTotal.setText(formatPrice(getCartTotal()));
    }

    private int getCartItemCount() {
        int count = 0;
        for (CartItem item : cartItems.values()) {
            count += item.getQuantity();
        }
        return count;
    }

    private double getCartTotal() {
        double total = 0;
        for (CartItem item : cartItems.values()) {
            total += item.getSubtotal();
        }
        return total;
    }

    private List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    private void showCartDialog() {
        if (cartItems.isEmpty()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_cart, null);
        TextView tvCartRestaurantName = view.findViewById(R.id.tvCartRestaurantName);
        TextView tvCartTotalPrice = view.findViewById(R.id.tvCartTotalPrice);
        TextView btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder);
        RecyclerView rvCartItems = view.findViewById(R.id.rvCartItems);

        if (currentPlace != null && currentPlace.getName() != null) {
            tvCartRestaurantName.setText(currentPlace.getName());
        }

        final CartAdapter[] adapterRef = new CartAdapter[1];
        CartAdapter adapter = new CartAdapter(new CartAdapter.CartItemActionListener() {
            @Override
            public void onIncrease(CartItem item) {
                increaseCartItem(item);
                refreshCartDialog(adapterRef[0], tvCartTotalPrice, dialog);
            }

            @Override
            public void onDecrease(CartItem item) {
                decreaseCartItem(item);
                refreshCartDialog(adapterRef[0], tvCartTotalPrice, dialog);
            }

            @Override
            public void onRemove(CartItem item) {
                removeCartItem(item);
                refreshCartDialog(adapterRef[0], tvCartTotalPrice, dialog);
            }
        });
        adapterRef[0] = adapter;

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.setAdapter(adapter);
        refreshCartDialog(adapter, tvCartTotalPrice, dialog);

        btnPlaceOrder.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                dialog.dismiss();
                return;
            }
            openCheckout();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void refreshCartDialog(CartAdapter adapter, TextView tvCartTotalPrice, BottomSheetDialog dialog) {
        updateCartBar();
        if (cartItems.isEmpty()) {
            dialog.dismiss();
            return;
        }
        adapter.setItems(getCartItems());
        tvCartTotalPrice.setText(formatPrice(getCartTotal()));
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format((long) price) + "d";
    }

    private void openCheckout() {
        if (currentPlace == null) {
            Toast.makeText(this, "Thong tin quan chua tai xong", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra(CheckoutActivity.EXTRA_RESTAURANT_ID, currentPlace.getId());
        intent.putExtra(CheckoutActivity.EXTRA_RESTAURANT_NAME, currentPlace.getName());
        intent.putExtra(CheckoutActivity.EXTRA_CART_ITEMS_JSON, serializeCartItems());
        startActivity(intent);
    }

    private String serializeCartItems() {
        JSONArray array = new JSONArray();
        for (CartItem item : cartItems.values()) {
            JSONObject object = new JSONObject();
            try {
                object.put("foodItemId", item.getFoodItemId());
                object.put("name", item.getName());
                object.put("price", item.getPrice());
                object.put("quantity", item.getQuantity());
                object.put("imageUrl", item.getImageUrl());
                array.put(object);
            } catch (JSONException ignored) {
                // Skip malformed item and keep checkout usable for the rest of the cart.
            }
        }
        return array.toString();
    }

    private void setupChatShare() {
        btnShare.setOnClickListener(view -> {
            if (currentPlace == null) {
                Toast.makeText(this, "Thông tin quán chưa tải xong", Toast.LENGTH_SHORT).show();
                return;
            }
            android.content.SharedPreferences preferences = getSharedPreferences("chat_settings", Context.MODE_PRIVATE);
            String roomId = preferences.getString("last_room_id", null);
            String roomName = preferences.getString("last_room_name", null);
            if (roomId == null || roomName == null) {
                Toast.makeText(this, "Hãy vào tab Cộng đồng để chọn phòng chat trước", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ChatRoomActivity.class);
            intent.putExtra(ChatRoomActivity.EXTRA_ROOM_ID, roomId);
            intent.putExtra(ChatRoomActivity.EXTRA_ROOM_NAME, roomName);
            intent.putExtra(ChatRoomActivity.EXTRA_RESTAURANT_ID, currentPlace.getId());
            intent.putExtra(ChatRoomActivity.EXTRA_RESTAURANT_NAME, currentPlace.getName());
            intent.putExtra(ChatRoomActivity.EXTRA_RESTAURANT_RATING, currentPlace.getAverageRating());
            if (currentPlace.getImageUrls() != null && !currentPlace.getImageUrls().isEmpty()) {
                intent.putExtra(ChatRoomActivity.EXTRA_RESTAURANT_IMAGE, currentPlace.getImageUrls().get(0));
            }
            startActivity(intent);
        });
    }

    private void toggleFavorite() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFavorited) {
            interactionRepository.removeFavorite(currentUserId, currentPlace.getId())
                    .addOnSuccessListener(aVoid -> {
                        isFavorited = false;
                        updateFavoriteUI();
                    });
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(currentUserId);
            favorite.setPlaceId(currentPlace.getId());
            favorite.setPlaceName(currentPlace.getName());
            favorite.setPlaceAddress(currentPlace.getAddress());
            favorite.setFoodType(currentPlace.getFoodType());
            favorite.setLatitude(currentPlace.getLatitude());
            favorite.setLongitude(currentPlace.getLongitude());
            favorite.setOpenLate(currentPlace.isOpenLate());
            favorite.setPlaceImageUrl(currentPlace.getImageUrls() != null && !currentPlace.getImageUrls().isEmpty() ? currentPlace.getImageUrls().get(0) : "");

            interactionRepository.addFavorite(favorite).addOnSuccessListener(aVoid -> {
                isFavorited = true;
                updateFavoriteUI();
            });
        }
    }

    private void toggleExplored() {
        if (currentUserId == null) return;

        ExploredPlace explored = new ExploredPlace();
        explored.setUserId(currentUserId);
        explored.setPlaceId(currentPlace.getId());
        explored.setPlaceName(currentPlace.getName());

        interactionRepository.markAsExplored(explored).addOnSuccessListener(aVoid -> {
            isExplored = true;
            updateExploredUI();
            Toast.makeText(this, "Đã đánh dấu đã khám phá!", Toast.LENGTH_SHORT).show();
        });
    }

    private void showReviewDialog() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedImageUris.clear(); // Clear previous selection
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_write_review, null);

        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        EditText etComment = view.findViewById(R.id.etComment);
        TextView btnSubmit = view.findViewById(R.id.btnSubmitReview);
        TextView btnAddImage = view.findViewById(R.id.btnAddImage);
        llImagePreview = view.findViewById(R.id.llImagePreview);
        tvImageCount = view.findViewById(R.id.tvImageCount);

        btnAddImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = etComment.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Đang gửi...");

            Review review = new Review();
            review.setPlaceId(currentPlace.getId());
            review.setPlaceName(currentPlace.getName());
            review.setRating(rating);
            review.setComment(comment);
            review.setUserId(currentUserId);
            
            // Get user info first
            new com.example.cuisine_finder.repositories.UserRepository().getUser(currentUserId).addOnSuccessListener(userDoc -> {
                com.example.cuisine_finder.models.User user = userDoc.toObject(com.example.cuisine_finder.models.User.class);
                if (user != null) {
                    review.setUserName(user.getFullName());
                } else {
                    review.setUserName("Người dùng");
                }

                if (!selectedImageUris.isEmpty()) {
                    // Upload images first
                    reviewRepository.uploadImages(selectedImageUris).addOnSuccessListener(urls -> {
                        review.setImageUrls(urls);
                        saveReviewToFirestore(review, dialog, btnSubmit);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Gửi đánh giá");
                    });
                } else {
                    saveReviewToFirestore(review, dialog, btnSubmit);
                }
            });
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void saveReviewToFirestore(Review review, BottomSheetDialog dialog, TextView btnSubmit) {
        reviewRepository.addReview(review).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi gửi đánh giá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Gửi đánh giá");
        });
    }

    private FoodPlace getMockPlace() {
        FoodPlace place = new FoodPlace();
        place.setId("mock_place_1");
        place.setName("Quán Bún Bò O Xuân");
        place.setFoodType("Bún Bò Huế");
        place.setAddress("22Bis Nguyễn Hữu Cảnh, P.19, Q.Bình Thạnh");
        place.setDescription("Hương vị bún bò chuẩn Huế, nước dùng đậm đà, sợi bún mềm dai cùng với topping đa dạng. Không gian quán rộng rãi, sạch sẽ và phục vụ nhanh nhẹn.");
        place.setAverageRating(4.8);
        place.setReviewCount(120);
        place.setOpenTime("06:00");
        place.setCloseTime("22:00");
        place.setStatus("APPROVED");
        return place;
    }
}
