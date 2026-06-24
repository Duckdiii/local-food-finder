package com.example.cuisine_finder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cuisine_finder.activities.AchievementsActivity;
import com.example.cuisine_finder.activities.EditProfileActivity;
import com.example.cuisine_finder.activities.FriendsActivity;
import com.example.cuisine_finder.activities.NotificationsActivity;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.FriendshipRepository;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.example.cuisine_finder.repositories.ReviewRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.AuthService;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvEmail, tvAvatarInit;
    private TextView tvExploredCount, tvContributedCount, tvReviewCount;
    private TextView tvFriendCount, tvPendingCount;
    private MaterialCardView cardFriends, cardPendingBadge;
    private View btnNotifications, btnSettings, btnSignOut, btnViewAchievements;

    private InteractionRepository interactionRepository;
    private ReviewRepository reviewRepository;
    private PlaceRepository placeRepository;
    private UserRepository userRepository;
    private FriendshipRepository friendshipRepository;
    private AuthService authService;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        interactionRepository = new InteractionRepository();
        reviewRepository = new ReviewRepository();
        placeRepository = new PlaceRepository();
        userRepository = new UserRepository();
        friendshipRepository = new FriendshipRepository();
        authService = new AuthService();
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        initViews(view);
        loadUserProfile();
        loadStatistics();
        loadFriendStats();

        return view;
    }

    private void initViews(View view) {
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvAvatarInit = view.findViewById(R.id.tvAvatarInit);
        tvExploredCount = view.findViewById(R.id.tvExploredCount);
        tvContributedCount = view.findViewById(R.id.tvContributedCount);
        tvReviewCount = view.findViewById(R.id.tvReviewCount);
        tvFriendCount = view.findViewById(R.id.tvFriendCount);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        cardFriends = view.findViewById(R.id.cardFriends);
        cardPendingBadge = view.findViewById(R.id.cardPendingBadge);

        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnViewAchievements = view.findViewById(R.id.btnViewAchievements);

        cardFriends.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), FriendsActivity.class));
        });

        if (tvExploredCount != null) {
            tvExploredCount.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), com.example.cuisine_finder.activities.ExploredPlacesActivity.class)));
        }

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });
            
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        btnSignOut.setOnClickListener(v -> {
            authService.signOut();
            navigateToSignIn();
        });

        btnViewAchievements.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AchievementsActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        if (currentUserId == null) return;
        
        userRepository.getUser(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                if (user != null) {
                    String name = user.getFullName() != null ? user.getFullName() : "Người dùng";
                    tvFullName.setText(name);
                    tvEmail.setText(user.getEmail());
                    if (!name.isEmpty()) {
                        tvAvatarInit.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    }
                }
            }
        });
    }

    private void loadFriendStats() {
        if (currentUserId == null) return;

        AtomicInteger friendsCount = new AtomicInteger(0);
        AtomicInteger pendingCount = new AtomicInteger(0);
        AtomicInteger queries = new AtomicInteger(2);

        friendshipRepository.getFriendsByRequester(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String status = doc.getString("status");
                    if ("ACCEPTED".equals(status)) friendsCount.incrementAndGet();
                }
            }
            if (queries.decrementAndGet() == 0) updateFriendStatsUI(friendsCount.get(), pendingCount.get());
        });

        friendshipRepository.getFriendsByReceiver(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String status = doc.getString("status");
                    if ("ACCEPTED".equals(status)) friendsCount.incrementAndGet();
                    else if ("PENDING".equals(status)) pendingCount.incrementAndGet();
                }
            }
            if (queries.decrementAndGet() == 0) updateFriendStatsUI(friendsCount.get(), pendingCount.get());
        });
    }

    private void updateFriendStatsUI(int friendCount, int pendingCount) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            tvFriendCount.setText(friendCount + " bạn bè");
            if (pendingCount > 0) {
                cardPendingBadge.setVisibility(View.VISIBLE);
                tvPendingCount.setText(String.valueOf(pendingCount));
            } else {
                cardPendingBadge.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        loadFriendStats();
        loadStatistics();
    }

    private void loadStatistics() {
        if (currentUserId == null) return;

        // Count Explored Places
        interactionRepository.getExploredByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvExploredCount.setText(String.valueOf(value.size()));
                    }
                });

        // Count Contributed Places
        placeRepository.getPlacesByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvContributedCount.setText(String.valueOf(value.size()));
                    }
                });

        // Count Reviews
        reviewRepository.getReviewsByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvReviewCount.setText(String.valueOf(value.size()));
                    }
                });
    }

    private void navigateToSignIn() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SignInFragment())
                    .commit();
        }
    }
}
