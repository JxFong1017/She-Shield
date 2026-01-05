package com.example.grpassignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

// Google Sign-In Imports
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

// Material Design Imports
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// Firebase Authentication Imports
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Activity for handling user login in the "She-Shield" application.
 * Supports standard email/password login and Firebase-linked Google Sign-In.
 */
public class LogInPage extends AppCompatActivity {

    private static final String TAG = "LogInPage";
    private static final int RC_SIGN_IN = 9001;

    // UI Elements
    private TextInputEditText etUsername, etPassword;
    private TextInputLayout tilUsername, tilPassword;
    private Button btnLogin;
    private SignInButton btnGoogleLogin;
    private TextView tvForgotPassword, tvSignUp;

    // Firebase and Google Client Variables
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize UI components from XML
        initializeUI();

        // 3. Configure Google Sign-In options
        configureGoogleSignIn();

        // 4. Set up click listeners
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
        btnGoogleLogin = findViewById(R.id.BTGoogleLogin);
        tvForgotPassword = findViewById(R.id.forgotpw);
        tvSignUp = findViewById(R.id.TVSignUp);
    }

    private void configureGoogleSignIn() {
        // Build GoogleSignInOptions with the Web Client ID from strings.xml
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogleLogin.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupListeners() {
        // Standard Email/Password Login
        btnLogin.setOnClickListener(v -> attemptStandardLogin());

        // Google Sign-In Button
        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMainActivity(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Firebase Authentication Failed", Toast.LENGTH_SHORT).show();
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