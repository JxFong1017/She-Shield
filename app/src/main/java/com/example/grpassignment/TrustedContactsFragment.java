package com.example.grpassignment;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper; // Import for Swipe
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

// --- OSM IMPORTS ---
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

// --- TIMESTAMP IMPORTS ---
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrustedContactsFragment extends Fragment {

    private static final String TAG = "TrustedContactsFragment";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId = "U0001"; // Fallback default

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Elements
    private CardView bottomLocationCard; // The main bottom card container
    private CardView btnStartSharing;    // The pill button
    private TextView locationTitleText;
    private TextView locationSubtitleText;
    private TextView btnSharingText;
    private TextView btnAddContact;
    private ImageButton btnCloseMap;
    private CardView mapCard;
    private MapView mapOsm;
    private TextView mapStatusText; // The red "LIVE" label
    private Marker userMarker;
    private Marker friendMarker;

    // TIMESTAMP FORMATTER
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm:ss a", Locale.getDefault());
    private com.google.firebase.firestore.ListenerRegistration trackingListener;
    private RecyclerView recyclerView;
    private TrustedContactsAdapter adapter;
    private List<TrustedContact> contactList;
    private View emptyStateLayout; // View for "No Contacts"

    // Listens to LocationService
    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "LocationUpdate".equals(intent.getAction())) {
                // Safely check if fragment is still active
                if (!isAdded() || getContext() == null) return;

                double lat = intent.getDoubleExtra("lat", 0.0);
                double lng = intent.getDoubleExtra("lng", 0.0);
                long time = intent.getLongExtra("time", System.currentTimeMillis());

                updateMapLocation(lat, lng, time);

                // Update Adapter for "Share Location" button
                if (adapter != null) {
                    adapter.updateCurrentLocation(lat, lng);
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Initialize OSM Configuration
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        
        // Set cache path to internal storage to avoid permission issues
        File osmBasePath = new File(ctx.getFilesDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmBasePath);
        File osmTileCache = new File(osmBasePath, "tiles");
        Configuration.getInstance().setOsmdroidTileCache(osmTileCache);
        
        return inflater.inflate(R.layout.fragment_trusted_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Get authenticated user or use fallback
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Using authenticated user: " + currentUserId);
        } else {
            Log.w(TAG, "No authenticated user, using fallback: " + currentUserId);
            Toast.makeText(getContext(), "Using test user (not authenticated)", Toast.LENGTH_SHORT).show();
        }

        // Initialize Views
        bottomLocationCard = view.findViewById(R.id.card_location_status);
        locationTitleText = view.findViewById(R.id.text_location_title);
        locationSubtitleText = view.findViewById(R.id.text_location_subtitle);
        btnStartSharing = view.findViewById(R.id.btn_start_sharing);
        btnSharingText = view.findViewById(R.id.text_btn_sharing);

        // MAP INITIALIZATION
        mapCard = view.findViewById(R.id.card_active_map);
        mapOsm = view.findViewById(R.id.map_osm);
        mapStatusText = view.findViewById(R.id.text_map_status);

        // Configure "Lite Mode"
        if (mapOsm != null) {
            mapOsm.setMultiTouchControls(false); // Disable zoom gestures so page scrolls smoothly
            mapOsm.getController().setZoom(16.0); // Set default close zoom
        }

        btnAddContact = view.findViewById(R.id.btn_add_contact);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state); // Ensure this ID is in XML

        recyclerView = view.findViewById(R.id.recycler_view_contacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactList = new ArrayList<>();

        // --- UPDATED ADAPTER INIT: Pass the click listener ---
        adapter = new TrustedContactsAdapter(contactList, this::onContactClicked);
        recyclerView.setAdapter(adapter);

        // Add Move/Delete Logic
        setupGestures();

        // Listeners
        btnAddContact.setOnClickListener(v -> showAddContactDialog());
        btnStartSharing.setOnClickListener(v -> toggleLocationService());
        btnCloseMap = view.findViewById(R.id.btn_close_map);
        btnCloseMap.setOnClickListener(v -> stopTracking());

        // Check if service is already running (to keep UI in sync)
        if (isServiceRunning()) {
            updateUIState(true);
        } else {
            updateUIState(false);
        }

        fetchContacts();
    }

    // NEW: HANDLE CONTACT CLICK
    public void onContactClicked(TrustedContact contact) {
        if (contact.getLinkedUserId() != null && !contact.getLinkedUserId().isEmpty()) {
            // helper to track id
            trackFriendById(contact.getLinkedUserId(), contact.getName());
        } else {
            Toast.makeText(getContext(), contact.getName() + " does not have the app installed.", Toast.LENGTH_SHORT).show();
        }
    }

    // TRACKING LOGIC
    private void trackFriendById(String friendId, String friendName) {
        // Stop any previous tracking to avoid duplicates
        if (trackingListener != null) {
            trackingListener.remove();
        }

        Toast.makeText(getContext(), "Searching for signal: " + friendName, Toast.LENGTH_SHORT).show();

        // SNAPSHOT LISTENER
        trackingListener = db.collection("user").document(friendId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Error tracking user", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Double lat = documentSnapshot.getDouble("latitude");
                        Double lng = documentSnapshot.getDouble("longitude");
                        Boolean isSharing = documentSnapshot.getBoolean("isSharing");
                        Long timestamp = documentSnapshot.getLong("lastUpdated");

                        if (Boolean.TRUE.equals(isSharing) && lat != null && lng != null) {
                            // UPDATE THE MAP LIVE
                            showFriendOnMap(lat, lng, friendName, timestamp != null ? timestamp : 0);
                        } else {
                            // STOP SHARING

                            // Remove the marker from the map
                            if (friendMarker != null && mapOsm != null) {
                                mapOsm.getOverlays().remove(friendMarker);
                                friendMarker = null; // Clear the variable
                                mapOsm.invalidate(); // Refresh the map view
                            }

                            // Update the status text
                            if (mapStatusText != null) {
                                mapStatusText.setText(friendName + " stops sharing location");
                                mapStatusText.setBackgroundColor(Color.parseColor("#6B7280")); // Set to Gray
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "User offline or not found.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showFriendOnMap(double lat, double lng, String name, long timestamp) {
        mapCard.setVisibility(View.VISIBLE);

        // Format the time
        String timeString = "Offline";
        if (timestamp > 0) {
            timeString = timeFormatter.format(new Date(timestamp));
        }

        // show WHO and WHEN
        mapStatusText.setText("TRACKING: " + name + " • " + timeString);
        mapStatusText.setBackgroundColor(Color.parseColor("#7C3AED")); // Purple

        if (mapOsm != null) {
            GeoPoint point = new GeoPoint(lat, lng);
            mapOsm.getController().animateTo(point);
            mapOsm.getController().setZoom(16.0);

            if (friendMarker == null) {
                friendMarker = new Marker(mapOsm);
                friendMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapOsm.getOverlays().add(friendMarker);
            }
            friendMarker.setTitle(name + "\nLast seen: " + timeString); // Tooltip on click
            friendMarker.setPosition(point);
            mapOsm.invalidate();
        }
    }

    // UPDATE OSM MAP POSITION (YOUR OWN)
    private void updateMapLocation(double lat, double lng, long time) {
        if (mapOsm == null) return;

        // Update Timestamp Label
        if (mapStatusText != null) {
            String timeString = timeFormatter.format(new Date(time));
            mapStatusText.setText("LIVE • UPDATED " + timeString.toUpperCase());
            mapStatusText.setBackgroundColor(Color.parseColor("#D32F2F")); // Red for you
        }

        GeoPoint point = new GeoPoint(lat, lng);

        // Center the map on the user
        mapOsm.getController().animateTo(point);

        // Create or Move the Marker
        if (userMarker == null) {
            userMarker = new Marker(mapOsm);
            userMarker.setTitle("You are here");
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapOsm.getOverlays().add(userMarker);
        }
        userMarker.setPosition(point);
        mapOsm.invalidate(); // Refresh the map view
    }
    private void stopTracking() {
        // Detach the listener
        if (trackingListener != null) {
            trackingListener.remove();
            trackingListener = null;
        }

        // Clear the friend marker
        if (mapOsm != null && friendMarker != null) {
            mapOsm.getOverlays().remove(friendMarker);
            friendMarker = null;
            mapOsm.invalidate(); // Refresh map
        }

        // Reset UI text
        mapStatusText.setText("");

        // Hide the map card
        mapCard.setVisibility(View.GONE);

        Toast.makeText(getContext(), "Stopped tracking.", Toast.LENGTH_SHORT).show();
    }

    // --- SERVICE & LIFECYCLE METHODS ---
    private void toggleLocationService() {
        if (!isServiceRunning()) {
            if (checkPermissions()) {
                startService();
            } else {
                requestPermissions();
            }
        } else {
            stopService();
        }
    }

    private void startService() {
        Intent serviceIntent = new Intent(getContext(), LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
        updateUIState(true);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(getContext(), LocationService.class);
        serviceIntent.setAction("STOP");
        requireContext().startService(serviceIntent);
        updateUIState(false);
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapOsm != null) mapOsm.onResume();

        // Listen for service updates
        if (getActivity() != null) {
            IntentFilter filter = new IntentFilter("LocationUpdate");

            // Use ContextCompat to handle Android 13/14 security flags automatically
            ContextCompat.registerReceiver(
                    getContext(),
                    locationReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED // Requires import androidx.core.content.ContextCompat
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapOsm != null) mapOsm.onPause();

        // Stop listening
        if (getActivity() != null) {
            try {
                // Unregister using the context you registered with
                getActivity().unregisterReceiver(locationReceiver);
            } catch (IllegalArgumentException e) {
                // Ignore if not registered
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop tracking friend
        if (trackingListener != null) {
            trackingListener.remove();
            trackingListener = null;
        }
    }

    // UI COLOR SWAP LOGIC
    private void updateUIState(boolean isSharing) {
        if (isSharing) {
            // ALERT MODE
            bottomLocationCard.setCardBackgroundColor(Color.parseColor("#FEF2F2")); // Light Red BG
            btnStartSharing.setCardBackgroundColor(Color.parseColor("#DC2626")); // Dark Red Button
            btnSharingText.setText("STOP SHARING");
            locationTitleText.setText("Sharing Live Location");
            locationTitleText.setTextColor(Color.parseColor("#991B1B"));
            locationSubtitleText.setText("Your contacts can see where you are");
            locationSubtitleText.setTextColor(Color.parseColor("#B91C1C"));

            // SHOW THE MAP
            mapCard.setVisibility(View.VISIBLE);
        } else {
            // SAFE MODE
            bottomLocationCard.setCardBackgroundColor(Color.parseColor("#FFFFFF")); // White BG
            btnStartSharing.setCardBackgroundColor(Color.parseColor("#10B981")); // Emerald Green Button
            btnSharingText.setText("Start Sharing");
            locationTitleText.setText("Location Sharing");
            locationTitleText.setTextColor(Color.parseColor("#1F2937"));
            locationSubtitleText.setText("Tap to start sharing");
            locationSubtitleText.setTextColor(Color.parseColor("#6B7280"));

            // HIDE THE MAP
            mapCard.setVisibility(View.GONE);
        }
    }

    // --- GESTURES & RECYCLERVIEW ---
    private void setupGestures() {
        //  UP | DOWN (Enables Dragging)
        //  LEFT (Enables Swiping)
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = source.getAdapterPosition();
                int toPos = target.getAdapterPosition();

                // Swap visually
                Collections.swap(contactList, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            // This method runs when you LET GO of the item
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // Save the new order to Firebase!
                saveNewOrderToFirebase();
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TrustedContact contactToDelete = contactList.get(position);

                // Show confirmation dialog
                new AlertDialog.Builder(getContext())
                        .setTitle("Delete Contact")
                        .setMessage("Are you sure you want to remove " + contactToDelete.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteContactFromFirebase(contactToDelete);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // Undo the swipe
                        })
                        .setCancelable(false)
                        .show();
            }
        });

        // Attach it to RecyclerView
        touchHelper.attachToRecyclerView(recyclerView);
    }

    // Helper method for update rank
    private void saveNewOrderToFirebase() {
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        for (int i = 0; i < contactList.size(); i++) {
            TrustedContact contact = contactList.get(i);
            contact.setRank(i); // Update local

            // Prepare the update in the batch
            com.google.firebase.firestore.DocumentReference ref = db
                    .collection("user")
                    .document(currentUserId)
                    .collection("trusted_contacts")
                    .document(contact.getId());

            batch.update(ref, "rank", i);
        }

        batch.commit();
    }

    // Helper method for delete
    private void deleteContactFromFirebase(TrustedContact contact) {
        db.collection("user").document(currentUserId)
                .collection("trusted_contacts")
                .document(contact.getId()) // Use the specific Document ID
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Contact removed", Toast.LENGTH_SHORT).show();
                    // No need to fetchContacts() again if you just remove it from the list locally
                    contactList.remove(contact);
                    adapter.notifyDataSetChanged();

                    // OPTIONAL: Re-rank the remaining items so there are no gaps
                    saveNewOrderToFirebase();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged(); // Restore the item in UI on failure
                });
    }

    // DATA FETCHING
    private void fetchContacts() {
        Log.d(TAG, "Fetching contacts for user: " + currentUserId);
        
        db.collection("user").document(currentUserId).collection("trusted_contacts")
                .orderBy("rank").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    
                    Log.d(TAG, "Successfully fetched " + queryDocumentSnapshots.size() + " contacts");
                    contactList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            TrustedContact contact = document.toObject(TrustedContact.class);
                            contact.setId(document.getId());
                            contactList.add(contact);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing contact: " + e.getMessage(), e);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Toggle Empty State
                    if (contactList.isEmpty()) {
                        if(emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        if(emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    
                    Log.e(TAG, "Firestore error: " + e.getMessage(), e);
                    String errorMsg = "Error loading contacts";
                    
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("PERMISSION_DENIED")) {
                            errorMsg = "Database permission denied. Please check Firestore security rules for 'user/" + currentUserId + "/trusted_contacts'";
                            Log.e(TAG, "FIRESTORE PERMISSION DENIED - Check Firebase Console security rules");
                        } else {
                            errorMsg = "Error: " + e.getMessage();
                        }
                    }
                    
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    
                    // Show empty state on error
                    if(emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Trusted Contact");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10); // Added slightly more padding

        final EditText inputName = new EditText(getContext());
        inputName.setHint("Name ");
        layout.addView(inputName);

        final EditText inputPhone = new EditText(getContext());
        inputPhone.setHint("Phone (e.g., 0123456789)");
        inputPhone.setInputType(InputType.TYPE_CLASS_PHONE); // Forces number pad
        layout.addView(inputPhone);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String rawPhone = inputPhone.getText().toString().trim();

            // Format the number
            String formattedPhone = formatPhoneNumber(rawPhone);

            // Validate
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if phone is valid
            if (formattedPhone.length() < 10) {
                Toast.makeText(getContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save the CLEAN version
            saveContactToFirebase(name, formattedPhone);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- SAVE CONTACT ---
    private void saveContactToFirebase(String name, String phone) {
        // Search if this phone number exists in the "user" collection
        db.collection("user")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    String foundUserId = null;

                    // If found a registered user with phone number
                    if (!querySnapshot.isEmpty()) {
                        foundUserId = querySnapshot.getDocuments().get(0).getId();
                        Toast.makeText(getContext(), "Linked to registered user!", Toast.LENGTH_SHORT).show();
                    }

                    // Create the contact object
                    int newRank = contactList.size();

                    TrustedContact newContact = new TrustedContact(name, phone, newRank, foundUserId);

                    // Save to trusted_contacts
                    db.collection("user").document(currentUserId).collection("trusted_contacts")
                            .add(newContact)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(getContext(), "Contact Added!", Toast.LENGTH_SHORT).show();
                                fetchContacts();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add contact", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> {
                    // Fallback if search fails, save without ID
                    int newRank = contactList.size();
                    TrustedContact newContact = new TrustedContact(name, phone, newRank, null);

                    db.collection("user").document(currentUserId).collection("trusted_contacts")
                            .add(newContact)
                            .addOnSuccessListener(doc -> fetchContacts());
                });
    }

    // --- PERMISSIONS ---
    private boolean checkPermissions() {
        boolean location = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean notification = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notification = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return location && notification;
    }

    private void requestPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        requestPermissions(perms.toArray(new String[0]), LOCATION_PERMISSION_REQUEST_CODE);
    }

    // FORMAT PHONE NUMBER
    private String formatPhoneNumber(String input) {
        // Remove all spaces, dashes, and parentheses
        String cleaned = input.replaceAll("[\\s\\-\\(\\)]", "");

        // check if it is empty
        if (cleaned.isEmpty()) return "";

        // If it starts with '0', replace with Country Code
        if (cleaned.startsWith("0")) {
            return "+60" + cleaned.substring(1);
        }

        // add +
        if (!cleaned.startsWith("+")) {
            return "+" + cleaned;
        }

        return cleaned;
    }
}