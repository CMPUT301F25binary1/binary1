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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

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

    public interface NotificationCallback {
        void onSuccess(int sentCount, int failureCount);
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
     */
    public void createEvent(Event event, EventTaskCallback callback) {
        FirebaseUser user = auth.getCurrentUser();

        eventsRef.add(event)
                .addOnSuccessListener(documentReference -> {
                    String eventId = documentReference.getId();
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
                Date now = new Date();

                for (QueryDocumentSnapshot document : value) {
                    Event event = document.toObject(Event.class);
                    event.setEventId(document.getId());

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

    // Organizer: listen only to events created by this organizer.
    public ListenerRegistration getEventsForOrganizer(String organizerId, EventListCallback callback) {
        return eventsRef
                .whereEqualTo("organizerId", organizerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting organizer events in real-time: ", error);
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
     * Gets the current number of users on a specific event's waiting list (One-time fetch).
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
     * Listens to the waiting list count in real-time.
     */
    public ListenerRegistration listenToWaitingListCount(String eventId, WaitlistCountCallback callback) {
        return eventsRef.document(eventId).collection("waitingList")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to waitlist count", error);
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (value != null) {
                        callback.onSuccess(value.size());
                    }
                });
    }

    /**
     * Adds the current user to an event's waiting list.
     */
    public void joinWaitingList(String eventId, Event event, EventTaskCallback callback) {
        long limit = event.getWaitingListLimit();

        if (limit > 0) {
            getWaitingListCount(eventId, new WaitlistCountCallback() {
                @Override
                public void onSuccess(int count) {
                    if (count >= limit) {
                        callback.onError("The waiting list for this event is full.");
                    } else {
                        joinWaitingListInternal(eventId, event, callback);
                    }
                }
                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } else {
            joinWaitingListInternal(eventId, event, callback);
        }
    }

    public void joinWaitingListWithLocation(String eventId, Event event, Double lat, Double lng, EventTaskCallback callback) {
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

    private void joinWaitingListWithLocationInternal(String eventId, Event event, Double lat, Double lng, EventTaskCallback callback) {
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

    public void updateEventFields(String eventId, Map<String, Object> updates, EventTaskCallback callback) {
        eventsRef.document(eventId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

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

    public ListenerRegistration getEventHistory(EventHistoryListCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is signed in.");
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
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error checking event status: ", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

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

                                    DocumentReference selRef = eventsRef.document(eventId)
                                            .collection("selected").document(uid);
                                    Map<String, Object> selData = new HashMap<>();
                                    selData.put("status", "pending");
                                    selData.put("sampledAt", now);
                                    batch.set(selRef, selData);

                                    DocumentReference histRef = usersRef.document(uid)
                                            .collection("eventHistory").document(eventId);
                                    Map<String, Object> hist = new HashMap<>();
                                    hist.put("status", "Selected");
                                    hist.put("updatedAt", now);
                                    batch.set(histRef, hist, SetOptions.merge());

                                    DocumentReference waitRef = eventsRef.document(eventId)
                                            .collection("waitingList").document(uid);
                                    batch.delete(waitRef);
                                }

                                batch.commit()
                                        .addOnSuccessListener(a -> callback.onSuccess(take))
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

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
                    if (!accepted) {
                        sampleAttendees(eventId, 1, new SampleAttendeesCallback() {
                            @Override
                            public void onSuccess(int selectedCount) {
                                callback.onSuccess();
                            }
                            @Override
                            public void onError(String message) {
                                callback.onSuccess();
                            }
                        });
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to respond to invitation", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Cancels all entrants who are currently in the 'selected' collection with status 'pending'.
     * - Marks them as 'cancelled' in the selected collection.
     * - Adds them to the 'cancelled' collection (for visibility in CancelledEntrantsFragment).
     * - Updates their user history status to "Cancelled".
     * Fulfills US 02.06.04.
     */
    public void cancelPendingEntrants(String eventId, EventTaskCallback callback) {
        eventsRef.document(eventId).collection("selected")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(); // Nothing to cancel
                        return;
                    }

                    WriteBatch batch = db.batch();
                    com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String userId = doc.getId();

                        // 1. Update status in 'selected' (used by SamplingReplacementFragment for replacement pool)
                        batch.update(doc.getReference(), "status", "cancelled");
                        batch.update(doc.getReference(), "cancelledAt", now);

                        // 2. Add to 'cancelled' collection (used by CancelledEntrantsFragment for list view)
                        DocumentReference cancelledRef = eventsRef.document(eventId)
                                .collection("cancelled").document(userId);
                        Map<String, Object> cancelledData = new HashMap<>();
                        cancelledData.put("cancelledAt", now);
                        cancelledData.put("reason", "organizer_timeout");
                        batch.set(cancelledRef, cancelledData);

                        // 3. Update User History
                        DocumentReference histRef = usersRef.document(userId)
                                .collection("eventHistory").document(eventId);
                        batch.update(histRef, "status", "Cancelled");
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Cancelled " + snapshot.size() + " pending entrants.");
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to batch cancel entrants", e);
                                callback.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

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

    public void getAllEventsWithPosters(EventListCallback callback) {
        eventsRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> result = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
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

    public void getOrganizerName(String organizerId, OrganizerNameCallback callback) {
        if (organizerId == null || organizerId.isEmpty()) {
            callback.onSuccess("Unknown");
            return;
        }
        usersRef.document(organizerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        if (name == null || name.isEmpty()) name = doc.getString("name");
                        if (name == null || name.isEmpty()) name = doc.getString("email");
                        if (name == null || name.isEmpty()) name = organizerId;
                        callback.onSuccess(name);
                    } else {
                        callback.onSuccess(organizerId);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendWaitingListNotifications(String eventId, String message, NotificationCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("message", message);

        FirebaseFunctions.getInstance().getHttpsCallable("sendWaitingListNotifications").call(data)
                .addOnSuccessListener(result -> {
                    int sentCount = 0;
                    int failureCount = 0;
                    Object raw = result.getData();
                    if (raw instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) raw;
                        Object s = map.get("sentCount");
                        Object f = map.get("failureCount");
                        if (s instanceof Number) sentCount = ((Number) s).intValue();
                        if (f instanceof Number) failureCount = ((Number) f).intValue();
                    }
                    if (callback != null) callback.onSuccess(sentCount, failureCount);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void sendCancelledNotifications(String eventId, String message, NotificationCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("message", message);

        FirebaseFunctions.getInstance().getHttpsCallable("sendCancelledNotifications").call(data)
                .addOnSuccessListener(result -> {
                    int sentCount = 0;
                    int failureCount = 0;
                    Object raw = result.getData();
                    if (raw instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) raw;
                        Object s = map.get("sentCount");
                        Object f = map.get("failureCount");
                        if (s instanceof Number) sentCount = ((Number) s).intValue();
                        if (f instanceof Number) failureCount = ((Number) f).intValue();
                    }
                    if (callback != null) callback.onSuccess(sentCount, failureCount);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void removeEventPoster(String eventId, String posterUrl, EventTaskCallback callback) {
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

    public void deleteEvent(String eventId, EventTaskCallback callback) {
        DocumentReference eventDoc = eventsRef.document(eventId);
        String[] subcollections = new String[] { "waitingList", "selected", "confirmedAttendees" };
        deleteSubcollectionsThenEvent(eventDoc, subcollections, 0, callback);
    }

    private void deleteSubcollectionsThenEvent(DocumentReference eventDoc, String[] subcollections, int index, EventTaskCallback callback) {
        if (index >= subcollections.length) {
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
                            .addOnSuccessListener(aVoid -> deleteSubcollectionsThenEvent(eventDoc, subcollections, index + 1, callback))
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
                        event.setEventId(doc.getId());
                        result.add(event);
                    }
                    listener.onEventsChanged(result);
                });
    }

    public void deactivateEventsByOrganizer(@NonNull String organizerId, @NonNull String adminId, @NonNull String reason, @NonNull EventTaskCallback callback) {
        eventsRef.whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("isActive", false);
                        updates.put("deactivatedAt", now);
                        updates.put("deactivatedByAdminId", adminId);
                        updates.put("deactivationReason", reason);
                        batch.set(doc.getReference(), updates, SetOptions.merge());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void drawReplacement(String eventId, EventTaskCallback callback) {
        sampleAttendees(eventId, 1, callback);
    }
}