package com.example.fairchance;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.fairchance.models.Event;
import com.example.fairchance.models.EventHistoryItem;
import com.example.fairchance.models.Invitation;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class implementing the "single source of truth" pattern for all
 * event-related data. It handles all interactions with the 'events' collection,
 * as well as its sub-collections and related user history.
 */
public class EventRepository {

    private static final String TAG = "EventRepository";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final CollectionReference eventsRef;
    private final CollectionReference usersRef;

    //region Callback Interfaces
    public interface EventTaskCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface EventCallback {
        void onSuccess(Event event);
        void onError(String message);
    }

    public interface EventListCallback {
        void onSuccess(List<Event> events);
        void onError(String message);
    }

    public interface WaitlistCountCallback {
        void onSuccess(int count);
        void onError(String message);
    }

    public interface EventHistoryCheckCallback {
        void onSuccess(String status);
        void onError(String message);
    }

    public interface EventHistoryListCallback {
        void onSuccess(List<EventHistoryItem> historyItems);
        void onError(String message);
    }

    public interface InvitationListCallback {
        void onSuccess(List<Invitation> historyItems);
        void onError(String message);
    }
    //endregion

    /**
     * Initializes the repository with instances of FirebaseAuth and FirebaseFirestore
     * and sets up references to the main collections.
     */
    public EventRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.eventsRef = db.collection("events");
        this.usersRef = db.collection("users");
    }

    /**
     * Creates a new event in the 'events' collection.
     * Corresponds to US 02.01.01.
     *
     * @param event    The Event object to be added.
     * @param callback Notifies of success or failure.
     */
    public void createEvent(Event event, EventTaskCallback callback) {
        eventsRef.add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
                    documentReference.update("eventId", eventId)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Event created with ID: " + eventId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating event with its ID", e);
                                callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating event", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Retrieves a single Event object from Firestore by its ID.
     *
     * @param eventId  The unique ID of the event.
     * @param callback Returns the Event or an error.
     */
    public void getEvent(String eventId, EventCallback callback) {
        eventsRef.document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(documentSnapshot.getId());
                            callback.onSuccess(event);
                        } else {
                            callback.onError("Failed to parse event object.");
                        }
                    } else {
                        callback.onError("Event not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting event", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Retrieves all events from the 'events' collection.
     * Corresponds to US 01.01.03.
     *
     * @param callback Returns the list of events or an error.
     */
    public void getAllEvents(EventListCallback callback) {
        eventsRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Event> eventList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            event.setEventId(document.getId());
                            eventList.add(event);
                        }
                        callback.onSuccess(eventList);
                    } else {
                        Log.e(TAG, "Error getting all events: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Gets the current number of users on a specific event's waiting list.
     * Note: This can be expensive if the list is large.
     *
     * @param eventId  The event to check.
     * @param callback Returns the count of documents or an error.
     */
    public void getWaitingListCount(String eventId, WaitlistCountCallback callback) {
        eventsRef.document(eventId).collection("waitingList")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        callback.onSuccess(count);
                    } else {
                        Log.e(TAG, "Error getting waitlist count: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Adds the current user to an event's waiting list using an atomic batch write.
     * 1. Creates a doc in events/{EventID}/waitingList/{UserID}
     * 2. Creates a doc in users/{UserID}/eventHistory/{EventID}
     * Corresponds to US 01.01.01 and US 01.02.03.
     *
     * @param eventId  The ID of the event to join.
     * @param event    The Event object (needed for its name/date).
     * @param callback Notifies of success or failure.
     */
    public void joinWaitingList(String eventId, Event event, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        WriteBatch batch = db.batch();

        DocumentReference waitingListRef = eventsRef.document(eventId)
                .collection("waitingList").document(userId);

        Map<String, Object> waitingListData = new HashMap<>();
        waitingListData.put("joinedAt", com.google.firebase.Timestamp.now());
        // We would add Geopoint here if geolocation is enabled
        // waitingListData.put("location", new GeoPoint(lat, lon));
        batch.set(waitingListRef, waitingListData);

        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);

        Map<String, Object> eventHistoryData = new HashMap<>();
        eventHistoryData.put("eventName", event.getName());
        eventHistoryData.put("eventDate", event.getEventDate());
        eventHistoryData.put("status", "Waiting");
        batch.set(eventHistoryRef, eventHistoryData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " successfully joined waiting list for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to join waiting list", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Removes the current user from an event's waiting list using an atomic batch write.
     * 1. Deletes events/{EventID}/waitingList/{UserID}
     * 2. Deletes users/{UserID}/eventHistory/{EventID}
     * Corresponds to US 01.01.02.
     *
     * @param eventId  The ID of the event to leave.
     * @param callback Notifies of success or failure.
     */
    public void leaveWaitingList(String eventId, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        WriteBatch batch = db.batch();

        DocumentReference waitingListRef = eventsRef.document(eventId)
                .collection("waitingList").document(userId);
        batch.delete(waitingListRef);

        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);
        batch.delete(eventHistoryRef);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " successfully left waiting list for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to leave waiting list", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Retrieves the event history for the currently signed-in user.
     * Corresponds to US 01.02.03.
     *
     * @param callback Returns the list of history items or an error.
     */
    public void getEventHistory(EventHistoryListCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        usersRef.document(userId).collection("eventHistory")
                .orderBy("eventDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<EventHistoryItem> historyList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EventHistoryItem item = document.toObject(EventHistoryItem.class);
                            item.setEventId(document.getId());
                            historyList.add(item);
                        }
                        callback.onSuccess(historyList);
                    } else {
                        Log.e(TAG, "Error getting event history: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Checks the user's history for a specific event to see their status.
     *
     * @param eventId  The event to check.
     * @param callback Returns the status string (e.g., "Waiting") or null if no record exists.
     */
    public void checkEventHistoryStatus(String eventId, EventHistoryCheckCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        usersRef.document(userId).collection("eventHistory").document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String status = document.getString("status");
                            callback.onSuccess(status);
                        } else {
                            callback.onSuccess(null); // User is not associated with this event
                        }
                    } else {
                        Log.e(TAG, "Error checking event status: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Retrieves all pending invitations for the current user.
     * (e.g., all events in their history with status "Selected")
     * Corresponds to US 01.05.02, US 01.05.03.
     *
     * @param callback Returns the list of pending invitations or an error.
     */
    public void getPendingInvitations(InvitationListCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        usersRef.document(userId).collection("eventHistory")
                .whereEqualTo("status", "Selected")
                .orderBy("eventDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Invitation> invitationList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Invitation item = document.toObject(Invitation.class);
                            item.setEventId(document.getId());
                            invitationList.add(item);
                        }
                        callback.onSuccess(invitationList);
                    } else {
                        Log.e(TAG, "Error getting pending invitations: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Allows a user to accept or decline a pending event invitation using an atomic batch write.
     * This updates the status in 3 places:
     * 1. users/{UserID}/eventHistory/{EventID} (status: "Confirmed" or "Declined")
     * 2. events/{EventID}/selected/{UserID} (status: "accepted" or "declined")
     * 3. (If accepted) events/{EventID}/confirmedAttendees/{UserID} (new document)
     *
     * @param eventId  The event to respond to.
     * @param accepted True if accepting, false if declining.
     * @param callback Notifies of success or failure.
     */
    public void respondToInvitation(String eventId, boolean accepted, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        WriteBatch batch = db.batch();

        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);
        String newStatus = accepted ? "Confirmed" : "Declined";
        batch.update(eventHistoryRef, "status", newStatus);

        DocumentReference selectedRef = eventsRef.document(eventId)
                .collection("selected").document(userId);
        String newSelectedStatus = accepted ? "accepted" : "declined";
        batch.update(selectedRef, "status", newSelectedStatus);

        if (accepted) {
            DocumentReference confirmedRef = eventsRef.document(eventId)
                    .collection("confirmedAttendees").document(userId);
            Map<String, Object> confirmedData = new HashMap<>();
            confirmedData.put("confirmedAt", com.google.firebase.Timestamp.now());
            batch.set(confirmedRef, confirmedData);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " responded to invitation for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to respond to invitation", e);
                    callback.onError(e.getMessage());
                });
    }

    // TODO: We will add more methods here later, such as:
    // - runLottery(String eventId, int count, ...)
    // - getWaitingList(String eventId, ...)
}