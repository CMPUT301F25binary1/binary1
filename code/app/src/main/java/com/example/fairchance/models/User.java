package com.example.fairchance.models;

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

    /**
     * A public, no-argument constructor is required by Firestore
     * for deserialization.
     */
    public User() {}

    /**
     * Gets the user's full name.
     * @return Full name string.
     */
    public String getName() { return name; }

    /**
     * Gets the user's email address.
     * @return Email string.
     */
    public String getEmail() { return email; }

    /**
     * Gets the user's optional phone number.
     * @return Phone number string.
     */
    public String getPhone() { return phone; }

    /**
     * Gets the user's role ("entrant", "organizer", "admin").
     * @return Role string.
     */
    public String getRole() { return role; }

    /**
     * Gets the user's notification preferences.
     * @return A Map where keys are notification types (e.g., "lotteryResults")
     * and values are booleans.
     */
    public Map<String, Boolean> getNotificationPreferences() { return notificationPreferences; }

    /**
     * Gets the user's unique Firebase Cloud Messaging (FCM) token for push notifications.
     * @return FCM token string.
     */
    public String getFcmToken() { return fcmToken; }
}