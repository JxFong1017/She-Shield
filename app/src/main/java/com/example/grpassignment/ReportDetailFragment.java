package com.example.grpassignment;

import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ReportDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        ImageView imageView = view.findViewById(R.id.detail_image_view);
        VideoView videoView = view.findViewById(R.id.detail_video_view);
        TextView noMediaText = view.findViewById(R.id.no_media_text);
        TextView incidentType = view.findViewById(R.id.detail_incident_type);
        TextView severity = view.findViewById(R.id.detail_severity);
        TextView dateTime = view.findViewById(R.id.detail_date_time);
        TextView location = view.findViewById(R.id.detail_location);
        TextView description = view.findViewById(R.id.detail_description);
        TextView anonymousStatus = view.findViewById(R.id.detail_anonymous_status);

        // Get data from arguments
        Report report = null;
        if (getArguments() != null) {
            report = (Report) getArguments().getSerializable("report");
        }

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
                    MediaController mediaController = new MediaController(requireContext());
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

        // Geocoding should be done on a background thread
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                String[] latLng = locationString.split(",");
                double latitude = Double.parseDouble(latLng[0]);
                double longitude = Double.parseDouble(latLng[1]);

                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String addressLine = address.getAddressLine(0);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> locationView.setText("Location: " + addressLine));
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> locationView.setText("Location: " + locationString));
                    }
                }
            } catch (IOException | NumberFormatException | IndexOutOfBoundsException e) {
                Log.e("ReportDetailFragment", "Error geocoding location", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> locationView.setText("Location: " + locationString));
                }
            }
        }).start();
    }
}
