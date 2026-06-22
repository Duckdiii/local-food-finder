package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.FriendListAdapter;
import com.example.cuisine_finder.adapters.FriendRequestAdapter;
import com.example.cuisine_finder.models.Friendship;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.FriendshipRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FriendsActivity extends AppCompatActivity {

    private TextView tabFriends, tabRequests, tvFriendCountHeader, btnAddFriend;
    private RecyclerView rvFriends, rvRequests;
    private LinearLayout layoutEmptyFriends, layoutEmptyRequests;

    private FriendListAdapter friendListAdapter;
    private FriendRequestAdapter friendRequestAdapter;

    private FriendshipRepository friendshipRepository;
    private UserRepository userRepository;
    private String currentUserId;

    // Data
    private final List<User> friendUsers = new ArrayList<>();
    private final List<Friendship> friendFriendships = new ArrayList<>();
    private final List<User> requestUsers = new ArrayList<>();
    private final List<Friendship> requestFriendships = new ArrayList<>();

    private boolean showingFriends = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        currentUserId = user.getUid();

        friendshipRepository = new FriendshipRepository();
        userRepository = new UserRepository();

        initViews();
        loadData();
    }

    private void initViews() {
        tabFriends = findViewById(R.id.tabFriends);
        tabRequests = findViewById(R.id.tabRequests);
        tvFriendCountHeader = findViewById(R.id.tvFriendCountHeader);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        rvFriends = findViewById(R.id.rvFriends);
        rvRequests = findViewById(R.id.rvRequests);
        layoutEmptyFriends = findViewById(R.id.layoutEmptyFriends);
        layoutEmptyRequests = findViewById(R.id.layoutEmptyRequests);

        friendListAdapter = new FriendListAdapter();
        friendListAdapter.setOnUnfriendListener((user, friendship) -> showUnfriendDialog(user, friendship));
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(friendListAdapter);

        friendRequestAdapter = new FriendRequestAdapter();
        friendRequestAdapter.setOnRequestActionListener(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(User user, Friendship friendship) {
                acceptRequest(user, friendship);
            }
            @Override
            public void onReject(User user, Friendship friendship) {
                rejectRequest(user, friendship);
            }
        });
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(friendRequestAdapter);

        tabFriends.setOnClickListener(v -> switchTab(true));
        tabRequests.setOnClickListener(v -> switchTab(false));

        btnAddFriend.setOnClickListener(v -> {
            startActivity(new Intent(this, UserSearchActivity.class));
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void switchTab(boolean showFriends) {
        showingFriends = showFriends;
        if (showFriends) {
            tabFriends.setBackgroundResource(R.drawable.bg_orange_button);
            tabFriends.setTextColor(getColor(R.color.white));
            tabFriends.setTypeface(null, android.graphics.Typeface.BOLD);
            tabRequests.setBackground(null);
            tabRequests.setTextColor(getColor(R.color.text_gray));
            tabRequests.setTypeface(null, android.graphics.Typeface.NORMAL);
            updateFriendsView();
        } else {
            tabRequests.setBackgroundResource(R.drawable.bg_orange_button);
            tabRequests.setTextColor(getColor(R.color.white));
            tabRequests.setTypeface(null, android.graphics.Typeface.BOLD);
            tabFriends.setBackground(null);
            tabFriends.setTextColor(getColor(R.color.text_gray));
            tabFriends.setTypeface(null, android.graphics.Typeface.NORMAL);
            updateRequestsView();
        }
    }

    private void updateFriendsView() {
        rvRequests.setVisibility(View.GONE);
        layoutEmptyRequests.setVisibility(View.GONE);

        if (friendUsers.isEmpty()) {
            rvFriends.setVisibility(View.GONE);
            layoutEmptyFriends.setVisibility(View.VISIBLE);
        } else {
            rvFriends.setVisibility(View.VISIBLE);
            layoutEmptyFriends.setVisibility(View.GONE);
        }
    }

    private void updateRequestsView() {
        rvFriends.setVisibility(View.GONE);
        layoutEmptyFriends.setVisibility(View.GONE);

        if (requestUsers.isEmpty()) {
            rvRequests.setVisibility(View.GONE);
            layoutEmptyRequests.setVisibility(View.VISIBLE);
        } else {
            rvRequests.setVisibility(View.VISIBLE);
            layoutEmptyRequests.setVisibility(View.GONE);
        }
    }

    private void loadData() {
        friendUsers.clear();
        friendFriendships.clear();
        requestUsers.clear();
        requestFriendships.clear();

        AtomicInteger pendingQueries = new AtomicInteger(2);

        // Query friendships where I'm the requester
        friendshipRepository.getFriendsByRequester(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Friendship f = doc.toObject(Friendship.class);
                    if (f == null) continue;
                    f.setId(doc.getId());
                    if (Friendship.STATUS_ACCEPTED.equals(f.getStatus())) {
                        loadFriendUser(f.getReceiverId(), f, friendUsers, friendFriendships);
                    }
                    // PENDING sent → not shown in this screen (shown in UserSearch)
                }
            }
            if (pendingQueries.decrementAndGet() == 0) onBothQueriesDone();
        });

        // Query friendships where I'm the receiver
        friendshipRepository.getFriendsByReceiver(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Friendship f = doc.toObject(Friendship.class);
                    if (f == null) continue;
                    f.setId(doc.getId());
                    if (Friendship.STATUS_ACCEPTED.equals(f.getStatus())) {
                        loadFriendUser(f.getRequesterId(), f, friendUsers, friendFriendships);
                    } else if (Friendship.STATUS_PENDING.equals(f.getStatus())) {
                        loadFriendUser(f.getRequesterId(), f, requestUsers, requestFriendships);
                    }
                }
            }
            if (pendingQueries.decrementAndGet() == 0) onBothQueriesDone();
        });
    }

    private void loadFriendUser(String userId, Friendship friendship,
                                List<User> targetUsers, List<Friendship> targetFriendships) {
        userRepository.getUser(userId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User u = task.getResult().toObject(User.class);
                if (u != null) {
                    u.setId(userId);
                    targetUsers.add(u);
                    targetFriendships.add(friendship);

                    if (targetUsers == friendUsers) {
                        friendListAdapter.setData(friendUsers, friendFriendships);
                        updateHeader();
                        if (showingFriends) updateFriendsView();
                    } else {
                        friendRequestAdapter.setData(requestUsers, requestFriendships);
                        updateRequestsTabBadge();
                        if (!showingFriends) updateRequestsView();
                    }
                }
            }
        });
    }

    private void onBothQueriesDone() {
        runOnUiThread(() -> {
            updateHeader();
            updateRequestsTabBadge();
            if (showingFriends) updateFriendsView();
            else updateRequestsView();
        });
    }

    private void updateHeader() {
        int count = friendUsers.size();
        tvFriendCountHeader.setText(count + " bạn bè");
    }

    private void updateRequestsTabBadge() {
        int count = requestUsers.size();
        String label = count > 0 ? "Lời mời (" + count + ")" : "Lời mời";
        tabRequests.setText(label);
    }

    private void showUnfriendDialog(User user, Friendship friendship) {
        String name = user.getFullName() != null ? user.getFullName() : "người này";
        new AlertDialog.Builder(this)
                .setTitle("Hủy kết bạn")
                .setMessage("Bạn có chắc muốn hủy kết bạn với " + name + "?")
                .setPositiveButton("Hủy kết bạn", (dialog, which) -> doUnfriend(user, friendship))
                .setNegativeButton("Không", null)
                .show();
    }

    private void doUnfriend(User user, Friendship friendship) {
        friendshipRepository.deleteFriendship(friendship.getId())
                .addOnSuccessListener(aVoid -> {
                    int idx = friendFriendships.indexOf(friendship);
                    if (idx >= 0) {
                        friendUsers.remove(idx);
                        friendFriendships.remove(idx);
                        friendListAdapter.setData(friendUsers, friendFriendships);
                        updateHeader();
                        updateFriendsView();
                    }
                    Toast.makeText(this, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void acceptRequest(User user, Friendship friendship) {
        friendshipRepository.acceptFriendRequest(friendship.getId())
                .addOnSuccessListener(aVoid -> {
                    // Move from requests to friends
                    int idx = requestFriendships.indexOf(friendship);
                    if (idx >= 0) {
                        requestUsers.remove(idx);
                        requestFriendships.remove(idx);
                        friendRequestAdapter.setData(requestUsers, requestFriendships);
                    }
                    friendship.setStatus(Friendship.STATUS_ACCEPTED);
                    friendUsers.add(user);
                    friendFriendships.add(friendship);
                    friendListAdapter.setData(friendUsers, friendFriendships);
                    updateHeader();
                    updateRequestsTabBadge();
                    updateRequestsView();
                    Toast.makeText(this, "Đã chấp nhận lời mời kết bạn", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void rejectRequest(User user, Friendship friendship) {
        friendshipRepository.deleteFriendship(friendship.getId())
                .addOnSuccessListener(aVoid -> {
                    int idx = requestFriendships.indexOf(friendship);
                    if (idx >= 0) {
                        requestUsers.remove(idx);
                        requestFriendships.remove(idx);
                        friendRequestAdapter.setData(requestUsers, requestFriendships);
                    }
                    updateRequestsTabBadge();
                    updateRequestsView();
                    Toast.makeText(this, "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}
