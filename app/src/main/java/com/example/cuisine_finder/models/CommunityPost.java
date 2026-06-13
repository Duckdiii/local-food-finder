package com.example.cuisine_finder.models;

import java.util.ArrayList;
import java.util.List;

public class CommunityPost {
    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String caption;
    private String imageUrl;
    private String placeId;
    private String placeName;
    private String placeImageUrl;
    private String placeAddress;
    private double placeRating;
    private double distanceKm;
    private boolean openLate;
    private List<String> tags = new ArrayList<>();
    private List<String> likedUserIds = new ArrayList<>();
    private int likeCount;
    private int commentCount;
    private long createdAt;

    public CommunityPost() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getPlaceImageUrl() {
        return placeImageUrl;
    }

    public void setPlaceImageUrl(String placeImageUrl) {
        this.placeImageUrl = placeImageUrl;
    }

    public String getPlaceAddress() {
        return placeAddress;
    }

    public void setPlaceAddress(String placeAddress) {
        this.placeAddress = placeAddress;
    }

    public double getPlaceRating() {
        return placeRating;
    }

    public void setPlaceRating(double placeRating) {
        this.placeRating = placeRating;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean isOpenLate() {
        return openLate;
    }

    public void setOpenLate(boolean openLate) {
        this.openLate = openLate;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public List<String> getLikedUserIds() {
        return likedUserIds;
    }

    public void setLikedUserIds(List<String> likedUserIds) {
        this.likedUserIds = likedUserIds != null ? likedUserIds : new ArrayList<>();
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isLikedBy(String userId) {
        return userId != null && likedUserIds != null && likedUserIds.contains(userId);
    }

    public void setLikedBy(String userId, boolean liked) {
        if (userId == null) return;
        if (likedUserIds == null) likedUserIds = new ArrayList<>();

        boolean currentlyLiked = likedUserIds.contains(userId);
        if (liked && !currentlyLiked) {
            likedUserIds.add(userId);
            likeCount++;
        } else if (!liked && currentlyLiked) {
            likedUserIds.remove(userId);
            likeCount = Math.max(0, likeCount - 1);
        }
    }
}
