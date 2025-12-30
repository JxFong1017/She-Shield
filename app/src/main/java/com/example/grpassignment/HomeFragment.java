package com.example.grpassignment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class HomeFragment extends Fragment {

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

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    shareLocation();
                    checkForNearbyReports();
                } else {
                    Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        sosButton = view.findViewById(R.id.sos_button);
        sosText = view.findViewById(R.id.sos_text);
        sosHoldText = view.findViewById(R.id.sos_hold_text);
        CardView communityReportButton = view.findViewById(R.id.btn_community_report_card);
        Button shareLocationButton = view.findViewById(R.id.button4);
        ImageView safetyMapImage = view.findViewById(R.id.imageView19);
        CardView educationHubCard = view.findViewById(R.id.btn_report);
        TextView viewAllText = view.findViewById(R.id.tv_view_all);
        Button sharePostButton = view.findViewById(R.id.btn_share_post_home);

        fetchLatestReport(view);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.sos_deactivate_title)
                        .setMessage(R.string.sos_deactivate_message)
                        .setPositiveButton(R.string.sos_deactivate_yes, (dialog, which) -> {
                            isSosActive = false;
                            stopBlinking();
                            sosButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_inactive_red));
                            sosText.setText(R.string.sos_press_for_help);
                            sosHoldText.setText(R.string.sos_hold_to_activate);
                            handler.removeCallbacks(runnable);
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
                    runnable = () -> {
                        isSosActive = true;
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_active_red));
                        sosText.setText(R.string.sos_active);
                        sosHoldText.setText(R.string.sos_active_message);
                        startBlinking();
                    };
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
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                shareLocation();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        safetyMapImage.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_nav_home_to_nav_map);
        });

        View.OnClickListener educationHubClickListener = v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_nav_home_to_nav_resources);
        };

        educationHubCard.setOnClickListener(educationHubClickListener);
        viewAllText.setOnClickListener(educationHubClickListener);
    }

    private void shareLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                        Toast.makeText(getContext(), "Could not retrieve location.", Toast.LENGTH_SHORT).show();
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

                                if (reportImage != null && report.getMediaUri() != null && !report.getMediaUri().isEmpty()) {
                                    // To load the image, you'll need a library like Glide or Picasso.
                                    // Add the dependency to your build.gradle, then uncomment the following line:
                                    // com.bumptech.glide.Glide.with(requireContext()).load(report.getMediaUri()).into(reportImage);
                                }
                            }
                        }
                    } else {
                        Log.d("HomeFragment", "Error getting documents or no documents found: ", task.getException());
                    }
                });
    }

    private void checkForNearbyReports() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Permissions are not granted.
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
        CardView safetyStatusCard = getView().findViewById(R.id.safety_status_card);
        TextView safetyStatusTitle = getView().findViewById(R.id.safety_status_title);
        TextView safetyStatusSubtitle = getView().findViewById(R.id.safety_status_subtitle);
        ImageView safetyStatusIcon = getView().findViewById(R.id.safety_status_icon);

        safetyStatusCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.safe_zone_bg));
        safetyStatusTitle.setText("You're in a Safe Zone");
        safetyStatusTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.safe_zone_text));
        safetyStatusSubtitle.setText("Within 500m of verified safe locations");
        safetyStatusSubtitle.setTextColor(ContextCompat.getColor(getContext(), R.color.safe_zone_text));
        safetyStatusIcon.setImageResource(R.drawable.screenshot_2025_11_18_152159);
    }

    private void updateToUnsafeZone() {
        CardView safetyStatusCard = getView().findViewById(R.id.safety_status_card);
        TextView safetyStatusTitle = getView().findViewById(R.id.safety_status_title);
        TextView safetyStatusSubtitle = getView().findViewById(R.id.safety_status_subtitle);
        ImageView safetyStatusIcon = getView().findViewById(R.id.safety_status_icon);

        safetyStatusCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.unsafe_zone_bg));
        safetyStatusTitle.setText("Alert: You're in a Reported Zone");
        safetyStatusTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.unsafe_zone_text));
        safetyStatusSubtitle.setText("A report has been filed within 500m of your location");
        safetyStatusSubtitle.setTextColor(ContextCompat.getColor(getContext(), R.color.unsafe_zone_text));
        safetyStatusIcon.setImageResource(R.drawable.screenshot_2025_11_18_152053);
    }


    private void startBlinking() {
        isBlinking = true;
        blinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBlinking) {
                    if (sosButton.getCardBackgroundColor().getDefaultColor() == ContextCompat.getColor(requireContext(), R.color.sos_active_red)) {
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_inactive_red));
                    } else {
                        sosButton.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.sos_active_red));
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
}
