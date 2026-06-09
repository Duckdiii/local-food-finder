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
        holder.name.setText(room.getName());
        holder.lastMessage.setText(room.getLastMessage() == null || room.getLastMessage().isEmpty()
                ? "Chưa có tin nhắn. Hãy bắt đầu cuộc trò chuyện."
                : room.getLastMessage());
        holder.members.setText(room.getMemberCount() + " thành viên");
        holder.time.setText(room.getLastMessageAt() == 0 ? "" : DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(room.getLastMessageAt())));
        holder.itemView.setOnClickListener(v -> listener.onRoomClick(room));
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView lastMessage;
        final TextView members;
        final TextView time;

        RoomViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvRoomName);
            lastMessage = itemView.findViewById(R.id.tvRoomLastMessage);
            members = itemView.findViewById(R.id.tvRoomMembers);
            time = itemView.findViewById(R.id.tvRoomTime);
        }
    }
}
