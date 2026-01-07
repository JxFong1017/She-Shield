package com.example.grpassignment;

import com.example.grpassignment.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles new user registration for the She-Shield application.
 * Integrates with Firebase Authentication to create credentials and store user names.
 */
public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    // UI Elements
    private TextInputLayout tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // 1. Initialize Firebase Auth & Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Initialize UI Views
        initializeUI();

        // 3. Set Up Click Listeners
        btnRegister.setOnClickListener(v -> handleRegistration());

        tvBackToLogin.setOnClickListener(v -> {
            // Navigate to Login Page
            Intent intent = new Intent(SignUpActivity.this, LogInPage.class);
            startActivity(intent);
            // Optional: finish() if you don't want to keep SignUp in back stack
            // finish(); 
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in; if so, navigate to Home
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToHome();
        }
    }

    private void initializeUI() {
        tilFullName = findViewById(R.id.TIL_fullName);
        tilEmail = findViewById(R.id.TIL_signupEmail);
        tilPhone = findViewById(R.id.TIL_signupPhone);
        tilPassword = findViewById(R.id.TIL_signupPassword);
        tilConfirmPassword = findViewById(R.id.TIL_confirmPassword);

        etFullName = findViewById(R.id.ET_fullName);
        etEmail = findViewById(R.id.ET_signupEmail);
        etPhone = findViewById(R.id.ET_signupPhone);
        etPassword = findViewById(R.id.ET_signupPassword);
        etConfirmPassword = findViewById(R.id.ET_confirmPassword);

        btnRegister = findViewById(R.id.BTRegister);
        tvBackToLogin = findViewById(R.id.TVBackToLogin);
    }

    private void handleRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 1. Form Validation
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Full name is required");
            return;
        } else { tilFullName.setError(null); }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return;
        } else { tilEmail.setError(null); }

        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Phone number is required");
            return;
        } else { tilPhone.setError(null); }

        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        } else { tilPassword.setError(null); }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return;
        } else { tilConfirmPassword.setError(null); }

        // 2. Create User in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // 3. Update User Profile and Save to Firestore
                        if (user != null) {
                            updateUserProfileAndSaveData(user, fullName, email, phone);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(SignUpActivity.this, "Authentication failed: " +
                                errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUserProfileAndSaveData(FirebaseUser user, String fullName, String email, String phone) {
        // A. Update Auth Profile (Display Name)
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // B. Save User Data to Firestore
                        saveUserToFirestore(user.getUid(), fullName, email, phone);
                    } else {
                        // Even if profile update fails, try to save to Firestore
                        saveUserToFirestore(user.getUid(), fullName, email, phone);
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String phone) {
        // Create User object
        User newUser = new User(name, email, phone);

        // Save to "user" collection with UID as document ID
        db.collection("user").document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved to Firestore");
                    Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data", e);
                    Toast.makeText(SignUpActivity.this, "Account created, but failed to save details.", Toast.LENGTH_LONG).show();
                    navigateToHome(); // Navigate anyway so user isn't stuck
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}