package com.example.communityissuereporter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.communityissuereporter.api.AuthResponse;
import com.example.communityissuereporter.api.GoogleAuthRequest;
import com.example.communityissuereporter.api.RegisterRequest;
import com.example.communityissuereporter.api.RetrofitClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.example.communityissuereporter.utils.ThemeManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

public class SignUpActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;

    private EditText displayNameEditText, emailEditText, passwordEditText;
    private Button signUpButton, googleSignUpButton;
    private TextView loginText;
    private CircularProgressIndicator progressIndicator;
    private GoogleSignInClient mGoogleSignInClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate() or setContentView()
        ThemeManager.applyTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        // Initialize Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize views
        displayNameEditText = findViewById(R.id.display_name);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        signUpButton = findViewById(R.id.sign_up_button);
        googleSignUpButton = findViewById(R.id.google_sign_up_button);
        loginText = findViewById(R.id.login_text);
        progressIndicator = findViewById(R.id.progress_indicator);

        // Sign Up button click listener
        signUpButton.setOnClickListener(v -> {
            String displayName = displayNameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (validateInput(displayName, email, password)) {
                showLoading(true);
                performSignUp(displayName, email, password);
            }
        });

        // Google sign-up button click listener
        googleSignUpButton.setOnClickListener(v -> signInWithGoogle());

        // Login text click listener
        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void performSignUp(String displayName, String email, String password) {
        // Show loading indicator
        showLoading(true);
        
        // Log the request parameters for debugging
        android.util.Log.d("SignUp", "Attempting registration with name: " + displayName + ", email: " + email);
        
        // Create the request with the correct field names (name instead of displayName)
        RegisterRequest request = new RegisterRequest(displayName, email, password);
        
        // Execute the request on a background thread
        RetrofitClient.getInstance().getApiService().register(request)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        android.util.Log.d("SignUp", "Response code: " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResponse = response.body();
                            
                            // Get token, user ID, and username
                            String token = authResponse.getToken();
                            String uid = authResponse.getUid();
                            String username = authResponse.getUsername();
                            
                            // Log the authentication response
                            android.util.Log.d("SignUp", "Registration successful - token: " + 
                                (token != null && !token.isEmpty() ? "present" : "missing") + 
                                ", uid: " + (uid != null ? uid : "null") +
                                ", username: " + (username != null ? username : "null"));
                            
                            // Ensure we have valid data before saving
                            if (token == null || token.isEmpty() || uid == null || uid.isEmpty()) {
                                final String errorMsg = "Registration error: Missing token or user ID";
                                android.util.Log.e("SignUp", errorMsg);
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(SignUpActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                });
                                return;
                            }
                            
                            // Save user details and set logged in state
                            sessionManager.saveUserDetails(
                                username != null ? username : displayNameEditText.getText().toString(),
                                emailEditText.getText().toString(),
                                token,
                                uid
                            );
                            
                            // Ensure the API client is initialized with the updated session
                            IssueReporterApplication app = (IssueReporterApplication) getApplication();
                            app.ensureProperSessionManagement();
                            
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(SignUpActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                navigateToMain();
                            });
                        } else {
                            String errorMsg = "Registration failed. ";
                            
                            // Extract error message from response body
                            if (response.errorBody() != null) {
                                try {
                                    String errorBodyString = response.errorBody().string();
                                    android.util.Log.e("SignUp", "Error body: " + errorBodyString);
                                    errorMsg += errorBodyString;
                                } catch (IOException e) {
                                    errorMsg += "Error code: " + response.code();
                                    android.util.Log.e("SignUp", "Error reading error body", e);
                                }
                            } else {
                                errorMsg += "Error code: " + response.code();
                            }
                            
                            android.util.Log.e("SignUp", errorMsg);
                            final String finalErrorMsg = errorMsg;
                            
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(SignUpActivity.this, finalErrorMsg, Toast.LENGTH_LONG).show();
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        String errorMessage = "Network error: ";
                        if (t instanceof UnknownHostException) {
                            errorMessage += "Cannot connect to server. Please check your internet connection.";
                        } else if (t instanceof SocketTimeoutException) {
                            errorMessage += "Connection timed out. Please try again.";
                        } else if (t instanceof SSLHandshakeException) {
                            errorMessage += "Secure connection failed. Please check your internet connection.";
                        } else {
                            errorMessage += t.getMessage();
                        }
                        
                        android.util.Log.e("SignUp", "Network error", t);
                        final String finalErrorMessage = errorMessage;
                        
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(SignUpActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                });
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
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken != null) {
                authenticateWithBackend(idToken, account);
            } else {
                Toast.makeText(this, "Failed to get Google ID token", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            String errorMessage;
            switch (e.getStatusCode()) {
                case 12500: // SIGN_IN_FAILED
                    errorMessage = "Google Play Services update is required";
                    break;
                case 12501: // SIGN_IN_CANCELLED
                    errorMessage = "Sign in was cancelled";
                    break;
                case 12502: // SIGN_IN_CURRENTLY_IN_PROGRESS
                    errorMessage = "Sign in is already in progress";
                    break;
                default:
                    errorMessage = "Google sign in failed: " + e.getStatusCode();
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }

    private void authenticateWithBackend(String idToken, GoogleSignInAccount account) {
        showLoading(true);
        
        // Validate inputs to prevent crashes
        if (idToken == null || idToken.isEmpty()) {
            showLoading(false);
            Toast.makeText(this, "Authentication failed: Invalid token", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (account == null) {
            showLoading(false);
            Toast.makeText(this, "Authentication failed: Account information missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Log the account information for debugging
        android.util.Log.d("GoogleAuth", "Account Email: " + account.getEmail());
        android.util.Log.d("GoogleAuth", "Account Display Name: " + account.getDisplayName());
        
        try {
            // Create the request with just idToken and email as per backend requirements
            GoogleAuthRequest request = new GoogleAuthRequest(
                idToken,
                account.getEmail()
            );
            
            // Log the request object
            android.util.Log.d("GoogleAuth", "Request Object - ID Token: " + request.getIdToken());
            android.util.Log.d("GoogleAuth", "Request Object - Email: " + request.getEmail());
            
            // Debug log for backend URL
            android.util.Log.d("GoogleAuth", "Backend URL: " + RetrofitClient.getInstance().getBaseUrl());
            
            // Use a try-catch block to handle any unexpected exceptions
            try {
                RetrofitClient.getInstance().getApiService().googleAuth(request)
                    .enqueue(new Callback<AuthResponse>() {
                        @Override
                        public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                            showLoading(false);
                            
                            // Log the response
                            android.util.Log.d("GoogleAuth", "Response code: " + response.code());
                            
                            try {
                                if (response.isSuccessful() && response.body() != null) {
                                    android.util.Log.d("GoogleAuth", "Response is successful");
                                    
                                    AuthResponse authResponse = response.body();
                                    
                                    // Get token, user ID, and username
                                    String token = authResponse.getToken();
                                    String uid = authResponse.getUid();
                                    String username = authResponse.getUsername();
                                    
                                    // Log the authentication response
                                    android.util.Log.d("GoogleAuth", "Login successful - token: " + 
                                        (token != null && !token.isEmpty() ? "present" : "missing") + 
                                        ", uid: " + (uid != null ? uid : "null") +
                                        ", username: " + (username != null ? username : "null"));
                                    
                                    // Ensure we have valid data before saving
                                    if (token == null || token.isEmpty() || uid == null || uid.isEmpty()) {
                                        android.util.Log.e("GoogleAuth", "Missing token or user ID in successful response");
                                        Toast.makeText(SignUpActivity.this, 
                                            "Authentication error: Missing token or user ID", 
                                            Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    
                                    // Save user details and set logged in state
                                    sessionManager.saveUserDetails(
                                        username != null ? username : account.getDisplayName(),
                                        account.getEmail(),
                                        token,
                                        uid
                                    );
                                    
                                    // Ensure the API client is initialized with the updated session
                                    IssueReporterApplication app = (IssueReporterApplication) getApplication();
                                    app.ensureProperSessionManagement();
                                    
                                    Toast.makeText(SignUpActivity.this, "Google sign-up successful!", Toast.LENGTH_SHORT).show();
                                    navigateToMain();
                                } else {
                                    // Handle unsuccessful response
                                    String errorMessage = "Google auth failed: ";
                                    
                                    try {
                                        if (response.errorBody() != null) {
                                            errorMessage += response.errorBody().string();
                                        } else {
                                            errorMessage += "Server error (HTTP " + response.code() + ")";
                                        }
                                    } catch (Exception e) {
                                        errorMessage += "Server error (HTTP " + response.code() + ")";
                                        android.util.Log.e("GoogleAuth", "Error reading error body", e);
                                    }
                                    
                                    android.util.Log.e("GoogleAuth", errorMessage);
                                    Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                    
                                    // Sign out from Google to allow retry
                                    mGoogleSignInClient.signOut();
                                }
                            } catch (Exception e) {
                                // Catch any unexpected exceptions during response processing
                                android.util.Log.e("GoogleAuth", "Error processing response", e);
                                Toast.makeText(SignUpActivity.this, 
                                    "Authentication error: " + e.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                                
                                // Sign out from Google to allow retry
                                mGoogleSignInClient.signOut();
                            }
                        }

                        @Override
                        public void onFailure(Call<AuthResponse> call, Throwable t) {
                            showLoading(false);
                            String errorMessage = "Network error: ";
                            if (t instanceof UnknownHostException) {
                                errorMessage += "Cannot connect to server. Please check if the server is running and accessible.";
                            } else if (t instanceof SocketTimeoutException) {
                                errorMessage += "Connection timed out. Please try again.";
                            } else if (t instanceof SSLHandshakeException) {
                                errorMessage += "SSL Certificate error. Check network security configuration.";
                            } else {
                                errorMessage += t.getMessage();
                            }
                            android.util.Log.e("GoogleAuth", "Network error", t);
                            Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            
                            // Sign out from Google to allow retry
                            mGoogleSignInClient.signOut();
                        }
                    });
            } catch (Exception e) {
                showLoading(false);
                android.util.Log.e("GoogleAuth", "Exception during API call setup", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                
                // Sign out from Google to allow retry
                mGoogleSignInClient.signOut();
            }
        } catch (Exception e) {
            showLoading(false);
            android.util.Log.e("GoogleAuth", "Exception during request creation", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateInput(String displayName, String email, String password) {
        if (displayName.isEmpty()) {
            displayNameEditText.setError("Display name required");
            displayNameEditText.requestFocus();
            return false;
        }

        if (displayName.length() < 3) {
            displayNameEditText.setError("Display name must be at least 3 characters");
            displayNameEditText.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email required");
            emailEditText.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password required");
            passwordEditText.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            signUpButton.setEnabled(!show);
            googleSignUpButton.setEnabled(!show);
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}