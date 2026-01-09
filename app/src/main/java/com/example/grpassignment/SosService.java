package com.example.grpassignment;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import static com.example.grpassignment.SheShieldApplication.SOS_CHANNEL_ID;

public class SosService extends Service implements LocationListener {

    private static final String TAG = "SosService";
    private static final String SENT_SMS_ACTION = "SENT_SMS_ACTION";
    private static final String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";

    private LocationManager locationManager;
    private List<TrustedContact> trustedContacts;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    private BroadcastReceiver sentSmsReceiver, deliveredSmsReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        trustedContacts = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Log.e(TAG, "No authenticated user. Cannot send SOS messages.");
            stopSelf();
            return;
        }

        sentSmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "SMS sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(context, "Generic failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(context, "No service", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(context, "Radio off", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        deliveredSmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "SMS delivered", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        ContextCompat.registerReceiver(this, sentSmsReceiver, new IntentFilter(SENT_SMS_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, deliveredSmsReceiver, new IntentFilter(DELIVERED_SMS_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void fetchTrustedContactsAndInitiateSos() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            return;
        }
        db.collection("user").document(currentUserId).collection("trusted_contacts")
                .orderBy("rank", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        trustedContacts.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            TrustedContact contact = document.toObject(TrustedContact.class);
                            trustedContacts.add(contact);
                        }
                        Log.d(TAG, "Fetched " + trustedContacts.size() + " trusted contacts for SOS.");
                        if (!trustedContacts.isEmpty()) {
                            // Log the phone number before making the call
                            Log.d(TAG, "Primary contact phone number: " + trustedContacts.get(0).getPhone());
                            // Call the primary contact (rank 0)
                            makePhoneCall(trustedContacts.get(0).getPhone());
                            startLocationUpdates();
                        } else {
                            Log.w(TAG, "No trusted contacts found for the user.");
                        }
                    } else {
                        Log.e(TAG, "Error fetching trusted contacts for SOS: ", task.getException());
                    }
                });
    }

    private void makePhoneCall(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    startActivity(callIntent);
                    Log.d(TAG, "Calling " + phoneNumber);
                } catch (Exception e) {
                    Log.e(TAG, "Error making phone call", e);
                }
            } else {
                Log.w(TAG, "CALL_PHONE permission not granted.");
                // Optionally, inform the user they need to grant permission.
            }
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        Intent notificationIntent = new Intent(this, HomeActivity.class);
        int pendingIntentFlags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, pendingIntentFlags);

        Notification notification = new NotificationCompat.Builder(this, SOS_CHANNEL_ID)
                .setContentTitle("SOS Active")
                .setContentText("Sending location updates to trusted contacts.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        fetchTrustedContactsAndInitiateSos();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Location permissions not granted.");
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
        } catch (SecurityException e) {
            Log.e(TAG, "Error requesting location updates", e);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
        if (trustedContacts != null && !trustedContacts.isEmpty()) {
            sendSmsWithLocation(location);
        } else {
            Log.w(TAG, "No trusted contacts to send SMS to.");
        }
    }

    private void sendSmsWithLocation(Location location) {
        String message = "SOS! I am in danger. My current location is: http://maps.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();
        SmsManager smsManager = SmsManager.getDefault();

        int pendingIntentFlags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT_SMS_ACTION), pendingIntentFlags);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED_SMS_ACTION), pendingIntentFlags);

        for (TrustedContact contact : trustedContacts) {
            try {
                smsManager.sendTextMessage(contact.getPhone(), null, message, sentPI, deliveredPI);
                Log.d(TAG, "SMS sent to " + contact.getPhone());
            } catch (Exception e) {
                Log.e(TAG, "Error sending SMS to " + contact.getPhone(), e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        if (sentSmsReceiver != null) {
            unregisterReceiver(sentSmsReceiver);
        }
        if (deliveredSmsReceiver != null) {
            unregisterReceiver(deliveredSmsReceiver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }
}
