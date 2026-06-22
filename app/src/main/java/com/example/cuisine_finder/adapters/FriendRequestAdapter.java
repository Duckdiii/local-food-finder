package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Friendship;
import com.example.cuisine_finder.models.User;
import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(User user, Friendship friendship);
        void onReject(User user, Friendship friendship);
    }

    private final List<User> users = new ArrayList<>();
    private final List<Friendship> friendships = new ArrayList<>();
    private OnRequestActionListener listener;

    public void setOnRequestActionListener(OnRequestActionListener listener) {
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
                .inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        Friendship friendship = friendships.get(position);

        String name = user.getFullName() != null ? user.getFullName() : "Người dùng";
        holder.tvName.setText(name);
        holder.tvInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(user, friendship);
        });
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(user, friendship);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, btnAccept, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvRequesterInitial);
            tvName = itemView.findViewById(R.id.tvRequesterName);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
