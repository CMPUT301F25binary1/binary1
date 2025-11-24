package com.example.fairchance.models;

/**
 * Simple model used by the admin image management screen.
 * One item corresponds to one stored image (usually an event poster).
 */
public class AdminImageItem {

    // We’ll treat the related eventId as the "id" here.
    private String id;
    private String imageUrl;
    private String title;       // e.g., event name
    private String description; // optional

    public AdminImageItem() {
        // Needed for Firestore/adapter if ever required
    }

    public AdminImageItem(String id, String imageUrl, String title, String description) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
