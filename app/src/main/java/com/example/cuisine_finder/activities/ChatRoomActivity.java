package com.example.cuisine_finder.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.adapters.ChatMessageAdapter;
import com.example.cuisine_finder.models.ChatMessage;
import com.example.cuisine_finder.repositories.ChatCache;
import com.example.cuisine_finder.repositories.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import java.util.UUID;

public class ChatRoomActivity extends AppCompatActivity implements ChatMessageAdapter.MessageActionListener {
    public static final String EXTRA_ROOM_ID = "roomId";
    public static final String EXTRA_ROOM_NAME = "roomName";
    public static final String EXTRA_RESTAURANT_ID = "restaurantId";
    public static final String EXTRA_RESTAURANT_NAME = "restaurantName";
    public static final String EXTRA_RESTAURANT_RATING = "restaurantRating";
    public static final String EXTRA_RESTAURANT_IMAGE = "restaurantImage";
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final long RECONNECT_CHECK_MS = 3000L;

    private final ChatRepository repository = new ChatRepository();
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private ChatCache cache;
    private ChatMessageAdapter adapter;
    private RecyclerView recyclerView;
    private EditText input;
    private TextView connectionBanner;
    private DocumentSnapshot oldest;
    private ListenerRegistration messagesRegistration;
    private String roomId;
    private FirebaseUser user;
    private boolean loadingOlder;
    private ActivityResultLauncher<String> imagePicker;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Runnable reconnectCheck = new Runnable() {
        @Override
        public void run() {
            updateConnectionBanner();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_chat_room);
        View root = findViewById(R.id.chatRoomRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (roomId == null || user == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để sử dụng chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cache = new ChatCache(this);
        String roomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);
        ((TextView) findViewById(R.id.tvChatTitle)).setText(roomName);
        findViewById(R.id.btnChatBack).setOnClickListener(view -> finish());
        input = findViewById(R.id.etChatMessage);
        connectionBanner = findViewById(R.id.tvConnectionBanner);
        recyclerView = findViewById(R.id.rvMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ChatMessageAdapter(user.getUid(), this);
        recyclerView.setAdapter(adapter);
        adapter.replaceRecent(cache.load(roomId));
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::uploadImage);
        findViewById(R.id.btnSendMessage).setOnClickListener(view -> sendTextMessage());
        findViewById(R.id.btnPickImage).setOnClickListener(view -> imagePicker.launch("image/*"));
        setupPagination(layoutManager);
        setupCharacterCount();
        setupNetworkBanner();
        if (savedInstanceState == null) sendSharedRestaurantIfPresent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        messagesRegistration = repository.listenRecentMessages(roomId, (messages, cursor, error) -> {
            if (error != null) {
                showReconnectBanner();
                if (adapter.getItemCount() == 0) {
                    Toast.makeText(this, "Đang hiển thị dữ liệu offline", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            boolean nearBottom = !recyclerView.canScrollVertically(1);
            oldest = oldest == null ? cursor : oldest;
            adapter.replaceRecent(messages);
            cache.save(roomId, adapter.getMessages());
            if (nearBottom && adapter.getItemCount() > 0) recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesRegistration != null) messagesRegistration.remove();
        cache.save(roomId, adapter.getMessages());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        reconnectHandler.removeCallbacks(reconnectCheck);
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private void sendTextMessage() {
        String content = input.getText().toString().trim();
        if (content.isEmpty()) return;
        if (content.length() > 500) {
            Toast.makeText(this, "Tin nhắn tối đa 500 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        input.setText("");
        sendMessage(newMessage(ChatMessage.TYPE_TEXT, content));
    }

    private ChatMessage newMessage(String type, String content) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSenderId(user.getUid());
        message.setSenderName(displayName());
        message.setSenderAvatar(user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString());
        message.setContent(content);
        message.setType(type);
        return message;
    }

    private void sendMessage(ChatMessage message) {
        repository.sendMessage(message).addOnFailureListener(error ->
                Toast.makeText(this, "Tin nhắn đang chờ gửi lại khi có mạng", Toast.LENGTH_SHORT).show());
    }

    private void uploadImage(Uri uri) {
        if (uri == null) return;
        long size = getUriSize(uri);
        if (size < 0) {
            Toast.makeText(this, "Không thể đọc kích thước ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        if (size > MAX_IMAGE_BYTES) {
            Toast.makeText(this, "Ảnh vượt quá giới hạn 5MB", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage optimistic = newMessage(ChatMessage.TYPE_IMAGE, "Đang tải ảnh...");
        optimistic.setId("local-" + UUID.randomUUID());
        optimistic.setAttachmentUrl(uri.toString());
        optimistic.setCreatedAt(System.currentTimeMillis());
        optimistic.setDeliveryState(ChatMessage.STATE_SENDING);
        adapter.addOptimistic(optimistic);
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);

        com.google.firebase.storage.StorageReference reference = FirebaseStorage.getInstance().getReference()
                .child("chat_images/" + roomId + "/" + UUID.randomUUID());
        reference.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) throw task.getException();
            return reference.getDownloadUrl();
        }).addOnSuccessListener(downloadUri -> {
            adapter.removeById(optimistic.getId());
            ChatMessage message = newMessage(ChatMessage.TYPE_IMAGE, "Đã gửi một ảnh");
            message.setAttachmentUrl(downloadUri.toString());
            sendMessage(message);
        }).addOnFailureListener(error -> {
            adapter.removeById(optimistic.getId());
            Toast.makeText(this, "Tải ảnh thất bại, đã hoàn tác tin nhắn", Toast.LENGTH_SHORT).show();
        });
    }

    private long getUriSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (column >= 0 && !cursor.isNull(column)) return cursor.getLong(column);
            }
        } catch (Exception ignored) {
        }
        try (android.content.res.AssetFileDescriptor descriptor = getContentResolver().openAssetFileDescriptor(uri, "r")) {
            return descriptor == null ? -1 : descriptor.getLength();
        } catch (Exception ignored) {
            return -1;
        }
    }

    private void sendSharedRestaurantIfPresent() {
        String restaurantId = getIntent().getStringExtra(EXTRA_RESTAURANT_ID);
        if (restaurantId == null) return;
        ChatMessage message = newMessage(ChatMessage.TYPE_RESTAURANT_CARD, "Đã chia sẻ một quán ăn");
        message.setRestaurantId(restaurantId);
        message.setRestaurantName(getIntent().getStringExtra(EXTRA_RESTAURANT_NAME));
        message.setRestaurantRating(getIntent().getDoubleExtra(EXTRA_RESTAURANT_RATING, 0));
        message.setRestaurantImageUrl(getIntent().getStringExtra(EXTRA_RESTAURANT_IMAGE));
        sendMessage(message);
        getIntent().removeExtra(EXTRA_RESTAURANT_ID);
    }

    private void setupPagination(LinearLayoutManager layoutManager) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                if (dy < 0 && layoutManager.findFirstVisibleItemPosition() <= 3 && oldest != null && !loadingOlder) {
                    loadingOlder = true;
                    repository.loadOlder(roomId, oldest, (messages, cursor, error) -> {
                        oldest = cursor;
                        adapter.prepend(messages);
                        if (!messages.isEmpty()) recyclerView.scrollToPosition(messages.size());
                        loadingOlder = false;
                    });
                }
            }
        });
    }

    private void setupCharacterCount() {
        TextView count = findViewById(R.id.tvCharacterCount);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable text) { count.setText(text.length() + "/500"); }
        });
    }

    private void setupNetworkBanner() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    reconnectHandler.removeCallbacks(reconnectCheck);
                    connectionBanner.setVisibility(View.GONE);
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(ChatRoomActivity.this::showReconnectBanner);
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        updateConnectionBanner();
    }

    private void showReconnectBanner() {
        connectionBanner.setVisibility(View.VISIBLE);
        reconnectHandler.removeCallbacks(reconnectCheck);
        reconnectHandler.postDelayed(reconnectCheck, RECONNECT_CHECK_MS);
    }

    private void updateConnectionBanner() {
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        boolean connected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        connectionBanner.setVisibility(connected ? View.GONE : View.VISIBLE);
        if (!connected) {
            reconnectHandler.removeCallbacks(reconnectCheck);
            reconnectHandler.postDelayed(reconnectCheck, RECONNECT_CHECK_MS);
        }
    }

    @Override
    public void onLongClick(ChatMessage message) {
        if (message.getId() == null || message.getId().startsWith("local-")) return;
        if (user.getUid().equals(message.getSenderId())) {
            Toast.makeText(this, "Bạn không thể báo cáo tin nhắn của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Báo cáo tin nhắn không phù hợp")
                .setItems(new String[]{"Spam", "Nội dung không phù hợp", "Thông tin sai lệch"}, (dialog, which) ->
                        repository.reportMessage(roomId, message.getId(), user.getUid(),
                                        new String[]{"spam", "inappropriate", "misinformation"}[which])
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "Đã báo cáo tin nhắn", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(error ->
                                        Toast.makeText(this, "Không thể báo cáo tin nhắn", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    @Override
    public void onRestaurantClick(ChatMessage message) {
        Intent intent = new Intent(this, FoodPlaceDetailActivity.class);
        intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, message.getRestaurantId());
        startActivity(intent);
    }

    private String displayName() {
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) return user.getDisplayName();
        if (user.getEmail() != null) return user.getEmail().split("@")[0];
        return "Thành viên";
    }
}
