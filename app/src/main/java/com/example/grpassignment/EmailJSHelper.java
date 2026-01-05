package com.example.grpassignment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EmailJSHelper {
    private static final String TAG = "EmailJSHelper";
    private static final String EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send";

    // ⚠️ IMPORTANT: Replace these with your actual EmailJS credentials
    private static final String SERVICE_ID = "service_sefot59";      // e.g., "service_abc123"
    private static final String TEMPLATE_ID = "template_qbq23zf";    // e.g., "template_xyz789"
    private static final String PUBLIC_KEY = "MSO-qn_TPcz3HfaJp";      // e.g., "user_def456"

    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public EmailJSHelper() {
        this.client = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "EmailJSHelper initialized");
        Log.d(TAG, "SERVICE_ID: " + SERVICE_ID);
        Log.d(TAG, "TEMPLATE_ID: " + TEMPLATE_ID);
        Log.d(TAG, "PUBLIC_KEY: " + PUBLIC_KEY);

        // Check if credentials are still default
        if (SERVICE_ID.equals("your_service_id") ||
                TEMPLATE_ID.equals("your_template_id") ||
                PUBLIC_KEY.equals("your_public_key")) {
            Log.e(TAG, "⚠️ WARNING: EmailJS credentials not configured! Please update SERVICE_ID, TEMPLATE_ID, and PUBLIC_KEY");
        }
    }

    public void sendRegistrationConfirmation(
            String userEmail,
            String userName,
            String workshopTitle,
            String eventDate,
            String eventTime,
            String location,
            EmailCallback callback) {

        Log.d(TAG, "========================================");
        Log.d(TAG, "Starting email send process...");
        Log.d(TAG, "To: " + userEmail);
        Log.d(TAG, "Name: " + userName);
        Log.d(TAG, "Workshop: " + workshopTitle);
        Log.d(TAG, "========================================");

        executor.execute(() -> {
            try {
                // Create JSON payload
                JSONObject emailParams = new JSONObject();
                emailParams.put("to_email", userEmail);
                emailParams.put("to_name", userName);
                emailParams.put("workshop_title", workshopTitle);
                emailParams.put("event_date", eventDate != null ? eventDate : "TBA");
                emailParams.put("event_time", eventTime != null ? eventTime : "TBA");
                emailParams.put("location", location != null ? location : "TBA");

                JSONObject payload = new JSONObject();
                payload.put("service_id", SERVICE_ID);
                payload.put("template_id", TEMPLATE_ID);
                payload.put("user_id", PUBLIC_KEY);
                payload.put("template_params", emailParams);

                Log.d(TAG, "📧 Payload created:");
                Log.d(TAG, payload.toString(2)); // Pretty print JSON

                // Create request
                RequestBody body = RequestBody.create(
                        payload.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(EMAILJS_API_URL)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                Log.d(TAG, "🌐 Sending request to EmailJS...");

                // Execute request
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";

                    Log.d(TAG, "📥 Response Code: " + response.code());
                    Log.d(TAG, "📥 Response Message: " + response.message());
                    Log.d(TAG, "📥 Response Body: " + responseBody);

                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Email sent successfully!");
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess();
                        });
                    } else {
                        String error = "Email failed - Code: " + response.code() +
                                ", Message: " + response.message() +
                                ", Body: " + responseBody;
                        Log.e(TAG, "❌ " + error);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(error);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception while sending email", e);
                Log.e(TAG, "Error message: " + e.getMessage());
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
            }
        });
    }

    public void sendAdminNotification(
            String adminEmail,
            String userName,
            String userEmail,
            String workshopTitle,
            String eventDate,
            EmailCallback callback) {

        Log.d(TAG, "========================================");
        Log.d(TAG, "Sending admin notification...");
        Log.d(TAG, "Admin Email: " + adminEmail);
        Log.d(TAG, "========================================");

        executor.execute(() -> {
            try {
                // Create JSON payload for admin notification
                JSONObject emailParams = new JSONObject();
                emailParams.put("to_email", adminEmail);
                emailParams.put("user_name", userName);
                emailParams.put("user_email", userEmail);
                emailParams.put("workshop_title", workshopTitle);
                emailParams.put("event_date", eventDate != null ? eventDate : "TBA");

                JSONObject payload = new JSONObject();
                payload.put("service_id", SERVICE_ID);
                payload.put("template_id", "admin_notification_template"); // Separate admin template
                payload.put("user_id", PUBLIC_KEY);
                payload.put("template_params", emailParams);

                Log.d(TAG, "📧 Admin notification payload:");
                Log.d(TAG, payload.toString(2));

                // Create request
                RequestBody body = RequestBody.create(
                        payload.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(EMAILJS_API_URL)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                Log.d(TAG, "🌐 Sending admin notification request...");

                // Execute request
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";

                    Log.d(TAG, "📥 Admin notification response code: " + response.code());
                    Log.d(TAG, "📥 Response body: " + responseBody);

                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Admin notification sent!");
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess();
                        });
                    } else {
                        String error = "Admin notification failed - Code: " + response.code() +
                                ", Body: " + responseBody;
                        Log.e(TAG, "❌ " + error);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure(error);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception sending admin notification", e);
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
            }
        });
    }

    public void shutdown() {
        Log.d(TAG, "Shutting down EmailJSHelper");
        executor.shutdown();
    }

    public interface EmailCallback {
        void onSuccess();

        void onFailure(String error);
    }
}