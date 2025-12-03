package com.example.grpassignment;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class SafetyMapFragment extends Fragment {

    private MapView mapPreview;
    private FrameLayout mapPreviewContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // OSM Droid configuration
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_safety_map, container, false);

        mapPreviewContainer = view.findViewById(R.id.map_preview_container);

        // Create a map view programmatically for the preview
        mapPreview = new MapView(requireContext());

        // Disable all user interactions on the preview map
        mapPreview.setClickable(false);
        mapPreview.setMultiTouchControls(false);
        mapPreview.setFlingEnabled(false);
        mapPreview.setScrollableAreaLimitLatitude(MapView.getTileSystem().getMaxLatitude(), MapView.getTileSystem().getMinLatitude(), 0);
        mapPreview.setScrollableAreaLimitLongitude(MapView.getTileSystem().getMinLongitude(), MapView.getTileSystem().getMaxLongitude(), 0);

        // Add the preview to its container
        mapPreviewContainer.addView(mapPreview);

        // Set initial camera position for the preview
        mapPreview.getController().setZoom(14.0);
        mapPreview.getController().setCenter(new GeoPoint(3.1390, 101.6869)); // Default to KL



        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        CardView BtnCat = view.findViewById(R.id.filter_all);
        View.OnClickListener OCLCat = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).navigate(R.id.fullScreenMapFragment);
            }
        };
        BtnCat.setOnClickListener(OCLCat);
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
