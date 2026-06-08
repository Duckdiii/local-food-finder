package com.example.cuisine_finder.models;

public class Friendship {
    private String id;
    private String requesterId;
    private String receiverId;

    public Friendship() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }



    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }



}
