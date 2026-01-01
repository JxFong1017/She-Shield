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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SafetyMapFragment extends Fragment implements SafetyZoneAdapter.OnItemClickListener {

    private MapView mapPreview;
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
        View view = inflater.inflate(R.layout.fragment_safety_map, container, false);

        mapPreviewContainer = view.findViewById(R.id.map_preview_container);
        mapPreview = new MapView(requireContext());
        mapPreview.setClickable(false);
        mapPreview.setMultiTouchControls(false);
        mapPreview.setFlingEnabled(false);
        mapPreview.getController().setZoom(15.0);
        mapPreviewContainer.addView(mapPreview);

        fetchCurrentLocationAndCenterPreview();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CardView mapCard = view.findViewById(R.id.map_preview_parent_container);
        mapCard.setOnClickListener(v -> {
            Bundle args = new Bundle();
            if (currentUserLocation != null) {
                args.putDouble("latitude", currentUserLocation.getLatitude());
                args.putDouble("longitude", currentUserLocation.getLongitude());
            }
            Navigation.findNavController(v).navigate(R.id.action_nav_map_to_fullScreenMapFragment, args);
        });

        recyclerView = view.findViewById(R.id.recyclerSafetyZones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass `this` fragment as the click listener to the adapter
        adapter = new SafetyZoneAdapter(this);
        recyclerView.setAdapter(adapter);

        loadDataFromFirestore();

        CardView filterAll = view.findViewById(R.id.filter_all);
        CardView filterShelters = view.findViewById(R.id.filter_shelters);
        CardView filterLegal = view.findViewById(R.id.filter_legal);
        CardView filterCounseling = view.findViewById(R.id.filter_counseling);

        filterAll.setOnClickListener(v -> filterAndDisplay("All"));
        filterShelters.setOnClickListener(v -> filterAndDisplay("Shelter"));
        filterLegal.setOnClickListener(v -> filterAndDisplay("Legal Aid"));
        filterCounseling.setOnClickListener(v -> filterAndDisplay("Counseling"));
    }

    private void loadDataFromFirestore() {
        FirebaseFirestore.getInstance().collection("safety_zones_pin").get()
                .addOnSuccessListener(query -> {
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
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchCurrentLocationAndCenterPreview() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

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

        for (SafetyZone zone : allSafetyZones) {
            if (zone.geolocation != null) {
                org.osmdroid.util.GeoPoint zonePoint = new org.osmdroid.util.GeoPoint(
                        zone.geolocation.getLatitude(),
                        zone.geolocation.getLongitude()
                );
                zone.distanceToUser = currentUserLocation.distanceToAsDouble(zonePoint);
            }
        }

        Collections.sort(allSafetyZones, Comparator.comparingDouble(z -> z.distanceToUser));
        filterAndDisplay(currentFilterType);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocationAndCenterPreview();
        }
    }

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

    // The click event from the adapter is handled here
    @Override
    public void onItemClick(SafetyZone zone) {
        Bundle args = new Bundle();
        args.putParcelable("safetyZone", zone);
        // Use Navigation Component to go to the detail fragment
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_map_to_safetyZoneDetailFragment, args);
    }
}
