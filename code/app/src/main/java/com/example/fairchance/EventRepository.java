package com.example.fairchance;

import android.util.Log;
import androidx.annotation.NonNull;

// Import the models you created
import com.example.fairchance.models.Event;
import com.example.fairchance.models.EventHistoryItem;

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
 * Repository class for handling all database operations related to Events.
 * This class is designed to work with the defined Firestore structure.
 */
public class EventRepository {

    private static final String TAG = "EventRepository";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final CollectionReference eventsRef;
    private final CollectionReference usersRef;

    // --- Callback Interfaces ---
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

    // --- ADDED NEW INTERFACE ---
    public interface EventHistoryCheckCallback {
        /**
         * @param status The user's status ("Waiting", "Confirmed", etc.), or null if no record exists.
         */
        void onSuccess(String status);
        void onError(String message);
    }
    // --- END NEW INTERFACE ---

    public interface EventHistoryListCallback {
        void onSuccess(List<EventHistoryItem> historyItems);
        void onError(String message);
    }

    /**
     * Constructor
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
     * @param event The Event object to be added.
     * @param callback Notifies of success or failure.
     */
    public void createEvent(Event event, EventTaskCallback callback) {
        // Add a new document with a generated ID
        eventsRef.add(event)
                .addOnSuccessListener(documentReference -> {
                    // Get the new ID and write it back to the document
                    // This is optional but very good practice
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
     * @param eventId The unique ID of the event.
     * @param callback Returns the Event or an error.
     */
    public void getEvent(String eventId, EventCallback callback) {
        eventsRef.document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(documentSnapshot.getId()); // Set the ID
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
     * @param callback Returns the list of events or an error.
     */
    public void getAllEvents(EventListCallback callback) {
        eventsRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Event> eventList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            event.setEventId(document.getId()); // Set the ID
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
     * @param eventId The event to check.
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
     * Adds the current user to an event's waiting list.
     * This performs an atomic (all-or-nothing) write to two locations,
     * as per the database design:
     * 1. events/{EventID}/waitingList/{UserID}
     * 2. users/{UserID}/eventHistory/{EventID}
     *
     * Corresponds to US 01.01.01 and US 01.02.03.
     *
     * @param eventId The ID of the event to join.
     * @param event The Event object (needed for its name/date).
     * @param callback Notifies of success or failure.
     */
    public void joinWaitingList(String eventId, Event event, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        // 1. Create a WriteBatch for an atomic operation
        WriteBatch batch = db.batch();

        // 2. Define the reference for the event's waitingList sub-collection
        DocumentReference waitingListRef = eventsRef.document(eventId)
                .collection("waitingList").document(userId);

        // 3. Define the data for the waiting list
        Map<String, Object> waitingListData = new HashMap<>();
        waitingListData.put("joinedAt", com.google.firebase.Timestamp.now());
        // We would add Geopoint here if geolocation is enabled
        // waitingListData.put("location", new GeoPoint(lat, lon));
        batch.set(waitingListRef, waitingListData);

        // 4. Define the reference for the user's eventHistory sub-collection
        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);

        // 5. Define the denormalized data for the user's history
        Map<String, Object> eventHistoryData = new HashMap<>();
        eventHistoryData.put("eventName", event.getName());
        eventHistoryData.put("eventDate", event.getEventDate());
        eventHistoryData.put("status", "Waiting");
        batch.set(eventHistoryRef, eventHistoryData);

        // 6. Commit the atomic batch
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


    // --- ADDED NEW METHOD ---
    /**
     * Removes the current user from an event's waiting list.
     * This performs an atomic (all-or-nothing) write to two locations:
     * 1. Deletes events/{EventID}/waitingList/{UserID}
     * 2. Deletes users/{UserID}/eventHistory/{EventID}
     *
     * Corresponds to US 01.01.02.
     *
     * @param eventId The ID of the event to leave.
     * @param callback Notifies of success or failure.
     */
    public void leaveWaitingList(String eventId, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();

        // 1. Create a WriteBatch for an atomic operation
        WriteBatch batch = db.batch();

        // 2. Define the reference for the event's waitingList sub-collection
        DocumentReference waitingListRef = eventsRef.document(eventId)
                .collection("waitingList").document(userId);
        batch.delete(waitingListRef);

        // 3. Define the reference for the user's eventHistory sub-collection
        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);
        batch.delete(eventHistoryRef);

        // 4. Commit the atomic batch
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
    // --- END NEW METHOD ---


    /**
     * Retrieves the event history for the currently signed-in user.
     * Corresponds to US 01.02.03.
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
                .orderBy("eventDate", Query.Direction.DESCENDING) // Show newest first
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<EventHistoryItem> historyList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EventHistoryItem item = document.toObject(EventHistoryItem.class);
                            item.setEventId(document.getId()); // Store the document ID
                            historyList.add(item);
                        }
                        callback.onSuccess(historyList);
                    } else {
                        Log.e(TAG, "Error getting event history: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }


    // --- ADDED NEW METHOD ---
    /**
     * Checks the user's history for a specific event to see their status.
     * @param eventId The event to check.
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
                            callback.onSuccess(status); // e.g., "Waiting", "Confirmed"
                        } else {
                            callback.onSuccess(null); // User is not associated with this event
                        }
                    } else {
                        Log.e(TAG, "Error checking event status: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }
    // --- END NEW METHOD ---

    // TODO: We will add more methods here later, such as:
    // - getPendingInvitations(String userId, ...)
    // - respondToInvitation(String eventId, boolean didAccept, ...)
    // - runLottery(String eventId, int count, ...)
    // - getWaitingList(String eventId, ...)
}