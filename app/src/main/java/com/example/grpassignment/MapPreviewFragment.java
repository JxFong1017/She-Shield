package com.example.grpassignment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

public class MapPreviewFragment extends Fragment {

    private MapView mapPreview;
    private FusedLocationProviderClient fusedLocationClient;

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
        mapPreview = new MapView(requireContext());
        mapPreview.setTileSource(TileSourceFactory.MAPNIK);
        mapPreview.setClickable(false);
        mapPreview.setMultiTouchControls(false);
        mapPreview.setFlingEnabled(false);
        mapPreview.getController().setZoom(15.0);

        fetchCurrentLocationAndCenterPreview();

        return mapPreview;
    }

    private void fetchCurrentLocationAndCenterPreview() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Default to a central location if permission is not granted
            GeoPoint defaultLocation = new GeoPoint(3.1390, 101.6869); // Kuala Lumpur
            mapPreview.getController().setCenter(defaultLocation);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            GeoPoint geoPoint;
            if (location != null) {
                geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            } else {
                geoPoint = new GeoPoint(3.1390, 101.6869); // Default to Kuala Lumpur
            }
            if (mapPreview != null) {
                mapPreview.getController().setCenter(geoPoint);
            }
        });
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