package com.example.grpassignment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ReportFragment extends Fragment {

    private MapView mapPreview;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Marker selectedLocationMarker;
    private Button btnConfirmReport;
    private ImageButton btnZoomIn, btnZoomOut;

    // Report List
    private RecyclerView recyclerView;
    private ReportAdapter reportAdapter;
    private final List<Report> reportList = new ArrayList<>(); // This list will be displayed
    private final List<Report> allReports = new ArrayList<>(); // Master list from Firestore

    // Filters
    private CardView filterAll, filterNearest, filterLatest;
    private String currentFilter = "ALL";
    private static final double NEARBY_RADIUS_METERS = 10000; // 10km

    private FirebaseFirestore db;

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
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        // Initialize Views
        btnConfirmReport = view.findViewById(R.id.btn_confirm_report);
        mapPreview = view.findViewById(R.id.map_view);
        btnZoomIn = view.findViewById(R.id.btn_zoom_in);
        btnZoomOut = view.findViewById(R.id.btn_zoom_out);
        recyclerView = view.findViewById(R.id.recycler_reports);
        filterAll = view.findViewById(R.id.filter_all);
        filterNearest = view.findViewById(R.id.filter_shelters);
        filterLatest = view.findViewById(R.id.filter_legal);

        // Setup components
        setupMap();
        setupZoomControls();
        setupRecyclerView();
        setupFilterListeners();
        loadReportsFromFirestore();

        btnConfirmReport.setOnClickListener(v -> {
            if (selectedLocationMarker != null) {
                Bundle args = new Bundle();
                args.putFloat("latitude", (float)selectedLocationMarker.getPosition().getLatitude());
                args.putFloat("longitude", (float)selectedLocationMarker.getPosition().getLongitude());
                Navigation.findNavController(v).navigate(R.id.action_nav_report_to_postReport, args);
            }
        });

        return view;
    }

    private void setupFilterListeners() {
        filterAll.setOnClickListener(v -> setFilter("ALL"));
        filterLatest.setOnClickListener(v -> setFilter("LATEST"));
        filterNearest.setOnClickListener(v -> setFilter("NEAREST"));
        updateFilterUI(); // Set initial state
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterUI();
        applyFilters();
    }

    private void updateFilterUI() {
        // Reset all backgrounds
        filterAll.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        filterLatest.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        filterNearest.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        // Highlight the selected one
        switch (currentFilter) {
            case "ALL":
                filterAll.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.filter_selected_bg));
                break;
            case "LATEST":
                filterLatest.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.filter_selected_bg));
                break;
            case "NEAREST":
                filterNearest.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.filter_selected_bg));
                break;
        }
    }

    private void applyFilters() {
        List<Report> filteredList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        switch (currentFilter) {
            case "LATEST": // Last 7 days
                cal.add(Calendar.DAY_OF_YEAR, -7);
                Date sevenDaysAgo = cal.getTime();
                for (Report report : allReports) {
                    if (report.getTimestamp() != null && report.getTimestamp().after(sevenDaysAgo)) {
                        filteredList.add(report);
                    }
                }
                break;

            case "NEAREST":
                applyNearestFilter(); // This is async and will update the adapter itself
                return;

            case "ALL":
            default: // Default to showing last 30 days
                cal.add(Calendar.DAY_OF_YEAR, -30);
                Date thirtyDaysAgo = cal.getTime();
                for (Report report : allReports) {
                    if (report.getTimestamp() != null && report.getTimestamp().after(thirtyDaysAgo)) {
                        filteredList.add(report);
                    }
                }
                break;
        }

        reportList.clear();
        reportList.addAll(filteredList);
        reportAdapter.notifyDataSetChanged();
    }

    private void applyNearestFilter() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission is needed to find nearby reports", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            setFilter("ALL"); // Revert to a non-location-based filter
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(getContext(), "Could not get current location. Showing all reports.", Toast.LENGTH_SHORT).show();
                setFilter("ALL");
                return;
            }

            List<Report> nearestReports = new ArrayList<>();
            for (Report report : allReports) {
                try {
                    String[] latLng = report.getLocation().split(",");
                    double reportLat = Double.parseDouble(latLng[0]);
                    double reportLng = Double.parseDouble(latLng[1]);

                    float[] results = new float[1];
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), reportLat, reportLng, results);

                    if (results[0] < NEARBY_RADIUS_METERS) {
                        nearestReports.add(report);
                    }
                } catch (Exception e) {
                    Log.e("ReportFragment", "Could not parse report location: " + report.getLocation());
                }
            }

            reportList.clear();
            reportList.addAll(nearestReports);
            reportAdapter.notifyDataSetChanged();
        });
    }

    private void loadReportsFromFirestore() {
        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("ReportFragment", "Listen failed.", e);
                        return;
                    }

                    allReports.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Report report = doc.toObject(Report.class);
                            allReports.add(report);
                        }
                    }
                    applyFilters(); // Apply the current (default) filter
                });
    }

    private void setupRecyclerView() {
        reportAdapter = new ReportAdapter(getContext(), reportList);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(reportAdapter);
    }

    private void setupMap() {
        if (mapPreview != null) {
            mapPreview.setMultiTouchControls(true);
            mapPreview.setBuiltInZoomControls(false);
            mapPreview.getController().setZoom(15.0);
            mapPreview.getController().setCenter(new GeoPoint(3.1390, 101.6869));
        }
        enableMyLocation();

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                updateSelectedLocation(p);
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };
        mapPreview.getOverlays().add(new MapEventsOverlay(mReceive));
    }

    private void setupZoomControls() {
        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(v -> {
                if (mapPreview != null) mapPreview.getController().zoomIn();
            });
        }
        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(v -> {
                if (mapPreview != null) mapPreview.getController().zoomOut();
            });
        }
    }

    private void updateSelectedLocation(GeoPoint p) {
        if (selectedLocationMarker != null) mapPreview.getOverlays().remove(selectedLocationMarker);
        selectedLocationMarker = new Marker(mapPreview);
        selectedLocationMarker.setPosition(p);
        selectedLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedLocationMarker.setTitle("Selected Location");
        mapPreview.getOverlays().add(selectedLocationMarker);
        btnConfirmReport.setVisibility(View.VISIBLE);
        mapPreview.invalidate();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapPreview);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
            mapPreview.getOverlays().add(myLocationOverlay);

            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    GeoPoint currentGeo = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapPreview.getController().setCenter(currentGeo);
                }
            });
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if ("NEAREST".equals(currentFilter)) {
                    applyNearestFilter();
                }
            }
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
}
