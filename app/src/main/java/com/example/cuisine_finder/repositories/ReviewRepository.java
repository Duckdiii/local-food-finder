package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.models.Review;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ReviewRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference reviewsRef = db.collection("reviews");
    private final CollectionReference placesRef = db.collection("food_places");

    public Task<Void> addReview(Review review) {
        return db.runTransaction(transaction -> {
            DocumentReference placeRef = placesRef.document(review.getPlaceId());
            FoodPlace place = transaction.get(placeRef).toObject(FoodPlace.class);

            if (place != null) {
                double currentTotal = place.getTotalRating();
                // Nếu totalRating chưa có nhưng đã có review cũ, hãy khôi phục nó
                if (currentTotal == 0 && place.getReviewCount() > 0) {
                    currentTotal = place.getAverageRating() * place.getReviewCount();
                }

                double newTotalRating = currentTotal + review.getRating();
                int newReviewCount = place.getReviewCount() + 1;
                double newAverageRating = newTotalRating / newReviewCount;

                // Update place stats
                transaction.update(placeRef, 
                    "totalRating", newTotalRating,
                    "reviewCount", newReviewCount,
                    "averageRating", newAverageRating
                );

                // Add the review document
                DocumentReference newReviewRef = reviewsRef.document();
                review.setId(newReviewRef.getId());
                review.setCreatedAt(System.currentTimeMillis());
                transaction.set(newReviewRef, review);
            }
            return null;
        });
    }

    public Query getReviewsByPlace(String placeId) {
        return reviewsRef.whereEqualTo("placeId", placeId);
    }

    public Query getReviewsByUser(String userId) {
        return reviewsRef.whereEqualTo("userId", userId);
    }
}
