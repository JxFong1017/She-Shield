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

/**
 * Handles new user registration for the She-Shield application.
 * Integrates with Firebase Authentication to create credentials and store user names.
 */
public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    // UI Elements
    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize UI Views
        initializeUI();

        // 3. Set Up Click Listeners
        btnRegister.setOnClickListener(v -> handleRegistration());

        tvBackToLogin.setOnClickListener(v -> {
            // Return to the previous Login screen
            finish();
        });
    }

    private void initializeUI() {
        tilFullName = findViewById(R.id.TIL_fullName);
        tilEmail = findViewById(R.id.TIL_signupEmail);
        tilPassword = findViewById(R.id.TIL_signupPassword);
        tilConfirmPassword = findViewById(R.id.TIL_confirmPassword);

        etFullName = findViewById(R.id.ET_fullName);
        etEmail = findViewById(R.id.ET_signupEmail);
        etPassword = findViewById(R.id.ET_signupPassword);
        etConfirmPassword = findViewById(R.id.ET_confirmPassword);

        btnRegister = findViewById(R.id.BTRegister);
        tvBackToLogin = findViewById(R.id.TVBackToLogin);
    }

    private void handleRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
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

        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        } else { tilPassword.setError(null); }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return;
        } else { tilConfirmPassword.setError(null); }

        // 2. Create User in Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // 3. Update User Profile with Display Name
                        updateUserProfile(user, fullName);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(SignUpActivity.this, "Authentication failed: " +
                                errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, String fullName) {
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                            // 4. Navigate to Home/Main Activity and clear backstack
                            Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            // Even if profile update fails, account is created, so navigate home
                            Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
        }
    }
}