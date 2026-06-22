package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Friendship;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.FriendshipRepository;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.example.cuisine_finder.repositories.ReviewRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.concurrent.atomic.AtomicInteger;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private TextView tvAvatarInit, tvFullName, tvEmail, tvFriendBadge;
    private TextView tvExploredCount, tvContributedCount, tvReviewCount;
    private LinearLayout btnUnfriend, btnAddFriend, layoutPending, layoutAcceptRequest;
    private View dividerUnfriend;
    private TextView btnCancelRequest, btnAcceptRequest, btnRejectRequest;

    private UserRepository userRepository;
    private FriendshipRepository friendshipRepository;
    private InteractionRepository interactionRepository;
    private PlaceRepository placeRepository;
    private ReviewRepository reviewRepository;

    private String currentUserId;
    private String targetUserId;
    private Friendship currentFriendship;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (targetUserId == null) { finish(); return; }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        userRepository = new UserRepository();
        friendshipRepository = new FriendshipRepository();
        interactionRepository = new InteractionRepository();
        placeRepository = new PlaceRepository();
        reviewRepository = new ReviewRepository();

        initViews();
        loadProfile();
        loadStats();
        loadFriendshipStatus();
    }

    private void initViews() {
        tvAvatarInit = findViewById(R.id.tvAvatarInit);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvFriendBadge = findViewById(R.id.tvFriendBadge);
        tvExploredCount = findViewById(R.id.tvExploredCount);
        tvContributedCount = findViewById(R.id.tvContributedCount);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        btnUnfriend = findViewById(R.id.btnUnfriend);
        dividerUnfriend = findViewById(R.id.dividerUnfriend);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        layoutPending = findViewById(R.id.layoutPending);
        layoutAcceptRequest = findViewById(R.id.layoutAcceptRequest);
        btnCancelRequest = findViewById(R.id.btnCancelRequest);
        btnAcceptRequest = findViewById(R.id.btnAcceptRequest);
        btnRejectRequest = findViewById(R.id.btnRejectRequest);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        btnUnfriend.setOnClickListener(v -> showUnfriendDialog());
        btnAddFriend.setOnClickListener(v -> sendFriendRequest());
        btnCancelRequest.setOnClickListener(v -> cancelFriendRequest());
        btnAcceptRequest.setOnClickListener(v -> acceptFriendRequest());
        btnRejectRequest.setOnClickListener(v -> rejectFriendRequest());
    }

    private void loadProfile() {
        userRepository.getUser(targetUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                if (user != null) {
                    String name = user.getFullName() != null ? user.getFullName() : "Người dùng";
                    tvFullName.setText(name);
                    tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                    tvAvatarInit.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());
                }
            }
        });
    }

    private void loadStats() {
        interactionRepository.getExploredByUser(targetUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        tvExploredCount.setText(String.valueOf(task.getResult().size()));
                    }
                });

        placeRepository.getPlacesByUser(targetUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        tvContributedCount.setText(String.valueOf(task.getResult().size()));
                    }
                });

        reviewRepository.getReviewsByUser(targetUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        tvReviewCount.setText(String.valueOf(task.getResult().size()));
                    }
                });
    }

    private void loadFriendshipStatus() {
        if (currentUserId == null) return;

        AtomicInteger pending = new AtomicInteger(2);

        friendshipRepository.getFriendsByRequester(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    if (targetUserId.equals(doc.getString("receiverId"))) {
                        Friendship f = doc.toObject(Friendship.class);
                        if (f != null) { f.setId(doc.getId()); currentFriendship = f; }
                    }
                }
            }
            if (pending.decrementAndGet() == 0) runOnUiThread(this::applyFriendshipUI);
        });

        friendshipRepository.getFriendsByReceiver(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    if (targetUserId.equals(doc.getString("requesterId"))) {
                        Friendship f = doc.toObject(Friendship.class);
                        if (f != null) { f.setId(doc.getId()); currentFriendship = f; }
                    }
                }
            }
            if (pending.decrementAndGet() == 0) runOnUiThread(this::applyFriendshipUI);
        });
    }

    private void applyFriendshipUI() {
        // Hide all action rows first
        btnUnfriend.setVisibility(View.GONE);
        dividerUnfriend.setVisibility(View.GONE);
        btnAddFriend.setVisibility(View.GONE);
        layoutPending.setVisibility(View.GONE);
        layoutAcceptRequest.setVisibility(View.GONE);
        tvFriendBadge.setVisibility(View.GONE);

        if (currentFriendship == null) {
            btnAddFriend.setVisibility(View.VISIBLE);
            return;
        }

        if (Friendship.STATUS_ACCEPTED.equals(currentFriendship.getStatus())) {
            tvFriendBadge.setVisibility(View.VISIBLE);
            btnUnfriend.setVisibility(View.VISIBLE);
            dividerUnfriend.setVisibility(View.VISIBLE);
        } else if (Friendship.STATUS_PENDING.equals(currentFriendship.getStatus())) {
            if (currentUserId.equals(currentFriendship.getRequesterId())) {
                // I sent the request
                layoutPending.setVisibility(View.VISIBLE);
            } else {
                // They sent the request, I need to accept/reject
                layoutAcceptRequest.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showUnfriendDialog() {
        String name = tvFullName.getText().toString();
        new AlertDialog.Builder(this)
                .setTitle("Hủy kết bạn")
                .setMessage("Bạn có chắc muốn hủy kết bạn với " + name + "?")
                .setPositiveButton("Hủy kết bạn", (d, w) -> doUnfriend())
                .setNegativeButton("Không", null)
                .show();
    }

    private void doUnfriend() {
        if (currentFriendship == null) return;
        friendshipRepository.deleteFriendship(currentFriendship.getId())
                .addOnSuccessListener(v -> {
                    currentFriendship = null;
                    applyFriendshipUI();
                    Toast.makeText(this, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void sendFriendRequest() {
        if (currentUserId == null) return;
        friendshipRepository.sendFriendRequest(currentUserId, targetUserId)
                .addOnSuccessListener(ref -> {
                    Friendship f = new Friendship();
                    f.setId(ref.getId());
                    f.setRequesterId(currentUserId);
                    f.setReceiverId(targetUserId);
                    f.setStatus(Friendship.STATUS_PENDING);
                    currentFriendship = f;
                    applyFriendshipUI();
                    Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void cancelFriendRequest() {
        if (currentFriendship == null) return;
        friendshipRepository.deleteFriendship(currentFriendship.getId())
                .addOnSuccessListener(v -> {
                    currentFriendship = null;
                    applyFriendshipUI();
                    Toast.makeText(this, "Đã hủy lời mời", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void acceptFriendRequest() {
        if (currentFriendship == null) return;
        friendshipRepository.acceptFriendRequest(currentFriendship.getId())
                .addOnSuccessListener(v -> {
                    currentFriendship.setStatus(Friendship.STATUS_ACCEPTED);
                    applyFriendshipUI();
                    Toast.makeText(this, "Đã chấp nhận lời mời kết bạn", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void rejectFriendRequest() {
        if (currentFriendship == null) return;
        friendshipRepository.deleteFriendship(currentFriendship.getId())
                .addOnSuccessListener(v -> {
                    currentFriendship = null;
                    applyFriendshipUI();
                    Toast.makeText(this, "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }
}
