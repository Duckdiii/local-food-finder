package com.example.cuisine_finder.repositories;

import com.example.cuisine_finder.models.CommunityComment;
import com.example.cuisine_finder.models.CommunityPost;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference postsRef = db.collection("community_posts");

    public interface PostsListener {
        void onPostsChanged(List<CommunityPost> posts);
        void onError(Exception error);
    }

    public interface CommentsListener {
        void onCommentsChanged(List<CommunityComment> comments);
        void onError(Exception error);
    }

    public ListenerRegistration listenPosts(PostsListener listener) {
        return postsRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    List<CommunityPost> posts = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            CommunityPost post = document.toObject(CommunityPost.class);
                            if (post != null) {
                                post.setId(document.getId());
                                posts.add(post);
                            }
                        }
                    }
                    listener.onPostsChanged(posts);
                });
    }

    public Task<Void> createPost(CommunityPost post) {
        DocumentReference postRef = postsRef.document();
        post.setId(postRef.getId());
        if (post.getCreatedAt() == 0) {
            post.setCreatedAt(System.currentTimeMillis());
        }
        return postRef.set(post);
    }

    public Task<Void> deletePost(String postId) {
        return postsRef.document(postId).delete();
    }

    public Task<Void> toggleLike(String postId, String userId) {
        DocumentReference postRef = postsRef.document(postId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(postRef);
            List<String> likedUserIds = getLikedUserIds(snapshot);
            boolean liked = likedUserIds.contains(userId);

            Map<String, Object> updates = new HashMap<>();
            if (liked) {
                updates.put("likedUserIds", FieldValue.arrayRemove(userId));
                updates.put("likeCount", FieldValue.increment(-1));
            } else {
                updates.put("likedUserIds", FieldValue.arrayUnion(userId));
                updates.put("likeCount", FieldValue.increment(1));
            }
            transaction.update(postRef, updates);
            return null;
        });
    }

    public ListenerRegistration listenComments(String postId, CommentsListener listener) {
        return postsRef.document(postId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    List<CommunityComment> comments = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            CommunityComment comment = document.toObject(CommunityComment.class);
                            if (comment != null) {
                                comment.setId(document.getId());
                                comments.add(comment);
                            }
                        }
                    }
                    listener.onCommentsChanged(comments);
                });
    }

    public Task<Void> addComment(String postId, CommunityComment comment) {
        DocumentReference postRef = postsRef.document(postId);
        DocumentReference commentRef = postRef.collection("comments").document();
        comment.setId(commentRef.getId());
        comment.setPostId(postId);
        if (comment.getCreatedAt() == 0) {
            comment.setCreatedAt(System.currentTimeMillis());
        }

        WriteBatch batch = db.batch();
        batch.set(commentRef, comment);
        batch.update(postRef, "commentCount", FieldValue.increment(1));
        return batch.commit();
    }

    public Task<Void> reportPost(String postId, String userId, String reason) {
        DocumentReference postRef = postsRef.document(postId);
        DocumentReference reportRef = db.collection("reports")
                .document("POST_" + postId + "_" + userId);
        return db.runTransaction(transaction -> {
            if (transaction.get(reportRef).exists()) return null;

            Map<String, Object> report = new HashMap<>();
            report.put("reporterId", userId);
            report.put("targetId", postId);
            report.put("targetType", "POST");
            report.put("reason", reason);
            report.put("status", "PENDING");
            report.put("createdAt", System.currentTimeMillis());

            transaction.set(reportRef, report);
            transaction.update(postRef, "reportCount", FieldValue.increment(1));
            return null;
        });
    }

    public Task<Void> removeLegacyMockPosts() {
        WriteBatch batch = db.batch();
        batch.delete(postsRef.document("seed_bun_bo_le_van_sy"));
        batch.delete(postsRef.document("seed_an_khuya_thu_duc"));
        return batch.commit();
    }

    @SuppressWarnings("unchecked")
    private List<String> getLikedUserIds(DocumentSnapshot snapshot) {
        Object value = snapshot.get("likedUserIds");
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }
}
