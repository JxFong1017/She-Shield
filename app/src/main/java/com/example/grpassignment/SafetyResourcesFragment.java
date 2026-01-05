package com.example.grpassignment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
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
        // Firebase initialization
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        // Inflate the layout - use the existing safety_resources.xml
        View view = inflater.inflate(R.layout.safety_resources, container, false);

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

        return view;
    }

    private void initializeViews(View view) {
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
    }

    private void setupRecyclerView() {
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
            return;
        }

        SafetyResource workshop = upcomingWorkshops.get(index);

        nextWorkshopTitle.setText(workshop.getTitle());
        nextWorkshopDescription.setText(workshop.getDescription());
        nextWorkshopDate.setText(workshop.getEventDate() != null ? workshop.getEventDate() : "TBA");
        nextWorkshopTime.setText(workshop.getEventTime() != null ? workshop.getEventTime() : "TBA");
        nextWorkshopLocation.setText(workshop.getLocation() != null ? workshop.getLocation() : "Online");
        nextWorkshopCapacity.setText(workshop.getCapacity() + " seats");
        workshopIndicator.setText((index + 1) + "/" + upcomingWorkshops.size());

        registerWorkshopCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RegistrationActivity.class);
            intent.putExtra("RESOURCE_TITLE", workshop.getTitle());
            intent.putExtra("EVENT_DATE", workshop.getEventDate());
            intent.putExtra("EVENT_TIME", workshop.getEventTime());
            intent.putExtra("LOCATION", workshop.getLocation());
            intent.putExtra("INSTRUCTOR", workshop.getInstructor());
            intent.putExtra("CAPACITY", workshop.getCapacity());
            intent.putExtra("DESCRIPTION", workshop.getDescription());
            startActivity(intent);
        });
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
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        db.collection("safety_resource")
                .get(Source.SERVER)
                .addOnCompleteListener(task -> {
                    if (isAdded()) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful() && task.getResult() != null) {
                            resourceList.clear();
                            upcomingWorkshops.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                SafetyResource resource = document.toObject(SafetyResource.class);
                                resource.setId(document.getId());
                                resourceList.add(resource);

                                if ("Workshop".equals(resource.getType())) {
                                    upcomingWorkshops.add(resource);
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

                            adapter.updateList(resourceList);
                            adapter.filterByType("All");

                            if (!upcomingWorkshops.isEmpty()) {
                                displayWorkshop(0);
                                workshopHandler.postDelayed(workshopRunnable, 5000);
                            }

                            updateEmptyView();
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