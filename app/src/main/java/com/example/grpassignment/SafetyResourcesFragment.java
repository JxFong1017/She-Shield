package com.example.grpassignment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SafetyResourcesFragment extends Fragment {

    private static final String TAG = "SafetyResourcesFragment";

    private RecyclerView recyclerView;
    private SafetyResourceAdapter adapter;
    private List<SafetyResource> resourceList;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    // Filter chips
    private CardView filterAll, filterVideos, filterArticles, filterWorkshops, filterLegal;
    private String currentFilter = "All";

    // Hero workshop rotation logic
    private List<SafetyResource> upcomingWorkshops;
    private int currentWorkshopIndex = 0;
    private Handler workshopHandler;
    private Runnable workshopRunnable;
    private TextView nextWorkshopTitle, nextWorkshopDescription, nextWorkshopDate,
            nextWorkshopTime, nextWorkshopLocation, nextWorkshopCapacity, workshopIndicator;
    private CardView registerWorkshopCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            // Firebase initialization
            db = FirebaseFirestore.getInstance();
            
            // Inflate the layout - use the existing safety_resources.xml
            View view = inflater.inflate(R.layout.safety_resources, container, false);
            Log.d(TAG, "Layout inflated successfully");

            // Initialize UI components
            initializeViews(view);

            // Initialize lists
            resourceList = new ArrayList<>();
            upcomingWorkshops = new ArrayList<>();

            // Setup components
            setupRecyclerView();
            setupFilterChips();
            setupWorkshopRotation();
            loadResources();

            Log.d(TAG, "onCreateView completed successfully");
            return view;
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR in onCreateView: " + e.getMessage(), e);
            e.printStackTrace();
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            // Create a simple text view showing the error
            TextView errorView = new TextView(requireContext());
            errorView.setText("Error loading resources:\n" + e.getMessage() + "\n\nPlease check Firestore permissions for 'safety_resource' collection");
            errorView.setPadding(32, 32, 32, 32);
            errorView.setTextSize(16);
            return errorView;
        }
    }

    private void initializeViews(View view) {
        if (view == null) {
            Log.e(TAG, "View is null in initializeViews");
            return;
        }
        
        try {
            recyclerView = view.findViewById(R.id.resourcesRecyclerView);
            progressBar = view.findViewById(R.id.progressBar);
            emptyTextView = view.findViewById(R.id.emptyTextView);

            filterAll = view.findViewById(R.id.filter_all);
            filterVideos = view.findViewById(R.id.filter_shelters);
            filterArticles = view.findViewById(R.id.filter_articles);
            filterWorkshops = view.findViewById(R.id.filter_workshop);
            filterLegal = view.findViewById(R.id.filter_legal);

            // Hero workshop views
            nextWorkshopTitle = view.findViewById(R.id.nextWorkshopTitle);
            nextWorkshopDescription = view.findViewById(R.id.nextWorkshopDescription);
            nextWorkshopDate = view.findViewById(R.id.nextWorkshopDate);
            nextWorkshopTime = view.findViewById(R.id.nextWorkshopTime);
            nextWorkshopLocation = view.findViewById(R.id.nextWorkshopLocation);
            nextWorkshopCapacity = view.findViewById(R.id.nextWorkshopCapacity);
            workshopIndicator = view.findViewById(R.id.workshopIndicator);
            registerWorkshopCard = view.findViewById(R.id.registerWorkshopCard);
            
            // Log missing views
            if (recyclerView == null) Log.e(TAG, "recyclerView is null");
            if (progressBar == null) Log.e(TAG, "progressBar is null");
            if (emptyTextView == null) Log.e(TAG, "emptyTextView is null");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null || getContext() == null) {
            Log.e(TAG, "Cannot setup RecyclerView - view or context is null");
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SafetyResourceAdapter(getContext(), resourceList);
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterChips() {
        filterAll.setOnClickListener(v -> applyFilter("All", filterAll));
        filterVideos.setOnClickListener(v -> applyFilter("Video", filterVideos));
        filterArticles.setOnClickListener(v -> applyFilter("Article", filterArticles));
        filterWorkshops.setOnClickListener(v -> applyFilter("Workshop", filterWorkshops));
        filterLegal.setOnClickListener(v -> applyFilter("Legal", filterLegal));
    }

    private void setupWorkshopRotation() {
        workshopHandler = new Handler();
        workshopRunnable = new Runnable() {
            @Override
            public void run() {
                if (upcomingWorkshops != null && !upcomingWorkshops.isEmpty()) {
                    currentWorkshopIndex = (currentWorkshopIndex + 1) % upcomingWorkshops.size();
                    displayWorkshop(currentWorkshopIndex);
                    workshopHandler.postDelayed(this, 5000);
                }
            }
        };
    }

    private void displayWorkshop(int index) {
        if (upcomingWorkshops == null || upcomingWorkshops.isEmpty() || index >= upcomingWorkshops.size()) {
            Log.d(TAG, "No workshops to display");
            return;
        }
        
        if (nextWorkshopTitle == null || nextWorkshopDescription == null) {
            Log.e(TAG, "Workshop views are null");
            return;
        }

        try {
            SafetyResource workshop = upcomingWorkshops.get(index);

            nextWorkshopTitle.setText(workshop.getTitle() != null ? workshop.getTitle() : "Untitled");
            nextWorkshopDescription.setText(workshop.getDescription() != null ? workshop.getDescription() : "");
            if (nextWorkshopDate != null) nextWorkshopDate.setText(workshop.getEventDate() != null ? workshop.getEventDate() : "TBA");
            if (nextWorkshopTime != null) nextWorkshopTime.setText(workshop.getEventTime() != null ? workshop.getEventTime() : "TBA");
            if (nextWorkshopLocation != null) nextWorkshopLocation.setText(workshop.getLocation() != null ? workshop.getLocation() : "Online");
            if (nextWorkshopCapacity != null) nextWorkshopCapacity.setText(workshop.getCapacity() + " seats");
            if (workshopIndicator != null) workshopIndicator.setText((index + 1) + "/" + upcomingWorkshops.size());

            if (registerWorkshopCard != null && getActivity() != null) {
                registerWorkshopCard.setOnClickListener(v -> {
                    Navigation.findNavController(v).navigate(R.id.action_nav_resources_to_registration);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying workshop: " + e.getMessage(), e);
        }
    }

    private void applyFilter(String filterType, CardView selectedCard) {
        currentFilter = filterType;
        resetFilterChips();
        selectedCard.setCardBackgroundColor(Color.parseColor("#F3E8FF"));

        if (adapter != null) {
            adapter.filterByType(filterType);
            updateEmptyView();
        }
    }

    private void resetFilterChips() {
        int white = Color.parseColor("#FFFFFF");
        filterAll.setCardBackgroundColor(white);
        filterVideos.setCardBackgroundColor(white);
        filterArticles.setCardBackgroundColor(white);
        filterWorkshops.setCardBackgroundColor(white);
        filterLegal.setCardBackgroundColor(white);
    }

    private void loadResources() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyTextView != null) emptyTextView.setVisibility(View.GONE);

        Log.d(TAG, "Loading safety resources from Firestore...");
        
        db.collection("safety_resource")
                .get(Source.SERVER)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d(TAG, "Successfully loaded " + task.getResult().size() + " resources");
                        resourceList.clear();
                        upcomingWorkshops.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                SafetyResource resource = document.toObject(SafetyResource.class);
                                if (resource != null) {
                                    resource.setId(document.getId());
                                    resourceList.add(resource);

                                    if ("Workshop".equals(resource.getType())) {
                                        upcomingWorkshops.add(resource);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing resource document: " + e.getMessage(), e);
                            }
                        }

                        Collections.sort(upcomingWorkshops, (w1, w2) -> {
                            if (w1.getEventTimestamp() != null && w2.getEventTimestamp() != null) {
                                return w1.getEventTimestamp().compareTo(w2.getEventTimestamp());
                            }
                            return 0;
                        });

                        if (upcomingWorkshops.size() > 3) {
                            upcomingWorkshops = upcomingWorkshops.subList(0, 3);
                        }

                        if (adapter != null) {
                            adapter.updateList(resourceList);
                            adapter.filterByType("All");
                        }

                        if (!upcomingWorkshops.isEmpty() && workshopHandler != null && workshopRunnable != null) {
                            displayWorkshop(0);
                            workshopHandler.postDelayed(workshopRunnable, 5000);
                        }

                        updateEmptyView();
                    } else {
                        // Handle error
                        String errorMsg = "Error loading resources";
                        if (task.getException() != null) {
                            Log.e(TAG, "Firestore error: " + task.getException().getMessage(), task.getException());
                            if (task.getException().getMessage() != null && 
                                task.getException().getMessage().contains("PERMISSION_DENIED")) {
                                errorMsg = "Database permission denied. Please check Firestore security rules for 'safety_resource' collection.";
                            }
                        }
                        if (isAdded()) {
                            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                        }
                        if (emptyTextView != null) {
                            emptyTextView.setVisibility(View.VISIBLE);
                            emptyTextView.setText(errorMsg);
                        }
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
    public void onDestroyView() {
        super.onDestroyView();
        if (workshopHandler != null && workshopRunnable != null) {
            workshopHandler.removeCallbacks(workshopRunnable);
        }
    }
}