package com.example.grpassignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

// Material Design Imports
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// Firebase Authentication Imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity for handling user login in the "She-Shield" application.
 * Supports standard email/password login.
 */
public class LogInPage extends AppCompatActivity {

    private static final String TAG = "LogInPage";

    // UI Elements
    private TextInputEditText etUsername, etPassword;
    private TextInputLayout tilUsername, tilPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp;

    // Firebase Variables
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize UI components from XML
        initializeUI();

        // 3. Set up click listeners
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in; if so, skip login
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity(currentUser);
        }
    }

    private void initializeUI() {
        tilUsername = findViewById(R.id.TIL_username);
        tilPassword = findViewById(R.id.TIL_password);
        etUsername = findViewById(R.id.ET_username);
        etPassword = findViewById(R.id.ET_password);
        btnLogin = findViewById(R.id.BTlogin);
        tvForgotPassword = findViewById(R.id.forgotpw);
        tvSignUp = findViewById(R.id.TVSignUp);
    }

    private void setupListeners() {
        // Standard Email/Password Login
        btnLogin.setOnClickListener(v -> attemptStandardLogin());

        // FIXED: Navigate to SignUpActivity
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LogInPage.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Placeholder for Forgot Password (could lead to a ResetPasswordActivity)
        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Reset link sent to your email (Demo)", Toast.LENGTH_SHORT).show());
    }

    private void attemptStandardLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Basic Validation
        if (email.isEmpty()) {
            tilUsername.setError("Email is required");
            return;
        } else {
            tilUsername.setError(null);
        }

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            return;
        } else {
            tilPassword.setError(null);
        }

        // Firebase Sign-In with Email
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMainActivity(mAuth.getCurrentUser());
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Redirects the user to the HomeActivity after successful authentication.
     */
    private void navigateToMainActivity(FirebaseUser user) {
        if (user != null) {
            String name = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
            Toast.makeText(this, "Welcome to She-Shield, " + name, Toast.LENGTH_SHORT).show();

            // Direct navigation to HomeActivity
            Intent intent = new Intent(LogInPage.this, HomeActivity.class);
            // Flags ensure that pressing 'Back' doesn't return the user to the login screen
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}