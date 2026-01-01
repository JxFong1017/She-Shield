package com.example.grpassignment;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

public class SafetyZone implements Parcelable {

    public String name;
    public GeoPoint geolocation;
    public boolean is24hour;
    public String type;
    public String phone;
    public String imageUrl;
    public String description;
    public String address;

    @Exclude
    public double distanceToUser;

    public SafetyZone() {}

    // --- Parcelable Implementation ---
    protected SafetyZone(Parcel in) {
        name = in.readString();
        // GeoPoint is not Parcelable by default, so we read lat/lon and recreate it
        double lat = in.readDouble();
        double lon = in.readDouble();
        geolocation = new GeoPoint(lat, lon);
        is24hour = in.readByte() != 0;
        type = in.readString();
        phone = in.readString();
        imageUrl = in.readString();
        description = in.readString();
        address = in.readString();
        distanceToUser = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        // Write GeoPoint as two separate doubles
        dest.writeDouble(geolocation != null ? geolocation.getLatitude() : 0);
        dest.writeDouble(geolocation != null ? geolocation.getLongitude() : 0);
        dest.writeByte((byte) (is24hour ? 1 : 0));
        dest.writeString(type);
        dest.writeString(phone);
        dest.writeString(imageUrl);
        dest.writeString(description);
        dest.writeString(address);
        dest.writeDouble(distanceToUser);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SafetyZone> CREATOR = new Creator<SafetyZone>() {
        @Override
        public SafetyZone createFromParcel(Parcel in) {
            return new SafetyZone(in);
        }

        @Override
        public SafetyZone[] newArray(int size) {
            return new SafetyZone[size];
        }
    };
}
