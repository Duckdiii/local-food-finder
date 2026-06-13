package com.example.cuisine_finder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cuisine_finder.activities.EditProfileActivity;
import com.example.cuisine_finder.activities.NotificationsActivity;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.example.cuisine_finder.repositories.ReviewRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvEmail, tvAvatarInit;
    private TextView tvExploredCount, tvContributedCount, tvReviewCount;
    private TextView btnNotifications, btnSettings, btnSignOut;
    
    private InteractionRepository interactionRepository;
    private ReviewRepository reviewRepository;
    private PlaceRepository placeRepository;
    private UserRepository userRepository;
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
        authService = new AuthService();
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        initViews(view);
        loadUserProfile();
        loadStatistics();

        return view;
    }

    private void initViews(View view) {
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvAvatarInit = view.findViewById(R.id.tvAvatarInit);
        tvExploredCount = view.findViewById(R.id.tvExploredCount);
        tvContributedCount = view.findViewById(R.id.tvContributedCount);
        tvReviewCount = view.findViewById(R.id.tvReviewCount);
        
        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnSignOut = view.findViewById(R.id.btnSignOut);

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

    @Override
    public void onResume() {
        super.onResume();
        // Reload profile data in case it was edited
        loadUserProfile();
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

