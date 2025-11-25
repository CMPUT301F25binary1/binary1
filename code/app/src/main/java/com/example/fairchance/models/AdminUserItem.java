package com.example.fairchance.models;

import com.google.firebase.Timestamp;

/**
 * Lightweight model for admin user list.
 */
public class AdminUserItem {

    private String id;            // Firestore doc id
    private String name;
    private String email;
    private String role;
    private Timestamp createdAt;  // registration date

    public AdminUserItem() { }

    public AdminUserItem(String id, String name, String email,
                         String role, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public String getRole() {
        return role != null ? role : "";
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
