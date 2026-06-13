package com.example.cuisine_finder.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.ChatMessage;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {
    public interface MessageActionListener {
        void onLongClick(ChatMessage message);
        void onRestaurantClick(ChatMessage message);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String currentUserId;
    private final MessageActionListener listener;

    public ChatMessageAdapter(String currentUserId, MessageActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void replaceRecent(List<ChatMessage> recent) {
        List<ChatMessage> older = new ArrayList<>();
        List<ChatMessage> optimistic = new ArrayList<>();
        long firstRecent = recent.isEmpty() ? Long.MIN_VALUE : recent.get(0).getCreatedAt();
        for (ChatMessage message : messages) {
            boolean local = message.getId() != null && message.getId().startsWith("local-");
            if (!local && !recent.isEmpty()) {
                if (message.getCreatedAt() < firstRecent) older.add(message);
            }
            if (local) optimistic.add(message);
        }
        messages.clear();
        messages.addAll(older);
        messages.addAll(recent);
        messages.addAll(optimistic);
        notifyDataSetChanged();
    }

    public void prepend(List<ChatMessage> older) {
        messages.addAll(0, older);
        notifyItemRangeInserted(0, older.size());
    }

    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public void addOptimistic(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeById(String messageId) {
        for (int index = 0; index < messages.size(); index++) {
            if (messageId.equals(messages.get(index).getId())) {
                messages.remove(index);
                notifyItemRemoved(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.sender.setText(currentUserId.equals(message.getSenderId()) ? "Bạn" : message.getSenderName());
        holder.content.setText(message.isDeleted() ? "Tin nhắn đã bị ẩn để chờ kiểm duyệt" : message.getContent());
        holder.content.setTextColor(holder.itemView.getContext().getColor(message.isDeleted() ? R.color.text_gray : R.color.text_dark));
        holder.time.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(message.getCreatedAt())));
        holder.status.setText(ChatMessage.STATE_SENDING.equals(message.getDeliveryState()) ? "⌛ Đang gửi..." : "");

        if (message.getSenderAvatar() != null && !message.getSenderAvatar().trim().isEmpty()) {
            Glide.with(holder.avatar)
                    .load(message.getSenderAvatar())
                    .placeholder(R.drawable.bg_avatar_orange)
                    .error(R.drawable.bg_avatar_orange)
                    .circleCrop()
                    .into(holder.avatar);
        } else {
            Glide.with(holder.avatar)
                    .load(R.drawable.bg_avatar_orange)
                    .circleCrop()
                    .into(holder.avatar);
        }

        boolean image = ChatMessage.TYPE_IMAGE.equals(message.getType()) && !message.isDeleted();
        holder.image.setVisibility(image ? View.VISIBLE : View.GONE);
        if (image) Glide.with(holder.image).load(message.getAttachmentUrl()).into(holder.image);
        else Glide.with(holder.image).clear(holder.image);

        boolean restaurant = ChatMessage.TYPE_RESTAURANT_CARD.equals(message.getType()) && !message.isDeleted();
        holder.restaurantCard.setVisibility(restaurant ? View.VISIBLE : View.GONE);
        if (restaurant) {
            holder.restaurantName.setText(message.getRestaurantName());
            holder.restaurantRating.setText("★ " + message.getRestaurantRating());
            boolean hasImage = message.getRestaurantImageUrl() != null && !message.getRestaurantImageUrl().isEmpty();
            holder.restaurantImage.setVisibility(View.VISIBLE);
            if (hasImage) {
                Glide.with(holder.restaurantImage)
                        .load(message.getRestaurantImageUrl())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .into(holder.restaurantImage);
            } else {
                Glide.with(holder.restaurantImage).load(R.drawable.bg_image_placeholder).into(holder.restaurantImage);
            }
            holder.restaurantCard.setOnClickListener(view -> listener.onRestaurantClick(message));
        }
        holder.itemView.setOnLongClickListener(view -> {
            listener.onLongClick(message);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView sender;
        final TextView content;
        final TextView time;
        final TextView status;
        final ImageView avatar;
        final ImageView image;
        final LinearLayout restaurantCard;
        final ImageView restaurantImage;
        final TextView restaurantName;
        final TextView restaurantRating;

        MessageViewHolder(View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.tvMessageSender);
            content = itemView.findViewById(R.id.tvMessageContent);
            time = itemView.findViewById(R.id.tvMessageTime);
            status = itemView.findViewById(R.id.tvMessageReports);
            avatar = itemView.findViewById(R.id.ivMessageAvatar);
            image = itemView.findViewById(R.id.ivMessageImage);
            restaurantCard = itemView.findViewById(R.id.layoutRestaurantCard);
            restaurantImage = itemView.findViewById(R.id.ivRestaurantCard);
            restaurantName = itemView.findViewById(R.id.tvRestaurantName);
            restaurantRating = itemView.findViewById(R.id.tvRestaurantRating);
        }
    }
}
