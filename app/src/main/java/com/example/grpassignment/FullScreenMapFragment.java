package com.example.grpassignment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
        if (args != null && args.containsKey("latitude") && args.containsKey("longitude")) {
            GeoPoint startPoint = new GeoPoint(args.getDouble("latitude"), args.getDouble("longitude"));
            map.getController().setCenter(startPoint);
        } else {
            fetchCurrentLocationAndCenterMap();
        }

        addSafetyZoneMarkers();
    }

    private void fetchCurrentLocationAndCenterMap() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
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

    private void addSafetyZoneMarkers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("safety_zones_pin").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.google.firebase.firestore.GeoPoint geoPoint = doc.getGeoPoint("geolocation");
                        if (geoPoint == null) continue;

                        GeoPoint osmPoint = new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());

                        String name = doc.getString("name");
                        String type = doc.getString("type");
                        String phone = doc.getString("phone");
                        Boolean is24hrObj = doc.getBoolean("is24hour");
                        boolean is24hr = is24hrObj != null && is24hrObj;

                        Marker marker = new Marker(map);
                        marker.setPosition(osmPoint);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        // 1. Set a SIMPLE, single-line title. This is safe.
                        marker.setTitle(name != null ? name : "Unknown");

                        // 2. DO NOT USE setSnippet() with multi-line strings. Store data in a Bundle instead.
                        Bundle markerData = new Bundle();
                        markerData.putString("name", name);
                        markerData.putString("type", type);
                        markerData.putString("phone", phone);
                        markerData.putBoolean("is24hr", is24hr);
                        marker.setRelatedObject(markerData);

                        // 3. Build the complex string inside the click listener.
                        marker.setOnMarkerClickListener((m, v1) -> {
                            Bundle data = (Bundle) m.getRelatedObject();
                            if (data == null) return true;

                            String toastName = data.getString("name", "");
                            String toastType = data.getString("type", "Unknown Type");
                            String toastPhone = data.getString("phone", "N/A");
                            boolean toastIs24hr = data.getBoolean("is24hr", false);

                            String toastMessage = toastName + "\n" +
                                    "Type: " + toastType + "\n" +
                                    "Contact: " + toastPhone +
                                    (toastIs24hr ? "\n🟢 24/7 Support" : "\n🔵 Limited Hours");

                            Toast.makeText(getContext(), toastMessage, Toast.LENGTH_LONG).show();
                            return true; // Consume the event
                        });

                        map.getOverlays().add(marker);
                    }
                    map.invalidate();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error fetching Safety Zones: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
                fetchCurrentLocationAndCenterMap();
            } else {
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
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
