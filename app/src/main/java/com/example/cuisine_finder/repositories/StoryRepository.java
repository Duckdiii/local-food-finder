package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.Story;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class StoryRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference storiesRef = db.collection("stories");

    public Task<Void> uploadStory(Story story) {
        long now = System.currentTimeMillis();
        story.setCreatedAt(now);
        story.setExpiresAt(now + (24 * 60 * 60 * 1000)); // 24 hours
        String id = storiesRef.document().getId();
        story.setId(id);
        return storiesRef.document(id).set(story);
    }

    public Query getActiveStories() {
        long now = System.currentTimeMillis();
        return storiesRef.whereGreaterThan("expiresAt", now)
                .orderBy("expiresAt", Query.Direction.ASCENDING);
    }
}
