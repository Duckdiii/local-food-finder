package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Badge;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    private RecyclerView rvAchievements;
    private BadgeAdapter adapter;
    private List<Badge> badges = new ArrayList<>();
    private UserRepository userRepository;
    private PlaceRepository placeRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        userRepository = new UserRepository();
        placeRepository = new PlaceRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        rvAchievements = findViewById(R.id.rvAchievements);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupBadges();
        setupRecyclerView();
        loadUserStats();
    }

    private void setupBadges() {
        badges.add(new Badge("exp_1", "Nhà khám phá", "Ghé thăm 10 quán ăn", "🧭", 10, "EXPLORED"));
        badges.add(new Badge("rev_1", "Reviewer Pro", "Viết 20 đánh giá", "✍️", 20, "REVIEW"));
        badges.add(new Badge("share_1", "Người chia sẻ", "Đóng góp 5 quán ăn mới", "🤝", 5, "SHARE"));
        badges.add(new Badge("exp_2", "Siêu cấp thám hiểm", "Ghé thăm 50 quán ăn", "🚀", 50, "EXPLORED"));
    }

    private void setupRecyclerView() {
        adapter = new BadgeAdapter(badges);
        rvAchievements.setLayoutManager(new LinearLayoutManager(this));
        rvAchievements.setAdapter(adapter);
    }

    private void loadUserStats() {
        if (currentUserId == null) return;
        userRepository.getUser(currentUserId).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                // Fetch real contribution count from Firestore
                placeRepository.getPlacesByUser(currentUserId).get().addOnSuccessListener(placesSnapshot -> {
                    int contributionCount = placesSnapshot.size();
                    updateBadgeStatus(user, contributionCount);
                });
            }
        });
    }

    private void updateBadgeStatus(User user, int contributionCount) {
        for (Badge badge : badges) {
            if ("EXPLORED".equals(badge.getRequirementType())) {
                badge.setEarned(user.getExploredCount() >= badge.getRequirementValue());
            } else if ("REVIEW".equals(badge.getRequirementType())) {
                badge.setEarned(user.getReviewCount() >= badge.getRequirementValue());
            } else if ("SHARE".equals(badge.getRequirementType())) {
                // Use actual contribution count instead of favoriteCount
                badge.setEarned(contributionCount >= badge.getRequirementValue());
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.VH> {
        private List<Badge> items;
        BadgeAdapter(List<Badge> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Badge badge = items.get(position);
            holder.tvIcon.setText(badge.getIconEmoji());
            holder.tvName.setText(badge.getName());
            holder.tvDesc.setText(badge.getDescription());
            holder.tvStatus.setText(badge.isEarned() ? "✅" : "🔒");
            holder.itemView.setAlpha(badge.isEarned() ? 1.0f : 0.6f);
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvDesc, tvStatus;
            VH(View v) {
                super(v);
                tvIcon = v.findViewById(R.id.tvBadgeIcon);
                tvName = v.findViewById(R.id.tvBadgeName);
                tvDesc = v.findViewById(R.id.tvBadgeDescription);
                tvStatus = v.findViewById(R.id.tvBadgeStatus);
            }
        }
    }
}
