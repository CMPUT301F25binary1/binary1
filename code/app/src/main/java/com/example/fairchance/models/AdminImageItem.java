package com.example.fairchance.models;

import com.google.firebase.Timestamp;

/**
 * Model for images shown in the admin image management screen.
 */
public class AdminImageItem {

    private String id;             // event id or image id
    private String imageUrl;       // storage url
    private String title;          // event name / label
    private String uploaderName;   // who uploaded it
    private Timestamp uploadedAt;  // when it was uploaded

    public AdminImageItem() {
        // Required empty constructor for Firestore/serialization
    }

    public AdminImageItem(String id,
                          String imageUrl,
                          String title,
                          String uploaderName,
                          Timestamp uploadedAt) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.uploaderName = uploaderName;
        this.uploadedAt = uploadedAt;
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

    public String getUploaderName() {
        return uploaderName;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
