package com.example.fairchance.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model class for an Event.
 * This is a POJO that maps directly to the "events" collection in Firestore,
 * as per the defined database structure.
 *
 * A no-argument constructor and public getters are required for Firestore
 * to deserialize documents back into this object.
 */
public class Event {

    // --- Fields from your database plan ---
    private String organizerId;
    private String name;
    private String description;
    private String posterImageUrl;
    private Date registrationStart;
    private Date registrationEnd;
    private Date eventDate; // The field we agreed to add
    private long capacity;
    private double price;
    private boolean geolocationRequired;
    private long waitingListLimit;
    private String guidelines; // <-- ADDED THIS FIELD

    // --- Utility Fields ---

    // This will hold the document ID from Firestore.
    // @Exclude ensures it isn't saved as a field *inside* the document.
    private String eventId;

    // This annotation tells Firestore to automatically set the timestamp
    // on the server when the object is first created.
    @ServerTimestamp
    private Date timeCreated;

    /**
     * A public, no-argument constructor is **required** by Firestore
     * for deserialization (e.g., event = doc.toObject(Event.class)).
     */
    public Event() {
        // Default constructor
    }

    // --- Getters and Setters ---
    // All of these are needed for Firestore to work correctly.

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

    // --- ADDED GETTERS AND SETTERS FOR GUIDELINES ---
    public String getGuidelines() {
        return guidelines;
    }

    public void setGuidelines(String guidelines) {
        this.guidelines = guidelines;
    }
    // --- END ADDED SECTION ---

    @Exclude
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
    }
}