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

    private List<Review> reviews = new ArrayList<>();

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
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

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserAvatar, tvUserName, tvRatingStars, tvReviewDate, tvComment;
        LinearLayout layoutReviewImages;
        View scrollReviewImages;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserAvatar = itemView.findViewById(R.id.tvUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvRatingStars = itemView.findViewById(R.id.tvRatingStars);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvComment = itemView.findViewById(R.id.tvComment);
            layoutReviewImages = itemView.findViewById(R.id.layoutReviewImages);
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
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i < (int) review.getRating()) {
                    stars.append("⭐");
                }
            }
            tvRatingStars.setText(stars.toString());

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
        }
    }
}

