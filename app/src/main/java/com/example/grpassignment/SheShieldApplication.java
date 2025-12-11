package com.example.grpassignment;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class SheShieldApplication extends Application {
    private static final String TAG = "SheShieldApp";

    @Override
    public void onCreate() {
        super.onCreate();
        // This log message is a definitive test to see if this class is being run.
        Log.d(TAG, "SHE-SHIELD APPLICATION onCreate CALLED");

        // This line is crucial. It initializes Firebase for the entire app.
        FirebaseApp.initializeApp(this);
    }
}
