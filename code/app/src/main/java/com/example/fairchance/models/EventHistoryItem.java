package com.example.fairchance.models;

import java.util.Date;

/**
 * Model class for an item in the user's event history.
 * This is a POJO (Plain Old Java Object) used by Firebase Firestore to retrieve data from the
 * 'users/{UserID}/eventHistory' sub-collection.
 */
public class EventHistoryItem {
    private String eventName;
    private Date eventDate;
    private String status;
    private String eventId;

    /**
     * A public, no-argument constructor is required by Firestore
     * for deserialization.
     */
    public EventHistoryItem() {}

    // --- Getters and Setters ---

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
}