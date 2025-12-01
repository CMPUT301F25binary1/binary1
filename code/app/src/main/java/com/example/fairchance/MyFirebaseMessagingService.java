package com.example.fairchance;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * A background service that handles Firebase Cloud Messaging (FCM).
 * This service is responsible for two main tasks:
 * 1. Receiving new FCM registration tokens and saving them to the user's profile in Firestore.
 * 2. Receiving incoming push notifications (messages) and displaying them to the user.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "FAIR_CHANCE_CHANNEL";

    /**
     * Called when a new FCM registration token is generated or an existing one is refreshed.
     * This token is saved to the user's document in Firestore.
     *
     * @param token The new FCM token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        AuthRepository authRepository = new AuthRepository();
        authRepository.saveFcmToken(token);
    }

    /**
     * Called when a new FCM message is received while the app is in the foreground or background.
     *
     * @param remoteMessage Object representing the message received from FCM.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = null;
        String body = null;
        String navTarget = null;

        if (remoteMessage.getData().size() > 0) {
            String eventId = remoteMessage.getData().get("eventId");
            String status = remoteMessage.getData().get("status");

            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");


            if (eventId != null && status != null) {
                Log.d(TAG, "Handling status update for event: " + eventId + " to " + status);

                AuthRepository authRepository = new AuthRepository();
                if (authRepository.getCurrentUser() != null) {
                    EventRepository eventRepository = new EventRepository();
                    eventRepository.updateEventHistoryStatus(eventId, status, new EventRepository.EventTaskCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Event status updated successfully in history.");
                        }

                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "Failed to update event status from notification: " + message);
                        }
                    });
                }

                if ("Selected".equalsIgnoreCase(status)) {
                    navTarget = "NAV_TO_INVITATIONS";
                }
            }

            if (remoteMessage.getNotification() != null) {
                title = remoteMessage.getNotification().getTitle();
                body = remoteMessage.getNotification().getBody();
            } else if (title == null || body == null) {
                title = remoteMessage.getData().getOrDefault("title", "FairChance Update");
                body = remoteMessage.getData().getOrDefault("body", "You have a new status update.");
            }

            if ("NAV_TO_INVITATIONS".equals(navTarget)) {
                body = (body != null ? body : "") + " Tap to review and confirm your spot!";
            } else if ("Not selected".equalsIgnoreCase(status)) {
                body = (body != null ? body : "") + " Check back soon for future events and lottery draws!";
            }

            sendNotification(title, body, navTarget);

        } else if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);

            sendNotification(title, body, null);
        }
    }

    /**
     * Create and show a simple system notification.
     *
     * @param messageTitle The title of the notification.
     * @param messageBody  The body text of the notification.
     * @param navTarget    The target extra key to pass (e.g., "NAV_TO_INVITATIONS").
     */
    private void sendNotification(String messageTitle, String messageBody, String navTarget) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (navTarget != null) {
            intent.putExtra(navTarget, true);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_invitations_nav)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "FairChance Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}