package com.example.grpassignment;

public class User {
    public String name;
    public String email;
    public String phone;
    public boolean isSharing;
    public long lastUpdated;
    public double latitude;
    public double longitude;

    // Empty constructor is required for Firebase
    public User() { }

    public User(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        // Set default values for new users
        this.isSharing = false;
        this.lastUpdated = 0;
        this.latitude = 0.0;  // Default until they turn on location
        this.longitude = 0.0; // Default until they turn on location
    }
}