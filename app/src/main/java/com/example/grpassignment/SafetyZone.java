package com.example.grpassignment;

import com.google.firebase.firestore.GeoPoint;

public class SafetyZone {
    public String name;
    public String type;
    public String phone;
    public boolean is24hour;
    public GeoPoint geolocation;   // Store latitude + longitude as a single GeoPoint

    public SafetyZone() {} // Required by Firestore

    public SafetyZone(String name, String type, String phone, boolean is24hour, GeoPoint geolocation) {
        this.name = name;
        this.type = type;
        this.phone = phone;
        this.is24hour = is24hour;
        this.geolocation = geolocation;
    }
}
