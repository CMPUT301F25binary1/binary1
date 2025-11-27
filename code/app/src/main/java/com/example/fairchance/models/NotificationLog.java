package com.example.fairchance.models;

import java.util.Date;
import java.util.List;

/**
 * Represents a single notification that was sent by an organizer.
 * Stored in Firestore under the "notificationLogs" collection.
 */
public class NotificationLog {

    private String id;
    private String senderId;
    private String senderName;
    private List<String> recipientIds;
    private long recipientCount;
    private String messageType;    // e.g., "WAITLIST_SELECTED", "WAITLIST_CUSTOM"
    private String messageBody;
    private String eventId;
    private String eventName;
    private Date timestamp;

    // Firestore requires an empty constructor
    public NotificationLog() { }

    public NotificationLog(String senderId,
                           String senderName,
                           List<String> recipientIds,
                           String messageType,
                           String messageBody,
                           String eventId,
                           String eventName,
                           Date timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientIds = recipientIds;
        this.messageType = messageType;
        this.messageBody = messageBody;
        this.eventId = eventId;
        this.eventName = eventName;
        this.timestamp = timestamp;
        if (recipientIds != null) {
            this.recipientCount = recipientIds.size();
        }
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public List<String> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<String> recipientIds) {
        this.recipientIds = recipientIds;
        if (recipientIds != null) {
            this.recipientCount = recipientIds.size();
        }
    }

    public long getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(long recipientCount) {
        this.recipientCount = recipientCount;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
