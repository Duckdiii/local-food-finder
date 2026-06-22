package com.example.cuisine_finder.models;

public class Friendship {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";

    private String id;
    private String requesterId;
    private String receiverId;
    private String status;
    private long createdAt;

    public Friendship() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getOtherUserId(String myUserId) {
        if (myUserId.equals(requesterId)) return receiverId;
        return requesterId;
    }
}
