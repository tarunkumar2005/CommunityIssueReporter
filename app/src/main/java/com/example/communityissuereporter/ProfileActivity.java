package com.example.communityissuereporter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.communityissuereporter.api.ApiClient;
import com.example.communityissuereporter.api.ApiResponse;
import com.example.communityissuereporter.api.UserProfileRequest;
import com.example.communityissuereporter.model.User;
import com.example.communityissuereporter.utils.DateUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    // Google Sign-In client for logout
    private GoogleSignInClient mGoogleSignInClient;
    
    // Session manager
    private SessionManager sessionManager;

    // UI components
    private TextView nameText, usernameText, joinedDateText, emailText;
    private TextView issuesReportedCount, resolvedIssuesCount;
    private TextView resolutionRateText;
    private ProgressBar resolutionRateProgress;
    private SwitchMaterial newIssuesSwitch;
    private MaterialButton saveNotificationsButton;
    private MaterialButton logoutButton;
    private FloatingActionButton editProfileButton;
    private ProgressBar progressBar;

    // User data
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize SessionManager
        sessionManager = new SessionManager(getApplicationContext());
        
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize views
        initializeViews();
        
        // Set up toolbar
        setupToolbar();

        // Load user profile
        loadUserProfile();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);

        // Profile header
        nameText = findViewById(R.id.nameText);
        usernameText = findViewById(R.id.usernameText);
        joinedDateText = findViewById(R.id.joinedDateText);

        // Stats
        issuesReportedCount = findViewById(R.id.issuesReportedCount);
        resolvedIssuesCount = findViewById(R.id.resolvedIssuesCount);
        resolutionRateText = findViewById(R.id.resolutionRateText);
        resolutionRateProgress = findViewById(R.id.resolutionRateProgress);

        // Contact info
        emailText = findViewById(R.id.emailText);

        // Notification preferences
        newIssuesSwitch = findViewById(R.id.newIssuesSwitch);
        saveNotificationsButton = findViewById(R.id.saveNotificationsButton);

        // Account actions
        logoutButton = findViewById(R.id.logoutButton);
        
        // Edit profile button
        editProfileButton = findViewById(R.id.editProfileButton);

        // Progress indicator
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void loadUserProfile() {
        showProgress(true);
        
        if (!sessionManager.isLoggedIn()) {
            showToast("No user signed in");
            navigateToLogin();
            return;
        }

        String userId = sessionManager.getUserId();
        
        ApiClient.getClient().getUserProfile(userId).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                showProgress(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentUser = response.body().getData();
                    updateUI();
                } else {
                    // Handle API error
                    String errorMessage = "Failed to load profile";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string();
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                showProgress(false);
                Log.e(TAG, "Error loading user profile", t);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void updateUI() {
        if (currentUser == null) return;

        // Set user name and username
        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            nameText.setText(displayName);
        } else {
            nameText.setText(currentUser.getUsername());
        }
        
        usernameText.setText("@" + currentUser.getUsername());
        
        // Set joined date
        if (currentUser.getAccountCreationDate() > 0) {
            String formattedDate = DateUtils.formatDateToString(new Date(currentUser.getAccountCreationDate()), "MMMM yyyy");
            joinedDateText.setText("Joined " + formattedDate);
        }
        
        // Set contact info
        emailText.setText(currentUser.getEmail());
        
        // Set stats
        if (currentUser.getStats() != null) {
            int reported = currentUser.getStats().getIssuesReported();
            int resolved = currentUser.getStats().getResolvedIssues();
            
            issuesReportedCount.setText(String.valueOf(reported));
            resolvedIssuesCount.setText(String.valueOf(resolved));
            
            // Calculate and update resolution rate
            updateResolutionRate(reported, resolved);
        }
        
        // Set notification preferences
        if (currentUser.getNotificationPreferences() != null) {
            Boolean notificationEnabled = currentUser.getNotificationPreferences().get("ownIssues");
            newIssuesSwitch.setChecked(notificationEnabled != null ? notificationEnabled : true);
        }
    }
    
    private void updateResolutionRate(int reported, int resolved) {
        int rate = 0;
        if (reported > 0) {
            rate = (int) (((float) resolved / reported) * 100);
        }
        
        resolutionRateText.setText(rate + "%");
        resolutionRateProgress.setProgress(rate);
    }

    private void setupClickListeners() {
        // Save notification preferences
        saveNotificationsButton.setOnClickListener(v -> saveNotificationPreferences());
        
        // Logout button
        logoutButton.setOnClickListener(v -> logout());
        
        // Edit profile button
        editProfileButton.setOnClickListener(v -> showEditProfileDialog());
    }
    
    private void showEditProfileDialog() {
        if (currentUser == null) {
            showError("Cannot edit profile. Profile not loaded.");
            return;
        }
        
        // Create dialog with custom layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle("Edit Profile");
        
        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        
        // Get references to form fields
        TextInputLayout displayNameInput = dialogView.findViewById(R.id.displayNameInput);
        EditText displayNameEditText = displayNameInput.getEditText();
        
        // Set current values
        if (displayNameEditText != null) {
            String currentName = currentUser.getDisplayName();
            if (currentName == null || currentName.isEmpty()) {
                currentName = currentUser.getUsername();
            }
            displayNameEditText.setText(currentName);
        }
        
        builder.setView(dialogView);
        
        // Add buttons
        builder.setPositiveButton("Save", null); // Set click listener later to prevent auto-dismiss
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        // Create and show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Set positive button listener after dialog is shown to prevent auto-dismiss on validation errors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (displayNameEditText != null && displayNameEditText.getText().toString().trim().isEmpty()) {
                displayNameInput.setError("Display name cannot be empty");
                return;
            }
            
            // Get updated values
            String newDisplayName = displayNameEditText != null ? displayNameEditText.getText().toString().trim() : "";
            
            // Update profile
            updateProfile(newDisplayName);
            
            // Dismiss dialog
            dialog.dismiss();
        });
    }
    
    private void updateProfile(String displayName) {
        if (currentUser == null) return;
        
        showProgress(true);
        
        // Create request
        UserProfileRequest request = new UserProfileRequest();
        request.setDisplayName(displayName);
        
        // Call API
        String userId = sessionManager.getUserId();
        
        ApiClient.getClient().updateUserProfile(userId, request).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                showProgress(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    // Update local user object
                    currentUser = response.body().getData();
                    
                    // Update UI
                    updateUI();
                    
                    // Show success message
                    showSuccess("Profile updated successfully");
                } else {
                    // Handle API error
                    String errorMessage = "Failed to update profile";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string();
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    showError(errorMessage);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                showProgress(false);
                Log.e(TAG, "Error updating profile", t);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void saveNotificationPreferences() {
        showProgress(true);
        
        if (currentUser == null) {
            showError("Cannot save preferences. Profile not loaded.");
            showProgress(false);
            return;
        }
        
        // Create notification preferences map
        Map<String, Boolean> preferences = new HashMap<>();
        preferences.put("ownIssues", newIssuesSwitch.isChecked());
        
        String userId = sessionManager.getUserId();
        
        ApiClient.getClient().updateNotificationPreferences(userId, preferences)
            .enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    showProgress(false);
                    
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        currentUser = response.body().getData();
                        showSuccess("Notification preferences saved");
                    } else {
                        String errorMessage = "Failed to save preferences";
                        if (response.errorBody() != null) {
                            try {
                                errorMessage = response.errorBody().string();
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                        }
                        showError(errorMessage);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    showProgress(false);
                    Log.e(TAG, "Error saving notification preferences", t);
                    showError("Network error: " + t.getMessage());
                }
            });
    }

    private void logout() {
        showProgress(true);
        
        // Sign out from Google
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // Clear session
                sessionManager.logout();
                
                showProgress(false);
                navigateToLogin();
            });
        } else {
            // Clear session
            sessionManager.logout();
            
            showProgress(false);
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        
        // Disable buttons during loading
        if (saveNotificationsButton != null) {
            saveNotificationsButton.setEnabled(!show);
        }
        
        if (logoutButton != null) {
            logoutButton.setEnabled(!show);
        }
        
        if (editProfileButton != null) {
            editProfileButton.setEnabled(!show);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getResources().getColor(R.color.colorError))
            .setTextColor(getResources().getColor(android.R.color.white))
            .show();
    }
    
    private void showSuccess(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(getResources().getColor(R.color.colorPrimary))
            .setTextColor(getResources().getColor(android.R.color.white))
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
