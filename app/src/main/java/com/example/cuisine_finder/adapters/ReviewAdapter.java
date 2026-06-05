package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Review;
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

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserAvatar = itemView.findViewById(R.id.tvUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvRatingStars = itemView.findViewById(R.id.tvRatingStars);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvComment = itemView.findViewById(R.id.tvComment);
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
        }
    }
}
