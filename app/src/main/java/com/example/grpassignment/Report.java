package com.example.grpassignment;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

public class Report implements Serializable {

    @DocumentId
    private String documentId;
    private String type;
    private String location;
    private String date;
    private String time;
    private String description;
    private String severity;
    private boolean isAnonymous;
    private String mediaUri;
    private String userId; // To store the ID of the user who posted
    private double latitude;
    private double longitude;

    @ServerTimestamp
    private Date timestamp; // For sorting by creation time

    // Required empty public constructor for Firestore deserialization
    public Report() {}

    // --- Getters ---
    public String getDocumentId() { return documentId; }
    public String getType() { return type; }
    public String getLocation() { return location; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public boolean isAnonymous() { return isAnonymous; }
    public String getMediaUri() { return mediaUri; }
    public String getUserId() { return userId; }
    public Date getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    // --- Setters ---
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setType(String type) { this.type = type; }
    public void setLocation(String location) { this.location = location; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
    public void setMediaUri(String mediaUri) { this.mediaUri = mediaUri; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
