package com.example.grpassignment;

import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide; // Import Glide

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ReportDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.toolbar_report_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Find views
        ImageView imageView = findViewById(R.id.detail_image_view);
        VideoView videoView = findViewById(R.id.detail_video_view);
        TextView noMediaText = findViewById(R.id.no_media_text);
        TextView incidentType = findViewById(R.id.detail_incident_type);
        TextView severity = findViewById(R.id.detail_severity);
        TextView dateTime = findViewById(R.id.detail_date_time);
        TextView location = findViewById(R.id.detail_location);
        TextView description = findViewById(R.id.detail_description);
        TextView anonymousStatus = findViewById(R.id.detail_anonymous_status);

        // Get data from intent
        Report report = (Report) getIntent().getSerializableExtra("report");

        if (report != null) {
            incidentType.setText("Incident Type: " + report.getType());
            severity.setText("Severity: " + report.getSeverity());
            dateTime.setText("Date & Time: " + report.getDate() + " " + report.getTime());
            description.setText(report.getDescription());

            if (report.isAnonymous()) {
                anonymousStatus.setText("Reported Anonymously");
            } else {
                anonymousStatus.setVisibility(View.GONE);
            }

            // --- Handle Location ---
            setLocationText(location, report.getLocation());

            // --- Handle Media with Glide ---
            String mediaUriString = report.getMediaUri();
            if (mediaUriString != null && !mediaUriString.isEmpty()) {
                noMediaText.setVisibility(View.GONE);
                // A simple check for video files, you might need a more robust one
                if (mediaUriString.contains(".mp4") || mediaUriString.contains(".mov") || mediaUriString.contains("video")) {
                    imageView.setVisibility(View.GONE);
                    videoView.setVisibility(View.VISIBLE);
                    videoView.setVideoURI(Uri.parse(mediaUriString));
                    MediaController mediaController = new MediaController(this);
                    mediaController.setAnchorView(videoView);
                    videoView.setMediaController(mediaController);
                } else {
                    videoView.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    Glide.with(this)
                         .load(mediaUriString)
                         .placeholder(R.drawable.screenshot_2025_11_18_130845_removebg_preview)
                         .into(imageView);
                }
            } else {
                noMediaText.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setLocationText(TextView locationView, String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            locationView.setText("Location: Not provided");
            return;
        }

        // Geocoding should be done on a background thread in a real app
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                String[] latLng = locationString.split(",");
                double latitude = Double.parseDouble(latLng[0]);
                double longitude = Double.parseDouble(latLng[1]);

                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String addressLine = address.getAddressLine(0);
                    runOnUiThread(() -> locationView.setText("Location: " + addressLine));
                } else {
                    runOnUiThread(() -> locationView.setText("Location: " + locationString));
                }
            } catch (IOException | NumberFormatException | IndexOutOfBoundsException e) {
                Log.e("ReportDetailActivity", "Error geocoding location", e);
                runOnUiThread(() -> locationView.setText("Location: " + locationString)); // Fallback
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // This will take you back to the previous screen
        return true;
    }
}
