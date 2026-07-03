package com.example.cuisine_finder.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "cuisine_finder_notifications";
    private static final String CHANNEL_NAME = "Cuisine Finder Notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }

        if (title == null) title = "Thông báo mới";
        if (body == null) body = "Bạn có một thông báo mới từ Cuisine Finder";

        showNotification(title, body);
        saveNotificationToFirestore(title, body);
    }

    private void showNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void saveNotificationToFirestore(String title, String body) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return; // Only save to user notification list if logged in

        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("body", body);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("userId", userId);
        notif.put("read", false);

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .add(notif);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Usually we would send the token to our server, but here we can optionally associate it with the current user in Firestore.
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token);
        }
    }
}
