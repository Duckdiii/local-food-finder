package com.example.cuisine_finder.models;

public class ChatMessage {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_RESTAURANT_CARD = "restaurant_card";
    public static final String STATE_SENDING = "sending";
    public static final String STATE_SENT = "sent";

    private String id;
    private String roomId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String attachmentUrl;
    private String restaurantId;
    private String restaurantName;
    private String restaurantImageUrl;
    private double restaurantRating;
    private String type;
    private String deliveryState;
    private long createdAt;
    private boolean isDeleted;
    private int reportCount;

    public ChatMessage() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getRestaurantImageUrl() { return restaurantImageUrl; }
    public void setRestaurantImageUrl(String restaurantImageUrl) { this.restaurantImageUrl = restaurantImageUrl; }

    public double getRestaurantRating() { return restaurantRating; }
    public void setRestaurantRating(double restaurantRating) { this.restaurantRating = restaurantRating; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDeliveryState() { return deliveryState; }
    public void setDeliveryState(String deliveryState) { this.deliveryState = deliveryState; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public int getReportCount() { return reportCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }
}
