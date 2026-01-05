package com.example.grpassignment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity4 extends AppCompatActivity {
    private static final String TAG = "MainActivity4";

    private RecyclerView recyclerView;
    private SafetyResourceAdapter adapter;
    private List<SafetyResource> resourceList;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    // Filter chips
    private CardView filterAll, filterVideos, filterArticles, filterWorkshops, filterLegal;
    private String currentFilter = "All";

    // Hero workshop rotation
    private List<SafetyResource> upcomingWorkshops;
    private int currentWorkshopIndex = 0;
    private Handler workshopHandler;
    private Runnable workshopRunnable;
    private TextView nextWorkshopTitle, nextWorkshopDescription, nextWorkshopDate,
            nextWorkshopTime, nextWorkshopLocation, nextWorkshopCapacity, workshopIndicator;
    private CardView registerWorkshopCard;
    private SafetyResource currentDisplayedWorkshop;

    // Bottom Navigation
    private ImageView navHome, navEmergency, navResources, navCommunity, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_safety_resources);

        // Window Insets
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Firebase initialization
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        // Initialize UI components
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup filter logic
        setupFilterChips();

        // Setup workshop rotation
        setupWorkshopRotation();

        // Setup bottom navigation
        setupBottomNavigation();

        // Fetch the resources
        loadResources();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.resourcesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);

        filterAll = findViewById(R.id.filter_all);
        filterVideos = findViewById(R.id.filter_shelters);
        filterArticles = findViewById(R.id.filter_articles);
        filterWorkshops = findViewById(R.id.filter_workshop);
        filterLegal = findViewById(R.id.filter_legal);

        // Hero workshop views
        nextWorkshopTitle = findViewById(R.id.nextWorkshopTitle);
        nextWorkshopDescription = findViewById(R.id.nextWorkshopDescription);
        nextWorkshopDate = findViewById(R.id.nextWorkshopDate);
        nextWorkshopTime = findViewById(R.id.nextWorkshopTime);
        nextWorkshopLocation = findViewById(R.id.nextWorkshopLocation);
        nextWorkshopCapacity = findViewById(R.id.nextWorkshopCapacity);
        workshopIndicator = findViewById(R.id.workshopIndicator);
        registerWorkshopCard = findViewById(R.id.registerWorkshopCard);

        // Bottom Navigation
        navHome = findViewById(R.id.imageView8);
        navEmergency = findViewById(R.id.imageView9);
        navResources = findViewById(R.id.imageView10);
        navCommunity = findViewById(R.id.imageView11);
        navProfile = findViewById(R.id.imageView12);

        resourceList = new ArrayList<>();
        upcomingWorkshops = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SafetyResourceAdapter(this, resourceList);
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterChips() {
        filterAll.setOnClickListener(v -> applyFilter("All", filterAll));
        filterVideos.setOnClickListener(v -> applyFilter("Video", filterVideos));
        filterArticles.setOnClickListener(v -> applyFilter("Article", filterArticles));
        filterWorkshops.setOnClickListener(v -> applyFilter("Workshop", filterWorkshops));
        filterLegal.setOnClickListener(v -> applyFilter("Legal", filterLegal));
    }

    private void setupBottomNavigation() {
        // Home
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity4.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Emergency
        if (navEmergency != null) {
            navEmergency.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity4.this, MainActivity2.class);
                startActivity(intent);
            });
        }

        // Resources (current page)
        if (navResources != null) {
            navResources.setOnClickListener(v -> {
                Toast.makeText(this, "Already on Resources", Toast.LENGTH_SHORT).show();
            });
        }

        // Community
        if (navCommunity != null) {
            navCommunity.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity4.this, MainActivity3.class);
                startActivity(intent);
            });
        }

        // Profile
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity4.this, MainActivity5.class);
                startActivity(intent);
            });
        }
    }

    private void setupWorkshopRotation() {
        workshopHandler = new Handler();
        workshopRunnable = new Runnable() {
            @Override
            public void run() {
                if (upcomingWorkshops != null && !upcomingWorkshops.isEmpty()) {
                    currentWorkshopIndex = (currentWorkshopIndex + 1) % upcomingWorkshops.size();
                    displayWorkshop(currentWorkshopIndex);
                    workshopHandler.postDelayed(this, 5000); // Switch every 5 seconds
                }
            }
        };
    }

    private void displayWorkshop(int index) {
        if (upcomingWorkshops == null || upcomingWorkshops.isEmpty() || index >= upcomingWorkshops.size()) {
            return;
        }

        SafetyResource workshop = upcomingWorkshops.get(index);
        currentDisplayedWorkshop = workshop;

        nextWorkshopTitle.setText(workshop.getTitle());
        nextWorkshopDescription.setText(workshop.getDescription());
        nextWorkshopDate.setText(workshop.getEventDate() != null ? workshop.getEventDate() : "TBA");
        nextWorkshopTime.setText(workshop.getEventTime() != null ? workshop.getEventTime() : "TBA");
        nextWorkshopLocation.setText(workshop.getLocation() != null ? workshop.getLocation() : "Online");
        nextWorkshopCapacity.setText(workshop.getCapacity() + " seats");
        workshopIndicator.setText((index + 1) + "/" + upcomingWorkshops.size());

        // Add click listener to register button in hero section
        registerWorkshopCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity4.this, RegistrationActivity.class);
            intent.putExtra("RESOURCE_TITLE", workshop.getTitle());
            intent.putExtra("EVENT_DATE", workshop.getEventDate());
            intent.putExtra("EVENT_TIME", workshop.getEventTime());
            intent.putExtra("LOCATION", workshop.getLocation());
            intent.putExtra("INSTRUCTOR", workshop.getInstructor());
            intent.putExtra("CAPACITY", workshop.getCapacity());
            intent.putExtra("DESCRIPTION", workshop.getDescription());
            // Don't use any special flags - let it maintain normal back stack
            startActivity(intent);
        });
    }
    private void applyFilter(String filterType, CardView selectedCard) {
        currentFilter = filterType;
        Log.d(TAG, "Applying filter: " + filterType);

        resetFilterChips();
        selectedCard.setCardBackgroundColor(Color.parseColor("#F3E8FF"));

        if (adapter != null) {
            adapter.filterByType(filterType);
            updateEmptyView();
        }
    }

    private void resetFilterChips() {
        filterAll.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        filterVideos.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        filterArticles.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        filterWorkshops.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        filterLegal.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
    }

    private void loadResources() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        db.collection("safety_resource")
                .get(Source.SERVER)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        resourceList.clear();
                        upcomingWorkshops.clear();
                        Log.d(TAG, "📦 Total documents found: " + task.getResult().size());

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            SafetyResource resource = document.toObject(SafetyResource.class);
                            resource.setId(document.getId());
                            resourceList.add(resource);

                            // Collect workshops for hero section
                            if ("Workshop".equals(resource.getType())) {
                                upcomingWorkshops.add(resource);
                            }
                        }

                        // Sort workshops by date (earliest first)
                        Collections.sort(upcomingWorkshops, (w1, w2) -> {
                            if (w1.getEventTimestamp() != null && w2.getEventTimestamp() != null) {
                                return w1.getEventTimestamp().compareTo(w2.getEventTimestamp());
                            }
                            return 0;
                        });

                        // Limit to 3 workshops
                        if (upcomingWorkshops.size() > 3) {
                            upcomingWorkshops = upcomingWorkshops.subList(0, 3);
                        }

                        adapter.updateList(resourceList);
                        adapter.filterByType("All");

                        // Start workshop rotation
                        if (!upcomingWorkshops.isEmpty()) {
                            displayWorkshop(0);
                            workshopHandler.postDelayed(workshopRunnable, 5000); // Start rotation after 5 seconds
                        }

                        runOnUiThread(() -> updateEmptyView());
                    } else {
                        Log.e(TAG, "❌ Error loading resources", task.getException());
                        Toast.makeText(MainActivity4.this,
                                "Failed to load resources. Check internet connection.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateEmptyView() {
        if (adapter == null || adapter.getItemCount() == 0) {
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setText("No " + (currentFilter.equals("All") ? "" : currentFilter + " ") + "resources found");
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop workshop rotation when activity is destroyed
        if (workshopHandler != null && workshopRunnable != null) {
            workshopHandler.removeCallbacks(workshopRunnable);
        }
    }
}