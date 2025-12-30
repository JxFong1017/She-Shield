package com.example.grpassignment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class FullScreenMapFragment extends Fragment {

    private static final String TAG = "FullScreenMapFragment";
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private MyLocationNewOverlay myLocationOverlay;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fullscreen_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        map = view.findViewById(R.id.fullscreen_map_view);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(18.0);

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        map.getOverlays().add(myLocationOverlay);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableMyLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        Bundle args = getArguments();
        if (args != null && args.containsKey("latitude")) {
            GeoPoint startPoint = new GeoPoint(args.getDouble("latitude"), args.getDouble("longitude"));
            map.getController().setCenter(startPoint);
        } else {
            fetchCurrentLocationAndCenterMap();
        }

        addSafetyZoneMarkers();
    }

    private void addSafetyZoneMarkers() {
        if (FirebaseApp.getApps(requireContext()).isEmpty()) {
            Log.e(TAG, "Firebase was not initialized. Forcing initialization now.");
            FirebaseApp.initializeApp(requireContext());
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("safety_zones_pin").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.exists()) {
                            com.google.firebase.firestore.GeoPoint geoPoint = doc.getGeoPoint("geolocation");
                            if (geoPoint == null) continue;

                            GeoPoint osmPoint = new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());

                            Marker marker = new Marker(map);
                            marker.setPosition(osmPoint);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setTitle(doc.getString("name"));

                            marker.setRelatedObject(doc);

                            marker.setOnMarkerClickListener((m, v1) -> {
                                DocumentSnapshot clickedDoc = (DocumentSnapshot) m.getRelatedObject();
                                if (clickedDoc != null) {
                                    showSafetyZoneDialog(clickedDoc);
                                }
                                return true;
                            });
                            map.getOverlays().add(marker);
                        }
                    }
                    map.invalidate();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error fetching Safety Zones: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showSafetyZoneDialog(DocumentSnapshot doc) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_safety_zone_detail, null);

        ImageView dialogImage = dialogView.findViewById(R.id.dialog_image);
        TextView dialogName = dialogView.findViewById(R.id.dialog_name);
        TextView dialogDescription = dialogView.findViewById(R.id.dialog_description);
        TextView dialogPhone = dialogView.findViewById(R.id.dialog_phone);
        TextView dialogStatus = dialogView.findViewById(R.id.dialog_status);
        Button detailsButton = dialogView.findViewById(R.id.dialog_details_button);

        // --- Populate the dialog views ---
        dialogName.setText(doc.getString("name"));
        dialogDescription.setText(doc.getString("description"));
        dialogPhone.setText("Contact: " + doc.getString("phone"));
        Boolean is24hr = doc.getBoolean("is24hour");
        dialogStatus.setText((is24hr != null && is24hr) ? "🟢 24/7 Support" : "🔵 Limited Hours");

        String imageUrl = doc.getString("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(dialogImage);
        }

        // --- Create the dialog so we can dismiss it from the button ---
        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // --- Set the button's click listener ---
        detailsButton.setOnClickListener(v -> {
            SafetyZone zone = doc.toObject(SafetyZone.class);
            if (zone != null) {
                Bundle args = new Bundle();
                args.putParcelable("safetyZone", zone);

                dialog.dismiss(); // Close the dialog

                // Navigate to the detail fragment using the action
                Navigation.findNavController(requireView()).navigate(R.id.action_fullScreenMap_to_safetyZoneDetailFragment, args);
            }
        });

        dialog.show();
    }


    private void fetchCurrentLocationAndCenterMap() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    if (map != null) map.getController().animateTo(startPoint);
                } else {
                    if (map != null) map.getController().setCenter(new GeoPoint(3.1390, 101.6869));
                }
            });
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
            fetchCurrentLocationAndCenterMap();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (myLocationOverlay != null && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }
}
