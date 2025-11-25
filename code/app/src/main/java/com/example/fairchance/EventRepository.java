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

    public interface OrganizerNameCallback {
        void onSuccess(String organizerName);
        void onError(String message);
    }

    public interface SampleAttendeesCallback {
        void onSuccess(int selectedCount);
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
     * Also stores metadata used by the Admin Image browser:
     *  - createdById
     *  - createdByName
     *  - createdAt
     * @param event    The Event object to be added.
     * @param callback Notifies of success or failure.
     *
     */
    public void createEvent(Event event, EventTaskCallback callback) {

        // Get current user to tag as creator/uploader
        FirebaseUser user = auth.getCurrentUser();

        eventsRef.add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();

                    // Build metadata to merge into the event doc
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("eventId", eventId);
                    updates.put("createdAt", com.google.firebase.Timestamp.now());

                    if (user != null) {
                        String uploaderName = user.getDisplayName();
                        if (uploaderName == null || uploaderName.isEmpty()) {
                            uploaderName = user.getEmail();
                        }

                        updates.put("createdById", user.getUid());
                        if (uploaderName != null && !uploaderName.isEmpty()) {
                            updates.put("createdByName", uploaderName);
                        }
                    }

                    // Merge the metadata into the new event document
                    documentReference.set(updates, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Event created with ID: " + eventId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating event with metadata", e);
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
     * Adds the current user to an event's waiting list, respecting the optional waitingListLimit.
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
     * Same as joinWaitingList but saves location if provided, respecting waitingListLimit.
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
     * Also records who uploaded the poster and when.
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
                            updates.put("posterUploadedAt", com.google.firebase.Timestamp.now());

                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                String name = user.getDisplayName();
                                if (name == null || name.isEmpty()) {
                                    name = user.getEmail();
                                }
                                updates.put("posterUploadedById", user.getUid());
                                if (name != null && !name.isEmpty()) {
                                    updates.put("posterUploadedByName", name);
                                }
                            }

                            updateEventFields(eventId, updates, callback);
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Retrieves the event history for the currently signed-in user in real-time.
     * Corresponds to US 01.02.03.
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
     * If there are fewer available entrants than N, all remaining entrants are selected.
     */
    public void sampleAttendees(String eventId, int count, SampleAttendeesCallback callback) {
        eventsRef.document(eventId).collection("selected").get()
                .addOnSuccessListener(selSnap -> {
                    java.util.Set<String> already = new java.util.HashSet<>();
                    for (QueryDocumentSnapshot d : selSnap) {
                        already.add(d.getId());
                    }

                    eventsRef.document(eventId).collection("waitingList").get()
                            .addOnSuccessListener(waitSnap -> {
                                java.util.List<String> pool = new java.util.ArrayList<>();
                                for (QueryDocumentSnapshot d : waitSnap) {
                                    String uid = d.getId();
                                    if (!already.contains(uid)) {
                                        pool.add(uid);
                                    }
                                }

                                if (pool.isEmpty()) {
                                    callback.onError("No entrants to sample.");
                                    return;
                                }

                                java.util.Collections.shuffle(pool);
                                int take = Math.min(count, pool.size());

                                WriteBatch batch = db.batch();
                                com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

                                for (int i = 0; i < take; i++) {
                                    String uid = pool.get(i);

                                    // events/{eventId}/selected/{uid}
                                    DocumentReference selRef = eventsRef.document(eventId)
                                            .collection("selected").document(uid);
                                    Map<String, Object> selData = new HashMap<>();
                                    selData.put("status", "pending");
                                    selData.put("sampledAt", now);
                                    batch.set(selRef, selData);

                                    // users/{uid}/eventHistory/{eventId} -> Selected (merge)
                                    DocumentReference histRef = usersRef.document(uid)
                                            .collection("eventHistory").document(eventId);
                                    Map<String, Object> hist = new HashMap<>();
                                    hist.put("status", "Selected");
                                    hist.put("updatedAt", now);
                                    batch.set(histRef, hist, SetOptions.merge());
                                }

                                batch.commit()
                                        .addOnSuccessListener(a -> callback.onSuccess(take))
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Backwards-compatible overload for any existing callers that used EventTaskCallback
    public void sampleAttendees(String eventId, int count, EventTaskCallback callback) {
        sampleAttendees(eventId, count, new SampleAttendeesCallback() {
            @Override
            public void onSuccess(int selectedCount) {
                callback.onSuccess();
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
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

    /**
     * Admin: listen to ALL events (no registration-date filtering).
     * Used by the Admin Event Management screen.
     */
    public ListenerRegistration listenToAllEventsForAdmin(EventListCallback callback) {
        return eventsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error getting all events for admin: ", error);
                callback.onError(error.getMessage());
                return;
            }

            if (value != null) {
                List<Event> eventList = new ArrayList<>();
                for (QueryDocumentSnapshot document : value) {
                    Event event = document.toObject(Event.class);
                    event.setEventId(document.getId());
                    eventList.add(event);
                }
                callback.onSuccess(eventList);
            }
        });
    }

    /**
     * Returns all events that currently have a posterImageUrl set.
     * Used by the Admin Image Management screen to list uploaded images.
     */
    public void getAllEventsWithPosters(EventListCallback callback) {
        eventsRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> result = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        if (event.getPosterImageUrl() != null &&
                                !event.getPosterImageUrl().isEmpty()) {
                            result.add(event);
                        }
                    }
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting events with posters", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Fetches the organizer's display name from the users collection.
     * Tries fullName → name → email → falls back to UID.
     */
    public void getOrganizerName(String organizerId, OrganizerNameCallback callback) {

        if (organizerId == null || organizerId.isEmpty()) {
            callback.onSuccess("Unknown");
            return;
        }

        usersRef.document(organizerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("name");
                        }
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("email");
                        }
                        if (name == null || name.isEmpty()) {
                            name = organizerId;
                        }
                        callback.onSuccess(name);
                    } else {
                        callback.onSuccess(organizerId);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Removes the poster image for a given event:
     * 1) Deletes the file from Firebase Storage (if possible).
     * 2) Clears posterImageUrl field in the event document.
     */
    public void removeEventPoster(String eventId, String posterUrl, EventTaskCallback callback) {
        // If no URL, just clear the field
        if (posterUrl == null || posterUrl.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("posterImageUrl", null);
            updateEventFields(eventId, updates, callback);
            return;
        }

        try {
            StorageReference ref = storage.getReferenceFromUrl(posterUrl);
            ref.delete()
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("posterImageUrl", null);
                        updateEventFields(eventId, updates, callback);
                    })
                    .addOnFailureListener(e -> {
                        // If deletion from Storage fails, at least clear Firestore
                        Log.e(TAG, "Failed to delete image from storage", e);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("posterImageUrl", null);
                        updateEventFields(eventId, updates, callback);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Invalid poster URL", e);
            Map<String, Object> updates = new HashMap<>();
            updates.put("posterImageUrl", null);
            updateEventFields(eventId, updates, callback);
        }
    }

    /**
     * Admin: delete an event and its known subcollections from Firestore.
     * Subcollections removed: waitingList, selected, confirmedAttendees.
     */
    public void deleteEvent(String eventId, EventTaskCallback callback) {
        DocumentReference eventDoc = eventsRef.document(eventId);

        String[] subcollections = new String[] {
                "waitingList",
                "selected",
                "confirmedAttendees"
        };

        deleteSubcollectionsThenEvent(eventDoc, subcollections, 0, callback);
    }

    // Helper: recursively delete subcollections then the parent event doc
    private void deleteSubcollectionsThenEvent(
            DocumentReference eventDoc,
            String[] subcollections,
            int index,
            EventTaskCallback callback
    ) {
        if (index >= subcollections.length) {
            // No more subcollections – delete event doc itself
            eventDoc.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Event deleted: " + eventDoc.getId());
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting event document", e);
                        callback.onError(e.getMessage());
                    });
            return;
        }

        String sub = subcollections[index];
        eventDoc.collection(sub).get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid ->
                                    deleteSubcollectionsThenEvent(eventDoc, subcollections, index + 1, callback))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting subcollection " + sub, e);
                                callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching subcollection " + sub, e);
                    callback.onError(e.getMessage());
                });
    }

    public interface AdminEventsListener {
        void onEventsChanged(List<Event> events);
        void onError(String message);
    }

    /**
     * Real-time listener for all events, ordered by creation time.
     * Used by the Admin Event Management screen.
     */
    public ListenerRegistration listenToAllEvents(final AdminEventsListener listener) {
        return db.collection("events")
                .orderBy("timeCreated", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (value == null) {
                        listener.onEventsChanged(new ArrayList<>());
                        return;
                    }

                    List<Event> result = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Event event = doc.toObject(Event.class);
                        // IMPORTANT: store the document ID into eventId
                        event.setEventId(doc.getId());
                        result.add(event);
                    }
                    listener.onEventsChanged(result);
                });
    }

    /**
     * Admin: Deactivates all events that belong to a given organizer.
     * Sets isActive=false and status="deactivated".
     */
    public void deactivateEventsByOrganizer(String organizerId,
                                            String adminId,
                                            EventTaskCallback callback) {

        if (organizerId == null || organizerId.isEmpty()) {
            if (callback != null) {
                callback.onError("Organizer id is empty.");
            }
            return;
        }

        eventsRef.whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        return;
                    }

                    WriteBatch batch = db.batch();
                    com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("isActive", false);
                        updates.put("status", "deactivated");
                        updates.put("deactivatedAt", now);
                        updates.put("deactivatedByAdminId", adminId);

                        batch.set(doc.getReference(), updates, SetOptions.merge());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to deactivate events for organizer", e);
                                if (callback != null) {
                                    callback.onError(e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query events for organizer", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Draws a single replacement entrant for an event.
     * Reuses the sampling logic with count = 1.
     */
    public void drawReplacement(String eventId, EventTaskCallback callback) {
        sampleAttendees(eventId, 1, callback);
    }


    // TODO: We will add more methods here later, such as:
    // - runLottery(String eventId, int count, ...)
    // - getWaitingList(String eventId, ...)
}
