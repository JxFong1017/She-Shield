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

public class LogInPage extends AppCompatActivity {

    private static final String TAG = "LogInPage";
    private static final int RC_SIGN_IN = 9001;

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private SignInButton btnGoogleLogin;
    private TextView tvForgotPassword;
    private TextView tvSignUp;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeUI();
        configureGoogleSignIn();
        setupListeners();
    }

    private void initializeUI() {
        etUsername = findViewById(R.id.ET_username);
        etPassword = findViewById(R.id.ET_password);
        btnLogin = findViewById(R.id.BTlogin);
        btnGoogleLogin = findViewById(R.id.BTGoogleLogin);
        tvForgotPassword = findViewById(R.id.forgotpw);
        tvSignUp = findViewById(R.id.TVSignUp);
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogleLogin.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptStandardLogin());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        tvForgotPassword.setOnClickListener(v -> Toast.makeText(LogInPage.this, "Opening Forgot Password...", Toast.LENGTH_SHORT).show());
        tvSignUp.setOnClickListener(v -> Toast.makeText(LogInPage.this, "Opening Sign Up Page...", Toast.LENGTH_SHORT).show());
    }

    private void attemptStandardLogin() {
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Attempting login for: " + username);
        Toast.makeText(this, "Standard Login successful! (Demo)", Toast.LENGTH_SHORT).show();

        // Navigate to the new HomeActivity
        navigateToHome();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

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
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(LogInPage.this, HomeActivity.class);
        startActivity(intent);
        finish(); // Prevents user from going back to the login page
    }
}
