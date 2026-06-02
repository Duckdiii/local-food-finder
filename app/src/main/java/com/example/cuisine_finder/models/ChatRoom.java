package com.example.cuisine_finder.models;

public class ChatRoom {
    private String id;
    private String name;
    private String areaName;
    private double centerLatitude;
    private double centerLongitude;
    private double radiusInKm;
    private int memberCount;
    private int messageCount;
    private long createdAt;
    private long updatedAt;

    public ChatRoom() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public double getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(double centerLatitude) { this.centerLatitude = centerLatitude; }

    public double getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(double centerLongitude) { this.centerLongitude = centerLongitude; }

    public double getRadiusInKm() { return radiusInKm; }
    public void setRadiusInKm(double radiusInKm) { this.radiusInKm = radiusInKm; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
