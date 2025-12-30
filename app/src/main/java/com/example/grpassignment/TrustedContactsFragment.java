package com.example.grpassignment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper; // Import for Swipe
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrustedContactsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;

    // Location Components
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Elements
    private CardView bottomLocationCard; // The main bottom card container
    private CardView btnStartSharing;    // The pill button
    private TextView locationTitleText;
    private TextView locationSubtitleText;
    private TextView btnSharingText;
    private TextView btnAddContact;

    private boolean isSharing = false;

    private RecyclerView recyclerView;
    private TrustedContactsAdapter adapter;
    private List<TrustedContact> contactList;
    private View emptyStateLayout; // View for "No Contacts"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trusted_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        //currentUserId = "U0001"; // Temporary ID
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            Toast.makeText(getContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }


        // Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Define what happens when we get a new location update
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;

                for (android.location.Location location : locationResult.getLocations()) {
                    // 1. Update Firebase
                    updateLocationInFirebase(location.getLatitude(), location.getLongitude());

                    // 2. Update Adapter
                    if (adapter != null) {
                        adapter.updateCurrentLocation(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };

        // Initialize Views (Updated to match Sticky Footer XML)
        bottomLocationCard = view.findViewById(R.id.card_location_status);
        locationTitleText = view.findViewById(R.id.text_location_title);
        locationSubtitleText = view.findViewById(R.id.text_location_subtitle);
        btnStartSharing = view.findViewById(R.id.btn_start_sharing);
        btnSharingText = view.findViewById(R.id.text_btn_sharing);

        btnAddContact = view.findViewById(R.id.btn_add_contact);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state); // Ensure this ID is in XML

        recyclerView = view.findViewById(R.id.recycler_view_contacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactList = new ArrayList<>();
        adapter = new TrustedContactsAdapter(contactList);
        recyclerView.setAdapter(adapter);

        // Add Move/Delete Logic
        setupGestures();

        // Listeners
        setupLocationSharing();
        btnAddContact.setOnClickListener(v -> showAddContactDialog());

        fetchContacts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop location updates when the user leaves this screen
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        // sharing to stop automatically when they leave the screen
        if (isSharing) {
            stopLocationUpdates();
        }
    }

    private void setupLocationSharing() {
        btnStartSharing.setOnClickListener(v -> {
            if (!isSharing) {
                // User wants to START sharing
                if (checkPermission()) {
                    startLocationUpdates();
                } else {
                    requestPermission();
                }
            } else {
                // User wants to STOP sharing
                stopLocationUpdates();
            }
        });
    }
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

                // Show Confirmation Dialog
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

        // Attach it to your RecyclerView
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

        // Commit all changes at once
        batch.commit().addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to save order", Toast.LENGTH_SHORT).show()
        );
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

    // --- LOCATION LOGIC ---
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        isSharing = true;
        updateUIState(true);
        Toast.makeText(getContext(), "Location Sharing Started", Toast.LENGTH_SHORT).show();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);

        Map<String, Object> update = new HashMap<>();
        update.put("isSharing", false);
        db.collection("user").document(currentUserId).update(update);

        isSharing = false;
        updateUIState(false);
        Toast.makeText(getContext(), "Location Sharing Stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateLocationInFirebase(double lat, double lng) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", lat);
        locationData.put("longitude", lng);
        locationData.put("lastUpdated", System.currentTimeMillis());
        locationData.put("isSharing", true);

        db.collection("user").document(currentUserId)
                .update(locationData)
                .addOnFailureListener(e -> Log.e("Location", "Error updating location", e));
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
        } else {
            // SAFE MODE
            bottomLocationCard.setCardBackgroundColor(Color.parseColor("#FFFFFF")); // White BG

            btnStartSharing.setCardBackgroundColor(Color.parseColor("#10B981")); // Emerald Green Button
            btnSharingText.setText("Start Sharing");

            locationTitleText.setText("Location Sharing");
            locationTitleText.setTextColor(Color.parseColor("#1F2937"));

            locationSubtitleText.setText("Tap to start sharing");
            locationSubtitleText.setTextColor(Color.parseColor("#6B7280"));
        }
    }

    // DATA FETCHING
    private void fetchContacts() {
        if (currentUserId == null) return;
        db.collection("user").document(currentUserId).collection("trusted_contacts")
                .orderBy("rank").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contactList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        TrustedContact contact = document.toObject(TrustedContact.class);
                        contact.setId(document.getId());
                        contactList.add(contact);
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
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading contacts", Toast.LENGTH_SHORT).show());
    }

    // ADD CONTACT
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

    private void saveContactToFirebase(String name, String phone) {
        // The new contact goes to the bottom of the list (rank = list size)
        int newRank = contactList.size();
        TrustedContact newContact = new TrustedContact(name, phone, newRank);

        db.collection("user").document(currentUserId).collection("trusted_contacts")
                .add(newContact)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Contact Added!", Toast.LENGTH_SHORT).show();
                    // Since we aren't using a listener, we MUST call fetchContacts() here
                    fetchContacts();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add contact", Toast.LENGTH_SHORT).show());
    }

    // PERMISSION LOGIC
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(getContext(), "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
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