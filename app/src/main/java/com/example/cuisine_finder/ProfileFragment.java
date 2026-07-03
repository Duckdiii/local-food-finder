package com.example.cuisine_finder;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.cuisine_finder.activities.AchievementsActivity;
import com.example.cuisine_finder.activities.EditProfileActivity;
import com.example.cuisine_finder.activities.FriendsActivity;
import com.example.cuisine_finder.activities.NotificationsActivity;
import com.example.cuisine_finder.activities.AdminOrdersActivity;
import com.example.cuisine_finder.activities.MyOrdersActivity;
import com.example.cuisine_finder.activities.ShipperOrdersActivity;
import com.example.cuisine_finder.models.Friendship;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.models.UserRole;
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
    private ImageView ivAvatarReal;
    private View avatarContainer;
    private ProgressBar avatarUploadProgress;
    private SwipeRefreshLayout swipeRefreshProfile;
    private TextView tvExploredCount, tvContributedCount, tvReviewCount;
    private TextView tvFriendCount, tvPendingCount, tvOrderManagementTitle;
    private MaterialCardView cardFriends, cardPendingBadge;
    private View btnNotifications, btnSettings, btnSignOut, btnViewAchievements,btnCustomerOrders, separatorCustomerOrders, btnOrderManagement, separatorAdmin;
    private View btnContributePlace, btnStatistics, btnAdminApprove, separatorAdminApprove;

    private InteractionRepository interactionRepository;
    private ReviewRepository reviewRepository;
    private PlaceRepository placeRepository;
    private UserRepository userRepository;
    private FriendshipRepository friendshipRepository;
    private AuthService authService;
    private String currentUserId;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatarFromUri(uri);
            });

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
        refreshAllData();

        return view;
    }

    private void initViews(View view) {
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvAvatarInit = view.findViewById(R.id.tvAvatarInit);
        swipeRefreshProfile = view.findViewById(R.id.swipeRefreshProfile);
        if (swipeRefreshProfile != null) {
            swipeRefreshProfile.setColorSchemeColors(
                    ContextCompat.getColor(requireContext(), R.color.orange_main)
            );
            swipeRefreshProfile.setOnRefreshListener(() -> {
                refreshAllData();
            });
        }

        ivAvatarReal = view.findViewById(R.id.ivAvatarReal);
        avatarContainer = view.findViewById(R.id.avatarContainer);
        avatarUploadProgress = view.findViewById(R.id.avatarUploadProgress);

        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        }

        tvExploredCount = view.findViewById(R.id.tvExploredCount);
        tvContributedCount = view.findViewById(R.id.tvContributedCount);
        tvReviewCount = view.findViewById(R.id.tvReviewCount);
        tvFriendCount = view.findViewById(R.id.tvFriendCount);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvOrderManagementTitle = view.findViewById(R.id.tvOrderManagementTitle);
        cardFriends = view.findViewById(R.id.cardFriends);
        cardPendingBadge = view.findViewById(R.id.cardPendingBadge);

        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnSignOut = view.findViewById(R.id.btnSignOut);
        btnCustomerOrders = view.findViewById(R.id.btnCustomerOrders);
        separatorCustomerOrders = view.findViewById(R.id.separatorCustomerOrders);
        btnOrderManagement = view.findViewById(R.id.btnOrderManagement);
        separatorAdmin = view.findViewById(R.id.separatorAdmin);
        btnViewAchievements = view.findViewById(R.id.btnViewAchievements);
        btnContributePlace = view.findViewById(R.id.btnContributePlace);
        btnStatistics = view.findViewById(R.id.btnStatistics);
        btnAdminApprove = view.findViewById(R.id.btnAdminApprove);
        separatorAdminApprove = view.findViewById(R.id.separatorAdminApprove);

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

        btnCustomerOrders.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyOrdersActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        btnSignOut.setOnClickListener(v -> {
            authService.signOut();
            if (getActivity() instanceof com.example.cuisine_finder.activities.MainActivity) {
                ((com.example.cuisine_finder.activities.MainActivity) getActivity()).updateBottomNavVisibility();
            }
            navigateToSignIn();
        });

        btnViewAchievements.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AchievementsActivity.class);
            startActivity(intent);
        });

        btnContributePlace.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new AddRestaurantFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnStatistics.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.example.cuisine_finder.activities.StatisticsActivity.class));
        });

        btnAdminApprove.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.example.cuisine_finder.activities.AdminApproveActivity.class));
        });
    }

    private void loadUserProfile() { loadUserProfileWithCallback(null); }

    private void loadUserProfileWithCallback(Runnable onDone) {
        if (currentUserId == null) { if (onDone != null) onDone.run(); return; }

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

                    // Load real avatar photo if exists
                    String avatarUrl = user.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty() && ivAvatarReal != null && isAdded()) {
                        ivAvatarReal.setVisibility(View.VISIBLE);
                        tvAvatarInit.setVisibility(View.GONE);
                        Glide.with(this)
                                .load(avatarUrl)
                                .transform(new CircleCrop())
                                .placeholder(R.drawable.bg_image_placeholder)
                                .into(ivAvatarReal);
                    } else if (ivAvatarReal != null) {
                        ivAvatarReal.setVisibility(View.GONE);
                        tvAvatarInit.setVisibility(View.VISIBLE);
                    }

                    boolean isAdmin = "SYSTEM_ADMIN".equals(user.getRole());
                    if (btnAdminApprove != null) {
                        btnAdminApprove.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    }
                    if (separatorAdminApprove != null) {
                        separatorAdminApprove.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                    }
                    updateOrderManagementVisibility(user);
                }
            }
            if (onDone != null) onDone.run();
        });
    }

    private void refreshAllData() {
        // Track when all 3 async loads are done so we can stop the spinner together
        AtomicInteger pendingLoads = new AtomicInteger(3);
        Runnable onLoadDone = () -> {
            if (pendingLoads.decrementAndGet() == 0) {
                if (swipeRefreshProfile != null) {
                    swipeRefreshProfile.post(() -> swipeRefreshProfile.setRefreshing(false));
                }
            }
        };

        loadUserProfileWithCallback(onLoadDone);
        loadStatisticsWithCallback(onLoadDone);
        loadFriendStatsWithCallback(onLoadDone);
    }

    private void uploadAvatarFromUri(Uri uri) {
        if (currentUserId == null || !isAdded()) return;

        // Show uploading indicator
        if (avatarUploadProgress != null) avatarUploadProgress.setVisibility(View.VISIBLE);
        if (avatarContainer != null) avatarContainer.setAlpha(0.6f);

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("avatars/" + currentUserId + ".jpg");

        storageRef.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                    return storageRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUrl -> {
                    if (!isAdded()) return;
                    String url = downloadUrl.toString();

                    // Update Firestore
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUserId)
                            .update("avatarUrl", url)
                            .addOnSuccessListener(v -> {
                                if (!isAdded()) return;
                                // Show new avatar immediately
                                if (ivAvatarReal != null) {
                                    ivAvatarReal.setVisibility(View.VISIBLE);
                                    tvAvatarInit.setVisibility(View.GONE);
                                    Glide.with(this)
                                            .load(url)
                                            .transform(new CircleCrop())
                                            .into(ivAvatarReal);
                                }
                                Toast.makeText(getContext(), "Đã cập nhật ảnh đại diện! 🎉", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Lỗi lưu URL: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );

                    if (avatarUploadProgress != null) avatarUploadProgress.setVisibility(View.GONE);
                    if (avatarContainer != null) avatarContainer.setAlpha(1f);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (avatarUploadProgress != null) avatarUploadProgress.setVisibility(View.GONE);
                    if (avatarContainer != null) avatarContainer.setAlpha(1f);
                    Toast.makeText(getContext(), "Upload thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateOrderManagementVisibility(User user) {
        if (btnCustomerOrders == null || btnOrderManagement == null || separatorAdmin == null) return;
        
        // Luôn hiện nút "Đơn hàng của tôi" cho mọi role
        btnCustomerOrders.setVisibility(View.VISIBLE);
        if (separatorCustomerOrders != null) {
            separatorCustomerOrders.setVisibility(View.VISIBLE);
        }

        boolean isMerchant = user != null && UserRole.isMerchant(user.getRole());
        boolean isShipper = user != null && UserRole.isShipper(user.getRole());
        boolean merchantHasRestaurants = isMerchant
                && user.getManagedRestaurantIds() != null
                && !user.getManagedRestaurantIds().isEmpty();

        boolean showOrderEntry = merchantHasRestaurants || isShipper;
        btnOrderManagement.setVisibility(showOrderEntry ? View.VISIBLE : View.GONE);
        separatorAdmin.setVisibility(showOrderEntry ? View.VISIBLE : View.GONE);

        if (!showOrderEntry) {
            btnOrderManagement.setOnClickListener(null);
            return;
        }
        tvOrderManagementTitle.setText(merchantHasRestaurants ? "Đơn hàng nhà hàng" : "Đơn giao hàng");
        btnOrderManagement.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), merchantHasRestaurants ? AdminOrdersActivity.class : ShipperOrdersActivity.class);
            startActivity(intent);
        });
    }

    private void loadFriendStats() { loadFriendStatsWithCallback(null); }

    private void loadFriendStatsWithCallback(Runnable onDone) {
        if (currentUserId == null) { if (onDone != null) onDone.run(); return; }

        AtomicInteger friendsCount = new AtomicInteger(0);
        AtomicInteger pendingCount = new AtomicInteger(0);
        AtomicInteger queries = new AtomicInteger(2);
        Runnable checkDone = () -> {
            if (queries.decrementAndGet() == 0) {
                updateFriendStatsUI(friendsCount.get(), pendingCount.get());
                if (onDone != null) onDone.run();
            }
        };

        friendshipRepository.getFriendsByRequester(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String status = doc.getString("status");
                    if ("ACCEPTED".equals(status)) friendsCount.incrementAndGet();
                }
            }
            checkDone.run();
        });

        friendshipRepository.getFriendsByReceiver(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String status = doc.getString("status");
                    if ("ACCEPTED".equals(status)) friendsCount.incrementAndGet();
                    else if ("PENDING".equals(status)) pendingCount.incrementAndGet();
                }
            }
            checkDone.run();
        });
    }

    private void updateFriendStatsUI(int friendCount, int pendingCount) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            animateCounter(tvFriendCount, friendCount);
            // Show the "X bạn bè" suffix after the animation finishes
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded()) tvFriendCount.setText(friendCount + " bạn bè");
            }, 820);
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
        refreshAllData();
    }

    private void loadStatistics() { loadStatisticsWithCallback(null); }

    private void loadStatisticsWithCallback(Runnable onDone) {
        if (currentUserId == null) { if (onDone != null) onDone.run(); return; }

        AtomicInteger pending = new AtomicInteger(3);
        Runnable checkDone = () -> { if (pending.decrementAndGet() == 0 && onDone != null) onDone.run(); };

        // Count Explored Places
        interactionRepository.getExploredByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) animateCounter(tvExploredCount, value.size());
                    checkDone.run();
                });

        // Count Contributed Places
        placeRepository.getPlacesByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) animateCounter(tvContributedCount, value.size());
                    checkDone.run();
                });

        // Count Reviews
        reviewRepository.getReviewsByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) animateCounter(tvReviewCount, value.size());
                    checkDone.run();
                });
    }

    /**
     * Animates a TextView's numeric text from 0 up to [target] over 800ms
     * using a decelerate interpolator for a satisfying "counting up" feel.
     */
    private void animateCounter(TextView textView, int target) {
        if (textView == null || !isAdded()) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            ValueAnimator animator = ValueAnimator.ofInt(0, target);
            animator.setDuration(800);
            animator.setInterpolator(new DecelerateInterpolator(1.5f));
            animator.addUpdateListener(anim -> {
                if (isAdded()) textView.setText(String.valueOf(anim.getAnimatedValue()));
            });
            animator.start();
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
