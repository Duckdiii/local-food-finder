package com.example.cuisine_finder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.example.cuisine_finder.repositories.ReviewRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvEmail;
    private TextView tvExploredCount, tvContributedCount, tvReviewCount;
    private InteractionRepository interactionRepository;
    private ReviewRepository reviewRepository;
    private PlaceRepository placeRepository;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        interactionRepository = new InteractionRepository();
        reviewRepository = new ReviewRepository();
        placeRepository = new PlaceRepository();
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        initViews(view);
        setupUserData(user);
        loadStatistics();

        return view;
    }

    private void initViews(View view) {
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvExploredCount = view.findViewById(R.id.tvExploredCount);
        tvContributedCount = view.findViewById(R.id.tvContributedCount);
        tvReviewCount = view.findViewById(R.id.tvReviewCount);
        
        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            // Handle navigation back to sign in if needed
        });
    }

    private void setupUserData(FirebaseUser user) {
        if (user != null) {
            tvFullName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");
            tvEmail.setText(user.getEmail());
        }
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
}
