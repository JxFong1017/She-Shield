package com.example.grpassignment;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

public class SafetyResource implements Parcelable {
    private String id;
    private String title;
    private String type;
    private String category;
    private String duration;
    private String format;
    private String file;
    public static final Creator<SafetyResource> CREATOR = new Creator<SafetyResource>() {
        @Override
        public SafetyResource createFromParcel(Parcel in) {
            return new SafetyResource(in);
        }

        @Override
        public SafetyResource[] newArray(int size) {
            return new SafetyResource[size];
        }
    };
    private String imageUrl;

    // Workshop-specific fields
    private Timestamp eventTimestamp;
    private String eventDate;
    private String eventTime;
    private String location;
    private String instructor;
    private int capacity;

    // Empty constructor required for Firebase
    public SafetyResource() {
    }
    private String description;

    // Full constructor
    public SafetyResource(String id, String title, String type, String category,
                          String duration, String format, String file, String imageUrl,
                          String description, Timestamp eventTimestamp, String eventDate,
                          String eventTime, String location, String instructor, int capacity) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.category = category;
        this.duration = duration;
        this.format = format;
        this.file = file;
        this.imageUrl = imageUrl;
        this.description = description;
        this.eventTimestamp = eventTimestamp;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.instructor = instructor;
        this.capacity = capacity;
    }

    // Parcelable implementation
    protected SafetyResource(Parcel in) {
        id = in.readString();
        title = in.readString();
        type = in.readString();
        category = in.readString();
        duration = in.readString();
        format = in.readString();
        file = in.readString();
        imageUrl = in.readString();
        description = in.readString();
        eventTimestamp = in.readParcelable(Timestamp.class.getClassLoader());
        eventDate = in.readString();
        eventTime = in.readString();
        location = in.readString();
        instructor = in.readString();
        capacity = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(type);
        dest.writeString(category);
        dest.writeString(duration);
        dest.writeString(format);
        dest.writeString(file);
        dest.writeString(imageUrl);
        dest.writeString(description);
        dest.writeParcelable(eventTimestamp, flags);
        dest.writeString(eventDate);
        dest.writeString(eventTime);
        dest.writeString(location);
        dest.writeString(instructor);
        dest.writeInt(capacity);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Timestamp eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "SafetyResource{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", category='" + category + '\'' +
                ", duration='" + duration + '\'' +
                ", format='" + format + '\'' +
                ", file='" + file + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", eventDate='" + eventDate + '\'' +
                ", eventTime='" + eventTime + '\'' +
                ", location='" + location + '\'' +
                ", instructor='" + instructor + '\'' +
                ", capacity=" + capacity +
                '}';
    }
}