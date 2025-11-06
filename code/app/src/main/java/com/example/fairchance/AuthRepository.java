package com.example.fairchance;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.fairchance.models.User; // Import the User model
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    // --- CALLBACK INTERFACES ---
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
    // --- END CALLBACK INTERFACES ---

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Fetches the full profile for the currently logged-in user.
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

    public void getUserRole(RoleCallback callback) {
        // ... (This method is unchanged)
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
     * Updates the user's profile data in Firestore.
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
        // We don't allow updating 'role'

        db.collection("users").document(fUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Deletes the user's Firestore document and their Authentication record.
     */
    public void deleteCurrentUser(TaskCallback callback) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            callback.onError("No user is logged in.");
            return;
        }
        String userId = fUser.getUid();

        // 1. Delete the user's document from Firestore
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    // 2. If Firestore delete succeeds, delete the auth record
                    fUser.delete()
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // --- START: ADDED NEW METHOD ---
    /**
     * Saves the user's FCM registration token to their Firestore document.
     * @param token The FCM token.
     */
    public void saveFcmToken(String token) {
        FirebaseUser fUser = auth.getCurrentUser();
        if (fUser == null) {
            // No user logged in, can't save token.
            // This is fine, it will be saved on next login via SplashActivity.
            Log.d(TAG, "No user logged in, skipping FCM token save.");
            return;
        }

        db.collection("users").document(fUser.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved to Firestore."))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM Token", e));
    }
    // --- END: ADDED NEW METHOD ---

    public void loginEmailPasswordUser(String email, String password, String expectedRole, AuthCallback callback) {
        // ... (This method is unchanged)
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

    public void registerEmailPasswordUser(String email, String password, String role,
                                          String firstName, String lastName, String phone, AuthCallback callback) {
        // ... (This method is unchanged)
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

    public void registerAndLoginEntrant(String email, String firstName, String lastName, String phone, AuthCallback callback) {
        // ... (This method is unchanged)
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

    // --- START: MODIFIED METHOD ---
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
        userData.put("fcmToken", null); // <-- ADDED THIS

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
    // --- END: MODIFIED METHOD ---

    public void signOut() {
        auth.signOut();
    }
}