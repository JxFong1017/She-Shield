package com.example.grpassignment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;

public class SafetyResourceDetailFragment extends Fragment {

    private static final String TAG = "SafetyResourceDetail";
    private SafetyResource resource;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            Log.d(TAG, "onCreateView started");
            return inflater.inflate(R.layout.fragment_safety_resource_detail, container, false);
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage(), e);
            TextView errorView = new TextView(requireContext());
            errorView.setText("Error loading detail page:\n" + e.getMessage());
            errorView.setPadding(32, 32, 32, 32);
            return errorView;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            Log.d(TAG, "onViewCreated started");
            
            // Get the SafetyResource object passed from the previous fragment/activity
            if (getArguments() != null) {
                resource = getArguments().getParcelable("safetyResource");
                Log.d(TAG, "Resource received: " + (resource != null ? resource.getTitle() : "null"));
            } else {
                Log.e(TAG, "No arguments passed to fragment");
            }
            
            if (resource == null) {
                Toast.makeText(requireContext(), "Error: No resource data found", Toast.LENGTH_LONG).show();
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
                return;
            }

            // Initialize views with null checks
            ImageView resourceImage = view.findViewById(R.id.resource_image);
            TextView detailTitle = view.findViewById(R.id.detail_title);
            TextView detailType = view.findViewById(R.id.detail_type);
            TextView detailDuration = view.findViewById(R.id.detail_duration);
            TextView detailDescription = view.findViewById(R.id.detail_description);

            // Workshop-specific views
            View workshopDetailsSection = view.findViewById(R.id.workshop_details_section);
            TextView detailEventDate = view.findViewById(R.id.detail_event_date);
            TextView detailEventTime = view.findViewById(R.id.detail_event_time);
            TextView detailLocation = view.findViewById(R.id.detail_location);
            TextView detailInstructor = view.findViewById(R.id.detail_instructor);
            TextView detailCapacity = view.findViewById(R.id.detail_capacity);

            Button actionButton = view.findViewById(R.id.action_button);
            
            // Log which views are null
            if (resourceImage == null) Log.e(TAG, "resourceImage is null");
            if (detailTitle == null) Log.e(TAG, "detailTitle is null");
            if (detailType == null) Log.e(TAG, "detailType is null");
            if (detailDuration == null) Log.e(TAG, "detailDuration is null");
            if (detailDescription == null) Log.e(TAG, "detailDescription is null");
            if (actionButton == null) Log.e(TAG, "actionButton is null");

            // Load image with Glide
            if (resourceImage != null) {
                if (resource.getImageUrl() != null && !resource.getImageUrl().isEmpty()) {
                    try {
                        Glide.with(this)
                                .load(resource.getImageUrl())
                                .placeholder(R.drawable.image_removebg_preview__24_)
                                .error(R.drawable.image_removebg_preview__24_)
                                .centerCrop()
                                .into(resourceImage);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading image: " + e.getMessage());
                        resourceImage.setImageResource(R.drawable.image_removebg_preview__24_);
                    }
                } else {
                    resourceImage.setImageResource(R.drawable.image_removebg_preview__24_);
                }
            }

            // Set common details with null checks
            if (detailTitle != null) detailTitle.setText(resource.getTitle() != null ? resource.getTitle() : "Untitled");
            if (detailType != null) detailType.setText(resource.getType() != null ? resource.getType() : "N/A");
            if (detailDuration != null) detailDuration.setText(resource.getDuration() != null ? resource.getDuration() : "N/A");
            if (detailDescription != null) detailDescription.setText(resource.getDescription() != null ? resource.getDescription() : "No description available");

            // Handle different resource types
            if ("Workshop".equals(resource.getType())) {
                // Show workshop-specific section
                if (workshopDetailsSection != null) workshopDetailsSection.setVisibility(View.VISIBLE);

                if (detailEventDate != null) detailEventDate.setText(resource.getEventDate() != null ? resource.getEventDate() : "TBA");
                if (detailEventTime != null) detailEventTime.setText(resource.getEventTime() != null ? resource.getEventTime() : "TBA");
                if (detailLocation != null) detailLocation.setText(resource.getLocation() != null ? resource.getLocation() : "Online");
                if (detailInstructor != null) detailInstructor.setText(resource.getInstructor() != null ? resource.getInstructor() : "N/A");
                if (detailCapacity != null) detailCapacity.setText(resource.getCapacity() + " participants");

                if (actionButton != null) {
                    actionButton.setText("Register for Workshop");
                    actionButton.setOnClickListener(v -> {
                        try {
                            // Navigate to registration fragment
                            Navigation.findNavController(v).navigate(R.id.action_safetyResourceDetail_to_registration);
                        } catch (Exception e) {
                            Log.e(TAG, "Error opening registration: " + e.getMessage());
                            Toast.makeText(getContext(), "Error opening registration", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                // Hide workshop-specific section
                if (workshopDetailsSection != null) workshopDetailsSection.setVisibility(View.GONE);

                if (actionButton != null) {
                    // Set appropriate button text based on type
                    if ("Video".equals(resource.getType())) {
                        actionButton.setText("▶️ Watch Video");
                    } else if ("Legal".equals(resource.getType())) {
                        actionButton.setText("📖 Read Legal Info");
                    } else if ("Article".equals(resource.getType())) {
                        actionButton.setText("📖 Read Article");
                    } else {
                        actionButton.setText("Open Resource");
                    }

                    // Open the resource URL
                    actionButton.setOnClickListener(v -> {
                        if (resource.getFile() != null && !resource.getFile().isEmpty()) {
                            try {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(resource.getFile()));
                                startActivity(browserIntent);
                            } catch (Exception e) {
                                Log.e(TAG, "Error opening link: " + e.getMessage());
                                Toast.makeText(getContext(), "Cannot open link", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "No link available", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            
            Log.d(TAG, "onViewCreated completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR in onViewCreated: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error loading details: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }
}