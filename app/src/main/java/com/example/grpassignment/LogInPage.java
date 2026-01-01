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

<<<<<<< HEAD
=======
// Material Design Imports
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// Firebase Imports (These resolve your 'auth' and 'FirebaseUser' errors)
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

>>>>>>> 31f9e94 (set up login)
public class LogInPage extends AppCompatActivity {

    private static final String TAG = "LogInPage";
    private static final int RC_SIGN_IN = 9001;

<<<<<<< HEAD
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
=======
    // UI Elements
    private TextInputEditText etUsername, etPassword;
    private TextInputLayout tilUsername, tilPassword;
>>>>>>> 31f9e94 (set up login)
    private Button btnLogin;
    private SignInButton btnGoogleLogin;
    private TextView tvForgotPassword, tvSignUp;

<<<<<<< HEAD
=======
    // Firebase & Google (Resolves 'auth' variable error)
    private FirebaseAuth mAuth;
>>>>>>> 31f9e94 (set up login)
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

<<<<<<< HEAD
=======
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

>>>>>>> 31f9e94 (set up login)
        initializeUI();
        configureGoogleSignIn();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in (Firebase level)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity(currentUser);
        }
    }

    private void initializeUI() {
<<<<<<< HEAD
=======
        tilUsername = findViewById(R.id.TIL_username);
        tilPassword = findViewById(R.id.TIL_password);
>>>>>>> 31f9e94 (set up login)
        etUsername = findViewById(R.id.ET_username);
        etPassword = findViewById(R.id.ET_password);
        btnLogin = findViewById(R.id.BTlogin);
        btnGoogleLogin = findViewById(R.id.BTGoogleLogin);
        tvForgotPassword = findViewById(R.id.forgotpw);
        tvSignUp = findViewById(R.id.TVSignUp);
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
<<<<<<< HEAD
=======

>>>>>>> 31f9e94 (set up login)
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogleLogin.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptStandardLogin());
<<<<<<< HEAD
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        tvForgotPassword.setOnClickListener(v -> Toast.makeText(LogInPage.this, "Opening Forgot Password...", Toast.LENGTH_SHORT).show());
        tvSignUp.setOnClickListener(v -> Toast.makeText(LogInPage.this, "Opening Sign Up Page...", Toast.LENGTH_SHORT).show());
=======

        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        tvSignUp.setOnClickListener(v -> {
            // Redirect to sign up activity if you have one
            Toast.makeText(this, "Redirecting to Sign Up...", Toast.LENGTH_SHORT).show();
        });
>>>>>>> 31f9e94 (set up login)
    }

    private void attemptStandardLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            tilUsername.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            return;
        }

<<<<<<< HEAD
        Log.d(TAG, "Attempting login for: " + username);
        Toast.makeText(this, "Standard Login successful! (Demo)", Toast.LENGTH_SHORT).show();

        // Navigate to the new HomeActivity
        navigateToHome();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

=======
        // Firebase Sign-In logic
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMainActivity(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

>>>>>>> 31f9e94 (set up login)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

<<<<<<< HEAD
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Google Sign-In successful. Display Name: " + account.getDisplayName());
            Toast.makeText(this, "Signed in with Google as " + account.getDisplayName(), Toast.LENGTH_LONG).show();

            // Navigate to the new HomeActivity
            navigateToHome();

        } catch (ApiException e) {
            Log.w(TAG, "Google Sign-In failed, status=" + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed. Error: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
=======
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMainActivity(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Google Firebase Auth Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // This resolves the 'Cannot resolve method navigateToMainActivity' error
    private void navigateToMainActivity(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
            // TODO: Uncomment these lines when your MainActivity is ready
            // Intent intent = new Intent(LogInPage.this, MainActivity.class);
            // startActivity(intent);
            // finish();
>>>>>>> 31f9e94 (set up login)
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(LogInPage.this, HomeActivity.class);
        startActivity(intent);
        finish(); // Prevents user from going back to the login page
    }
}
