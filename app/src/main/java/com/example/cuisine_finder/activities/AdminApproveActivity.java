package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.models.PlaceSubmission;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminApproveActivity extends AppCompatActivity {

    private RecyclerView rvPendingPlaces;
    private TextView tvEmptyStatePending;
    private ApproveAdapter adapter;
    private final List<PlaceSubmission> pendingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approve);

        rvPendingPlaces = findViewById(R.id.rvPendingPlaces);
        tvEmptyStatePending = findViewById(R.id.tvEmptyStatePending);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvPendingPlaces.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApproveAdapter(pendingList);
        rvPendingPlaces.setAdapter(adapter);

        loadPendingSubmissions();
    }

    private void loadPendingSubmissions() {
        FirebaseFirestore.getInstance()
                .collection("place_submissions")
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Không thể tải danh sách chờ duyệt: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    pendingList.clear();
                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            PlaceSubmission sub = doc.toObject(PlaceSubmission.class);
                            if (sub != null) {
                                sub.setId(doc.getId());
                                pendingList.add(sub);
                            }
                        }
                        // Sort locally by createdAt descending to avoid requiring a Firestore composite index
                        pendingList.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    }

                    adapter.notifyDataSetChanged();


                    if (pendingList.isEmpty()) {
                        tvEmptyStatePending.setVisibility(View.VISIBLE);
                        rvPendingPlaces.setVisibility(View.GONE);
                    } else {
                        tvEmptyStatePending.setVisibility(View.GONE);
                        rvPendingPlaces.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void approvePlace(PlaceSubmission sub) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Create a new FoodPlace document
        FoodPlace place = new FoodPlace();
        place.setName(sub.getName());
        place.setDescription(sub.getDescription());
        place.setAddress(sub.getAddress());
        place.setFoodType(sub.getFoodType());
        place.setLatitude(sub.getLatitude());
        place.setLongitude(sub.getLongitude());
        place.setOpenTime(sub.getOpenTime());
        place.setCloseTime(sub.getCloseTime());
        place.setOpenLate(sub.isOpenLate());
        place.setPriceRange(sub.getPriceRange());
        place.setCreatedBy(sub.getSubmittedBy());
        place.setStatus("APPROVED");
        place.setCreatedAt(System.currentTimeMillis());
        place.setUpdatedAt(System.currentTimeMillis());
        place.setAverageRating(0.0);
        place.setReviewCount(0);
        place.setFavoriteCount(0);
        place.setExploredCount(0);

        if (sub.getSignboardImageUrl() != null && !sub.getSignboardImageUrl().isEmpty()) {
            List<String> imgList = new ArrayList<>();
            imgList.add(sub.getSignboardImageUrl());
            place.setImageUrls(imgList);
        }

        // 2. Run batched write or transaction to add place and update submission
        db.collection("food_places")
                .add(place)
                .addOnSuccessListener(ref -> {
                    // Update status in submission doc
                    db.collection("place_submissions")
                            .document(sub.getId())
                            .update("status", "APPROVED")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Đã duyệt quán: " + sub.getName(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi duyệt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectPlace(PlaceSubmission sub) {
        FirebaseFirestore.getInstance()
                .collection("place_submissions")
                .document(sub.getId())
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã từ chối quán: " + sub.getName(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi từ chối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private class ApproveAdapter extends RecyclerView.Adapter<ApproveAdapter.ViewHolder> {

        private final List<PlaceSubmission> list;

        ApproveAdapter(List<PlaceSubmission> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_approve_place, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PlaceSubmission item = list.get(position);

            holder.tvPlaceName.setText(item.getName());
            holder.tvFoodType.setText(item.getFoodType());
            holder.tvAddress.setText("Địa chỉ: " + item.getAddress());
            holder.tvDescription.setText(item.getDescription());
            holder.tvTimeOpenClose.setText(String.format("🕒 %s - %s", item.getOpenTime(), item.getCloseTime()));
            holder.tvPriceRange.setText(String.format("💰 %s", item.getPriceRange()));
            holder.tvCoordinates.setText(String.format(Locale.US, "📍 %.6f, %.6f", item.getLatitude(), item.getLongitude()));

            String submittedInfo = "Đóng góp bởi: " + (item.getSubmittedByName() != null ? item.getSubmittedByName() : "Ẩn danh");
            holder.tvSubmittedBy.setText(submittedInfo);

            if (item.getSignboardImageUrl() != null && !item.getSignboardImageUrl().isEmpty()) {
                holder.cardSignboard.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(item.getSignboardImageUrl())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.ivSignboard);
            } else {
                holder.cardSignboard.setVisibility(View.GONE);
            }

            holder.btnApprove.setOnClickListener(v -> approvePlace(item));
            holder.btnReject.setOnClickListener(v -> rejectPlace(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPlaceName, tvFoodType, tvAddress, tvDescription, tvTimeOpenClose, tvPriceRange, tvCoordinates, tvSubmittedBy;
            TextView btnApprove, btnReject;
            ImageView ivSignboard;
            View cardSignboard;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
                tvFoodType = itemView.findViewById(R.id.tvFoodType);
                tvAddress = itemView.findViewById(R.id.tvAddress);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvTimeOpenClose = itemView.findViewById(R.id.tvTimeOpenClose);
                tvPriceRange = itemView.findViewById(R.id.tvPriceRange);
                tvCoordinates = itemView.findViewById(R.id.tvCoordinates);
                tvSubmittedBy = itemView.findViewById(R.id.tvSubmittedBy);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
                ivSignboard = itemView.findViewById(R.id.ivSignboard);
                cardSignboard = itemView.findViewById(R.id.cardSignboard);
            }
        }
    }
}
