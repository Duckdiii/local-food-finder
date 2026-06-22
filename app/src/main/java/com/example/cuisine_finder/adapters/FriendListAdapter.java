package com.example.cuisine_finder.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.activities.UserProfileActivity;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Friendship;
import com.example.cuisine_finder.models.User;
import java.util.ArrayList;
import java.util.List;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {

    public interface OnUnfriendListener {
        void onUnfriend(User user, Friendship friendship);
    }

    private final List<User> users = new ArrayList<>();
    private final List<Friendship> friendships = new ArrayList<>();
    private OnUnfriendListener listener;

    public void setOnUnfriendListener(OnUnfriendListener listener) {
        this.listener = listener;
    }

    public void setData(List<User> users, List<Friendship> friendships) {
        this.users.clear();
        this.friendships.clear();
        this.users.addAll(users);
        this.friendships.addAll(friendships);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        Friendship friendship = friendships.get(position);

        String name = user.getFullName() != null ? user.getFullName() : "Người dùng";
        holder.tvName.setText(name);
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        holder.tvInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

        holder.btnUnfriend.setOnClickListener(v -> {
            if (listener != null) listener.onUnfriend(user, friendship);
        });

        holder.itemView.setOnClickListener(v -> {
            if (user.getId() != null) {
                Context ctx = holder.itemView.getContext();
                Intent intent = new Intent(ctx, UserProfileActivity.class);
                intent.putExtra(UserProfileActivity.EXTRA_USER_ID, user.getId());
                ctx.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, tvEmail, btnUnfriend;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvFriendInitial);
            tvName = itemView.findViewById(R.id.tvFriendName);
            tvEmail = itemView.findViewById(R.id.tvFriendEmail);
            btnUnfriend = itemView.findViewById(R.id.btnUnfriend);
        }
    }
}
