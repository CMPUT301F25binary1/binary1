package com.example.fairchance;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.fairchance.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging; // FIX: Added import
import java.util.HashMap;
import java.util.Map;

/**
 * Repository class implementing the "single source of truth" pattern for all
 * authentication and user profile-related data. It handles all interactions
 * with Firebase Authentication and the 'users' collection in Firestore.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    //region Callback Interfaces
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String message);
    }

    public interface RoleCallback {
        void onRoleFetched(String role);
        void onError(String message);
    }

    public interface UserProfileCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface TaskCallback {
        void onSuccess();
        void onError(String message);
    }
    //endregion

    /**
     * Initializes the repository with instances of FirebaseAuth and FirebaseFirestore.
     */
    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Gets the currently signed-in FirebaseUser.
     *
     * @return The current FirebaseUser, or null if no user is signed in.
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Fetches the full profile (as a User object) for the currently logged-in user
     * from the 'users' collection in Firestore.
     *
     * @param callback A callback to return the User object on success or an error message.
     */
    public void getUserProfile(UserProfileCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user is logged in.");
            return;
        }

        db.collection("users").document(fUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onError("Failed to parse user data.");
                        }
                    } else {
                        callback.onError("User profile document not found.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Fetches just the 'role' field for the currently logged-in user.
     * Used for determining which dashboard to display.
     *
     * @param callback A callback to return the role string on success or an error message.
     */
    public void getUserRole(RoleCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user is currently logged in.");
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String role = document.getString("role");
                            if (role != null) {
                                callback.onRoleFetched(role);
                            } else {
                                callback.onError("Role field is missing in user document.");
                            }
                        } else {
                            callback.onError("User profile document not found.");
                        }
                    } else {
                        callback.onError("Failed to fetch user role: " + task.getException().getMessage());
                    }
                });
    }

    /**
     * Updates the user's profile data in their Firestore document.
     *
     * @param newName           The user's updated name.
     * @param newEmail          The user's updated email.
     * @param newPhone          The user's updated phone number.
     * @param notificationPrefs A map of the user's notification preferences.
     * @param callback          A callback to notify of success or failure.
     */
    public void updateUserProfile(String newName, String newEmail, String newPhone,
                                  Map<String, Object> notificationPrefs, TaskCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user is logged in.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);
        updates.put("phone", newPhone);
        updates.put("notificationPreferences", notificationPrefs);

        db.collection("users").document(fUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Deletes the user's Firestore document and their Authentication record.
     * This is an irreversible action.
     *
     * @param callback A callback to notify of success or failure.
     */
    public void deleteCurrentUser(TaskCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user is logged in.");
            return;
        }
        String userId = fUser.getUid();

        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    fUser.delete()
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Saves the user's FCM registration token to their Firestore document.
     *
     * @param token The FCM token (or null to remove it).
     */
    public void saveFcmToken(String token) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            Log.d(TAG, "No user logged in, skipping FCM token save.");
            return;
        }

        db.collection("users").document(fUser.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved to Firestore."))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM Token", e));
    }

    /**
     * FIX: Private method to save FCM token with a TaskCallback.
     */
    private void saveFcmToken(String token, TaskCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user logged in.");
            return;
        }

        db.collection("users").document(fUser.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FCM Token update successful: " + (token == null ? "removed" : "set"));
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving FCM Token with callback", e);
                    callback.onError(e.getMessage());
                });
    }


    /**
     * FIX: Updates the user's overall notification status by deleting or setting the FCM token.
     * Fulfills US 01.04.03 Criterion 2: Mechanism to stop push notifications.
     *
     * @param enableNotifications True to fetch and set token, false to set token to null.
     * @param callback Notifies of success or failure.
     */
    public void updateNotificationStatus(boolean enableNotifications, TaskCallback callback) {
        if (!enableNotifications) {
            // Option 1: User opts out - set token to null
            saveFcmToken(null, callback);
            return;
        }

        // Option 2: User opts in - fetch new token and save
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError("Fetching FCM registration token failed: " + task.getException().getMessage());
                        return;
                    }
                    String token = task.getResult();
                    saveFcmToken(token, callback);
                });
    }

    /**
     * Logs in a user with email and password (for Organizer or Admin).
     * Also verifies that the user's role in Firestore matches the expected role.
     *
     * @param email        User's email.
     * @param password     User's password.
     * @param expectedRole The role the user is trying to log in as ("organizer" or "admin").
     * @param callback     A callback to return the FirebaseUser on success or an error message.
     */
    public void loginEmailPasswordUser(String email, String password, String expectedRole, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user == null) {
                            callback.onError("Login successful but user was null.");
                            return;
                        }

                        // Authorize by fetching role from Firestore
                        db.collection("users").document(user.getUid()).get()
                                .addOnCompleteListener(docTask -> {
                                    if (docTask.isSuccessful()) {
                                        DocumentSnapshot document = docTask.getResult();
                                        if (document != null && document.exists()) {
                                            String actualRole = document.getString("role");

                                            if (expectedRole.equals(actualRole)) {
                                                Log.d(TAG, "Role match. Login successful.");
                                                callback.onSuccess(user);
                                            } else {
                                                Log.w(TAG, "Role mismatch. Expected: " + expectedRole + ", Got: " + actualRole);
                                                auth.signOut(); // Log the user back out
                                                callback.onError("Access Denied: You do not have " + expectedRole + " privileges.");
                                            }
                                        } else {
                                            Log.w(TAG, "Login failed: User profile document not found.");
                                            auth.signOut();
                                            callback.onError("Login failed: User profile not found.");
                                        }
                                    } else {
                                        Log.w(TAG, "Login failed: Could not read user document.", docTask.getException());
                                        auth.signOut();
                                        callback.onError("Login failed: Could not read user profile.");
                                    }
                                });
                    } else {
                        Log.w(TAG, "Email/Password login failed.", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Registers a new user with email and password (for Organizer or Admin).
     * Creates their Auth record and their user document in Firestore.
     *
     * @param email     User's email.
     * @param password  User's password.
     * @param role      The role to assign ("organizer" or "admin").
     * @param firstName User's first name.
     * @param lastName  User's last name.
     * @param phone     User's phone number.
     * @param callback  A callback to return the FirebaseUser on success or an error message.
     */
    public void registerEmailPasswordUser(String email, String password, String role,
                                          String firstName, String lastName, String phone, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email/Password registration successful.");
                        FirebaseUser user = task.getResult().getUser();
                        createUserDocument(user, email, role, firstName, lastName, phone, callback);
                    } else {
                        Log.w(TAG, "Email/Password registration failed.", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Registers and logs in a new Entrant user via Anonymous Authentication.
     * Creates their Auth record and their user document in Firestore.
     *
     * @param email     User's email.
     * @param firstName User's first name.
     * @param lastName  User's last name.
     * @param phone     User's phone number.
     * @param callback  A callback to return the FirebaseUser on success or an error message.
     */
    public void registerAndLoginEntrant(String email, String firstName, String lastName, String phone, AuthCallback callback) {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous sign-in successful.");
                        FirebaseUser user = task.getResult().getUser();
                        createUserDocument(user, email, "entrant", firstName, lastName, phone, callback);
                    } else {
                        Log.w(TAG, "Anonymous sign-in failed.", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Private helper method to create a new user document in the 'users' collection
     * after a successful Firebase Authentication registration.
     */
    private void createUserDocument(FirebaseUser user, String email, String role,
                                    String firstName, String lastName, String phone, AuthCallback callback) {

        if (user == null) {
            callback.onError("User is null, cannot create profile.");
            return;
        }

        Map<String, Object> notificationPrefs = new HashMap<>();
        notificationPrefs.put("lotteryResults", true);
        notificationPrefs.put("organizerUpdates", true);

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("role", role);
        userData.put("name", firstName + " " + lastName);
        userData.put("phone", phone);
        userData.put("notificationPreferences", notificationPrefs);
        userData.put("fcmToken", null);

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created for " + user.getUid());
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user document", e);
                    callback.onError("Failed to save user profile: " + e.getMessage());
                });
    }

    /**
     * Signs out the current user.
     */
    public void signOut() {
        auth.signOut();
    }
}