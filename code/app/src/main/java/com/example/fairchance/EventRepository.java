package com.example.fairchance;

import android.util.Log;
import android.net.Uri;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.Date;
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
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
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
     * Retrieves all events from the 'events' collection in real-time.
     * Filters for events currently open for registration (US 01.01.03).
     *
     * @param callback Returns the list of events or an error.
     * @return A ListenerRegistration object to stop listening for updates.
     */
    public ListenerRegistration getAllEvents(EventListCallback callback) {
        return eventsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error getting all events in real-time: ", error);
                callback.onError(error.getMessage());
                return;
            }

            if (value != null) {
                List<Event> eventList = new ArrayList<>();
                Date now = new Date(); // Current time for registration filtering

                for (QueryDocumentSnapshot document : value) {
                    Event event = document.toObject(Event.class);
                    event.setEventId(document.getId());

                    // Logic for US 01.01.03 Criterion 3: Only show events open for registration
                    Date registrationStart = event.getRegistrationStart();
                    Date registrationEnd = event.getRegistrationEnd();

                    boolean isRegistrationOpen =
                            (registrationStart == null || registrationStart.before(now)) &&
                                    (registrationEnd == null || registrationEnd.after(now));

                    if (isRegistrationOpen) {
                        eventList.add(event);
                    }
                }
                callback.onSuccess(eventList);
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
    /**
     * Adds the current user to an event's waiting list, **respecting the optional waitingListLimit**.
     * If waitingListLimit <= 0, the list is treated as unlimited.
     */
    public void joinWaitingList(String eventId, Event event, EventTaskCallback callback) {
        long limit = event.getWaitingListLimit();   // 0 or less = "no limit"

        if (limit > 0) {
            // Check how many people are already on the waiting list
            getWaitingListCount(eventId, new WaitlistCountCallback() {
                @Override
                public void onSuccess(int count) {
                    if (count >= limit) {
                        // Reject join: waiting list is full
                        callback.onError("The waiting list for this event is full.");
                    } else {
                        // Still room – proceed as before
                        joinWaitingListInternal(eventId, event, callback);
                    }
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } else {
            // No limit set – behave as before
            joinWaitingListInternal(eventId, event, callback);
        }
    }

    /**
     * Same as joinWaitingList but saves location if provided, **respecting waitingListLimit**.
     */
    public void joinWaitingListWithLocation(
            String eventId,
            Event event,
            Double lat,
            Double lng,
            EventTaskCallback callback
    ) {
        long limit = event.getWaitingListLimit();

        if (limit > 0) {
            getWaitingListCount(eventId, new WaitlistCountCallback() {
                @Override
                public void onSuccess(int count) {
                    if (count >= limit) {
                        callback.onError("The waiting list for this event is full.");
                    } else {
                        joinWaitingListWithLocationInternal(eventId, event, lat, lng, callback);
                    }
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } else {
            joinWaitingListWithLocationInternal(eventId, event, lat, lng, callback);
        }
    }


    private void joinWaitingListInternal(String eventId, Event event, EventTaskCallback callback) {
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

    private void joinWaitingListWithLocationInternal(
            String eventId,
            Event event,
            Double lat,
            Double lng,
            EventTaskCallback callback
    ) {
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
        if (lat != null && lng != null) {
            waitingListData.put("location", new GeoPoint(lat, lng));
        }
        batch.set(waitingListRef, waitingListData, SetOptions.merge());

        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);

        Map<String, Object> eventHistoryData = new HashMap<>();
        eventHistoryData.put("eventName", event.getName());
        eventHistoryData.put("eventDate", event.getEventDate());
        eventHistoryData.put("status", "Waiting");
        batch.set(eventHistoryRef, eventHistoryData, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " successfully joined waiting list (with location) for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to join waiting list (with location)", e);
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
     * Updates one or more fields on an event document.
     */
    public void updateEventFields(String eventId, Map<String, Object> updates, EventTaskCallback callback) {
        eventsRef.document(eventId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }


    /**
     * Updates the user's status for a specific event in their event history.
     * This is typically used by the system to mark an event as "Not selected" or for
     * organizer/admin actions.
     *
     * @param eventId  The ID of the event to update.
     * @param newStatus The new status to set (e.g., "Not selected", "Cancelled").
     * @param callback Notifies of success or failure.
     */
    public void updateEventHistoryStatus(String eventId, String newStatus, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            return;
        }
        String userId = user.getUid();
        DocumentReference eventHistoryRef = usersRef.document(userId)
                .collection("eventHistory").document(eventId);
        eventHistoryRef.update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + userId + " history status updated to " + newStatus + " for " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update event history status", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Uploads an image to Firebase Storage and updates posterImageUrl on the event.
     */
    public void uploadPosterAndUpdate(String eventId, Uri imageUri, EventTaskCallback callback) {
        if (imageUri == null) {
            callback.onError("No image selected.");
            return;
        }
        String fileName = "poster_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference()
                .child("event_posters")
                .child(eventId)
                .child(fileName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("posterImageUrl", uri.toString());
                            updateEventFields(eventId, updates, callback);
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }


    /**
     * Retrieves the event history for the currently signed-in user in real-time.
     * Corresponds to US 01.02.03. (FIX: Now real-time)
     *
     * @param callback Returns the list of history items or an error.
     * @return A ListenerRegistration object to stop listening for updates.
     */
    public ListenerRegistration getEventHistory(EventHistoryListCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
            // Return a no-op registration if no user to prevent crashes
            return () -> {};
        }
        String userId = user.getUid();

        return usersRef.document(userId).collection("eventHistory")
                .orderBy("eventDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting event history in real-time: ", error);
                        callback.onError(error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<EventHistoryItem> historyList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : value) {
                            EventHistoryItem item = document.toObject(EventHistoryItem.class);
                            item.setEventId(document.getId());
                            historyList.add(item);
                        }
                        callback.onSuccess(historyList);
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
     * Samples N entrants from events/{eventId}/waitingList into events/{eventId}/selected.
     * Skips users already in 'selected'. Also sets users/{uid}/eventHistory/{eventId}.status = "Selected".
     * Firestore-only (Spark/free).
     */
    public void sampleAttendees(String eventId, int count, EventTaskCallback callback) {
        eventsRef.document(eventId).collection("selected").get()
                .addOnSuccessListener(selSnap -> {
                    java.util.Set<String> already = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot d : selSnap) {
                        already.add(d.getId());
                    }

                    eventsRef.document(eventId).collection("waitingList").get()
                            .addOnSuccessListener(waitSnap -> {
                                java.util.List<String> pool = new java.util.ArrayList<>();
                                for (com.google.firebase.firestore.QueryDocumentSnapshot d : waitSnap) {
                                    String uid = d.getId();
                                    if (!already.contains(uid)) pool.add(uid);
                                }
                                if (pool.isEmpty()) { callback.onError("No entrants to sample."); return; }

                                java.util.Collections.shuffle(pool);
                                int take = Math.min(count, pool.size());

                                com.google.firebase.firestore.WriteBatch batch = db.batch();
                                com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

                                for (int i = 0; i < take; i++) {
                                    String uid = pool.get(i);

                                    // events/{eventId}/selected/{uid}
                                    DocumentReference selRef = eventsRef.document(eventId)
                                            .collection("selected").document(uid);
                                    java.util.Map<String,Object> selData = new java.util.HashMap<>();
                                    selData.put("status", "pending");
                                    selData.put("sampledAt", now);
                                    batch.set(selRef, selData);

                                    // users/{uid}/eventHistory/{eventId} -> Selected (merge)
                                    DocumentReference histRef = usersRef.document(uid)
                                            .collection("eventHistory").document(eventId);
                                    java.util.Map<String,Object> hist = new java.util.HashMap<>();
                                    hist.put("status", "Selected");
                                    hist.put("updatedAt", now);
                                    batch.set(histRef, hist, com.google.firebase.firestore.SetOptions.merge());
                                }

                                batch.commit()
                                        .addOnSuccessListener(a -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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
    /**
     * Marks all events created by a given organizer as DEACTIVATED.
     */
    public void deactivateEventsByOrganizer(String organizerId, EventTaskCallback callback) {
        eventsRef.whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Nothing to update
                        callback.onSuccess();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.update(doc.getReference(), "status", "DEACTIVATED");
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }


    // TODO: We will add more methods here later, such as:
    // - runLottery(String eventId, int count, ...)
    // - getWaitingList(String eventId, ...)
}