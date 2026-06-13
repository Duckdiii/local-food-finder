package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.ChatRoomAdapter;
import com.example.cuisine_finder.models.ChatRoom;
import com.example.cuisine_finder.repositories.ChatRepository;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CommunityChatRoomsActivity extends AppCompatActivity {
    private final ChatRepository repository = new ChatRepository();
    private ChatRoomAdapter adapter;
    private TextView state;
    private ListenerRegistration roomsRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_community_chat_rooms);
        View root = findViewById(R.id.communityChatRoomsRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return windowInsets;
        });

        state = findViewById(R.id.tvChatRoomsState);
        RecyclerView roomsView = findViewById(R.id.rvCommunityChatRooms);
        roomsView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatRoomAdapter(this::openRoom);
        roomsView.setAdapter(adapter);
        findViewById(R.id.btnChatRoomsBack).setOnClickListener(view -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        state.setVisibility(View.VISIBLE);
        state.setText("Đang tải phòng chat...");
        roomsRegistration = repository.listenCommunityRooms((rooms, error) -> {
            if (error != null && rooms.isEmpty()) {
                state.setVisibility(View.VISIBLE);
                state.setText("Không tải được danh sách phòng chat");
                return;
            }

            List<ChatRoom> orderedRooms = new ArrayList<>(rooms);
            orderedRooms.sort(Comparator.comparingInt(room ->
                    ChatRepository.communityRoomOrder(room.getId())));
            adapter.submitList(orderedRooms);
            state.setVisibility(orderedRooms.isEmpty() ? View.VISIBLE : View.GONE);
            if (orderedRooms.isEmpty()) {
                state.setText("Chưa có phòng chat cộng đồng trên Firebase");
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (roomsRegistration != null) {
            roomsRegistration.remove();
            roomsRegistration = null;
        }
    }

    private void openRoom(ChatRoom room) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EXTRA_ROOM_ID, room.getId());
        intent.putExtra(
                ChatRoomActivity.EXTRA_ROOM_NAME,
                room.getName() == null || room.getName().trim().isEmpty()
                        ? ChatRoomAdapter.displayName(room.getId())
                        : room.getName()
        );
        startActivity(intent);
    }
}
