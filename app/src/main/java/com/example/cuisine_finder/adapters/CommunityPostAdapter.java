package com.example.cuisine_finder.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.CommunityPost;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {
    public interface OnPostActionListener {
        void onLikeClicked(CommunityPost post);
        void onCommentClicked(CommunityPost post);
        void onShareClicked(CommunityPost post);
        void onPlaceClicked(CommunityPost post);
        void onTagClicked(String tag);
    }

    private final List<CommunityPost> posts = new ArrayList<>();
    private final Set<String> expandedPostIds = new HashSet<>();
    private final OnPostActionListener listener;
    private String currentUserId;

    public CommunityPostAdapter(OnPostActionListener listener) {
        this.listener = listener;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    public void setPosts(List<CommunityPost> newPosts) {
        posts.clear();
        if (newPosts != null) {
            posts.addAll(newPosts);
        }
        notifyDataSetChanged();
    }

    public void applyOptimisticLike(String postId, String userId, boolean liked) {
        for (int i = 0; i < posts.size(); i++) {
            CommunityPost post = posts.get(i);
            if (postId.equals(post.getId())) {
                post.setLikedBy(userId, liked);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAuthorAvatar;
        private final TextView tvAuthorName;
        private final TextView tvPostTime;
        private final TextView tvDistanceBadge;
        private final TextView tvCaption;
        private final TextView tvReadMore;
        private final ImageView ivPostImage;
        private final FrameLayout layoutImageContainer;
        private final LinearLayout layoutPlaceOverlay;
        private final TextView tvPlaceNameOverlay;
        private final LinearLayout layoutTags;
        private final LinearLayout layoutMiniCard;
        private final ImageView ivPlaceImage;
        private final TextView tvMiniPlaceName;
        private final TextView tvMiniPlaceAddress;
        private final TextView tvMiniPlaceStats;
        private final TextView btnLike;
        private final TextView tvLikeCount;
        private final TextView btnComment;
        private final TextView tvCommentCount;
        private final TextView btnShare;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorAvatar = itemView.findViewById(R.id.tvPostAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvPostAuthorName);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvDistanceBadge = itemView.findViewById(R.id.tvPostDistanceBadge);
            tvCaption = itemView.findViewById(R.id.tvPostCaption);
            tvReadMore = itemView.findViewById(R.id.tvPostReadMore);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            layoutImageContainer = itemView.findViewById(R.id.layoutPostImageContainer);
            layoutPlaceOverlay = itemView.findViewById(R.id.layoutPostPlaceOverlay);
            tvPlaceNameOverlay = itemView.findViewById(R.id.tvPostPlaceOverlay);
            layoutTags = itemView.findViewById(R.id.layoutPostTags);
            layoutMiniCard = itemView.findViewById(R.id.layoutMiniPlaceCard);
            ivPlaceImage = itemView.findViewById(R.id.ivMiniPlaceImage);
            tvMiniPlaceName = itemView.findViewById(R.id.tvMiniPlaceName);
            tvMiniPlaceAddress = itemView.findViewById(R.id.tvMiniPlaceAddress);
            tvMiniPlaceStats = itemView.findViewById(R.id.tvMiniPlaceStats);
            btnLike = itemView.findViewById(R.id.btnPostLike);
            tvLikeCount = itemView.findViewById(R.id.tvPostLikeCount);
            btnComment = itemView.findViewById(R.id.btnPostComment);
            tvCommentCount = itemView.findViewById(R.id.tvPostCommentCount);
            btnShare = itemView.findViewById(R.id.btnPostShare);
        }

        void bind(CommunityPost post) {
            Context context = itemView.getContext();
            String authorName = safeText(post.getAuthorName(), "Người dùng");
            tvAuthorAvatar.setText(authorName.substring(0, 1).toUpperCase(Locale.getDefault()));
            tvAuthorName.setText(authorName);
            tvPostTime.setText(formatTime(post.getCreatedAt()));
            tvDistanceBadge.setText(formatDistanceBadge(post.getDistanceKm()));
            tvDistanceBadge.setVisibility(post.getDistanceKm() > 0 ? View.VISIBLE : View.GONE);

            bindCaption(post);
            bindImage(context, post);
            bindTags(context, post.getTags());
            bindMiniCard(context, post);
            bindActions(context, post);
        }

        private void bindCaption(CommunityPost post) {
            String caption = safeText(post.getCaption(), "");
            tvCaption.setText(caption);
            boolean expandable = caption.length() > 130;
            boolean expanded = post.getId() != null && expandedPostIds.contains(post.getId());

            tvCaption.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
            tvCaption.setEllipsize(expanded ? null : TextUtils.TruncateAt.END);
            tvReadMore.setVisibility(expandable ? View.VISIBLE : View.GONE);
            tvReadMore.setText(expanded ? "Thu gọn" : "Xem thêm");
            tvReadMore.setOnClickListener(v -> {
                if (post.getId() == null) return;
                if (expandedPostIds.contains(post.getId())) {
                    expandedPostIds.remove(post.getId());
                } else {
                    expandedPostIds.add(post.getId());
                }
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position);
                }
            });
        }

        private void bindImage(Context context, CommunityPost post) {
            boolean hasImage = !TextUtils.isEmpty(post.getImageUrl());
            boolean existingPlace = hasExistingPlace(post);
            layoutImageContainer.setVisibility((hasImage || existingPlace) ? View.VISIBLE : View.GONE);

            if (hasImage) {
                Glide.with(context)
                        .load(post.getImageUrl())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(ivPostImage);
            } else if (existingPlace && !TextUtils.isEmpty(post.getPlaceImageUrl())) {
                Glide.with(context)
                        .load(post.getPlaceImageUrl())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(ivPostImage);
            } else {
                ivPostImage.setImageResource(R.drawable.bg_image_placeholder);
            }

            layoutPlaceOverlay.setVisibility(existingPlace ? View.VISIBLE : View.GONE);
            tvPlaceNameOverlay.setText(safeText(post.getPlaceName(), "Xem bản đồ"));
            layoutImageContainer.setOnClickListener(
                    existingPlace ? v -> listener.onPlaceClicked(post) : null
            );
        }

        private void bindTags(Context context, List<String> tags) {
            layoutTags.removeAllViews();
            if (tags == null || tags.isEmpty()) {
                layoutTags.setVisibility(View.GONE);
                return;
            }

            layoutTags.setVisibility(View.VISIBLE);
            for (String tag : tags) {
                if (TextUtils.isEmpty(tag)) continue;
                TextView tagView = new TextView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(context, 24)
                );
                params.setMargins(0, 0, dp(context, 8), 0);
                tagView.setLayoutParams(params);
                tagView.setBackgroundResource(R.drawable.bg_tag_orange);
                tagView.setGravity(Gravity.CENTER);
                tagView.setPadding(dp(context, 10), 0, dp(context, 10), 0);
                tagView.setText(tag);
                tagView.setTextColor(ContextCompat.getColor(context, R.color.orange_main));
                tagView.setTextSize(11);
                tagView.setOnClickListener(v -> listener.onTagClicked(tag));
                layoutTags.addView(tagView);
            }
        }

        private void bindMiniCard(Context context, CommunityPost post) {
            boolean manuallyEnteredPlace = !hasExistingPlace(post)
                    && (!TextUtils.isEmpty(post.getPlaceName()) || !TextUtils.isEmpty(post.getPlaceAddress()));
            layoutMiniCard.setVisibility(manuallyEnteredPlace ? View.VISIBLE : View.GONE);
            layoutMiniCard.setOnClickListener(null);
            ivPlaceImage.setVisibility(View.GONE);
            tvMiniPlaceStats.setVisibility(View.GONE);
            tvMiniPlaceName.setText(safeText(post.getPlaceName(), "Địa điểm được nhắc đến"));
            tvMiniPlaceAddress.setText(safeText(post.getPlaceAddress(), "Chưa có địa chỉ"));
        }

        private void bindActions(Context context, CommunityPost post) {
            boolean liked = post.isLikedBy(currentUserId);
            btnLike.setText(liked ? "❤" : "♡");
            btnLike.setTextColor(ContextCompat.getColor(
                    context,
                    liked ? R.color.red_close : R.color.text_gray
            ));
            tvLikeCount.setText(String.valueOf(Math.max(0, post.getLikeCount())));
            tvCommentCount.setText(String.valueOf(Math.max(0, post.getCommentCount())));

            View.OnClickListener likeClick = v -> listener.onLikeClicked(post);
            btnLike.setOnClickListener(likeClick);
            tvLikeCount.setOnClickListener(likeClick);

            View.OnClickListener commentClick = v -> listener.onCommentClicked(post);
            btnComment.setOnClickListener(commentClick);
            tvCommentCount.setOnClickListener(commentClick);

            btnShare.setOnClickListener(v -> listener.onShareClicked(post));
        }

        private boolean hasPlaceInfo(CommunityPost post) {
            return !TextUtils.isEmpty(post.getPlaceName())
                    || !TextUtils.isEmpty(post.getPlaceAddress())
                    || !TextUtils.isEmpty(post.getPlaceId());
        }

        private boolean hasExistingPlace(CommunityPost post) {
            return !TextUtils.isEmpty(post.getPlaceId());
        }

        private String safeText(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value.trim();
        }

        private int dp(Context context, int value) {
            return (int) (value * context.getResources().getDisplayMetrics().density);
        }

        private String formatDistanceBadge(double distanceKm) {
            return String.format(Locale.getDefault(), "%.1f", distanceKm);
        }

        private String formatDistanceText(double distanceKm) {
            if (distanceKm < 1) {
                return Math.round(distanceKm * 1000) + " m";
            }
            return String.format(Locale.getDefault(), "%.1f km", distanceKm);
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
