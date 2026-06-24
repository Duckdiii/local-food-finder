package com.example.cuisine_finder.models;

import java.util.List;

public class Story {
    private String id;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private String imageUrl;
    private String caption;
    private long createdAt;
    private long expiresAt;
    private List<String> viewers; // List of user IDs who saw this

    public Story() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatarUrl() { return userAvatarUrl; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public List<String> getViewers() { return viewers; }
    public void setViewers(List<String> viewers) { this.viewers = viewers; }
}
