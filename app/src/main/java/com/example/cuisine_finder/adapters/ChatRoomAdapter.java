package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.ChatRoom;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.RoomViewHolder> {
    public interface OnRoomClickListener {
        void onRoomClick(ChatRoom room);
    }

    private final List<ChatRoom> rooms = new ArrayList<>();
    private final OnRoomClickListener listener;

    public ChatRoomAdapter(OnRoomClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ChatRoom> newRooms) {
        rooms.clear();
        rooms.addAll(newRooms);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        ChatRoom room = rooms.get(position);

        // For user-created community rooms, use the room's actual name/description/icon
        boolean isUserGroup = ChatRoom.TYPE_COMMUNITY.equals(room.getType())
                && room.getName() != null && !room.getName().equals(room.getId());

        holder.name.setText(isUserGroup ? room.getName() : displayName(room.getId()));

        String desc = (isUserGroup && room.getDescription() != null && !room.getDescription().isEmpty())
                ? room.getDescription()
                : description(room.getId());
        holder.description.setText(desc);

        holder.icon.setText(isUserGroup ? "👥" : icon(room.getId()));

        holder.lastMessage.setText(room.getLastMessage() == null || room.getLastMessage().isEmpty()
                ? "Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện."
                : room.getLastMessage());
        holder.members.setText(room.getMemberCount() > 0
                ? room.getMemberCount() + " thành viên"
                : "Phòng chat cộng đồng");
        holder.time.setText(room.getLastMessageAt() == 0
                ? ""
                : DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(room.getLastMessageAt())));
        holder.itemView.setOnClickListener(v -> listener.onRoomClick(room));
    }

    public static String displayName(String roomId) {
        if (roomId != null && roomId.startsWith("room_")) roomId = roomId.substring(5);
        if ("hcmute".equals(roomId)) return "Cộng đồng HCMUTE";
        if ("night_food".equals(roomId)) return "Hội ăn đêm";
        if ("saigon_food".equals(roomId)) return "Ẩm thực Sài Gòn";
        if ("thu_duc_night".equals(roomId)) return "Ăn khuya Thủ Đức";
        return roomId == null ? "Phòng cộng đồng" : roomId;
    }

    private String description(String roomId) {
        if (roomId != null && roomId.startsWith("room_")) roomId = roomId.substring(5);
        if ("hcmute".equals(roomId)) return "Chuyện ăn uống quanh trường HCMUTE";
        if ("night_food".equals(roomId)) return "Gợi ý món ngon và quán mở muộn";
        if ("saigon_food".equals(roomId)) return "Khám phá món ngon khắp Sài Gòn";
        if ("thu_duc_night".equals(roomId)) return "Điểm ăn khuya quanh khu vực Thủ Đức";
        return "Trò chuyện cùng cộng đồng";
    }

    private String icon(String roomId) {
        if (roomId != null && roomId.startsWith("room_")) roomId = roomId.substring(5);
        if ("hcmute".equals(roomId)) return "🎓";
        if ("night_food".equals(roomId)) return "🌙";
        if ("saigon_food".equals(roomId)) return "🍜";
        if ("thu_duc_night".equals(roomId)) return "🍢";
        return "💬";
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView description;
        final TextView icon;
        final TextView lastMessage;
        final TextView members;
        final TextView time;

        RoomViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvRoomName);
            description = itemView.findViewById(R.id.tvRoomDescription);
            icon = itemView.findViewById(R.id.tvRoomIcon);
            lastMessage = itemView.findViewById(R.id.tvRoomLastMessage);
            members = itemView.findViewById(R.id.tvRoomMembers);
            time = itemView.findViewById(R.id.tvRoomTime);
        }
    }
}
