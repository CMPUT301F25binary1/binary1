package com.example.fairchance.models;

import com.google.firebase.firestore.Exclude;
import java.util.Map;

/**
 * Model class for a User.
 * This is a POJO (Plain Old Java Object) that maps directly to documents
 * in the "users" collection in Firestore.
 */
public class User {
    private String name;
    private String email;
    private String phone;
    private String role;
    private Map<String, Boolean> notificationPreferences;
    private String fcmToken;

    // NEW: account status ("ACTIVE", "DEACTIVATED")
    private String status;

    // NEW: Firestore document ID (not stored as a field in Firestore)
    @Exclude
    private String userId;

    /**
     * A public, no-argument constructor is required by Firestore
     * for deserialization.
     */
    public User() {}

    // --- Getters ---

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getPhone() { return phone; }

    /**
     * Gets the user's role ("entrant", "organizer", "admin").
     */
    public String getRole() { return role; }

    public Map<String, Boolean> getNotificationPreferences() { return notificationPreferences; }

    public String getFcmToken() { return fcmToken; }

    /**
     * Gets the account status ("ACTIVE" or "DEACTIVATED").
     */
    public String getStatus() { return status; }

    /**
     * Gets the Firestore document ID for this user.
     */
    @Exclude
    public String getUserId() { return userId; }

    // --- Setters ---

    public void setName(String name) { this.name = name; }

    public void setEmail(String email) { this.email = email; }

    public void setPhone(String phone) { this.phone = phone; }

    public void setRole(String role) { this.role = role; }

    public void setNotificationPreferences(Map<String, Boolean> notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }

    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public void setStatus(String status) { this.status = status; }

    public void setUserId(String userId) { this.userId = userId; }
}
