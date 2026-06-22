package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Friendship;
import com.example.cuisine_finder.models.User;
import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {

    public enum FriendStatus { NONE, PENDING_SENT, PENDING_RECEIVED, FRIENDS }

    public static class UserSearchItem {
        public User user;
        public Friendship friendship;
        public FriendStatus status;

        public UserSearchItem(User user, Friendship friendship, FriendStatus status) {
            this.user = user;
            this.friendship = friendship;
            this.status = status;
        }
    }

    public interface OnActionListener {
        void onAddFriend(UserSearchItem item);
        void onCancelRequest(UserSearchItem item);
        void onAcceptRequest(UserSearchItem item);
        void onRejectRequest(UserSearchItem item);
    }

    private final List<UserSearchItem> items = new ArrayList<>();
    private OnActionListener listener;

    public void setOnActionListener(OnActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<UserSearchItem> items) {
        this.items.clear();
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    public void updateItem(int position, FriendStatus newStatus, Friendship friendship) {
        if (position >= 0 && position < items.size()) {
            items.get(position).status = newStatus;
            items.get(position).friendship = friendship;
            notifyItemChanged(position);
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserSearchItem item = items.get(position);
        User user = item.user;

        String name = user.getFullName() != null ? user.getFullName() : "Người dùng";
        holder.tvName.setText(name);
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        holder.tvInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

        // Reset all visibility
        holder.btnAdd.setVisibility(View.GONE);
        holder.tvPendingSent.setVisibility(View.GONE);
        holder.tvAlreadyFriends.setVisibility(View.GONE);
        holder.layoutAcceptReject.setVisibility(View.GONE);

        switch (item.status) {
            case NONE:
                holder.btnAdd.setVisibility(View.VISIBLE);
                holder.btnAdd.setOnClickListener(v -> {
                    if (listener != null) listener.onAddFriend(item);
                });
                break;
            case PENDING_SENT:
                holder.tvPendingSent.setVisibility(View.VISIBLE);
                holder.tvPendingSent.setOnClickListener(v -> {
                    if (listener != null) listener.onCancelRequest(item);
                });
                break;
            case PENDING_RECEIVED:
                holder.layoutAcceptReject.setVisibility(View.VISIBLE);
                holder.btnAcceptRequest.setOnClickListener(v -> {
                    if (listener != null) listener.onAcceptRequest(item);
                });
                holder.btnRejectRequest.setOnClickListener(v -> {
                    if (listener != null) listener.onRejectRequest(item);
                });
                break;
            case FRIENDS:
                holder.tvAlreadyFriends.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, tvEmail;
        TextView btnAdd, tvPendingSent, tvAlreadyFriends;
        LinearLayout layoutAcceptReject;
        TextView btnAcceptRequest, btnRejectRequest;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvUserInitial);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            btnAdd = itemView.findViewById(R.id.btnAddFriend);
            tvPendingSent = itemView.findViewById(R.id.tvPendingSent);
            tvAlreadyFriends = itemView.findViewById(R.id.tvAlreadyFriends);
            layoutAcceptReject = itemView.findViewById(R.id.layoutAcceptReject);
            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
            btnRejectRequest = itemView.findViewById(R.id.btnRejectRequest);
        }
    }
}
