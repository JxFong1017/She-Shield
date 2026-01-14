package com.example.grpassignment;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrationFragment extends Fragment {

    private static final String TAG = "RegistrationFragment";

    private Spinner workshopSpinner;
    private EditText nameInput, emailInput;
    private Button registerBtn;

    private FirebaseFirestore db;
    private EmailJSHelper emailHelper;

    private List<String> workshopDisplayList;
    private List<String> workshopIdList;
    private Map<String, WorkshopData> workshopDataMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        emailHelper = new EmailJSHelper();

        workshopDisplayList = new ArrayList<>();
        workshopIdList = new ArrayList<>();
        workshopDataMap = new HashMap<>();

        workshopSpinner = view.findViewById(R.id.workshopSpinner);
        nameInput = view.findViewById(R.id.userName);
        emailInput = view.findViewById(R.id.userEmail);
        registerBtn = view.findViewById(R.id.btnRegister);

        workshopDisplayList.add("Loading workshops...");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                workshopDisplayList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workshopSpinner.setAdapter(adapter);

        loadWorkshopsFromFirestore(adapter);

        registerBtn.setOnClickListener(v -> handleRegistration());
    }

    // ------------------ Load Workshops ------------------
    private void loadWorkshopsFromFirestore(ArrayAdapter<String> adapter) {
        db.collection("safety_resource")
                .whereEqualTo("type", "Workshop")
                .get()
                .addOnSuccessListener(query -> {
                    workshopDisplayList.clear();
                    workshopIdList.clear();
                    workshopDataMap.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String date = doc.getString("eventDate");
                        String time = doc.getString("eventTime");
                        String location = doc.getString("location");
                        String instructor = doc.getString("instructor");
                        String description = doc.getString("description");

                        int capacity = 0;
                        Object capObj = doc.get("capacity");
                        if (capObj instanceof Long) capacity = ((Long) capObj).intValue();

                        String display = title + " - " + date + " @ " + time;

                        workshopDisplayList.add(display);
                        workshopIdList.add(id);
                        workshopDataMap.put(id,
                                new WorkshopData(id, title, date, time, location,
                                        instructor, capacity, description));
                    }

                    if (workshopDisplayList.isEmpty()) {
                        workshopDisplayList.add("No workshops available");
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load workshops", e);
                    Toast.makeText(requireContext(), "Failed to load workshops", Toast.LENGTH_SHORT).show();
                });
    }

    // ------------------ Registration Logic ------------------
    private void handleRegistration() {

        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        int pos = workshopSpinner.getSelectedItemPosition();

        if (name.isEmpty()) {
            nameInput.setError("Required");
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Invalid email");
            return;
        }

        if (pos < 0 || pos >= workshopIdList.size()) {
            Toast.makeText(requireContext(), "Select a workshop", Toast.LENGTH_SHORT).show();
            return;
        }

        String workshopId = workshopIdList.get(pos);
        WorkshopData data = workshopDataMap.get(workshopId);

        registerBtn.setEnabled(false);

        Map<String, Object> registration = new HashMap<>();
        registration.put("userName", name);
        registration.put("userEmail", email);
        registration.put("workshopId", workshopId);
        registration.put("workshopTitle", data.title);
        registration.put("eventDate", data.eventDate);
        registration.put("eventTime", data.eventTime);
        registration.put("location", data.location);
        registration.put("instructor", data.instructor);
        registration.put("capacity", data.capacity);
        registration.put("registrationTime", Timestamp.now());
        registration.put("status", "registered");

        db.collection("workshop_registrations")
                .add(registration)
                .addOnSuccessListener(doc -> {

                    if (emailHelper != null) {
                        emailHelper.sendRegistrationConfirmation(
                                email,
                                name,
                                data.title,
                                data.eventDate,
                                data.eventTime,
                                data.location,
                                null
                        );
                    }

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Registration Successful")
                            .setMessage(
                                    "You have successfully registered for:\n\n" +
                                            data.title + "\n\n" +
                                            "A confirmation email has been sent to:\n" +
                                            email
                            )
                            .setCancelable(false)
                            .setPositiveButton("OK", (d, w) -> 
                                Navigation.findNavController(requireView()).navigateUp())
                            .show();
                })
                .addOnFailureListener(e -> {
                    registerBtn.setEnabled(true);
                    Toast.makeText(requireContext(), "Registration failed. Try again.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (emailHelper != null) emailHelper.shutdown();
    }

    // ------------------ Workshop Model ------------------
    private static class WorkshopData {
        String id, title, eventDate, eventTime, location, instructor, description;
        int capacity;

        WorkshopData(String id, String title, String eventDate, String eventTime,
                     String location, String instructor, int capacity, String description) {
            this.id = id;
            this.title = title;
            this.eventDate = eventDate;
            this.eventTime = eventTime;
            this.location = location;
            this.instructor = instructor;
            this.capacity = capacity;
            this.description = description;
        }
    }
}
