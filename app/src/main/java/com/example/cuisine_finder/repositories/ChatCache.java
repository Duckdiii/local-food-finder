package com.example.cuisine_finder.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.cuisine_finder.models.ChatMessage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChatCache {
    private static final int CACHE_LIMIT = 50;
    private final SharedPreferences preferences;

    public ChatCache(Context context) {
        preferences = context.getSharedPreferences("chat_message_cache", Context.MODE_PRIVATE);
    }

    public void save(String roomId, List<ChatMessage> messages) {
        JSONArray json = new JSONArray();
        int start = Math.max(0, messages.size() - CACHE_LIMIT);
        for (int i = start; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message.getId() != null && message.getId().startsWith("local-")) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("id", message.getId());
                item.put("roomId", message.getRoomId());
                item.put("senderId", message.getSenderId());
                item.put("senderName", message.getSenderName());
                item.put("senderAvatar", message.getSenderAvatar());
                item.put("content", message.getContent());
                item.put("attachmentUrl", message.getAttachmentUrl());
                item.put("restaurantId", message.getRestaurantId());
                item.put("restaurantName", message.getRestaurantName());
                item.put("restaurantImageUrl", message.getRestaurantImageUrl());
                item.put("restaurantRating", message.getRestaurantRating());
                item.put("type", message.getType());
                item.put("deliveryState", message.getDeliveryState());
                item.put("createdAt", message.getCreatedAt());
                item.put("isDeleted", message.isDeleted());
                item.put("reportCount", message.getReportCount());
                json.put(item);
            } catch (Exception ignored) {
            }
        }
        preferences.edit().putString(roomId, json.toString()).apply();
    }

    public List<ChatMessage> load(String roomId) {
        List<ChatMessage> messages = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(preferences.getString(roomId, "[]"));
            for (int i = 0; i < json.length(); i++) {
                JSONObject item = json.getJSONObject(i);
                ChatMessage message = new ChatMessage();
                message.setId(item.optString("id"));
                message.setRoomId(item.optString("roomId"));
                message.setSenderId(item.optString("senderId"));
                message.setSenderName(item.optString("senderName"));
                message.setSenderAvatar(item.optString("senderAvatar"));
                message.setContent(item.optString("content"));
                message.setAttachmentUrl(item.optString("attachmentUrl"));
                message.setRestaurantId(item.optString("restaurantId"));
                message.setRestaurantName(item.optString("restaurantName"));
                message.setRestaurantImageUrl(item.optString("restaurantImageUrl"));
                message.setRestaurantRating(item.optDouble("restaurantRating"));
                message.setType(item.optString("type", ChatMessage.TYPE_TEXT));
                message.setDeliveryState(item.optString("deliveryState", ChatMessage.STATE_SENT));
                message.setCreatedAt(item.optLong("createdAt"));
                message.setDeleted(item.optBoolean("isDeleted"));
                message.setReportCount(item.optInt("reportCount"));
                messages.add(message);
            }
        } catch (Exception ignored) {
        }
        return messages;
    }
}
