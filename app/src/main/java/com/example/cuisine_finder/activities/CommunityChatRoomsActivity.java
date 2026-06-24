package com.example.cuisine_finder.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CommunityChatRoomsActivity extends AppCompatActivity {
    private final ChatRepository repository = new ChatRepository();
    private ChatRoomAdapter adapter;
    private ChatRoomAdapter myGroupsAdapter;
    private TextView tvState;
    private TextView tvMyGroupsState;
    private TextView tvMyGroupsCount;
    private LinearLayout layoutMyGroups;
    private ListenerRegistration roomsRegistration;
    private ListenerRegistration myGroupsRegistration;
    private FirebaseUser currentUser;

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

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        tvState = findViewById(R.id.tvChatRoomsState);
        tvMyGroupsState = findViewById(R.id.tvMyGroupsState);
        tvMyGroupsCount = findViewById(R.id.tvMyGroupsCount);
        layoutMyGroups = findViewById(R.id.layoutMyGroups);

        // Public rooms RecyclerView
        RecyclerView rvPublic = findViewById(R.id.rvCommunityChatRooms);
        rvPublic.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatRoomAdapter(this::openRoom);
        rvPublic.setAdapter(adapter);

        // My groups RecyclerView
        RecyclerView rvMyGroups = findViewById(R.id.rvMyGroups);
        rvMyGroups.setLayoutManager(new LinearLayoutManager(this));
        myGroupsAdapter = new ChatRoomAdapter(this::openRoom);
        rvMyGroups.setAdapter(myGroupsAdapter);

        // Show "My Groups" section only when logged in
        if (currentUser != null) {
            layoutMyGroups.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnChatRoomsBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCreateRoom).setOnClickListener(v -> showCreateRoomDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Listen public community rooms
        tvState.setVisibility(View.VISIBLE);
        tvState.setText("Đang tải phòng chat...");
        roomsRegistration = repository.listenCommunityRooms((rooms, error) -> {
            if (error != null && rooms.isEmpty()) {
                tvState.setVisibility(View.VISIBLE);
                tvState.setText("Không tải được danh sách phòng chat");
                return;
            }
            List<ChatRoom> ordered = new ArrayList<>(rooms);
            ordered.sort(Comparator.comparingInt(r -> ChatRepository.communityRoomOrder(r.getId())));
            adapter.submitList(ordered);
            tvState.setVisibility(ordered.isEmpty() ? View.VISIBLE : View.GONE);
            if (ordered.isEmpty()) tvState.setText("Chưa có phòng chat cộng đồng");
        });

        // Listen user's group rooms
        if (currentUser != null) {
            tvMyGroupsState.setVisibility(View.VISIBLE);
            tvMyGroupsState.setText("Đang tải...");
            myGroupsRegistration = repository.listenUserGroupRooms(currentUser.getUid(), (rooms, error) -> {
                myGroupsAdapter.submitList(rooms);
                if (rooms.isEmpty()) {
                    tvMyGroupsState.setVisibility(View.VISIBLE);
                    tvMyGroupsState.setText("Bạn chưa có nhóm nào. Nhấn + để tạo nhóm mới!");
                    tvMyGroupsCount.setVisibility(View.GONE);
                } else {
                    tvMyGroupsState.setVisibility(View.GONE);
                    tvMyGroupsCount.setVisibility(View.VISIBLE);
                    tvMyGroupsCount.setText(rooms.size() + " nhóm");
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (roomsRegistration != null) { roomsRegistration.remove(); roomsRegistration = null; }
        if (myGroupsRegistration != null) { myGroupsRegistration.remove(); myGroupsRegistration = null; }
    }

    private void openRoom(ChatRoom room) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra(ChatRoomActivity.EXTRA_ROOM_ID, room.getId());
        boolean hasCustomName = room.getName() != null
                && !room.getName().isEmpty()
                && !room.getName().equals(room.getId());
        intent.putExtra(ChatRoomActivity.EXTRA_ROOM_NAME,
                hasCustomName ? room.getName() : ChatRoomAdapter.displayName(room.getId()));
        intent.putExtra(ChatRoomActivity.EXTRA_ROOM_TYPE, room.getType());

        // Forward restaurant share extras if present
        if (getIntent().hasExtra(ChatRoomActivity.EXTRA_RESTAURANT_ID)) {
            intent.putExtra(ChatRoomActivity.EXTRA_RESTAURANT_ID, getIntent().getStringExtra(ChatRoomActivity.EXTRA_RESTAURANT_ID));
            intent.putExtra(ChatRoomActivity.EXTRA_RESTAURANT_NAME, getIntent().getStringExtra(ChatRoomActivity.EXTRA_RESTAURANT_NAME));
            intent.putExtra(ChatRoomActivity.EXTRA_RESTAURANT_RATING, getIntent().getDoubleExtra(ChatRoomActivity.EXTRA_RESTAURANT_RATING, 0.0));
            if (getIntent().hasExtra(ChatRoomActivity.EXTRA_RESTAURANT_IMAGE)) {
                intent.putExtra(ChatRoomActivity.EXTRA_RESTAURANT_IMAGE, getIntent().getStringExtra(ChatRoomActivity.EXTRA_RESTAURANT_IMAGE));
            }
        }

        startActivity(intent);

        // If we are in sharing mode, finish the room picker activity after launching ChatRoomActivity
        if (getIntent().hasExtra(ChatRoomActivity.EXTRA_RESTAURANT_ID)) {
            finish();
        }
    }

    private void showCreateRoomDialog() {
        if (currentUser == null) {
            Toast.makeText(this, "Đăng nhập để tạo nhóm", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_community_room, null);
        EditText etName = dialogView.findViewById(R.id.etRoomName);
        EditText etDesc = dialogView.findViewById(R.id.etRoomDescription);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tạo nhóm chat")
                .setView(dialogView)
                .setPositiveButton("Tạo nhóm", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Vui lòng nhập tên nhóm");
                return;
            }
            String desc = etDesc.getText().toString().trim();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            repository.createCommunityRoom(name, desc, currentUser.getUid())
                    .addOnSuccessListener(docRef -> {
                        dialog.dismiss();
                        Intent intent = new Intent(this, ChatRoomActivity.class);
                        intent.putExtra(ChatRoomActivity.EXTRA_ROOM_ID, docRef.getId());
                        intent.putExtra(ChatRoomActivity.EXTRA_ROOM_NAME, name);
                        intent.putExtra(ChatRoomActivity.EXTRA_ROOM_TYPE, ChatRoom.TYPE_COMMUNITY);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(this, "Có lỗi khi tạo nhóm, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    });
        }));

        dialog.show();
    }
}
