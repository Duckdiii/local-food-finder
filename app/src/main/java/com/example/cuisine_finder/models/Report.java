package com.example.cuisine_finder.models;

public class Report {
    private String id;
    private String reporterId;
    private String targetId;
    private String targetType; // PLACE, REVIEW, MESSAGE
    private String reason;
    private String description;
    private String status; // PENDING, RESOLVED, REJECTED
    private long createdAt;
    private long resolvedAt;

    public Report() {
    }

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(long resolvedAt) { this.resolvedAt = resolvedAt; }
}
