package com.example.grpassignment;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class SheShieldApplication extends Application {
    private static final String TAG = "SheShieldApp";
    public static final String SOS_CHANNEL_ID = "SosServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        // This log message is a definitive test to see if this class is being run.
        Log.d(TAG, "SHE-SHIELD APPLICATION onCreate CALLED");

        // This line is crucial. It initializes Firebase for the entire app.
        FirebaseApp.initializeApp(this);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    SOS_CHANNEL_ID,
                    "SOS Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
