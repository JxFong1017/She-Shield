package com.example.grpassignment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.grpassignment.SafetyResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private CardView sosButton;
    private TextView sosText;
    private TextView sosHoldText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean isSosActive = false;

    private Runnable blinkRunnable;
    private boolean isBlinking = false;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<String> trustedContacts; // You need to populate this list
    private String currentAlertId;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    shareLocation();
                    checkForNearbyReports();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String[]> requestSosPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    activateSos();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "SOS permissions denied", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        // Ensure user is authenticated
        ensureAuthentication();
        
        if (getActivity() != null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        }

        trustedContacts = new ArrayList<>();
        // TODO: Populate trustedContacts from your storage (SharedPreferences, DB, etc.)
        trustedContacts.add("601159806213"); // Placeholder

        sosButton = view.findViewById(R.id.sos_button);
        sosText = view.findViewById(R.id.sos_text);
        sosHoldText = view.findViewById(R.id.sos_hold_text);
        CardView communityReportButton = view.findViewById(R.id.btn_community_report_card);
        Button shareLocationButton = view.findViewById(R.id.button4);
        CardView educationHubCard = view.findViewById(R.id.btn_report);
        TextView viewAllText = view.findViewById(R.id.tv_view_all);
        Button sharePostButton = view.findViewById(R.id.btn_share_post_home);
        View mapPreviewContainer = view.findViewById(R.id.map_preview_container);
        CardView btnSafetyZones = view.findViewById(R.id.btn_safety_zones);

        if (savedInstanceState == null) {
            Fragment mapPreviewFragment = new MapPreviewFragment();
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.map_preview_container, mapPreviewFragment).commit();
        }

        btnSafetyZones.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_nav_home_to_nav_map);
        });

        fetchLatestReport(view);
        fetchLatestSafetyResource(view);

        if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkForNearbyReports();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        sharePostButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PostReportActivity.class);
            startActivity(intent);
        });

        sosButton.setOnClickListener(v -> {
            if (isSosActive) {
                Context context = getContext();
                if (context == null) return;

                new AlertDialog.Builder(context)
                        .setTitle(R.string.sos_deactivate_title)
                        .setMessage(R.string.sos_deactivate_message)
                        .setPositiveButton(R.string.sos_deactivate_yes, (dialog, which) -> {
                            Activity activity = getActivity();
                            if (activity == null) return;

                            isSosActive = false;
                            stopBlinking();
                            sosButton.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.sos_inactive_red));
                            sosText.setText(R.string.sos_press_for_help);
                            sosHoldText.setText(R.string.sos_hold_to_activate);
                            handler.removeCallbacks(runnable);
                            Intent intent = new Intent(activity, SosService.class);
                            activity.stopService(intent);
                            deactivateAlert();
                        })
                        .setNegativeButton(R.string.sos_deactivate_no, null)
                        .show();
            }
        });

        sosButton.setOnTouchListener((v, event) -> {
            if (isSosActive) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    runnable = this::checkAndRequestSosPermissions;
                    handler.postDelayed(runnable, 3000);
                    return true;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(runnable);
                    v.performClick();
                    return true;
            }
            return false;
        });

        communityReportButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_nav_home_to_nav_report);
        });

        shareLocationButton.setOnClickListener(v -> {
            if (getContext() != null && ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                shareLocation();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        View.OnClickListener educationHubClickListener = v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_nav_home_to_nav_resources);
        };

        educationHubCard.setOnClickListener(educationHubClickListener);
        viewAllText.setOnClickListener(educationHubClickListener);
    }

    private void checkAndRequestSosPermissions() {
        Context context = getContext();
        if (context == null) return;

        List<String> permissionsToRequest = new ArrayList<>();
        permissionsToRequest.add(Manifest.permission.SEND_SMS);
        permissionsToRequest.add(Manifest.permission.CALL_PHONE);
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        boolean permissionsGranted = true;
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsGranted = false;
                break;
            }
        }

        if (permissionsGranted) {
            activateSos();
        } else {
            requestSosPermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    private void activateSos() {
        final Context context = getContext();
        final Activity activity = getActivity();
        if (context == null || activity == null) {
            return;
        }

        isSosActive = true;
        sosButton.setCardBackgroundColor(ContextCompat.getColor(context, R.color.sos_active_red));
        sosText.setText(R.string.sos_active);
        sosHoldText.setText(R.string.sos_active_message);
        startBlinking();
        saveAlertToFirestore();

        // Only make the call if the app is in the foreground
        if (isResumed()) {
            makePhoneCall();
        }

        Intent intent = new Intent(activity, SosService.class);
        ContextCompat.startForegroundService(activity, intent);
    }

    private void saveAlertToFirestore() {
        Log.d(TAG, "Attempting to save alert to Firestore.");

        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot save alert.");
            Toast.makeText(getContext(), "Error: Context not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted, cannot save alert.");
            Toast.makeText(getContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.d(TAG, "Successfully retrieved location: " + location.getLatitude() + ", " + location.getLongitude());
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    Log.e(TAG, "User not signed in, cannot save alert. Attempting re-authentication...");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Authentication issue. Please restart the app and log in again.", Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                Log.d(TAG, "User is signed in: " + currentUser.getUid() + " (Anonymous: " + currentUser.isAnonymous() + ")");

                Map<String, Object> alert = new HashMap<>();
                alert.put("initiatorId", currentUser.getUid());
                alert.put("location", new GeoPoint(location.getLatitude(), location.getLongitude()));
                alert.put("status", "active");
                alert.put("time", new Date());

                db.collection("alerts")
                        .add(alert)
                        .addOnSuccessListener(documentReference -> {
                            currentAlertId = documentReference.getId();
                            Log.d(TAG, "Alert saved with ID: " + currentAlertId);
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "SOS alert created in Firestore.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error adding alert to Firestore", e);
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error saving alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Log.e(TAG, "Failed to retrieve location, cannot save alert.");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Could not get location to save alert.", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error getting location for alert", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to get location: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deactivateAlert() {
        if (currentAlertId != null) {
            db.collection("alerts").document(currentAlertId)
                    .update("status", "inactive")
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Alert successfully deactivated"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error deactivating alert", e));
        }
    }


    private void makePhoneCall() {
        if (!trustedContacts.isEmpty()) {
            String phoneNumber = trustedContacts.get(0);
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            try {
                startActivity(callIntent);
            } catch (SecurityException e) {
                Log.e(TAG, "Error making phone call", e);
            }
        }
    }

    private void shareLocation() {
        if (getContext() == null || (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        String uri = "http://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude();
                        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My Current Location");
                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Here is my current location: " + uri);
                        startActivity(Intent.createChooser(sharingIntent, "Share Location"));
                    } else {
                        if (getContext() != null) {
                           Toast.makeText(getContext(), "Could not retrieve location.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchLatestReport(View view) {
        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Report report = document.toObject(Report.class);
                            View reportCard = view.findViewById(R.id.latest_report_card);

                            if (reportCard != null) {
                                TextView reportTitle = reportCard.findViewById(R.id.txt_report_title);
                                TextView reportLocationTime = reportCard.findViewById(R.id.txt_report_location_time);
                                Button viewDetailsButton = reportCard.findViewById(R.id.btn_view_details);
                                ImageView reportImage = reportCard.findViewById(R.id.img_report);

                                if (reportTitle != null) {
                                    reportTitle.setText(report.getType());
                                }
                                if (reportLocationTime != null) {
                                    reportLocationTime.setText("Location: " + report.getLocation() + " | " + report.getTime());
                                }

                                if (viewDetailsButton != null) {
                                    viewDetailsButton.setVisibility(View.GONE);
                                }

                                if (reportImage != null && report.getMediaUri() != null && !report.getMediaUri().isEmpty() && getContext() != null) {
                                    Glide.with(requireContext()).load(report.getMediaUri()).into(reportImage);
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Error getting documents or no documents found: ", task.getException());
                    }
                });
    }

    private void fetchLatestSafetyResource(View view) {
        db.collection("safety_resource")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            SafetyResource resource = document.toObject(SafetyResource.class);
                            View resourceCard = view.findViewById(R.id.latest_safety_resource);

                            if (resourceCard != null) {
                                TextView titleTextView = resourceCard.findViewById(R.id.titleTextView);
                                TextView categoryTextView = resourceCard.findViewById(R.id.categoryTextView);
                                TextView durationTextView = resourceCard.findViewById(R.id.durationTextView);
                                ImageView resourceImageView = resourceCard.findViewById(R.id.iconImageView);

                                if (titleTextView != null) {
                                    titleTextView.setText(resource.getTitle());
                                }
                                if (categoryTextView != null) {
                                    categoryTextView.setText(resource.getCategory());
                                }
                                if (durationTextView != null) {
                                    durationTextView.setText(resource.getDuration());
                                }
                                if (resourceImageView != null && resource.getImageUrl() != null && !resource.getImageUrl().isEmpty() && getContext() != null) {
                                    Glide.with(requireContext()).load(resource.getImageUrl()).into(resourceImageView);
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Error getting safety resource: ", task.getException());
                    }
                });
    }

    private void checkForNearbyReports() {
        if (getContext() == null || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Permissions are not granted or context is null
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), currentLocation -> {
            if (currentLocation == null) {
                updateToSafeZone();
                return;
            }

            db.collection("reports").get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    updateToSafeZone();
                    return;
                }

                boolean isNearReport = false;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Report report = document.toObject(Report.class);
                    if (report.getLatitude() != 0 && report.getLongitude() != 0) {
                        Location reportLocation = new Location("");
                        reportLocation.setLatitude(report.getLatitude());
                        reportLocation.setLongitude(report.getLongitude());

                        if (currentLocation.distanceTo(reportLocation) <= 500) {
                            isNearReport = true;
                            break;
                        }
                    }
                }

                if (isNearReport) {
                    updateToUnsafeZone();
                } else {
                    updateToSafeZone();
                }
            });
        });
    }

    private void updateToSafeZone() {
        View view = getView();
        Context context = getContext();
        if (view == null || context == null) return;

        CardView safetyStatusCard = view.findViewById(R.id.safety_status_card);
        TextView safetyStatusTitle = view.findViewById(R.id.safety_status_title);
        TextView safetyStatusSubtitle = view.findViewById(R.id.safety_status_subtitle);
        ImageView safetyStatusIcon = view.findViewById(R.id.safety_status_icon);

        safetyStatusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.safe_zone_bg));
        safetyStatusTitle.setText("You're in a Safe Zone");
        safetyStatusTitle.setTextColor(ContextCompat.getColor(context, R.color.safe_zone_text));
        safetyStatusSubtitle.setText("Within 500m of verified safe locations");
        safetyStatusSubtitle.setTextColor(ContextCompat.getColor(context, R.color.safe_zone_text));
        safetyStatusIcon.setImageResource(R.drawable.screenshot_2025_11_18_152159);
    }

    private void updateToUnsafeZone() {
        View view = getView();
        Context context = getContext();
        if (view == null || context == null) return;

        CardView safetyStatusCard = view.findViewById(R.id.safety_status_card);
        TextView safetyStatusTitle = view.findViewById(R.id.safety_status_title);
        TextView safetyStatusSubtitle = view.findViewById(R.id.safety_status_subtitle);
        ImageView safetyStatusIcon = view.findViewById(R.id.safety_status_icon);

        safetyStatusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.unsafe_zone_bg));
        safetyStatusTitle.setText("Alert: You're in a Reported Zone");
        safetyStatusTitle.setTextColor(ContextCompat.getColor(context, R.color.unsafe_zone_text));
        safetyStatusSubtitle.setText("A report has been filed within 500m of your location");
        safetyStatusSubtitle.setTextColor(ContextCompat.getColor(context, R.color.unsafe_zone_text));
        safetyStatusIcon.setImageResource(R.drawable.screenshot_2025_11_18_152053);
    }

    private void startBlinking() {
        if (getContext() == null) return;

        isBlinking = true;
        blinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBlinking) {
                    Context context = getContext();
                    if (context == null) return;

                    if (sosButton.getCardBackgroundColor().getDefaultColor() == ContextCompat.getColor(context, R.color.sos_active_red)) {
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(context, R.color.sos_inactive_red));
                    } else {
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(context, R.color.sos_active_red));
                    }
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(blinkRunnable);
    }

    private void stopBlinking() {
        isBlinking = false;
        handler.removeCallbacks(blinkRunnable);
    }

    /**
     * Ensures the user is authenticated before using Firebase features.
     * If not logged in with email/password, signs in anonymously as a fallback.
     */
    private void ensureAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User not authenticated, signing in anonymously...");
            mAuth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Anonymous sign-in successful");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d(TAG, "User ID: " + user.getUid());
                            }
                        } else {
                            Log.e(TAG, "Anonymous sign-in failed", task.getException());
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Authentication failed. Some features may not work.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } else {
            Log.d(TAG, "User already authenticated: " + currentUser.getUid());
        }
    }
}
