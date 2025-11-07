package com.example.fairchance.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model class for an Event.
 * This is a POJO (Plain Old Java Object) that maps directly to documents
 * in the "events" collection in Firestore.
 */
public class Event {

    private String organizerId;
    private String name;
    private String description;
    private String posterImageUrl;
    private Date registrationStart;
    private Date registrationEnd;
    private Date eventDate;
    private long capacity;
    private double price;
    private boolean geolocationRequired;
    private long waitingListLimit;
    private String guidelines;
    private String category;
    private String location;

    @Exclude
    private String eventId;

    @ServerTimestamp
    private Date timeCreated;

    /**
     * A public, no-argument constructor is required by Firestore
     * for deserialization.
     */
    public Event() {
        // Default constructor
    }

    // --- Getters and Setters ---

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterImageUrl() {
        return posterImageUrl;
    }

    public void setPosterImageUrl(String posterImageUrl) {
        this.posterImageUrl = posterImageUrl;
    }

    public Date getRegistrationStart() {
        return registrationStart;
    }

    public void setRegistrationStart(Date registrationStart) {
        this.registrationStart = registrationStart;
    }

    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    public void setRegistrationEnd(Date registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    public long getWaitingListLimit() {
        return waitingListLimit;
    }

    public void setWaitingListLimit(long waitingListLimit) {
        this.waitingListLimit = waitingListLimit;
    }

    public String getGuidelines() {
        return guidelines;
    }

    public void setGuidelines(String guidelines) {
        this.guidelines = guidelines;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the event's unique Document ID.
     * @Exclude ensures this is not saved as a field *inside* the Firestore document.
     *
     * @return The string Document ID.
     */
    @Exclude
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the event's unique Document ID.
     * This is typically called after fetching the document from Firestore.
     *
     * @param eventId The string Document ID.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the server-side timestamp of when the event was created.
     *
     * @return The creation date.
     */
    public Date getTimeCreated() {
        return timeCreated;
    }

    /**
     * Sets the server-side timestamp of when the event was created.
     *
     * @param timeCreated The creation date.
     */
    public void setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
    }
}