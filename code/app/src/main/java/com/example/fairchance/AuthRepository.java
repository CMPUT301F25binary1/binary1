package com.example.fairchance;

import android.util.Log;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * (No change to this section)
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String message);
    }

    public interface RoleCallback {
        void onRoleFetched(String role);
        void onError(String message);
    }

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

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
     * Registers a new Organizer or Admin.
     */
    // *** UPDATED METHOD SIGNATURE ***
    public void registerEmailPasswordUser(String email, String password, String role,
                                          String firstName, String lastName, String phone, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email/Password registration successful.");
                        FirebaseUser user = task.getResult().getUser();
                        // *** UPDATED METHOD CALL ***
                        createUserDocument(user, email, role, firstName, lastName, phone, callback);
                    } else {
                        Log.w(TAG, "Email/Password registration failed.", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Registers and logs in a new Entrant using Anonymous Authentication.
     */
    // *** UPDATED METHOD SIGNATURE ***
    public void registerAndLoginEntrant(String email, String firstName, String lastName, String phone, AuthCallback callback) {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous sign-in successful.");
                        FirebaseUser user = task.getResult().getUser();
                        // *** UPDATED METHOD CALL ***
                        createUserDocument(user, email, "entrant", firstName, lastName, phone, callback);
                    } else {
                        Log.w(TAG, "Anonymous sign-in failed.", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Private helper to create the user's profile in the 'users' collection.
     */
    // *** UPDATED METHOD SIGNATURE ***
    private void createUserDocument(FirebaseUser user, String email, String role,
                                    String firstName, String lastName, String phone, AuthCallback callback) {
        if (user == null) {
            callback.onError("User is null, cannot create profile.");
            return;
        }

        // 1. Create the default notification preferences map
        Map<String, Object> notificationPrefs = new HashMap<>();
        notificationPrefs.put("lotteryResults", true);
        notificationPrefs.put("organizerUpdates", true);

        // 2. Create the main user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("role", role);

        // Combine firstName and lastName into a single 'name' field
        userData.put("name", firstName + " " + lastName);

        // Add the new fields
        userData.put("phone", phone); // <-- USE THE VARIABLE, NOT ""
        userData.put("notificationPreferences", notificationPrefs);


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
     * Signs out the current user from Firebase Authentication.
     */
    public void signOut() {
        auth.signOut();
    }
}