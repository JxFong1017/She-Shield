package com.example.grpassignment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FullScreenMapFragment extends Fragment {

    private static final String TAG = "FullScreenMapFragment";
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private MyLocationNewOverlay myLocationOverlay;
    private Polyline routeLine; // Store the route line to remove it later
    private FloatingActionButton fabCloseRoute;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2;

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

        // Initialize FAB for closing route
        fabCloseRoute = view.findViewById(R.id.fab_close_route);
        fabCloseRoute.setOnClickListener(v -> clearRoute());

        // Enable location if permission is already granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableMyLocation();
        } else {
            // Request permission if not granted
            checkAndRequestPermissions();
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

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // Check location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            // Enable location if already granted
            if (myLocationOverlay != null) {
                myLocationOverlay.enableMyLocation();
            }
        }
        
        // Check storage permission for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // Android 12L and below
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        // Request all needed permissions at once
        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void addSafetyZoneMarkers() {
        if (FirebaseApp.getApps(requireContext()).isEmpty()) {
            Log.e(TAG, "Firebase was not initialized. Forcing initialization now.");
            FirebaseApp.initializeApp(requireContext());
        }

        Log.d(TAG, "Fetching safety zones from Firestore...");
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
                    Log.d(TAG, "Successfully loaded " + queryDocumentSnapshots.size() + " safety zones");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error: " + e.getMessage(), e);
                    if (isAdded()) {
                        String errorMsg = "Error loading safety zones";
                        if (e.getMessage() != null) {
                            if (e.getMessage().contains("PERMISSION_DENIED")) {
                                errorMsg = "Database permission denied. Please check Firestore security rules.";
                                Log.e(TAG, "FIRESTORE PERMISSION DENIED - Check Firebase Console security rules");
                            } else {
                                errorMsg = "Error: " + e.getMessage();
                            }
                        }
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showSafetyZoneDialog(DocumentSnapshot doc) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_safety_zone_detail, null);

        ImageView dialogImage = dialogView.findViewById(R.id.dialog_image);
        ImageView dialogCloseButton = dialogView.findViewById(R.id.dialog_close_button);
        TextView dialogName = dialogView.findViewById(R.id.dialog_name);
        TextView dialogDescription = dialogView.findViewById(R.id.dialog_description);
        TextView dialogPhone = dialogView.findViewById(R.id.dialog_phone);
        TextView dialogStatus = dialogView.findViewById(R.id.dialog_status);
        TextView dialogDistance = dialogView.findViewById(R.id.dialog_distance);
        Button directionButton = dialogView.findViewById(R.id.dialog_direction_button);
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

        // Get the safety zone location
        com.google.firebase.firestore.GeoPoint geoPoint = doc.getGeoPoint("geolocation");

        // Calculate and show route distance
        if (geoPoint != null && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            GeoPoint destination = new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());
            calculateRouteDistance(destination, dialogDistance);
        }

        // --- Create the dialog so we can dismiss it from the button ---
        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // --- Set the close button click listener ---
        dialogCloseButton.setOnClickListener(v -> dialog.dismiss());

        // --- Set the direction button click listener ---
        directionButton.setOnClickListener(v -> {
            if (geoPoint != null) {
                GeoPoint destination = new GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude());
                drawRouteToLocation(destination);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
            }
        });

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

    /**
     * Draws a route from current location to the specified destination
     */
    private void drawRouteToLocation(GeoPoint destination) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Calculating route...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                
                // Remove previous route if exists
                if (routeLine != null) {
                    map.getOverlays().remove(routeLine);
                }

                // Calculate route in background thread
                new Thread(() -> {
                    try {
                        // Create OSRM road manager for realistic routing
                        RoadManager roadManager = new OSRMRoadManager(requireContext(), "SheShield/1.0");
                        
                        // Create waypoints list
                        ArrayList<GeoPoint> waypoints = new ArrayList<>();
                        waypoints.add(currentLocation);
                        waypoints.add(destination);
                        
                        // Get the road (this is a network call, must be on background thread)
                        Road road = roadManager.getRoad(waypoints);
                        
                        // Update UI on main thread
                        requireActivity().runOnUiThread(() -> {
                            if (road != null && road.mRouteHigh != null && road.mRouteHigh.size() > 0) {
                                // Create polyline from the road
                                routeLine = RoadManager.buildRoadOverlay(road);
                                routeLine.setWidth(8f);
                                routeLine.setColor(0xFF6B21A8); // Purple color matching app theme
                                
                                // Add route to map
                                map.getOverlays().add(routeLine);
                                
                                // Zoom to show the entire route with the road's bounding box
                                if (road.mBoundingBox != null) {
                                    map.zoomToBoundingBox(road.mBoundingBox, true, 100);
                                } else {
                                    // Fallback: manual bounding box calculation
                                    double minLat = Math.min(currentLocation.getLatitude(), destination.getLatitude());
                                    double maxLat = Math.max(currentLocation.getLatitude(), destination.getLatitude());
                                    double minLon = Math.min(currentLocation.getLongitude(), destination.getLongitude());
                                    double maxLon = Math.max(currentLocation.getLongitude(), destination.getLongitude());
                                    
                                    double latPadding = (maxLat - minLat) * 0.3;
                                    double lonPadding = (maxLon - minLon) * 0.3;
                                    
                                    org.osmdroid.util.BoundingBox boundingBox = new org.osmdroid.util.BoundingBox(
                                        maxLat + latPadding, maxLon + lonPadding,
                                        minLat - latPadding, minLon - lonPadding
                                    );
                                    map.zoomToBoundingBox(boundingBox, true);
                                }
                                
                                map.invalidate();
                                
                                // Show FAB to allow closing the route
                                fabCloseRoute.show();
                                
                                // Show route distance and duration
                                String routeInfo = String.format("Route: %.1f km, ~%.0f min", 
                                    road.mLength, road.mDuration / 60);
                                Toast.makeText(getContext(), routeInfo, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), "Could not calculate route", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error calculating route", e);
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Error calculating route: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show()
                        );
                    }
                }).start();
                
            } else {
                Toast.makeText(getContext(), "Current location not available", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get current location", e);
            Toast.makeText(getContext(), "Failed to get location", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
                if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
                fetchCurrentLocationAndCenterMap();
            } else {
                Toast.makeText(requireContext(), "Location permission is required to show your position on the map", Toast.LENGTH_LONG).show();
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

    /**
     * Clears the route from the map and hides the FAB
     */
    private void clearRoute() {
        if (routeLine != null && map != null) {
            map.getOverlays().remove(routeLine);
            routeLine = null;
            map.invalidate();
            fabCloseRoute.hide();
            Toast.makeText(getContext(), "Route cleared", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Calculates route distance and updates the distance TextView
     */
    private void calculateRouteDistance(GeoPoint destination, TextView distanceTextView) {
        if (!isAdded() || getContext() == null || getActivity() == null) {
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && isAdded() && getActivity() != null) {
                GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                
                // Calculate route in background thread
                new Thread(() -> {
                    try {
                        Context context = getContext();
                        if (context == null) return;
                        
                        RoadManager roadManager = new OSRMRoadManager(context, "SheShield/1.0");
                        
                        ArrayList<GeoPoint> waypoints = new ArrayList<>();
                        waypoints.add(currentLocation);
                        waypoints.add(destination);
                        
                        Road road = roadManager.getRoad(waypoints);
                        
                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                if (road != null && road.mLength > 0 && isAdded()) {
                                    String distanceText = String.format("📍 Distance: %.1f km (~%.0f min)", 
                                        road.mLength, road.mDuration / 60);
                                    distanceTextView.setText(distanceText);
                                    distanceTextView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error calculating route distance", e);
                    }
                }).start();
            }
        });
    }
}
