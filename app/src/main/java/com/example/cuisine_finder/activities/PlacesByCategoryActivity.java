package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.PlacesByCategoryAdapter;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PlacesByCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_NAME = "category_name";
    public static final String EXTRA_CATEGORY_DESCRIPTION = "category_description";
    public static final String EXTRA_CATEGORY_ICON_URL = "category_icon_url";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_MIN_RATING = "min_rating";
    public static final String MODE_FEATURED = "featured";

    private TextView tvCategoryName, tvCategoryDescription, tvCategoryEmoji, tvPlaceCount;
    private ImageView ivCategoryHero;
    private RecyclerView rvPlaces;
    private LinearLayout layoutEmpty;
    private PlacesByCategoryAdapter adapter;
    private PlaceRepository placeRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_by_category);

        placeRepository = new PlaceRepository();

        initViews();
        loadIntentData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvCategoryDescription = findViewById(R.id.tvCategoryDescription);
        tvCategoryEmoji = findViewById(R.id.tvCategoryEmoji);
        tvPlaceCount = findViewById(R.id.tvPlaceCount);
        ivCategoryHero = findViewById(R.id.ivCategoryHero);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvPlaces = findViewById(R.id.rvPlaces);

        adapter = new PlacesByCategoryAdapter();
        rvPlaces.setLayoutManager(new LinearLayoutManager(this));
        rvPlaces.setAdapter(adapter);

        adapter.setOnPlaceClickListener(place -> {
            Intent intent = new Intent(this, FoodPlaceDetailActivity.class);
            if (place.getId() != null) {
                intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, place.getId());
            }
            startActivity(intent);
        });
    }

    private void loadIntentData() {
        String mode = getIntent().getStringExtra(EXTRA_MODE);

        if (MODE_FEATURED.equals(mode)) {
            // Mode: Gợi ý siêu hot — hiển thị quán top rating
            double minRating = getIntent().getDoubleExtra(EXTRA_MIN_RATING, 4.5);
            tvCategoryName.setText("Gợi ý siêu hot 🔥");
            tvCategoryDescription.setText("Các quán được đánh giá cao nhất");
            tvCategoryDescription.setVisibility(View.VISIBLE);
            tvCategoryEmoji.setText("🔥");
            ivCategoryHero.setVisibility(View.GONE);
            loadTopRatedPlaces(minRating);
            return;
        }

        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        String description = getIntent().getStringExtra(EXTRA_CATEGORY_DESCRIPTION);
        String iconUrl = getIntent().getStringExtra(EXTRA_CATEGORY_ICON_URL);

        if (categoryName == null) {
            finish();
            return;
        }

        tvCategoryName.setText(categoryName);

        if (description != null && !description.isEmpty()) {
            tvCategoryDescription.setText(description);
            tvCategoryDescription.setVisibility(View.VISIBLE);
        } else {
            tvCategoryDescription.setVisibility(View.GONE);
        }

        tvCategoryEmoji.setText(getEmojiForCategory(categoryName));

        if (iconUrl != null && !iconUrl.isEmpty()) {
            ivCategoryHero.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(iconUrl)
                    .centerCrop()
                    .into(ivCategoryHero);
        }

        loadPlacesByCategory(categoryName);
    }

    private void loadPlacesByCategory(String categoryName) {
        tvPlaceCount.setText("Đang tải...");

        placeRepository.getPlacesByFoodType(categoryName)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<FoodPlace> places = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            FoodPlace place = doc.toObject(FoodPlace.class);
                            if (place != null) {
                                place.setId(doc.getId());
                                places.add(place);
                            }
                        }

                        adapter.setPlaces(places);

                        int count = places.size();
                        tvPlaceCount.setText(count + " quán");

                        if (count == 0) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            rvPlaces.setVisibility(View.GONE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                            rvPlaces.setVisibility(View.VISIBLE);
                        }
                    } else {
                        tvPlaceCount.setText("0 quán");
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvPlaces.setVisibility(View.GONE);
                    }
                });
    }

    private void loadTopRatedPlaces(double minRating) {
        tvPlaceCount.setText("Đang tải...");

        placeRepository.getTopRatedPlaces(minRating)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<FoodPlace> places = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            FoodPlace place = doc.toObject(FoodPlace.class);
                            if (place != null) {
                                place.setId(doc.getId());
                                places.add(place);
                            }
                        }

                        adapter.setPlaces(places);

                        int count = places.size();
                        tvPlaceCount.setText(count + " quán");

                        if (count == 0) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            rvPlaces.setVisibility(View.GONE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                            rvPlaces.setVisibility(View.VISIBLE);
                        }
                    } else {
                        tvPlaceCount.setText("0 quán");
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvPlaces.setVisibility(View.GONE);
                    }
                });
    }

    private String getEmojiForCategory(String name) {
        if (name == null) return "🍲";
        String lower = name.toLowerCase();
        if (lower.contains("coffee") || lower.contains("cà phê") || lower.contains("cafe")) return "☕";
        if (lower.contains("phở") || lower.contains("bún") || lower.contains("hủ tiếu")) return "🍜";
        if (lower.contains("cơm")) return "🍚";
        if (lower.contains("lẩu")) return "🫕";
        if (lower.contains("ốc") || lower.contains("hải sản")) return "🦪";
        if (lower.contains("bánh") || lower.contains("bake")) return "🥐";
        if (lower.contains("nướng") || lower.contains("bbq") || lower.contains("grills")) return "🍖";
        if (lower.contains("kem") || lower.contains("ice cream")) return "🍦";
        if (lower.contains("trà sữa") || lower.contains("milk tea") || lower.contains("bubble")) return "🧋";
        if (lower.contains("trà") || lower.contains("tea")) return "🍵";
        if (lower.contains("pizza")) return "🍕";
        if (lower.contains("burger") || lower.contains("hamburger")) return "🍔";
        if (lower.contains("sushi") || lower.contains("nhật")) return "🍣";
        if (lower.contains("chè")) return "🍡";
        if (lower.contains("gà") || lower.contains("chicken")) return "🍗";
        if (lower.contains("chay") || lower.contains("vegetarian")) return "🥗";
        if (lower.contains("xôi")) return "🍙";
        return "🍲";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
