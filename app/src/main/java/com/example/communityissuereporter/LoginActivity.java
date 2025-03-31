package com.example.communityissuereporter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.communityissuereporter.api.AuthResponse;
import com.example.communityissuereporter.api.GoogleAuthRequest;
import com.example.communityissuereporter.api.LoginRequest;
import com.example.communityissuereporter.api.RetrofitClient;
import com.example.communityissuereporter.utils.ThemeManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";
    
    private EditText emailEditText, passwordEditText;
    private Button loginButton, googleSignInButton;
    private TextView signUpText;
    private CircularProgressIndicator progressIndicator;
    private GoogleSignInClient mGoogleSignInClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate() or setContentView()
        ThemeManager.applyTheme(this);
        
        super.onCreate(savedInstanceState);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        // Initialize Google Sign In with web client ID from Firebase
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check for existing Google Sign In account
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            // User is already signed in, get a fresh token
            account.getIdToken();
        }

        // Initialize views
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        signUpText = findViewById(R.id.sign_up_text);
        progressIndicator = findViewById(R.id.progress_indicator);

        // Before setting up click listeners, ensure Google Play Services is available
        int playServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (playServicesAvailable == ConnectionResult.SUCCESS) {
            // Google Play Services is available, set up click listeners
            googleSignInButton.setEnabled(true);
            
            // Google sign-in button click listener
            googleSignInButton.setOnClickListener(v -> {
                showLoading(true);
                signInWithGoogle();
            });
        } else {
            // Google Play Services is not available
            googleSignInButton.setEnabled(false);
            GoogleApiAvailability.getInstance().getErrorDialog(this, playServicesAvailable, RC_SIGN_IN,
                dialog -> {
                    // Dialog dismissed without resolving
                    googleSignInButton.setEnabled(false);
                    Toast.makeText(this, "Google Sign In requires Google Play Services", Toast.LENGTH_LONG).show();
                }).show();
        }

        // Login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (validateInput(email, password)) {
                showLoading(true);
                performLogin(email, password);
            }
        });

        // Sign-up text click listener
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin(String email, String password) {
        showLoading(true);
        RetrofitClient.getInstance().getApiService().login(new LoginRequest(email, password))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        Log.d(TAG, "Login response code: " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResponse = response.body();
                            
                            // Get token, user ID, and username
                            String token = authResponse.getToken();
                            String uid = authResponse.getUid();
                            String username = authResponse.getUsername();
                            
                            // Log the authentication response
                            Log.d(TAG, "Login successful - token: " + 
                                (token != null && !token.isEmpty() ? "present" : "missing") + 
                                ", uid: " + (uid != null ? uid : "null") +
                                ", username: " + (username != null ? username : "null"));
                            
                            // Ensure we have valid data before saving
                            if (token == null || token.isEmpty() || uid == null || uid.isEmpty()) {
                                final String errorMsg = "Authentication error: Missing token or user ID";
                                Log.e(TAG, errorMsg);
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                });
                                return;
                            }
                            
                            // Save user details and set logged in state
                            sessionManager.saveUserDetails(
                                username,
                                email,  // Now using the email from login form instead of empty string
                                token,
                                uid
                            );
                            
                            // Ensure the API client is initialized with the updated session
                            IssueReporterApplication app = (IssueReporterApplication) getApplication();
                            app.ensureProperSessionManagement();
                            
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                navigateToMain();
                            });
                        } else {
                            String errorMsg = "Login failed. ";
                            
                            // Extract error message from response body
                            if (response.errorBody() != null) {
                                try {
                                    String errorBodyString = response.errorBody().string();
                                    Log.e(TAG, "Error body: " + errorBodyString);
                                    errorMsg += errorBodyString;
                                } catch (IOException e) {
                                    errorMsg += "Error code: " + response.code();
                                    Log.e(TAG, "Error reading error body", e);
                                }
                            } else {
                                errorMsg += "Error code: " + response.code();
                            }
                            
                            Log.e(TAG, errorMsg);
                            final String finalErrorMsg = errorMsg;
                            
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(LoginActivity.this, finalErrorMsg, Toast.LENGTH_LONG).show();
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
                        
                        Log.e(TAG, "Network error", t);
                        final String finalErrorMessage = errorMessage;
                        
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(LoginActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();
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
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to get Google ID token. Please try again.", Toast.LENGTH_SHORT).show();
                    // Sign out to ensure a clean state
                    mGoogleSignInClient.signOut();
                });
            }
        } catch (ApiException e) {
            runOnUiThread(() -> {
                showLoading(false);
                String errorMessage;
                boolean showPlayServicesDialog = false;
                
                switch (e.getStatusCode()) {
                    case 12500: // SIGN_IN_FAILED
                        errorMessage = "Google Play Services needs to be updated";
                        showPlayServicesDialog = true;
                        break;
                    case 12501: // SIGN_IN_CANCELLED
                        errorMessage = "Google Sign In was cancelled";
                        break;
                    case 12502: // SIGN_IN_CURRENTLY_IN_PROGRESS
                        errorMessage = "Google Sign In is already in progress";
                        break;
                    case 7: // NETWORK_ERROR
                        errorMessage = "Network error occurred. Please check your internet connection";
                        break;
                    case 8: // INTERNAL_ERROR
                        errorMessage = "Internal error occurred. Please try again";
                        showPlayServicesDialog = true;
                        break;
                    case 5: // INVALID_ACCOUNT
                        errorMessage = "Please choose a valid Google account";
                        break;
                    case 13: // ERROR
                        errorMessage = "Error occurred during Google Sign In. Please make sure Google Play Services is up to date";
                        showPlayServicesDialog = true;
                        break;
                    default:
                        errorMessage = "Google Sign In failed (code: " + e.getStatusCode() + ")";
                        showPlayServicesDialog = true;
                }
                
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                
                if (showPlayServicesDialog) {
                    android.app.Dialog errorDialog = GoogleApiAvailability.getInstance()
                        .getErrorDialog(this, e.getStatusCode(), RC_SIGN_IN,
                            dialog -> {
                                // Dialog dismissed without resolving
                                Toast.makeText(this, 
                                    "Please update Google Play Services and try again", 
                                    Toast.LENGTH_LONG).show();
                            });
                    if (errorDialog != null) {
                        errorDialog.show();
                    }
                }
                
                // Sign out to ensure a clean state
                mGoogleSignInClient.signOut();
            });
        }
    }

    private void authenticateWithBackend(String idToken, GoogleSignInAccount account) {
        showLoading(true);
        
        // Validate inputs to prevent crashes
        if (idToken == null || idToken.isEmpty()) {
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, "Authentication failed: Invalid token", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        if (account == null) {
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, "Authentication failed: Account information missing", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        try {
            GoogleAuthRequest request = new GoogleAuthRequest(
                idToken,
                account.getEmail()
            );
            
            // Log the request for debugging
            Log.d(TAG, "Google Auth Request - Email: " + request.getEmail());
            Log.d(TAG, "Google Auth Request - Token length: " + (request.getIdToken() != null ? request.getIdToken().length() : 0));
            
            RetrofitClient.getInstance().getApiService().googleAuth(request)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        Log.d(TAG, "Google Auth Response Code: " + response.code());
                        
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                AuthResponse authResponse = response.body();
                                
                                // Get token, user ID, and username
                                String token = authResponse.getToken();
                                String uid = authResponse.getUid();
                                String username = authResponse.getUsername();
                                
                                // Log the authentication response
                                Log.d(TAG, "Login successful - token: " + 
                                    (token != null && !token.isEmpty() ? "present" : "missing") + 
                                    ", uid: " + (uid != null ? uid : "null") +
                                    ", username: " + (username != null ? username : "null"));
                                
                                // Ensure we have valid data before saving
                                if (token == null || token.isEmpty() || uid == null || uid.isEmpty()) {
                                    Log.e(TAG, "Missing token or user ID in successful response");
                                    runOnUiThread(() -> {
                                        showLoading(false);
                                        Toast.makeText(LoginActivity.this, 
                                            "Authentication error: Missing token or user ID", 
                                            Toast.LENGTH_LONG).show();
                                    });
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
                                
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(LoginActivity.this, "Google sign-in successful!", Toast.LENGTH_SHORT).show();
                                    navigateToMain();
                                });
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
                                    Log.e(TAG, "Error reading error body", e);
                                }
                                
                                Log.e(TAG, errorMessage);
                                final String finalErrorMessage = errorMessage;
                                
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(LoginActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();
                                    
                                    // Sign out from Google to allow retry
                                    mGoogleSignInClient.signOut();
                                });
                            }
                        } catch (Exception e) {
                            // Catch any unexpected exceptions during response processing
                            Log.e(TAG, "Error processing response", e);
                            final String errorMessage = "Authentication error: " + e.getMessage();
                            
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                
                                // Sign out from Google to allow retry
                                mGoogleSignInClient.signOut();
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
                        Log.e(TAG, "Network error", t);
                        final String finalErrorMessage = errorMessage;
                        
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(LoginActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();
                            
                            // Sign out from Google to allow retry
                            mGoogleSignInClient.signOut();
                        });
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception during request creation", e);
            final String errorMessage = "Error: " + e.getMessage();
            
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                
                // Sign out from Google to allow retry
                mGoogleSignInClient.signOut();
            });
        }
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            emailEditText.setError("Email required");
            emailEditText.requestFocus();
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
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
            loginButton.setEnabled(!show);
            googleSignInButton.setEnabled(!show);
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}