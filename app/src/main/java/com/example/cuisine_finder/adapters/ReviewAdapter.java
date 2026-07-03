package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Review;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    public interface OnReplyClickListener {
        void onReplyClick(Review review);
    }

    private List<Review> reviews = new ArrayList<>();
    private OnReplyClickListener replyClickListener;
    private boolean showReplyButton = false;

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    public void setShowReplyButton(boolean showReplyButton) {
        this.showReplyButton = showReplyButton;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserAvatar, tvUserName, tvReviewDate, tvComment, tvMerchantReplyText, btnReplyReview;
        android.widget.RatingBar ratingBar;
        LinearLayout layoutReviewImages, layoutMerchantReply;
        View scrollReviewImages;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserAvatar = itemView.findViewById(R.id.tvUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvMerchantReplyText = itemView.findViewById(R.id.tvMerchantReplyText);
            btnReplyReview = itemView.findViewById(R.id.btnReplyReview);
            layoutReviewImages = itemView.findViewById(R.id.layoutReviewImages);
            layoutMerchantReply = itemView.findViewById(R.id.layoutMerchantReply);
            scrollReviewImages = itemView.findViewById(R.id.scrollReviewImages);
        }

        public void bind(Review review) {
            tvUserName.setText(review.getUserName() != null ? review.getUserName() : "Người dùng");
            tvComment.setText(review.getComment());
            
            // Avatar (first letter)
            if (review.getUserName() != null && !review.getUserName().isEmpty()) {
                tvUserAvatar.setText(review.getUserName().substring(0, 1).toUpperCase());
            }

            // Rating Stars
            if (ratingBar != null) {
                ratingBar.setRating((float) review.getRating());
            }

            // Date
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvReviewDate.setText(sdf.format(new Date(review.getCreatedAt())));

            // Load Review Images
            layoutReviewImages.removeAllViews();
            if (review.getImageUrls() != null && !review.getImageUrls().isEmpty()) {
                scrollReviewImages.setVisibility(View.VISIBLE);
                for (String url : review.getImageUrls()) {
                    MaterialCardView card = new MaterialCardView(itemView.getContext());
                    card.setRadius(24f);
                    card.setStrokeWidth(0);
                    
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(240, 240);
                    params.setMargins(0, 0, 16, 0);
                    card.setLayoutParams(params);

                    ImageView iv = new ImageView(itemView.getContext());
                    iv.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    
                    card.addView(iv);
                    layoutReviewImages.addView(card);
                    
                    Glide.with(itemView.getContext())
                            .load(url)
                            .placeholder(R.drawable.bg_image_placeholder)
                            .into(iv);
                }
            } else {
                scrollReviewImages.setVisibility(View.GONE);
            }

            if (review.getMerchantReply() != null && !review.getMerchantReply().trim().isEmpty()) {
                tvMerchantReplyText.setText(review.getMerchantReply().trim());
                layoutMerchantReply.setVisibility(View.VISIBLE);
                btnReplyReview.setVisibility(View.GONE);
            } else {
                layoutMerchantReply.setVisibility(View.GONE);
                if (showReplyButton) {
                    btnReplyReview.setVisibility(View.VISIBLE);
                    btnReplyReview.setOnClickListener(v -> {
                        if (replyClickListener != null) {
                            replyClickListener.onReplyClick(review);
                        }
                    });
                } else {
                    btnReplyReview.setVisibility(View.GONE);
                }
            }
        }
    }
}

