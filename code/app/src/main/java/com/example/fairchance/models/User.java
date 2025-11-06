package com.example.fairchance.models;

import java.util.Map;

/**
 * Model class for a User.
 * This is a POJO used by Firebase Firestore to retrieve user data.
 */
public class User {
    private String name;
    private String email;
    private String phone;
    private String role;
    private Map<String, Boolean> notificationPreferences;

    // A no-argument constructor is required for Firestore
    public User() {}

    // --- Getters ---
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public Map<String, Boolean> getNotificationPreferences() { return notificationPreferences; }
}