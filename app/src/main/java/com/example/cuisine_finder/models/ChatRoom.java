package com.example.cuisine_finder.models;

public class ChatRoom {
    public static final String TYPE_DISTRICT = "district";
    public static final String TYPE_PLACE = "place";
    public static final String TYPE_COMMUNITY = "community";

    private String id;
    private String type;
    private String linkedPlaceId;
    private String name;
    private String description;
    private String createdBy;
    private java.util.List<String> memberIds;
    private int memberCount;
    private String lastMessage;
    private long lastMessageAt;
    private long createdAt;

    public ChatRoom() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLinkedPlaceId() { return linkedPlaceId; }
    public void setLinkedPlaceId(String linkedPlaceId) { this.linkedPlaceId = linkedPlaceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public java.util.List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(java.util.List<String> memberIds) { this.memberIds = memberIds; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(long lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
