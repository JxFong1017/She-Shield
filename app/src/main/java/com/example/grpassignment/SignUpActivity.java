package com.example.grpassignment;

import com.example.grpassignment.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
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
    private ProgressBar progressBar;

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
        progressBar = findViewById(R.id.progressBar);
    }

    private void handleRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String rawPhone = etPhone.getText().toString().trim();
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
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email address");
            return;
        } else { tilEmail.setError(null); }

        if (TextUtils.isEmpty(rawPhone)) {
            tilPhone.setError("Phone number is required");
            return;
        } else { tilPhone.setError(null); }

        // Format Phone Number
        String formattedPhone = formatPhoneNumber(rawPhone);
        if (formattedPhone.length() < 10) {
             tilPhone.setError("Invalid phone number format");
             return;
        } else {
             tilPhone.setError(null);
        }

        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        } else { tilPassword.setError(null); }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return;
        } else { tilConfirmPassword.setError(null); }

        // Show Loading State
        setLoading(true);
        Log.d(TAG, "Attempting to register user: " + email);

        // 2. Create User in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // 3. Update User Profile and Save to Firestore
                        if (user != null) {
                            updateUserProfileAndSaveData(user, fullName, email, formattedPhone);
                        } else {
                            setLoading(false);
                        }
                    } else {
                        setLoading(false);
                        Exception exception = task.getException();
                        Log.w(TAG, "createUserWithEmail:failure", exception);

                        String errorMessage = "Authentication failed.";

                        if (exception != null) {
                            try {
                                throw exception;
                            } catch (FirebaseAuthWeakPasswordException e) {
                                tilPassword.setError("Password is too weak.");
                                tilPassword.requestFocus();
                                return;
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                tilEmail.setError("Invalid email format.");
                                tilEmail.requestFocus();
                                return;
                            } catch (FirebaseAuthUserCollisionException e) {
                                tilEmail.setError("Email already in use.");
                                tilEmail.requestFocus();
                                return;
                            } catch (FirebaseNetworkException e) {
                                errorMessage = "Network error. Check your connection.";
                            } catch (Exception e) {
                                errorMessage = "Error: " + e.getMessage();
                            }
                        }

                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
            btnRegister.setText("Creating Account...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            btnRegister.setText("CREATE ACCOUNT");
        }
    }

    private String formatPhoneNumber(String input) {
        // Remove all spaces, dashes, and parentheses
        String cleaned = input.replaceAll("[\\s\\-\\(\\)]", "");

        // check if it is empty
        if (cleaned.isEmpty()) return "";

        // If it starts with '0', replace with Country Code
        if (cleaned.startsWith("0")) {
            return "+60" + cleaned.substring(1);
        }

        // add + if missing
        if (!cleaned.startsWith("+")) {
            return "+" + cleaned;
        }

        return cleaned;
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
                    setLoading(false);
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data", e);
                    Toast.makeText(SignUpActivity.this, "Account created, but failed to save details.", Toast.LENGTH_LONG).show();
                    setLoading(false);
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