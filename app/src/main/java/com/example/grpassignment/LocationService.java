package com.example.grpassignment;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_channel";
    private static final int NOTIFICATION_ID = 12345;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private String currentUserId = "U0001";
    // private String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Prepare the callback once
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();

                    Log.d(TAG, "Location Update: " + lat + ", " + lng);

                    updateFirebase(lat, lng);

                    Intent intent = new Intent("LocationUpdate");
                    intent.putExtra("lat", lat);
                    intent.putExtra("lng", lng);
                    intent.putExtra("time", locationResult.getLastLocation().getTime());

                    // Specify the package
                    intent.setPackage(getPackageName());

                    sendBroadcast(intent);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("STOP".equals(action)) {
                stopForegroundService();
            } else {
                startLocationUpdates();
            }
        }
        return START_STICKY; // Restarts service if system kills it
    }

    private void startLocationUpdates() {
        // Create notification channel
        createNotificationChannel();

        // Build the notification
        Intent notificationIntent = new Intent(this, HomeActivity.class); // Opens app when clicked
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SheShield Active")
                .setContentText("Sharing your live location with trusted contacts...")
                .setSmallIcon(R.mipmap.ic_launcher) // Make sure this icon exists!
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();

        // Start foreground
        startForeground(NOTIFICATION_ID, notification);

        // Request location updates
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Toast.makeText(this, "Location Sharing Started (Background)", Toast.LENGTH_SHORT).show();
    }

    private void stopForegroundService() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();

        // Mark as not sharing in Firebase
        Map<String, Object> update = new HashMap<>();
        update.put("isSharing", false);
        db.collection("user").document(currentUserId).update(update);
    }

    private void updateFirebase(double lat, double lng) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", lat);
        locationData.put("longitude", lng);
        locationData.put("lastUpdated", System.currentTimeMillis());
        locationData.put("isSharing", true);

        db.collection("user").document(currentUserId).update(locationData);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Sharing Channel",
                    NotificationManager.IMPORTANCE_LOW // Low importance = no sound/popup annoyance
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // no need binding
    }
}