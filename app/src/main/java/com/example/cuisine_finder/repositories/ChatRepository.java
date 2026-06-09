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
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    public interface MessagesCallback {
        void onResult(List<ChatMessage> messages, @Nullable DocumentSnapshot oldest, @Nullable Exception error);
    }

    public interface RoomsCallback {
        void onResult(List<ChatRoom> rooms, @Nullable Exception error);
    }

    private static final int PAGE_SIZE = 50;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference rooms = db.collection("chatRooms");

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

        Map<String, Object> roomUpdate = new HashMap<>();
        roomUpdate.put("lastMessage", message.getContent());
        roomUpdate.put("lastMessageAt", message.getCreatedAt());

        Task<Void> write = db.runBatch(batch -> {
            batch.set(messageRef, message);
            batch.set(rooms.document(message.getRoomId()), roomUpdate, SetOptions.merge());
        });
        write.addOnSuccessListener(unused -> messageRef.update("deliveryState", ChatMessage.STATE_SENT));
        return write;
    }

    public Task<Void> reportMessage(String roomId, String messageId, String userId, String reason) {
        DocumentReference ref = messages(roomId).document(messageId);
        DocumentReference reportRef = db.collection("messageReports").document(messageId + "_" + userId);
        return db.runTransaction(transaction -> {
            if (transaction.get(reportRef).exists()) return null;
            DocumentSnapshot snapshot = transaction.get(ref);
            Long current = snapshot.getLong("reportCount");
            long next = (current == null ? 0 : current) + 1;
            Map<String, Object> report = new HashMap<>();
            report.put("messageId", messageId);
            report.put("roomId", roomId);
            report.put("userId", userId);
            report.put("reason", reason);
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
            ChatMessage message = document.toObject(ChatMessage.class);
            if (message != null) {
                message.setId(document.getId());
                result.add(message);
            }
        }
        Collections.reverse(result);
        return result;
    }
}
