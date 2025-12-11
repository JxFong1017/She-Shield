package com.example.grpassignment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.List;

public class SafetyMapFragment extends Fragment {

    private MapView mapPreview;
    private FrameLayout mapPreviewContainer;
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint currentUserLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // OSM Droid configuration
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_safety_map, container, false);

        mapPreviewContainer = view.findViewById(R.id.map_preview_container);

        mapPreview = new MapView(requireContext());
        mapPreview.setClickable(false);
        mapPreview.setMultiTouchControls(false);
        mapPreview.setFlingEnabled(false);
        mapPreview.getController().setZoom(15.0);

        mapPreviewContainer.addView(mapPreview);

        // Fetch location to center the preview
        fetchCurrentLocationAndCenterPreview();

        return view;
    }

@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    CardView BtnCat = view.findViewById(R.id.map_preview_parent_container);

    // Listener merged with argument passing
    View.OnClickListener OCLCat = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Bundle args = new Bundle();

            // Pass Lat & Lng if exist
            if (currentUserLocation != null) {
                args.putDouble("latitude", currentUserLocation.getLatitude());
                args.putDouble("longitude", currentUserLocation.getLongitude());
            }

            // Navigate with Safe Arguments
            Navigation.findNavController(v)
                    .navigate(R.id.action_nav_map_to_fullScreenMapFragment, args);
        }
    };

    BtnCat.setOnClickListener(OCLCat);

    // Card List (Verified Safety Zones Near You)
    RecyclerView recyclerView = view.findViewById(R.id.recyclerSafetyZones);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    SafetyZoneAdapter adapter = new SafetyZoneAdapter();
    recyclerView.setAdapter(adapter);

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Load Safety Zones from Firestore
    db.collection("safety_zones_pin")
            .get()
            .addOnSuccessListener(query -> {
                List<SafetyZone> zones = new ArrayList<>();
                for (DocumentSnapshot doc : query) {
                    SafetyZone z = doc.toObject(SafetyZone.class);
                    zones.add(z);
                }

                adapter.setData(zones);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
}


    /*SAMPLE
    * ImageButton BtnDog = view.findViewById(R.id.BtnDog);
        View.OnClickListener OCLDog = new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Navigation.findNavController(view).navigate(R.id.NextToDog);
            }
        };
        BtnDog.setOnClickListener(OCLDog);
    * */

    private void fetchCurrentLocationAndCenterPreview() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                currentUserLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
            } else {
                // Default to KL if location is unavailable
                currentUserLocation = new GeoPoint(3.1390, 101.6869);
            }
            if (mapPreview != null) {
                mapPreview.getController().setCenter(currentUserLocation);
            }

            // Add overlay to show and track current location
            MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(
                    new GpsMyLocationProvider(requireContext()), mapPreview);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation(); // Auto-center on movement
            mapPreview.getOverlays().add(myLocationOverlay);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocationAndCenterPreview();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapPreview != null) {
            mapPreview.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapPreview != null) {
            mapPreview.onPause();
        }
    }
}
