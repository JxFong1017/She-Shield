package com.example.grpassignment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

public class SafetyZoneDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_safety_zone_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView detailImage = view.findViewById(R.id.detail_image);
        TextView detailName = view.findViewById(R.id.detail_name);
        TextView detailType = view.findViewById(R.id.detail_type);
        TextView detailDescription = view.findViewById(R.id.detail_description);
        TextView detailAddress = view.findViewById(R.id.detail_address);
        TextView detailPhone = view.findViewById(R.id.detail_phone);
        TextView detailIs24Hour = view.findViewById(R.id.detail_is24hour);

        // Get the SafetyZone object passed from the previous fragment
        SafetyZone zone = null;
        if (getArguments() != null) {
            zone = getArguments().getParcelable("safetyZone");
        }

        if (zone != null) {
            // Use the data to populate the views
            detailName.setText(zone.name);
            detailType.setText("Type: " + zone.type);
            detailDescription.setText(zone.description);
            detailAddress.setText("Address: " + zone.address);
            detailPhone.setText("Phone: " + zone.phone);

            if (zone.is24hour) {
                detailIs24Hour.setText("🟢 24/7 Support Available");
            } else {
                detailIs24Hour.setText("🔵 Limited Hours");
            }

            // Load the image using Glide
            if (zone.imageUrl != null && !zone.imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(zone.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(detailImage);
            }
        }
    }
}
