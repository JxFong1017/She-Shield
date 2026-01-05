package com.example.grpassignment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import com.bumptech.glide.Glide;

public class SafetyResourceDetailFragment extends Fragment {

    private SafetyResource resource;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_safety_resource_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        ImageView backButton = view.findViewById(R.id.back_button);
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

        // Get the SafetyResource object passed from the previous fragment/activity
        if (getArguments() != null) {
            resource = getArguments().getParcelable("safetyResource");
        }

        // Back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        if (resource != null) {
            // Load image with Glide
            if (resource.getImageUrl() != null && !resource.getImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(resource.getImageUrl())
                        .placeholder(R.drawable.image_removebg_preview__24_)
                        .error(R.drawable.image_removebg_preview__24_)
                        .centerCrop()
                        .into(resourceImage);
            } else {
                resourceImage.setImageResource(R.drawable.image_removebg_preview__24_);
            }

            // Set common details
            detailTitle.setText(resource.getTitle());
            detailType.setText(resource.getType());
            detailDuration.setText(resource.getDuration());
            detailDescription.setText(resource.getDescription());

            // Handle different resource types
            if ("Workshop".equals(resource.getType())) {
                // Show workshop-specific section
                workshopDetailsSection.setVisibility(View.VISIBLE);

                detailEventDate.setText(resource.getEventDate() != null ? resource.getEventDate() : "TBA");
                detailEventTime.setText(resource.getEventTime() != null ? resource.getEventTime() : "TBA");
                detailLocation.setText(resource.getLocation() != null ? resource.getLocation() : "Online");
                detailInstructor.setText(resource.getInstructor() != null ? resource.getInstructor() : "N/A");
                detailCapacity.setText(resource.getCapacity() + " participants");

                actionButton.setText("Register for Workshop");
                actionButton.setOnClickListener(v -> {
                    // Open registration activity
                    Intent intent = new Intent(getActivity(), RegistrationActivity.class);
                    intent.putExtra("RESOURCE_TITLE", resource.getTitle());
                    intent.putExtra("EVENT_DATE", resource.getEventDate());
                    intent.putExtra("EVENT_TIME", resource.getEventTime());
                    intent.putExtra("LOCATION", resource.getLocation());
                    intent.putExtra("INSTRUCTOR", resource.getInstructor());
                    intent.putExtra("CAPACITY", resource.getCapacity());
                    intent.putExtra("DESCRIPTION", resource.getDescription());
                    intent.putExtra("IMAGE_URL", resource.getImageUrl());
                    startActivity(intent);
                });
            } else {
                // Hide workshop-specific section
                workshopDetailsSection.setVisibility(View.GONE);

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
                            Toast.makeText(getContext(), "Cannot open link", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "No link available", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}