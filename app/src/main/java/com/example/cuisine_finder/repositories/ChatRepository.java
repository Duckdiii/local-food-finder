package com.example.cuisine_finder.repositories;

import androidx.annotation.Nullable;
import com.example.cuisine_finder.models.ChatMessage;
import com.example.cuisine_finder.models.ChatRoom;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class ChatRepository {
    public interface MessagesCallback {
        void onResult(List<ChatMessage> messages, @Nullable DocumentSnapshot oldest, @Nullable Exception error);
    }

    public interface RoomsCallback {
        void onResult(List<ChatRoom> rooms, @Nullable Exception error);
    }

    private static final int PAGE_SIZE = 50;
    private static final List<String> COMMUNITY_ROOM_IDS = Arrays.asList(
            "room_hcmute",
            "room_night_food",
            "room_saigon_food",
            "room_thu_duc_night"
    );
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference rooms = db.collection("chat_rooms");

    public static String roomIdForDistrict(String districtName) {
        String asciiDistrict = districtName.toLowerCase().replace('\u0111', 'd');
        String normalized = java.text.Normalizer.normalize(asciiDistrict, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return "hcm-" + normalized;
    }

    public Task<Void> ensureDistrictRoom(String districtName) {
        String id = roomIdForDistrict(districtName);
        ChatRoom room = new ChatRoom();
        room.setId(id);
        room.setType(ChatRoom.TYPE_DISTRICT);
        room.setName(districtName);
        room.setCreatedAt(System.currentTimeMillis());
        return rooms.document(id).set(room, SetOptions.merge());
    }

    public ListenerRegistration listenDistrictRooms(RoomsCallback callback) {
        return rooms.whereEqualTo("type", ChatRoom.TYPE_DISTRICT)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        callback.onResult(Collections.emptyList(), error);
                        return;
                    }
                    List<ChatRoom> result = new ArrayList<>();
                    for (QueryDocumentSnapshot document : snapshots) {
                        ChatRoom room = document.toObject(ChatRoom.class);
                        room.setId(document.getId());
                        result.add(room);
                    }
                    result.sort((first, second) -> Long.compare(second.getLastMessageAt(), first.getLastMessageAt()));
                    callback.onResult(result, null);
                });
    }

    public ListenerRegistration listenCommunityRooms(RoomsCallback callback) {
        return rooms.addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onResult(defaultCommunityRooms(), error);
                        return;
                    }
                    List<ChatRoom> result = defaultCommunityRooms();
                    if (snapshots != null) for (QueryDocumentSnapshot document : snapshots) {
                        String storedId = document.getString("id");
                        String storedRoomId = document.getString("roomId");
                        String storedSnakeRoomId = document.getString("room_id");
                        String storedName = document.getString("name");
                        String communityKey = communityRoomKey(
                                document.getId(), storedId, storedRoomId, storedSnakeRoomId, storedName);
                        if (communityKey == null) continue;
                        ChatRoom room = document.toObject(ChatRoom.class);
                        room.setId(document.getId());
                        result.set(COMMUNITY_ROOM_IDS.indexOf(communityKey), room);
                    }
                    callback.onResult(result, null);
                });
    }

    private String normalizedCommunityRoom(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase().replace(' ', '_');
        if (!normalized.startsWith("room_")) normalized = "room_" + normalized;
        return COMMUNITY_ROOM_IDS.contains(normalized) ? normalized : null;
    }

    private String communityRoomKey(String... values) {
        for (String value : values) {
            String key = normalizedCommunityRoom(value);
            if (key != null) return key;
        }
        return null;
    }

    private List<ChatRoom> defaultCommunityRooms() {
        List<ChatRoom> result = new ArrayList<>();
        for (String roomId : COMMUNITY_ROOM_IDS) {
            ChatRoom room = new ChatRoom();
            room.setId(roomId);
            room.setName(roomId);
            result.add(room);
        }
        return result;
    }

    public static int communityRoomOrder(String roomId) {
        int index = COMMUNITY_ROOM_IDS.indexOf(roomId);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    public Task<DocumentReference> createCommunityRoom(String name, String description, String creatorId) {
        List<String> members = new ArrayList<>();
        members.add(creatorId);
        Map<String, Object> data = new HashMap<>();
        data.put("name", name.trim());
        data.put("description", description != null ? description.trim() : "");
        data.put("type", ChatRoom.TYPE_COMMUNITY);
        data.put("createdBy", creatorId);
        data.put("memberIds", members);
        data.put("memberCount", 1);
        data.put("lastMessage", "");
        data.put("lastMessageAt", System.currentTimeMillis());
        data.put("createdAt", System.currentTimeMillis());
        return rooms.add(data);
    }

    public Task<Void> addMembersToRoom(String roomId, List<String> userIds) {
        return rooms.document(roomId).update("memberIds", FieldValue.arrayUnion(userIds.toArray(new Object[0])));
    }

    public ListenerRegistration listenUserGroupRooms(String userId, RoomsCallback callback) {
        return rooms.whereArrayContains("memberIds", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        callback.onResult(Collections.emptyList(), error);
                        return;
                    }
                    List<ChatRoom> result = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ChatRoom room = doc.toObject(ChatRoom.class);
                        room.setId(doc.getId());
                        result.add(room);
                    }
                    result.sort((a, b) -> Long.compare(b.getLastMessageAt(), a.getLastMessageAt()));
                    callback.onResult(result, null);
                });
    }

    public Task<DocumentSnapshot> getRoom(String roomId) {
        return rooms.document(roomId).get();
    }

    public void loadLatest(String roomId, MessagesCallback callback) {
        messageQuery(roomId).limit(PAGE_SIZE).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onResult(Collections.emptyList(), null, task.getException());
                return;
            }
            List<DocumentSnapshot> docs = task.getResult().getDocuments();
            callback.onResult(toAscendingMessages(docs), docs.isEmpty() ? null : docs.get(docs.size() - 1), null);
        });
    }

    public void loadOlder(String roomId, DocumentSnapshot oldest, MessagesCallback callback) {
        messageQuery(roomId).startAfter(oldest).limit(PAGE_SIZE).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                callback.onResult(Collections.emptyList(), oldest, task.getException());
                return;
            }
            List<DocumentSnapshot> docs = task.getResult().getDocuments();
            callback.onResult(toAscendingMessages(docs), docs.isEmpty() ? oldest : docs.get(docs.size() - 1), null);
        });
    }

    public ListenerRegistration listenRecentMessages(String roomId, MessagesCallback callback) {
        return messageQuery(roomId).limit(PAGE_SIZE).addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) {
                callback.onResult(Collections.emptyList(), null, error);
                return;
            }
            List<DocumentSnapshot> docs = snapshots.getDocuments();
            callback.onResult(toAscendingMessages(docs), docs.isEmpty() ? null : docs.get(docs.size() - 1), null);
        });
    }

    public Task<Void> sendMessage(ChatMessage message) {
        DocumentReference messageRef = messages(message.getRoomId()).document();
        message.setId(messageRef.getId());
        message.setCreatedAt(System.currentTimeMillis());
        message.setDeliveryState(ChatMessage.STATE_SENDING);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", message.getId());
        messageData.put("roomId", message.getRoomId());
        messageData.put("senderId", message.getSenderId());
        messageData.put("senderName", message.getSenderName());
        messageData.put("senderAvatarUrl", message.getSenderAvatar());
        messageData.put("message", message.getContent());
        messageData.put("imageUrl", message.getAttachmentUrl() == null ? "" : message.getAttachmentUrl());
        messageData.put("type", message.getType().toUpperCase());
        messageData.put("createdAt", message.getCreatedAt());
        messageData.put("deliveryState", ChatMessage.STATE_SENDING);
        messageData.put("reportCount", message.getReportCount());
        messageData.put("isDeleted", message.isDeleted());
        if (message.getRestaurantId() != null) {
            messageData.put("restaurantId", message.getRestaurantId());
            messageData.put("restaurantName", message.getRestaurantName());
            messageData.put("restaurantImageUrl", message.getRestaurantImageUrl());
            messageData.put("restaurantRating", message.getRestaurantRating());
        }

        Map<String, Object> roomUpdate = new HashMap<>();
        roomUpdate.put("lastMessage", message.getContent());
        roomUpdate.put("lastMessageAt", message.getCreatedAt());

        Task<Void> write = db.runBatch(batch -> {
            batch.set(messageRef, messageData);
            batch.set(rooms.document(message.getRoomId()), roomUpdate, SetOptions.merge());
        });
        write.addOnSuccessListener(unused -> messageRef.update("deliveryState", ChatMessage.STATE_SENT));
        return write;
    }

    public Task<Void> reportMessage(String roomId, String messageId, String userId, String reason) {
        DocumentReference ref = messages(roomId).document(messageId);
        DocumentReference reportRef = db.collection("reports")
                .document("MESSAGE_" + messageId + "_" + userId);
        return db.runTransaction(transaction -> {
            if (transaction.get(reportRef).exists()) return null;
            DocumentSnapshot snapshot = transaction.get(ref);
            Long current = snapshot.getLong("reportCount");
            long next = (current == null ? 0 : current) + 1;
            Map<String, Object> report = new HashMap<>();
            report.put("reporterId", userId);
            report.put("targetId", messageId);
            report.put("targetType", "MESSAGE");
            report.put("roomId", roomId);
            report.put("reason", reason);
            report.put("status", "PENDING");
            report.put("createdAt", System.currentTimeMillis());
            Map<String, Object> update = new HashMap<>();
            update.put("reportCount", next);
            if (next >= 5) update.put("isDeleted", true);
            transaction.set(reportRef, report);
            transaction.update(ref, update);
            return null;
        });
    }

    public Task<Void> permanentlyDeleteMessage(String roomId, String messageId) {
        return messages(roomId).document(messageId).delete();
    }

    private Query messageQuery(String roomId) {
        return messages(roomId).orderBy("createdAt", Query.Direction.DESCENDING);
    }

    private CollectionReference messages(String roomId) {
        return rooms.document(roomId).collection("messages");
    }

    private List<ChatMessage> toAscendingMessages(List<DocumentSnapshot> docs) {
        List<ChatMessage> result = new ArrayList<>();
        for (DocumentSnapshot document : docs) {
            ChatMessage message = toChatMessage(document, null);
            if (message != null) {
                result.add(message);
            }
        }
        result.sort((first, second) -> Long.compare(first.getCreatedAt(), second.getCreatedAt()));
        return result;
    }

    private void mergeMessageSnapshot(
            @Nullable List<DocumentSnapshot> docs,
            String roomId,
            Map<String, ChatMessage> merged,
            MessagesCallback callback,
            @Nullable Exception error
    ) {
        if (docs != null) {
            for (DocumentSnapshot document : docs) {
                ChatMessage message = toChatMessage(document, roomId);
                if (message != null) merged.put(document.getId(), message);
            }
        }
        List<ChatMessage> result = new ArrayList<>(merged.values());
        result.sort((first, second) -> Long.compare(first.getCreatedAt(), second.getCreatedAt()));
        if (result.size() > PAGE_SIZE) result = new ArrayList<>(result.subList(result.size() - PAGE_SIZE, result.size()));
        callback.onResult(result, null, result.isEmpty() ? error : null);
    }

    private ChatMessage toChatMessage(DocumentSnapshot document, @Nullable String fallbackRoomId) {
        ChatMessage message;
        try {
            message = document.toObject(ChatMessage.class);
        } catch (RuntimeException ignored) {
            message = null;
        }
        if (message == null) message = new ChatMessage();
        message.setId(document.getId());
        message.setRoomId(valueOrFallback(firstString(document, "roomId", "room_id"), fallbackRoomId));
        message.setSenderId(firstString(document, "senderId", "sender_id", "userId", "user_id"));
        message.setSenderName(firstString(document, "senderName", "sender_name", "userName", "user_name"));
        message.setSenderAvatar(firstString(document, "senderAvatar", "sender_avatar", "senderAvatarUrl", "avatarUrl", "avatar_url"));
        message.setContent(firstString(document, "content", "text", "message"));
        message.setAttachmentUrl(firstString(document, "attachmentUrl", "attachment_url", "imageUrl", "image_url"));
        message.setRestaurantId(firstString(document, "restaurantId", "restaurant_id"));
        message.setRestaurantName(firstString(document, "restaurantName", "restaurant_name"));
        message.setRestaurantImageUrl(firstString(document, "restaurantImageUrl", "restaurant_image_url"));
        String type = valueOrFallback(firstString(document, "type"), ChatMessage.TYPE_TEXT);
        message.setType(type.toLowerCase());
        message.setDeliveryState(valueOrFallback(firstString(document, "deliveryState", "delivery_state"), ChatMessage.STATE_SENT));
        message.setCreatedAt(firstLong(document, "createdAt", "created_at", "timestamp"));
        message.setDeleted(firstBoolean(document, "isDeleted", "is_deleted"));
        message.setReportCount((int) firstLong(document, "reportCount", "report_count"));
        Object rating = firstValue(document, "restaurantRating", "restaurant_rating");
        if (rating instanceof Number) message.setRestaurantRating(((Number) rating).doubleValue());
        return message;
    }

    private String firstString(DocumentSnapshot document, String... fieldsOrFallback) {
        for (String field : fieldsOrFallback) {
            Object value = document.get(field);
            if (value instanceof String && !((String) value).isEmpty()) return (String) value;
        }
        return null;
    }

    private String valueOrFallback(@Nullable String value, @Nullable String fallback) {
        return value == null ? fallback : value;
    }

    private Object firstValue(DocumentSnapshot document, String... fields) {
        for (String field : fields) {
            Object value = document.get(field);
            if (value != null) return value;
        }
        return null;
    }

    private long firstLong(DocumentSnapshot document, String... fields) {
        Object value = firstValue(document, fields);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        return 0L;
    }

    private boolean firstBoolean(DocumentSnapshot document, String... fields) {
        Object value = firstValue(document, fields);
        return value instanceof Boolean && (Boolean) value;
    }
}
