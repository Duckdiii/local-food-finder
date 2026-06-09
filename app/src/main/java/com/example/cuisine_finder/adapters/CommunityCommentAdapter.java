package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.CommunityComment;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityCommentAdapter extends RecyclerView.Adapter<CommunityCommentAdapter.CommentViewHolder> {
    private final List<CommunityComment> comments = new ArrayList<>();

    public void setComments(List<CommunityComment> newComments) {
        comments.clear();
        if (newComments != null) {
            comments.addAll(newComments);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvAuthorName;
        private final TextView tvTime;
        private final TextView tvContent;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvCommentAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvCommentAuthorName);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
        }

        void bind(CommunityComment comment) {
            String authorName = safeText(comment.getAuthorName(), "Người dùng");
            tvAvatar.setText(authorName.substring(0, 1).toUpperCase(Locale.getDefault()));
            tvAuthorName.setText(authorName);
            tvTime.setText(formatTime(comment.getCreatedAt()));
            tvContent.setText(comment.getContent());
        }

        private String safeText(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value.trim();
        }

        private String formatTime(long createdAt) {
            if (createdAt <= 0) return "vừa xong";
            long diff = System.currentTimeMillis() - createdAt;
            long minute = 60 * 1000;
            long hour = 60 * minute;
            long day = 24 * hour;

            if (diff < minute) return "vừa xong";
            if (diff < hour) return (diff / minute) + " phút trước";
            if (diff < day) return (diff / hour) + " giờ trước";
            if (diff < 7 * day) return (diff / day) + " ngày trước";
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(createdAt));
        }
    }
}
