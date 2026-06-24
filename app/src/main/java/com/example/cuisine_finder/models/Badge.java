package com.example.cuisine_finder.models;

public class Badge {
    private String id;
    private String name;
    private String description;
    private String iconEmoji;
    private int requirementValue;
    private String requirementType; // EXPLORED, REVIEW, SHARE
    private boolean isEarned;

    public Badge(String id, String name, String description, String iconEmoji, int requirementValue, String requirementType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconEmoji = iconEmoji;
        this.requirementValue = requirementValue;
        this.requirementType = requirementType;
        this.isEarned = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconEmoji() { return iconEmoji; }
    public int getRequirementValue() { return requirementValue; }
    public String getRequirementType() { return requirementType; }
    public boolean isEarned() { return isEarned; }
    public void setEarned(boolean earned) { isEarned = earned; }
}
