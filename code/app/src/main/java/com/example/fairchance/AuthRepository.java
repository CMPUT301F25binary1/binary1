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
 * A repository class that abstracts all authentication and user-related
 * data logic from the UI. It handles communication with Firebase Authentication
 * and Cloud Firestore for user profile data.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    /**
     * Callback for asynchronous authentication actions.
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String message);
    }

    /**
     * Callback for fetching a user's role from Firestore.
     */
    public interface RoleCallback {
        void onRoleFetched(String role);
        void onError(String message);
    }

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Gets the currently signed-in FirebaseUser.
     * @return The current FirebaseUser, or null if no one is signed in.
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Fetches the user's role ("entrant", "organizer", "admin") from their
     * profile document in Firestore.
     * @param callback The callback to return the role string or an error.
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
     * Logs in an Organizer or Admin. This performs a 2-step check:
     * 1. Authenticates with Email/Password.
     * 2. Authorizes by checking their role in Firestore against the expectedRole.
     * @param email The user's email.
     * @param password The user's password.
     * @param expectedRole The role ("organizer" or "admin") required to log in.
     * @param callback The callback to notify the UI.
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
     * Registers a new Organizer or Admin.
     */
    public void registerEmailPasswordUser(String email, String password, String role,
                                          String firstName, String lastName, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email/Password registration successful.");
                        FirebaseUser user = task.getResult().getUser();
                        createUserDocument(user, email, role, firstName, lastName, callback);
                    } else {
                        Log.w(TAG, "Email/Password registration failed.", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Registers and logs in a new Entrant using Anonymous Authentication.
     */
    public void registerAndLoginEntrant(String email, String firstName, String lastName, AuthCallback callback) {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Anonymous sign-in successful.");
                        FirebaseUser user = task.getResult().getUser();
                        createUserDocument(user, email, "entrant", firstName, lastName, callback);
                    } else {
                        Log.w(TAG, "Anonymous sign-in failed.", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    /**
     * Private helper to create the user's profile in the 'users' collection.
     */
    private void createUserDocument(FirebaseUser user, String email, String role,
                                    String firstName, String lastName, AuthCallback callback) {
        if (user == null) {
            callback.onError("User is null, cannot create profile.");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("role", role);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);

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