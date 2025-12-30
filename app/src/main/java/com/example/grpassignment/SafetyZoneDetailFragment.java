package com.example.grpassignment;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class SafetyZoneDetailFragment extends Fragment {

    private MapView mapPreview;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_safety_zone_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Find all the views ---
        ImageView detailImage = view.findViewById(R.id.detail_image);
        TextView detailName = view.findViewById(R.id.detail_name);
        TextView detailType = view.findViewById(R.id.detail_type);
        TextView detailDescription = view.findViewById(R.id.detail_description);
        TextView detailAddress = view.findViewById(R.id.detail_address);
        TextView detailPhone = view.findViewById(R.id.detail_phone);
        TextView detailIs24Hour = view.findViewById(R.id.detail_is24hour);
        Button viewOnMapButton = view.findViewById(R.id.button_view_on_map);
        mapPreview = view.findViewById(R.id.detail_map_preview);

        SafetyZone zone = getArguments() != null ? getArguments().getParcelable("safetyZone") : null;

        if (zone != null) {
            // --- Populate the standard views ---
            detailName.setText(zone.name);
            detailType.setText("Type: " + zone.type);
            detailDescription.setText(zone.description);
            detailAddress.setText("Address: " + zone.address);
            detailPhone.setText("Phone: " + zone.phone);
            detailIs24Hour.setText(zone.is24hour ? "🟢 24/7 Support Available" : "🔵 Limited Hours");

            if (zone.imageUrl != null && !zone.imageUrl.isEmpty()) {
                Glide.with(this).load(zone.imageUrl).into(detailImage);
            }

            // --- Initialize the Map Preview ---
            if (zone.geolocation != null) {
                mapPreview.setTileSource(TileSourceFactory.MAPNIK);
                mapPreview.setMultiTouchControls(false);
                mapPreview.setBuiltInZoomControls(false);

                GeoPoint zonePoint = new GeoPoint(zone.geolocation.getLatitude(), zone.geolocation.getLongitude());
                mapPreview.getController().setZoom(16.0);
                mapPreview.getController().setCenter(zonePoint);

                Marker zoneMarker = new Marker(mapPreview);
                zoneMarker.setPosition(zonePoint);
                zoneMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapPreview.getOverlays().add(zoneMarker);
                mapPreview.invalidate();
            }

            // --- Set up button click listener ---
            viewOnMapButton.setOnClickListener(v -> {
                if (zone.geolocation != null) {
                    Bundle args = new Bundle();
                    args.putDouble("latitude", zone.geolocation.getLatitude());
                    args.putDouble("longitude", zone.geolocation.getLongitude());
                    Navigation.findNavController(v).navigate(R.id.action_safetyZoneDetail_to_fullScreenMap, args);
                }
            });

            // --- Make Phone Number and Address Clickable ---
             if (zone.phone != null && !zone.phone.isEmpty()) {
                detailPhone.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_700));
                detailPhone.setOnClickListener(v -> {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + zone.phone));
                    try {
                        startActivity(dialIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), "No phone app found", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (zone.address != null && !zone.address.isEmpty()) {
                detailAddress.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_700));
                detailAddress.setOnClickListener(v -> {
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(zone.address));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                });
            }
        }
    }

    // --- MapView lifecycle methods ---
    @Override
    public void onResume() {
        super.onResume();
        if (mapPreview != null) mapPreview.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapPreview != null) mapPreview.onPause();
    }
}
