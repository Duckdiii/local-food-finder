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
import com.example.cuisine_finder.adapters.ReviewAdapter;
import com.example.cuisine_finder.models.ExploredPlace;
import com.example.cuisine_finder.models.Favorite;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.models.Review;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.example.cuisine_finder.repositories.ReviewRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FoodPlaceDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PLACE_ID = "placeId";

    private ImageView ivPlaceImage;
    private TextView tvPlaceName, tvStatus, tvRating, tvReviewCount, tvFoodType;
    private TextView tvAddress, tvDescription, tvOpenTime, tvCloseTime;
    private TextView btnFavorite, btnShare, btnCall, btnOrder, btnExplored, btnWriteReview;
    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;

    private FoodPlace currentPlace;
    private ReviewRepository reviewRepository;
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
        interactionRepository = new InteractionRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupToolbar();
        setupRecyclerView();
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
                            loadReviews(); // Load reviews after we have the place
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
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter();
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);
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
        
        if ("APPROVED".equals(place.getStatus())) {
            tvStatus.setText("Đang mở");
        } else {
            tvStatus.setText("Tạm đóng");
        }
    }

    private void setupClickListeners() {
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnExplored.setOnClickListener(v -> toggleExplored());
        btnWriteReview.setOnClickListener(v -> showReviewDialog());
        
        btnShare.setOnClickListener(v -> Toast.makeText(this, "Chia sẻ địa điểm này", Toast.LENGTH_SHORT).show());
        btnCall.setOnClickListener(v -> Toast.makeText(this, "Đang gọi hotline quán...", Toast.LENGTH_SHORT).show());
        btnOrder.setOnClickListener(v -> Toast.makeText(this, "Chuyển đến màn hình đặt món", Toast.LENGTH_SHORT).show());
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

            Review review = new Review();
            review.setPlaceId(currentPlace.getId());
            review.setPlaceName(currentPlace.getName());
            review.setRating(rating);
            review.setComment(comment);
            review.setUserId(currentUserId);
            review.setUserName("Người dùng"); 
            // review.setImageUrls(...); // Here you would upload to Storage first

            reviewRepository.addReview(review).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.setContentView(view);
        dialog.show();
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
