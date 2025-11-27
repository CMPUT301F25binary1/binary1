package com.example.fairchance;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fairchance.models.NotificationLog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repository for reading/writing notification logs in Firestore.
 * Collection: "notificationLogs"
 */
public class NotificationLogRepository {

    private static final String TAG = "NotificationLogRepo";

    public interface LogWriteCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface LogListCallback {
        void onSuccess(List<NotificationLog> logs);
        void onError(String message);
    }

    private final FirebaseFirestore db;
    private final CollectionReference logsRef;

    public NotificationLogRepository() {
        db = FirebaseFirestore.getInstance();
        logsRef = db.collection("notificationLogs");
    }

    /**
     * Writes a new notification log document.
     */
    public void logNotification(NotificationLog log, LogWriteCallback callback) {
        if (log.getTimestamp() == null) {
            log.setTimestamp(new Date());
        }
        if (log.getRecipientIds() != null) {
            log.setRecipientCount(log.getRecipientIds().size());
        }

        logsRef.add(log)
                .addOnSuccessListener(docRef -> {
                    log.setId(docRef.getId());
                    docRef.update("id", docRef.getId());
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing notification log", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Fetch all logs ordered by newest first.
     */
    public void fetchAllLogs(LogListCallback callback) {
        logsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> handleListResult(task, callback));
    }

    /**
     * Fetch logs, optionally filtered by date range and/or eventId.
     * Pass null for any filter you don't want to use.
     */
    public void fetchLogsFiltered(Date from, Date to, String eventId, LogListCallback callback) {
        Query query = logsRef;

        if (from != null) {
            query = query.whereGreaterThanOrEqualTo("timestamp", from);
        }
        if (to != null) {
            query = query.whereLessThanOrEqualTo("timestamp", to);
        }
        if (eventId != null && !eventId.isEmpty()) {
            query = query.whereEqualTo("eventId", eventId);
        }

        query = query.orderBy("timestamp", Query.Direction.DESCENDING);

        query.get().addOnCompleteListener(task -> handleListResult(task, callback));
    }

    private void handleListResult(Task<QuerySnapshot> task, LogListCallback callback) {
        if (!task.isSuccessful()) {
            Log.e(TAG, "Failed to fetch logs", task.getException());
            callback.onError(task.getException() != null
                    ? task.getException().getMessage()
                    : "Unknown error");
            return;
        }

        List<NotificationLog> result = new ArrayList<>();
        for (var doc : task.getResult()) {
            NotificationLog log = doc.toObject(NotificationLog.class);
            log.setId(doc.getId());
            result.add(log);
        }
        callback.onSuccess(result);
    }
}
