package com.example.grpassignment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SafetyMapFragment extends Fragment implements SafetyZoneAdapter.OnItemClickListener {

    private MapView mapPreview;
    private MyLocationNewOverlay myLocationOverlay;
    private FrameLayout mapPreviewContainer;
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint currentUserLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private RecyclerView recyclerView;
    private SafetyZoneAdapter adapter;
    private final List<SafetyZone> allSafetyZones = new ArrayList<>();
    private boolean isDataLoaded = false;
    private boolean isLocationAvailable = false;
    private String currentFilterType = "All";

    private CardView filterAll, filterShelters, filterLegal, filterCounseling;
    private List<CardView> filterChips;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext().getApplicationContext();
        
        // Configure OSMDroid with proper cache paths
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        
        // Set cache path to internal storage to avoid permission issues
        File osmBasePath = new File(ctx.getFilesDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmBasePath);
        File osmTileCache = new File(osmBasePath, "tiles");
        Configuration.getInstance().setOsmdroidTileCache(osmTileCache);
        
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

        // Initialize and add the user location overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapPreview);
        mapPreview.getOverlays().add(myLocationOverlay);

        fetchCurrentLocationAndCenterPreview();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView zoomInMap = view.findViewById(R.id.zoom_in_map);
        zoomInMap.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (currentUserLocation != null) {
                args.putDouble("latitude", currentUserLocation.getLatitude());
                args.putDouble("longitude", currentUserLocation.getLongitude());
            }
            Navigation.findNavController(v).navigate(R.id.action_nav_map_to_fullScreenMapFragment, args);
        });

        recyclerView = view.findViewById(R.id.recyclerSafetyZones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SafetyZoneAdapter(this);
        recyclerView.setAdapter(adapter);

        filterAll = view.findViewById(R.id.filter_all);
        filterShelters = view.findViewById(R.id.filter_shelters);
        filterLegal = view.findViewById(R.id.filter_legal);
        filterCounseling = view.findViewById(R.id.filter_counseling);

        filterChips = new ArrayList<>();
        filterChips.add(filterAll);
        filterChips.add(filterShelters);
        filterChips.add(filterLegal);
        filterChips.add(filterCounseling);

        filterAll.setOnClickListener(v -> {
            updateChipStyles(filterAll);
            filterAndDisplay("All");
        });
        filterShelters.setOnClickListener(v -> {
            updateChipStyles(filterShelters);
            filterAndDisplay("Shelter");
        });
        filterLegal.setOnClickListener(v -> {
            updateChipStyles(filterLegal);
            filterAndDisplay("Legal Aid");
        });
        filterCounseling.setOnClickListener(v -> {
            updateChipStyles(filterCounseling);
            filterAndDisplay("Counseling");
        });

        loadDataFromFirestore();
    }

    private void updateChipStyles(CardView selectedChip) {
        if (getContext() == null) return;
        for (CardView chip : filterChips) {
            TextView textView = (TextView) chip.getChildAt(0);
            if (chip == selectedChip) {
                chip.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.chip_selected_background));
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_selected_text));
            } else {
                chip.setCardBackgroundColor(Color.WHITE);
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_unselected_text));
            }
        }
    }

    private void loadDataFromFirestore() {
        FirebaseFirestore.getInstance().collection("safety_zones_pin").get()
                .addOnSuccessListener(query -> {
                    if (!isAdded()) return;
                    allSafetyZones.clear();
                    for (DocumentSnapshot doc : query) {
                        SafetyZone z = doc.toObject(SafetyZone.class);
                        if (z != null) {
                            allSafetyZones.add(z);
                        }
                    }
                    isDataLoaded = true;
                    calculateAndSortDistances();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    String errorMsg = "Error loading safety zones";
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        errorMsg = "Database permission denied. Please check Firestore security rules.";
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    private void fetchCurrentLocationAndCenterPreview() {
        // Check and request permissions if needed
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        // Check storage permission for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        myLocationOverlay.enableMyLocation(); // Enable the location overlay
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                currentUserLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
            } else {
                currentUserLocation = new GeoPoint(3.1390, 101.6869);
            }
            isLocationAvailable = true;
            if (mapPreview != null) {
                mapPreview.getController().setCenter(currentUserLocation);
            }
            calculateAndSortDistances();
        });
    }

    private void calculateAndSortDistances() {
        if (!isDataLoaded || !isLocationAvailable) return;

        // Set initial distance to 0 to show "Calculating..." while actual distance is being calculated
        for (SafetyZone zone : allSafetyZones) {
            zone.distanceToUser = 0;
        }

        updateMapPreviewMarkers();
        updateChipStyles(filterAll);
        filterAndDisplay(currentFilterType);

        // Calculate actual road distances in background
        calculateRoadDistances();
    }

    private void calculateRoadDistances() {
        new Thread(() -> {
            try {
                if (getContext() == null) return;
                
                RoadManager roadManager = new OSRMRoadManager(getContext(), "SheShield/1.0");
                
                for (int i = 0; i < allSafetyZones.size(); i++) {
                    SafetyZone zone = allSafetyZones.get(i);
                    
                    if (zone.geolocation != null && getActivity() != null) {
                        try {
                            org.osmdroid.util.GeoPoint zonePoint = new org.osmdroid.util.GeoPoint(
                                    zone.geolocation.getLatitude(),
                                    zone.geolocation.getLongitude()
                            );
                            
                            ArrayList<GeoPoint> waypoints = new ArrayList<>();
                            waypoints.add(currentUserLocation);
                            waypoints.add(zonePoint);
                            
                            Road road = roadManager.getRoad(waypoints);
                            
                            if (road != null && road.mLength > 0) {
                                zone.distanceToUser = road.mLength * 1000; // Convert km to meters
                            }
                            
                            // Add small delay to avoid overwhelming the API
                            Thread.sleep(200);
                            
                        } catch (Exception e) {
                            // Keep straight-line distance on error
                            Log.e("SafetyMapFragment", "Error calculating road distance for zone", e);
                        }
                    }
                }
                
                // Update UI after all calculations
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Collections.sort(allSafetyZones, Comparator.comparingDouble(z -> z.distanceToUser));
                        filterAndDisplay(currentFilterType);
                    });
                }
                
            } catch (Exception e) {
                Log.e("SafetyMapFragment", "Error in calculateRoadDistances", e);
            }
        }).start();
    }

    private void filterAndDisplay(String type) {
        currentFilterType = type;
        List<SafetyZone> filteredList = new ArrayList<>();
        if (type.equalsIgnoreCase("All")) {
            filteredList.addAll(allSafetyZones);
        } else {
            for (SafetyZone zone : allSafetyZones) {
                if (zone.type != null && zone.type.equalsIgnoreCase(type)) {
                    filteredList.add(zone);
                }
            }
        }
        adapter.setData(filteredList);
        adapter.setUserLocation(currentUserLocation);
    }

    private void updateMapPreviewMarkers() {
        if (mapPreview == null) return;

        // Clear existing markers, but keep the MyLocationNewOverlay
        List<Overlay> overlaysToKeep = new ArrayList<>();
        for (Overlay overlay : mapPreview.getOverlays()) {
            if (overlay instanceof MyLocationNewOverlay) {
                overlaysToKeep.add(overlay);
            }
        }
        mapPreview.getOverlays().clear();
        mapPreview.getOverlays().addAll(overlaysToKeep);

        // Add markers for all safety zones from the master list
        for (SafetyZone zone : allSafetyZones) {
            if (zone.geolocation != null) {
                Marker marker = new Marker(mapPreview);
                marker.setPosition(new GeoPoint(zone.geolocation.getLatitude(), zone.geolocation.getLongitude()));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                // You could set an icon here if you want
                // marker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_safety_pin));
                mapPreview.getOverlays().add(marker);
            }
        }
        mapPreview.invalidate(); // Redraw the map
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean locationGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) 
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                    break;
                }
            }
            
            if (locationGranted) {
                fetchCurrentLocationAndCenterPreview();
            } else {
                Toast.makeText(requireContext(), "Location permission is required to show your position", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapPreview != null) mapPreview.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapPreview != null) mapPreview.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    @Override
    public void onItemClick(SafetyZone zone) {
        Bundle args = new Bundle();
        args.putParcelable("safetyZone", zone);
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_map_to_safetyZoneDetailFragment, args);
    }
}
