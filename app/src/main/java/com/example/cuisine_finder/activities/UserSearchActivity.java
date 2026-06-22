package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.UserSearchAdapter;
import com.example.cuisine_finder.adapters.UserSearchAdapter.FriendStatus;
import com.example.cuisine_finder.adapters.UserSearchAdapter.UserSearchItem;
import com.example.cuisine_finder.models.Friendship;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.FriendshipRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UserSearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private ProgressBar progressBar;
    private LinearLayout layoutNoResults;
    private TextView tvSearchHint;

    private UserSearchAdapter adapter;
    private FriendshipRepository friendshipRepository;
    private UserRepository userRepository;
    private String currentUserId;

    // Cache of existing friendships: otherUserId → Friendship
    private final Map<String, Friendship> friendshipMap = new HashMap<>();
    private boolean friendshipsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        currentUserId = user.getUid();

        friendshipRepository = new FriendshipRepository();
        userRepository = new UserRepository();

        initViews();
        loadMyFriendships();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        progressBar = findViewById(R.id.progressBar);
        layoutNoResults = findViewById(R.id.layoutNoResults);
        tvSearchHint = findViewById(R.id.tvSearchHint);

        adapter = new UserSearchAdapter();
        adapter.setOnActionListener(new UserSearchAdapter.OnActionListener() {
            @Override
            public void onAddFriend(UserSearchItem item) {
                sendFriendRequest(item);
            }
            @Override
            public void onCancelRequest(UserSearchItem item) {
                cancelFriendRequest(item);
            }
            @Override
            public void onAcceptRequest(UserSearchItem item) {
                acceptFriendRequest(item);
            }
            @Override
            public void onRejectRequest(UserSearchItem item) {
                rejectFriendRequest(item);
            }
        });
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                if (q.length() >= 2 && friendshipsLoaded) {
                    performSearch(q);
                } else if (q.isEmpty()) {
                    showHint();
                }
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = etSearch.getText().toString().trim();
                if (!q.isEmpty() && friendshipsLoaded) performSearch(q);
                return true;
            }
            return false;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void loadMyFriendships() {
        friendshipMap.clear();
        AtomicInteger pending = new AtomicInteger(2);

        friendshipRepository.getFriendsByRequester(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Friendship f = doc.toObject(Friendship.class);
                    if (f != null) {
                        f.setId(doc.getId());
                        friendshipMap.put(f.getReceiverId(), f);
                    }
                }
            }
            if (pending.decrementAndGet() == 0) friendshipsLoaded = true;
        });

        friendshipRepository.getFriendsByReceiver(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Friendship f = doc.toObject(Friendship.class);
                    if (f != null) {
                        f.setId(doc.getId());
                        friendshipMap.put(f.getRequesterId(), f);
                    }
                }
            }
            if (pending.decrementAndGet() == 0) friendshipsLoaded = true;
        });
    }

    private void performSearch(String query) {
        showLoading();

        userRepository.getAllUsers().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                showNoResults();
                return;
            }

            List<UserSearchItem> results = new ArrayList<>();
            String queryLower = query.toLowerCase();

            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                User u = doc.toObject(User.class);
                if (u == null) continue;
                u.setId(doc.getId());

                // Exclude self
                if (currentUserId.equals(u.getId())) continue;

                // Filter by name
                String name = u.getFullName() != null ? u.getFullName().toLowerCase() : "";
                String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                if (!name.contains(queryLower) && !email.contains(queryLower)) continue;

                Friendship friendship = friendshipMap.get(u.getId());
                FriendStatus status = resolveFriendStatus(u.getId(), friendship);
                results.add(new UserSearchItem(u, friendship, status));
            }

            runOnUiThread(() -> {
                if (results.isEmpty()) {
                    showNoResults();
                } else {
                    adapter.setItems(results);
                    showResults();
                }
            });
        });
    }

    private FriendStatus resolveFriendStatus(String otherUserId, Friendship f) {
        if (f == null) return FriendStatus.NONE;
        if (Friendship.STATUS_ACCEPTED.equals(f.getStatus())) return FriendStatus.FRIENDS;
        if (Friendship.STATUS_PENDING.equals(f.getStatus())) {
            if (currentUserId.equals(f.getRequesterId())) return FriendStatus.PENDING_SENT;
            else return FriendStatus.PENDING_RECEIVED;
        }
        return FriendStatus.NONE;
    }

    private void sendFriendRequest(UserSearchItem item) {
        friendshipRepository.sendFriendRequest(currentUserId, item.user.getId())
                .addOnSuccessListener(ref -> {
                    Friendship f = new Friendship();
                    f.setId(ref.getId());
                    f.setRequesterId(currentUserId);
                    f.setReceiverId(item.user.getId());
                    f.setStatus(Friendship.STATUS_PENDING);
                    friendshipMap.put(item.user.getId(), f);
                    refreshSearch();
                    Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void cancelFriendRequest(UserSearchItem item) {
        if (item.friendship == null) return;
        friendshipRepository.deleteFriendship(item.friendship.getId())
                .addOnSuccessListener(aVoid -> {
                    friendshipMap.remove(item.user.getId());
                    refreshSearch();
                    Toast.makeText(this, "Đã hủy lời mời", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void acceptFriendRequest(UserSearchItem item) {
        if (item.friendship == null) return;
        friendshipRepository.acceptFriendRequest(item.friendship.getId())
                .addOnSuccessListener(aVoid -> {
                    item.friendship.setStatus(Friendship.STATUS_ACCEPTED);
                    friendshipMap.put(item.user.getId(), item.friendship);
                    refreshSearch();
                    Toast.makeText(this, "Đã chấp nhận lời mời kết bạn", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void rejectFriendRequest(UserSearchItem item) {
        if (item.friendship == null) return;
        friendshipRepository.deleteFriendship(item.friendship.getId())
                .addOnSuccessListener(aVoid -> {
                    friendshipMap.remove(item.user.getId());
                    refreshSearch();
                    Toast.makeText(this, "Đã từ chối lời mời", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show());
    }

    private void refreshSearch() {
        String q = etSearch.getText().toString().trim();
        if (q.length() >= 2) performSearch(q);
    }

    private void showHint() {
        tvSearchHint.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);
        layoutNoResults.setVisibility(View.GONE);
    }

    private void showLoading() {
        tvSearchHint.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        layoutNoResults.setVisibility(View.GONE);
    }

    private void showResults() {
        tvSearchHint.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.VISIBLE);
        layoutNoResults.setVisibility(View.GONE);
    }

    private void showNoResults() {
        tvSearchHint.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);
        layoutNoResults.setVisibility(View.VISIBLE);
    }
}
