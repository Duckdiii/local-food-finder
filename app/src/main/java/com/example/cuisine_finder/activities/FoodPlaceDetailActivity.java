package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.FoodPlace;

public class FoodPlaceDetailActivity extends AppCompatActivity {

    private ImageView ivPlaceImage;
    private TextView tvPlaceName, tvStatus, tvRating, tvReviewCount, tvFoodType;
    private TextView tvAddress, tvDescription, tvOpenTime, tvCloseTime;
    private TextView btnFavorite, btnShare, btnCall, btnOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_place_detail);

        initViews();
        setupToolbar();
        
        // Demo data (in real app, this would come from Intent)
        loadPlaceDetails(getMockPlace());

        setupClickListeners();
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
        tvRating.setText(String.valueOf(place.getAverageRating()));
        tvReviewCount.setText("(" + place.getReviewCount() + "+ reviews)");
        tvOpenTime.setText("Mở: " + place.getOpenTime());
        tvCloseTime.setText("Đóng: " + place.getCloseTime());
        
        if ("APPROVED".equals(place.getStatus())) {
            tvStatus.setText("Đang mở");
        } else {
            tvStatus.setText("Tạm đóng");
        }
    }

    private void setupClickListeners() {
        btnFavorite.setOnClickListener(v -> Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show());
        btnShare.setOnClickListener(v -> Toast.makeText(this, "Chia sẻ địa điểm này", Toast.LENGTH_SHORT).show());
        btnCall.setOnClickListener(v -> Toast.makeText(this, "Đang gọi hotline quán...", Toast.LENGTH_SHORT).show());
        btnOrder.setOnClickListener(v -> Toast.makeText(this, "Chuyển đến màn hình đặt món", Toast.LENGTH_SHORT).show());
    }

    private FoodPlace getMockPlace() {
        FoodPlace place = new FoodPlace();
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
