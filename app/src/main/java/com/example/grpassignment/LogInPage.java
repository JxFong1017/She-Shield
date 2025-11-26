package com.example.grpassignment;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Activity for handling user login in the "She-Shield" application.
 * Supports standard username/password login and Google Sign-In.
 */
public class LogInPage extends AppCompatActivity {

    private static final String TAG = "LogInPage";
    // Unique request code for Google Sign-In intent
    private static final int RC_SIGN_IN = 9001;

    // UI Elements
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private SignInButton btnGoogleLogin;
    private TextView tvForgotPassword;
    private TextView tvSignUp;

    // Google Sign-In Client
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout file to the one you provided/modified
        setContentView(R.layout.activity_login);

        // 1. Initialize UI components from the XML layout
        initializeUI();

        // 2. Configure Google Sign-In
        configureGoogleSignIn();

        // 3. Set up click listeners for all interactive elements
        setupListeners();
    }

    private void initializeUI() {
        // Standard Login Fields
        etUsername = findViewById(R.id.ET_username);
        etPassword = findViewById(R.id.ET_password);
        btnLogin = findViewById(R.id.BTlogin);

        // Google Sign-In Button (from Google Play Services)
        btnGoogleLogin = findViewById(R.id.BTGoogleLogin);

        // Links
        tvForgotPassword = findViewById(R.id.forgotpw);
        tvSignUp = findViewById(R.id.TVSignUp);
    }

    private void configureGoogleSignIn() {
        // Request only the user's ID and basic profile information.
        // If you are using Firebase Authentication or need server-side access,
        // you would request the ID token by using .requestIdToken(getString(R.string.default_web_client_id))
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Optional: Customize the Google Sign In Button
        btnGoogleLogin.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupListeners() {
        // --- 1. Standard Login Button Listener ---
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptStandardLogin();
            }
        });

        // --- 2. Google Sign-In Button Listener ---
        btnGoogleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        // --- 3. Forgot Password Link Listener ---
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement navigation to a Forgot Password screen
                Toast.makeText(LogInPage.this, "Opening Forgot Password...", Toast.LENGTH_SHORT).show();
            }
        });

        // --- 4. Sign Up Link Listener ---
        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement navigation to a Registration/Sign Up screen
                Toast.makeText(LogInPage.this, "Opening Sign Up Page...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Attempts to log in using the provided username and password.
     */
    private void attemptStandardLogin() {
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_LONG).show();
            return;
        }

        // TODO: Implement your actual authentication logic here (e.g., API call, database check)
        // For demonstration, we just show a success message:
        Log.d(TAG, "Attempting login for: " + username);
        Toast.makeText(this, "Standard Login successful! (Demo)", Toast.LENGTH_SHORT).show();

        // Example: Navigate to the main application activity
        // Intent intent = new Intent(LogInPage.this, MainActivity.class);
        // startActivity(intent);
        // finish();
    }

    /**
     * Starts the Google Sign-In flow by launching the sign-in intent.
     */
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // --- Handling Google Sign-In Result ---

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the result is from the Google Sign-In request
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from GoogleSignIn.getSignedInAccountFromIntent() is always completed,
            // but we must check for errors.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    /**
     * Processes the result of the Google Sign-In task.
     */
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI and navigate
            Log.d(TAG, "Google Sign-In successful. Display Name: " + account.getDisplayName());
            Toast.makeText(this, "Signed in with Google as " + account.getDisplayName(), Toast.LENGTH_LONG).show();

            // TODO: Use the account information (ID, ID Token, Email) to authenticate with your backend/Firebase.
            // String idToken = account.getIdToken();
            // authenticateWithBackend(idToken);

            // Example: Navigate to the main application activity
            // Intent intent = new Intent(LogInPage.this, MainActivity.class);
            // startActivity(intent);
            // finish();

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w(TAG, "Google Sign-In failed, status=" + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed. Error: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            // Handle specific status codes, like CANCELED (12501), NETWORK_ERROR (7), etc.
        }
    }
}