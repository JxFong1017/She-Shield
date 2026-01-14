package com.example.grpassignment;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class PostReportFragment extends Fragment {

    // UI Components
    private Spinner spinnerType;
    private EditText editDate, editTime, editDescription;
    private ImageView btnDate, btnTime, imageUploadPreview;
    private Button btnSubmit;
    private MapView mapPreview;
    private CardView btnUploadEvidence;
    private LinearLayout placeholderUploadLayout;

    // Severity Level Cards
    private CardView cardLow, cardMedium, cardHigh;
    private String selectedSeverity = "Low"; // Default

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint selectedLocation;
    private Marker selectedMarker;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // Media Upload
    private static final int PICK_MEDIA_REQUEST_CODE = 200;
    private Uri selectedMediaUri;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // OSM Droid configuration
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        
        // Set cache path to internal storage to avoid permission issues
        File osmBasePath = new File(ctx.getFilesDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmBasePath);
        File osmTileCache = new File(osmBasePath, "tiles");
        Configuration.getInstance().setOsmdroidTileCache(osmTileCache);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // --- Initialize Views ---
        spinnerType = view.findViewById(R.id.spinner_incident_type);
        editDate = view.findViewById(R.id.edit_date);
        editTime = view.findViewById(R.id.edit_time);
        editDescription = view.findViewById(R.id.edit_description);
        btnDate = view.findViewById(R.id.icon_date_picker);
        btnTime = view.findViewById(R.id.icon_time_picker);
        btnSubmit = view.findViewById(R.id.btn_submit_report);
        mapPreview = view.findViewById(R.id.map_preview);
        btnUploadEvidence = view.findViewById(R.id.btn_upload_evidence);
        imageUploadPreview = view.findViewById(R.id.image_upload_preview);
        placeholderUploadLayout = view.findViewById(R.id.placeholder_upload_layout);

        cardLow = view.findViewById(R.id.severity_low);
        cardMedium = view.findViewById(R.id.severity_medium);
        cardHigh = view.findViewById(R.id.severity_high);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Set Current Date and Time
        setCurrentDateTime();

        // Handle incoming location from arguments
        handleArguments();

        // --- Initialize Map ---
        setupMap();

        // --- Setup Date Picker ---
        View.OnClickListener dateClickListener = v -> showDatePicker();
        editDate.setOnClickListener(dateClickListener);
        btnDate.setOnClickListener(dateClickListener);

        // --- Setup Time Picker ---
        View.OnClickListener timeClickListener = v -> showTimePicker();
        editTime.setOnClickListener(timeClickListener);
        btnTime.setOnClickListener(timeClickListener);

        // --- Setup Upload Evidence ---
        btnUploadEvidence.setOnClickListener(v -> openMediaPicker());

        // --- Setup Severity Selection ---
        setupSeveritySelection();

        // --- Setup Submit Button ---
        btnSubmit.setOnClickListener(v -> submitReportFlow());
    }

    private void handleArguments() {
        if (getArguments() != null) {
            if (getArguments().containsKey("latitude") && getArguments().containsKey("longitude")) {
                float lat = getArguments().getFloat("latitude", 0f);
                float lon = getArguments().getFloat("longitude", 0f);
                selectedLocation = new GeoPoint((double)lat, (double)lon);
            }
        }
    }

    private void setupSeveritySelection() {
        View.OnClickListener listener = v -> {
            // Reset all cards first
            resetSeverityCards();

            // Highlight selected card
            if (v.getId() == R.id.severity_low) {
                selectedSeverity = "Low";
                cardLow.setAlpha(1.0f);
                cardLow.setCardElevation(8f);
            } else if (v.getId() == R.id.severity_medium) {
                selectedSeverity = "Medium";
                cardMedium.setAlpha(1.0f);
                cardMedium.setCardElevation(8f);
            } else if (v.getId() == R.id.severity_high) {
                selectedSeverity = "High";
                cardHigh.setAlpha(1.0f);
                cardHigh.setCardElevation(8f);
            }
        };

        cardLow.setOnClickListener(listener);
        cardMedium.setOnClickListener(listener);
        cardHigh.setOnClickListener(listener);

        // Initial State: Select Low by default visually
        resetSeverityCards();
        cardLow.setAlpha(1.0f);
        cardLow.setCardElevation(8f);
    }

    private void resetSeverityCards() {
        // Dim unselected cards slightly and reduce elevation
        cardLow.setAlpha(0.5f);
        cardLow.setCardElevation(2f);

        cardMedium.setAlpha(0.5f);
        cardMedium.setCardElevation(2f);

        cardHigh.setAlpha(0.5f);
        cardHigh.setCardElevation(2f);
    }

    private void setCurrentDateTime() {
        final Calendar c = Calendar.getInstance();

        // Set Date
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        editDate.setText(String.format(Locale.getDefault(), "%d/%d/%d", day, month + 1, year));

        // Set Time
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        String amPm = (hour >= 12) ? "PM" : "AM";
        int currentHour = (hour > 12) ? (hour - 12) : hour;
        if (currentHour == 0) currentHour = 12;

        String minuteString = (minute < 10) ? "0" + minute : String.valueOf(minute);
        editTime.setText(String.format(Locale.getDefault(), "%d:%s %s", currentHour, minuteString, amPm));
    }

    private void setupMap() {
        mapPreview.setMultiTouchControls(true);
        mapPreview.getController().setZoom(15.0);

        // Add user location overlay
        MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapPreview);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapPreview.getOverlays().add(myLocationOverlay);

        // Handle Map Taps for Manual Selection
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                updateSelectedLocation(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        MapEventsOverlay OverlayEvents = new MapEventsOverlay(mReceive);
        mapPreview.getOverlays().add(OverlayEvents);

        // Check if we have a pre-selected location from arguments
        if (selectedLocation != null) {
            // Use the passed location
            mapPreview.getController().setCenter(selectedLocation);
            updateSelectedLocation(selectedLocation);
        } else {
            // Otherwise fetch current location
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                GeoPoint currentGeo = new GeoPoint(location.getLatitude(), location.getLongitude());

                // Only center if we haven't already selected a location manually or via arguments
                if (selectedLocation == null) {
                    mapPreview.getController().setCenter(currentGeo);
                }
            } else {
                // Default to KL
                if (selectedLocation == null) {
                    GeoPoint kl = new GeoPoint(3.1390, 101.6869);
                    mapPreview.getController().setCenter(kl);
                }
            }
        });
    }

    private void updateSelectedLocation(GeoPoint p) {
        selectedLocation = p;

        // Remove previous marker
        if (selectedMarker != null) {
            mapPreview.getOverlays().remove(selectedMarker);
        }

        // Add new marker
        selectedMarker = new Marker(mapPreview);
        selectedMarker.setPosition(p);
        selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedMarker.setTitle("Selected Location");
        mapPreview.getOverlays().add(selectedMarker);
        mapPreview.invalidate(); // Refresh map
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (selectedLocation == null) {
                getCurrentLocation();
            }
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    editDate.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute1) -> {
                    String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                    int currentHour = (hourOfDay > 12) ? (hourOfDay - 12) : hourOfDay;
                    if (currentHour == 0) currentHour = 12;

                    String minuteString = (minute1 < 10) ? "0" + minute1 : String.valueOf(minute1);
                    editTime.setText(currentHour + ":" + minuteString + " " + amPm);
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private void openMediaPicker() {
        // Use ACTION_OPEN_DOCUMENT for modern, reliable file access
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, PICK_MEDIA_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_MEDIA_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedMediaUri = data.getData();

            // Take persistable URI permission
            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                requireContext().getContentResolver().takePersistableUriPermission(selectedMediaUri, takeFlags);
            } catch (SecurityException e) {
                Log.e("PostReportFragment", "Failed to take persistable permission for URI", e);
            }

            // Show the preview and hide the placeholder
            imageUploadPreview.setImageURI(selectedMediaUri);
            imageUploadPreview.setVisibility(View.VISIBLE);
            placeholderUploadLayout.setVisibility(View.GONE);
        }
    }

    private void submitReportFlow() {
        if (selectedMediaUri != null) {
            // If there is media, upload it first
            uploadMediaAndSaveReport();
        } else {
            // Otherwise, save the report directly
            saveReportToFirestore(null);
        }
    }

    private void uploadMediaAndSaveReport() {
        // Create a unique filename
        String filename = UUID.randomUUID().toString();
        StorageReference storageRef = storage.getReference().child("reports/" + filename);

        Toast.makeText(requireContext(), "Uploading media...", Toast.LENGTH_SHORT).show();

        storageRef.putFile(selectedMediaUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    saveReportToFirestore(downloadUrl);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Media upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveReportToFirestore(@Nullable String mediaUrl) {
        String type = spinnerType.getSelectedItem().toString();
        String date = editDate.getText().toString();
        String time = editTime.getText().toString();
        String desc = editDescription.getText().toString();

        if (date.isEmpty() || time.isEmpty() || desc.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in Date, Time, and Description", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLocation == null) {
            Toast.makeText(requireContext(), "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        String locationString = selectedLocation.getLatitude() + "," + selectedLocation.getLongitude();

        // Create Report object
        Report report = new Report();
        report.setType(type);
        report.setDate(date);
        report.setTime(time);
        report.setDescription(desc);
        report.setLocation(locationString);
        report.setSeverity(selectedSeverity);
        report.setAnonymous(true);
        report.setMediaUri(mediaUrl);

        // Assign a random dummy user ID
        List<String> dummyUserIds = Arrays.asList("user-alpha-111", "user-beta-222", "user-gamma-333", "user-delta-444");
        String randomUserId = dummyUserIds.get(new Random().nextInt(dummyUserIds.size()));
        report.setUserId(randomUserId);

        // Save to Firestore
        db.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error submitting report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
