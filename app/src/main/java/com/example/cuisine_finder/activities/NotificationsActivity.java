package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvEmptyState;
    private TextView btnMarkAllRead;
    private NotificationAdapter adapter;
    private final List<NotificationItem> notificationList = new ArrayList<>();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        currentUserId = FirebaseAuth.getInstance().getUid();

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        btnMarkAllRead.setOnClickListener(v -> markAllNotificationsAsRead());

        loadNotifications();
    }

    private void loadNotifications() {
        if (currentUserId == null) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Vui lòng đăng nhập để xem thông báo");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Không thể tải thông báo", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notificationList.clear();
                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            NotificationItem item = new NotificationItem(
                                    doc.getId(),
                                    doc.getString("title"),
                                    doc.getString("body"),
                                    doc.getLong("timestamp") != null ? doc.getLong("timestamp") : System.currentTimeMillis(),
                                    doc.getBoolean("read") != null && doc.getBoolean("read")
                            );
                            notificationList.add(item);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (notificationList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvNotifications.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        rvNotifications.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void markAllNotificationsAsRead() {
        if (currentUserId == null || notificationList.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.update(doc.getReference(), "read", true);
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    // Static inner data class representing a notification
    private static class NotificationItem {
        String id;
        String title;
        String body;
        long timestamp;
        boolean read;

        NotificationItem(String id, String title, String body, long timestamp, boolean read) {
            this.id = id;
            this.title = title;
            this.body = body;
            this.timestamp = timestamp;
            this.read = read;
        }
    }

    // RecyclerView Adapter for Notifications
    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        private final List<NotificationItem> list;

        NotificationAdapter(List<NotificationItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = list.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvBody.setText(item.body);

            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            holder.tvTime.setText(timeAgo);

            // Change icon based on keywords
            if (item.title != null && item.title.toLowerCase().contains("quán")) {
                holder.tvIcon.setText("🍜");
            } else if (item.title != null && item.title.toLowerCase().contains("yêu thích")) {
                holder.tvIcon.setText("⭐");
            } else if (item.title != null && item.title.toLowerCase().contains("kết bạn")) {
                holder.tvIcon.setText("👥");
            } else {
                holder.tvIcon.setText("🔔");
            }

            // Unread styling
            if (!item.read) {
                holder.viewUnreadDot.setVisibility(View.VISIBLE);
                holder.cardNotification.setCardBackgroundColor(0xFFFFF9F5); // very light orange
            } else {
                holder.viewUnreadDot.setVisibility(View.GONE);
                holder.cardNotification.setCardBackgroundColor(0xFFFFFFFF); // white
            }

            holder.itemView.setOnClickListener(v -> {
                if (!item.read) {
                    FirebaseFirestore.getInstance()
                            .collection("notifications")
                            .document(item.id)
                            .update("read", true);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvTitle, tvBody, tvTime;
            View viewUnreadDot;
            com.google.android.material.card.MaterialCardView cardNotification;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tvIcon);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvBody = itemView.findViewById(R.id.tvBody);
                tvTime = itemView.findViewById(R.id.tvTime);
                viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
                cardNotification = itemView.findViewById(R.id.cardNotification);
            }
        }
    }
}
