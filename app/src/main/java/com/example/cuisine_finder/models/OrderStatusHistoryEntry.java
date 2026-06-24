package com.example.cuisine_finder.models;

public class OrderStatusHistoryEntry {
    private String status;
    private String actorId;
    private String actorRole;
    private String note;
    private long timestamp;

    public OrderStatusHistoryEntry() {
    }

    public OrderStatusHistoryEntry(String status, String actorId, String actorRole, String note) {
        this.status = status;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.note = note;
        this.timestamp = System.currentTimeMillis();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
