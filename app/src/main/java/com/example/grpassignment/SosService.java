package com.example.grpassignment;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import static com.example.grpassignment.SheShieldApplication.SOS_CHANNEL_ID;

public class SosService extends Service implements LocationListener {

    private static final String TAG = "SosService";
    private static final String SENT_SMS_ACTION = "SENT_SMS_ACTION";
    private static final String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";

    private LocationManager locationManager;
    private List<String> trustedContacts; // You need to populate this list

    private BroadcastReceiver sentSmsReceiver, deliveredSmsReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        trustedContacts = new ArrayList<>();
        // TODO: Populate trustedContacts from your storage (SharedPreferences, DB, etc.)
        // a placeholder
        trustedContacts.add("601159806213");

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

        startLocationUpdates();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
        } catch (SecurityException e) {
            Log.e(TAG, "Error requesting location updates", e);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
        sendSmsWithLocation(location);
    }

    private void sendSmsWithLocation(Location location) {
        String message = "SOS! I am in danger. My current location is: http://maps.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();
        SmsManager smsManager = SmsManager.getDefault();

        int pendingIntentFlags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT_SMS_ACTION), pendingIntentFlags);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED_SMS_ACTION), pendingIntentFlags);

        for (String phoneNumber : trustedContacts) {
            try {
                smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
                Log.d(TAG, "SMS sent to " + phoneNumber);
            } catch (Exception e) {
                Log.e(TAG, "Error sending SMS to " + phoneNumber, e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        locationManager.removeUpdates(this);
        unregisterReceiver(sentSmsReceiver);
        unregisterReceiver(deliveredSmsReceiver);
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
