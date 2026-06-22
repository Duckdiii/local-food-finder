package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.ExploredPlace;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExploredPlacesActivity extends AppCompatActivity {

    private RecyclerView rvExploredPlaces;
    private LinearLayout layoutExploredEmpty;
    private TextView tvExploredSubtitle;
    private ExploredAdapter adapter;
    private InteractionRepository interactionRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explored_places);

        interactionRepository = new InteractionRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvExploredPlaces = findViewById(R.id.rvExploredPlaces);
        layoutExploredEmpty = findViewById(R.id.layoutExploredEmpty);
        tvExploredSubtitle = findViewById(R.id.tvExploredSubtitle);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new ExploredAdapter();
        rvExploredPlaces.setLayoutManager(new LinearLayoutManager(this));
        rvExploredPlaces.setAdapter(adapter);

        loadExploredPlaces();
    }

    private void loadExploredPlaces() {
        if (currentUserId == null) {
            showEmpty();
            return;
        }

        interactionRepository.getExploredByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    List<ExploredPlace> places = value.toObjects(ExploredPlace.class);
                    adapter.setItems(places);
                    tvExploredSubtitle.setText(places.size() + " quán đã ghé thăm");
                    if (places.isEmpty()) showEmpty();
                    else hideEmpty();
                });
    }

    private void showEmpty() {
        layoutExploredEmpty.setVisibility(View.VISIBLE);
        rvExploredPlaces.setVisibility(View.GONE);
    }

    private void hideEmpty() {
        layoutExploredEmpty.setVisibility(View.GONE);
        rvExploredPlaces.setVisibility(View.VISIBLE);
    }

    private class ExploredAdapter extends RecyclerView.Adapter<ExploredAdapter.VH> {
        private final List<ExploredPlace> items = new ArrayList<>();

        void setItems(List<ExploredPlace> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_explored_place, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvName, tvType, tvAddress, tvVisitCount, tvLastVisit;

            VH(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivExploredImage);
                tvName = itemView.findViewById(R.id.tvExploredName);
                tvType = itemView.findViewById(R.id.tvExploredType);
                tvAddress = itemView.findViewById(R.id.tvExploredAddress);
                tvVisitCount = itemView.findViewById(R.id.tvExploredVisitCount);
                tvLastVisit = itemView.findViewById(R.id.tvExploredLastVisit);
            }

            void bind(ExploredPlace place) {
                tvName.setText(TextUtils.isEmpty(place.getPlaceName()) ? "Quán ăn" : place.getPlaceName());

                if (!TextUtils.isEmpty(place.getFoodType())) {
                    tvType.setText(place.getFoodType());
                    tvType.setVisibility(View.VISIBLE);
                } else {
                    tvType.setVisibility(View.GONE);
                }

                if (!TextUtils.isEmpty(place.getAddress())) {
                    tvAddress.setText("📍 " + place.getAddress());
                    tvAddress.setVisibility(View.VISIBLE);
                } else {
                    tvAddress.setVisibility(View.GONE);
                }

                int visits = place.getVisitCount() > 0 ? place.getVisitCount() : 1;
                tvVisitCount.setText(visits + " lần ghé");
                tvLastVisit.setText("Lần cuối: " + formatDate(place.getLastVisitedAt()));

                if (!TextUtils.isEmpty(place.getPlaceImageUrl())) {
                    Glide.with(itemView.getContext())
                            .load(place.getPlaceImageUrl())
                            .placeholder(R.drawable.bg_image_placeholder)
                            .centerCrop()
                            .into(ivImage);
                } else {
                    ivImage.setImageResource(R.drawable.bg_image_placeholder);
                }

                itemView.setOnClickListener(v -> {
                    if (!TextUtils.isEmpty(place.getPlaceId())) {
                        Intent intent = new Intent(itemView.getContext(), FoodPlaceDetailActivity.class);
                        intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, place.getPlaceId());
                        itemView.getContext().startActivity(intent);
                    }
                });
            }

            private String formatDate(long millis) {
                if (millis <= 0) return "chưa rõ";
                long diff = System.currentTimeMillis() - millis;
                long day = 24 * 60 * 60 * 1000L;
                if (diff < day) return "hôm nay";
                if (diff < 2 * day) return "hôm qua";
                if (diff < 7 * day) return (diff / day) + " ngày trước";
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(millis));
            }
        }
    }
}
