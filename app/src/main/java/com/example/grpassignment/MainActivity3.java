package com.example.grpassignment;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Initialize map
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import android.preference.PreferenceManager;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

// Set start location to current location
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;


public class MainActivity3 extends AppCompatActivity {

    private MapView map;
    // Variable for the location client
    private FusedLocationProviderClient fusedLocationClient;
    // Constant for the permission request
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Location overlay
    private MyLocationNewOverlay myLocationOverlay;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);

        // 1. Essential osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // 2. Now, set the content view for the activity.
        setContentView(R.layout.safety_zone_map);

        // 3. Get a reference to the MapView
        map = findViewById(R.id.map);

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set map properties: Set the tile source (this is the map style)
        map.setTileSource(TileSourceFactory.MAPNIK);
        // 4. Set map properties
        map.setMultiTouchControls(true); // Allow pinch-to-zoom
        map.getController().setZoom(15.0); // Set initial zoom level

        // 5. Set the initial map center
        //GeoPoint startPoint = new GeoPoint(3.1390, 101.6869); // Example: Kuala Lumpur
        //map.getController().setCenter(startPoint);


        // Call the new method to get the current location
        fetchCurrentLocationAndCenterMap();


        // Create a marker
        Marker shelterMarker = new Marker(map);

        // Set its position
        shelterMarker.setPosition(new GeoPoint(3.1450, 101.7110)); // Example location

        // Set its title and description
        shelterMarker.setTitle("Women's Shelter & Support");
        shelterMarker.setSnippet("24/7 Support Available");

        // (Optional) Customize the icon
        // shelterMarker.setIcon(getResources().getDrawable(R.drawable.your_custom_icon));

        // Add the marker to the map
        map.getOverlays().add(shelterMarker);

        // This line sets up the blue dot overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        myLocationOverlay.enableMyLocation(); // This enables the overlay to start receiving location updates
        map.getOverlays().add(myLocationOverlay); // Add the overlay to the map
    }

    @Override
    public void onResume() {
        super.onResume();
        // This will refresh the map including overlays and my location
        if (map != null) {
            map.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // This will pause the map and all overlays
        if (map != null) {
            map.onPause();
        }
    }

    // Method to Fetch Location
    // If have user location permission, get the location.
    // If don't, ask the user for permission.
    private void fetchCurrentLocationAndCenterMap() {
        // 1. Check if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 2. Permission is already available, get the location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Location found, create a GeoPoint and center the map
                            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            map.getController().setCenter(startPoint);
                        } else {
                            // Location is null, fall back to a default location
                            GeoPoint startPoint = new GeoPoint(3.1390, 101.6869); // Kuala Lumpur
                            map.getController().setCenter(startPoint);
                        }
                    });
        } else {
            // 3. Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Handle the Permission Request Result
    // When the user accepts or denies the permission request, this method will be called.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, call the method again to get the location
                fetchCurrentLocationAndCenterMap();
            }
            // If permission is denied, the map will just stay at its default view.
            // You could add a toast or message here if you want.
        }
    }
}