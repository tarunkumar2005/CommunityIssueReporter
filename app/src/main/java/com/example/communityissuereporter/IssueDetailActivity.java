package com.example.communityissuereporter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.communityissuereporter.api.ApiClient;
import com.example.communityissuereporter.model.Issue;
import com.example.communityissuereporter.repository.IssueRepository;
import com.example.communityissuereporter.SessionManager;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IssueDetailActivity extends AppCompatActivity {
    private static final String TAG = "IssueDetailActivity";
    public static final String EXTRA_ISSUE_ID = "issue_id";
    public static final String EXTRA_ISSUE_TITLE = "issue_title";
    public static final String EXTRA_ISSUE_DESC = "issue_description";
    public static final String EXTRA_ISSUE_LOCATION = "issue_location";
    public static final String EXTRA_ISSUE_REPORTER = "issue_reporter";
    public static final String EXTRA_ISSUE_UPVOTES = "issue_upvotes";
    public static final String EXTRA_ISSUE_STATUS = "issue_status";
    public static final String EXTRA_ISSUE_TIMESTAMP = "issue_timestamp";
    public static final String EXTRA_ISSUE_IMAGE_URL = "issue_image_url";
    public static final String EXTRA_ISSUE_LATITUDE = "issue_latitude";
    public static final String EXTRA_ISSUE_LONGITUDE = "issue_longitude";

    private boolean isToolbarTransparent = true;
    private double latitude;
    private double longitude;
    private String issueId;
    private IssueRepository issueRepository;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView titleText;
    private TextView descriptionText;
    private TextView locationText;
    private TextView reporterText;
    private TextView upvoteCountText;
    private TextView timeText;
    private Chip statusChip;
    private ImageView imageView;
    private MaterialButton viewOnMapButton;
    private View upvoteButton;
    private SessionManager sessionManager;
    private MaterialButton editButton;
    private MaterialButton deleteButton;
    private Issue currentIssue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.issue_detail_fragment);

        // Initialize SessionManager from Application to ensure consistent session
        IssueReporterApplication app = (IssueReporterApplication) getApplication();
        sessionManager = app.getSessionManager();
        app.ensureProperSessionManagement();
        
        // Log session info
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "User is logged in, username: " + sessionManager.getUsername() + 
                  ", uid: " + sessionManager.getUserId() +
                  ", token: " + (sessionManager.getToken() != null && !sessionManager.getToken().isEmpty() ? "present" : "missing"));
        } else {
            Log.d(TAG, "User is not logged in in IssueDetailActivity");
        }

        // Initialize repository
        issueRepository = IssueRepository.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize views
        titleText = findViewById(R.id.issueTitle);
        descriptionText = findViewById(R.id.issueDescription);
        locationText = findViewById(R.id.issueLocation);
        reporterText = findViewById(R.id.issueReporter);
        upvoteCountText = findViewById(R.id.upvoteCount);
        timeText = findViewById(R.id.issueTime);
        statusChip = findViewById(R.id.statusChip);
        ImageButton backButton = findViewById(R.id.backButton);
        upvoteButton = findViewById(R.id.upvoteButton);
        viewOnMapButton = findViewById(R.id.viewOnMapButton);
        imageView = findViewById(R.id.issueImage);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);

        // Get issue ID from intent
        issueId = getIntent().getStringExtra(EXTRA_ISSUE_ID);
        
        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadIssueFromApi);

        // Handle toolbar transparency on scroll
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        appBarLayout.addOnOffsetChangedListener((appBarLayout1, verticalOffset) -> {
            boolean isCollapsed = Math.abs(verticalOffset) >= appBarLayout1.getTotalScrollRange() / 2;
            if (isCollapsed && isToolbarTransparent) {
                toolbar.setBackgroundResource(R.color.white);
                isToolbarTransparent = false;
            } else if (!isCollapsed && !isToolbarTransparent) {
                toolbar.setBackgroundResource(android.R.color.transparent);
                isToolbarTransparent = true;
            }
        });

        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        upvoteButton.setOnClickListener(v -> upvoteIssue());
        
        // Set edit and delete button click listeners
        if (editButton != null) {
            editButton.setOnClickListener(v -> editIssue());
        }
        
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> confirmDeleteIssue());
        }

        if (issueId != null && !issueId.isEmpty()) {
            // Try to load issue from API
            loadIssueFromApi();
        } else {
            // Fallback to intent extras
            loadIssueFromIntent();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Ensure proper session management when activity resumes
        IssueReporterApplication app = (IssueReporterApplication) getApplication();
        sessionManager = app.getSessionManager();
        app.ensureProperSessionManagement();
        
        // Refresh issue data if we have an ID
        if (issueId != null && !issueId.isEmpty() && !issueId.startsWith("local_")) {
            loadIssueFromApi();
        }
    }
    
    private void loadIssueFromApi() {
        if (issueId == null || issueId.isEmpty() || issueId.startsWith("local_")) {
            // If it's a local ID, use the intent data
            swipeRefreshLayout.setRefreshing(false);
            loadIssueFromIntent();
            return;
        }
        
        // Show loading indicator
        swipeRefreshLayout.setRefreshing(true);
        
        // Fetch issue details from API
        issueRepository.getIssueDetails(issueId, new IssueRepository.RepositoryCallback<Issue>() {
            @Override
            public void onSuccess(Issue issue) {
                runOnUiThread(() -> {
                    // Update UI with issue details
                    displayIssue(issue);
                    swipeRefreshLayout.setRefreshing(false);
                    
                    // Check if user has already upvoted this issue
                    checkUserUpvoteStatus();
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> {
                    // Show error and fallback to intent data
                    Toast.makeText(IssueDetailActivity.this, 
                        "Failed to load issue details: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    
                    // Fallback to intent extras
                    loadIssueFromIntent();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
            
            @Override
            public void onAlreadyUpvoted() {
                // Not applicable for this callback
            }
            
            @Override
            public void onAuthenticationError() {
                runOnUiThread(() -> {
                    Toast.makeText(IssueDetailActivity.this, 
                        "Authentication error. Please login again.", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Fallback to intent extras
                    loadIssueFromIntent();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }
    
    private void loadIssueFromIntent() {
        // Get data from intent
        String title = getIntent().getStringExtra(EXTRA_ISSUE_TITLE);
        String description = getIntent().getStringExtra(EXTRA_ISSUE_DESC);
        String location = getIntent().getStringExtra(EXTRA_ISSUE_LOCATION);
        String reporter = getIntent().getStringExtra(EXTRA_ISSUE_REPORTER);
        int upvotes = getIntent().getIntExtra(EXTRA_ISSUE_UPVOTES, 0);
        String status = getIntent().getStringExtra(EXTRA_ISSUE_STATUS);
        long timestamp = getIntent().getLongExtra(EXTRA_ISSUE_TIMESTAMP, 0);
        String imageUrl = getIntent().getStringExtra(EXTRA_ISSUE_IMAGE_URL);
        latitude = getIntent().getDoubleExtra(EXTRA_ISSUE_LATITUDE, 0);
        longitude = getIntent().getDoubleExtra(EXTRA_ISSUE_LONGITUDE, 0);

        // Set data to views
        titleText.setText(title);
        descriptionText.setText(description);
        locationText.setText(location);
        reporterText.setText("Reported by " + reporter);
        upvoteCountText.setText(upvotes + " Upvotes");
        statusChip.setText(status);
        timeText.setText(formatTimestamp(timestamp));
        
        // Setup view on map button
        if (latitude != 0 && longitude != 0) {
            viewOnMapButton.setVisibility(View.VISIBLE);
            viewOnMapButton.setOnClickListener(v -> openLocationInMaps());
        } else {
            viewOnMapButton.setVisibility(View.GONE);
        }
        
        // Load image
        loadImage(imageUrl);
    }
    
    private void displayIssue(Issue issue) {
        currentIssue = issue;
        
        // Set issue details
        titleText.setText(issue.getTitle());
        descriptionText.setText(issue.getDescription());
        locationText.setText(issue.getLocation());
        reporterText.setText(issue.getReporterName());
        upvoteCountText.setText(String.valueOf(issue.getUpvotes()) + " Upvotes");
        
        // Format and set time
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
        String formattedTime = sdf.format(new Date(issue.getCreatedAt()));
        timeText.setText(formattedTime);
        
        // Set status chip
        String status = issue.getStatus();
        if (status != null) {
            statusChip.setText(status);
            
            // Set chip color based on status
            int chipColor;
            if (status.equalsIgnoreCase("OPEN")) {
                chipColor = getResources().getColor(R.color.status_open);
            } else if (status.equalsIgnoreCase("IN_PROGRESS")) {
                chipColor = getResources().getColor(R.color.status_in_progress);
            } else if (status.equalsIgnoreCase("RESOLVED")) {
                chipColor = getResources().getColor(R.color.status_resolved);
            } else {
                chipColor = getResources().getColor(R.color.status_default);
            }
            statusChip.setChipBackgroundColorResource(R.color.white);
            statusChip.setTextColor(chipColor);
            statusChip.setChipStrokeColorResource(R.color.white);
        }
        
        // Set image - handle all possible image URL formats
        List<String> imageUrls = issue.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String imageUrl = imageUrls.get(0);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                loadImage(imageUrl);
            } else {
                // No image URL, set placeholder
                imageView.setImageResource(R.drawable.img);
                imageView.setVisibility(View.VISIBLE);
            }
        } else {
            // No image URLs available, set placeholder
            imageView.setImageResource(R.drawable.img);
            imageView.setVisibility(View.VISIBLE);
        }
        
        // Set map button visibility based on coordinates
        latitude = issue.getLatitude();
        longitude = issue.getLongitude();
        if (issue.hasValidCoordinates()) {
            viewOnMapButton.setVisibility(View.VISIBLE);
            viewOnMapButton.setOnClickListener(v -> openLocationInMaps());
        } else {
            viewOnMapButton.setVisibility(View.GONE);
        }
        
        // Show/hide edit and delete buttons based on ownership
        if (sessionManager.isLoggedIn() && 
            sessionManager.getUserId() != null && 
            sessionManager.getUserId().equals(issue.getReporterUid())) {
            // User is the creator of this issue
            if (editButton != null) editButton.setVisibility(View.VISIBLE);
            if (deleteButton != null) deleteButton.setVisibility(View.VISIBLE);
        } else {
            // User is not the creator
            if (editButton != null) editButton.setVisibility(View.GONE);
            if (deleteButton != null) deleteButton.setVisibility(View.GONE);
        }
        
        // Check upvote status
        checkUserUpvoteStatus();
    }
    
    private void loadImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.d(TAG, "No image URL provided, using placeholder");
            imageView.setImageResource(R.drawable.img);
            imageView.setVisibility(View.VISIBLE);
            return;
        }

        try {
            Log.d(TAG, "Loading image from URL: " + imageUrl);
            
            // Check if this is a relative URL from the backend (starts with /api/images/)
            if (imageUrl.startsWith("/api/images/") || imageUrl.startsWith("api/images/")) {
                // Get the base URL from ApiClient without the trailing slash
                String baseUrl = ApiClient.getBaseUrl();
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                
                // Combine for a full URL
                String fullUrl = baseUrl + (imageUrl.startsWith("/") ? imageUrl : "/" + imageUrl);
                Log.d(TAG, "Constructed full URL: " + fullUrl);
                
                // Load image from the full URL
                Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imageView);
            } else if (imageUrl.startsWith("http")) {
                // It's already a full URL, use it as is
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imageView);
            } else {
                // Handle other types of URLs or fallback to placeholder
                Log.w(TAG, "Unrecognized URL format: " + imageUrl);
                imageView.setImageResource(R.drawable.img);
            }
            
            // Ensure the image view is visible
            imageView.setVisibility(View.VISIBLE);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            imageView.setImageResource(R.drawable.img);
            imageView.setVisibility(View.VISIBLE);
        }
    }
    
    private void upvoteIssue() {
        if (issueId == null || issueId.isEmpty() || issueId.startsWith("local_")) {
            // Local issue, just show a message
            Snackbar.make(upvoteButton, "Cannot upvote local issues", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Initialize repository if it's null
        if (issueRepository == null) {
            issueRepository = IssueRepository.getInstance();
        }

        // Ensure we have the latest session
        IssueReporterApplication app = (IssueReporterApplication) getApplication();
        sessionManager = app.getSessionManager();
        app.ensureProperSessionManagement();
        
        Log.d(TAG, "upvoteIssue: User login status - isLoggedIn: " + sessionManager.isLoggedIn() + 
              ", userId: " + (sessionManager.getUserId() != null ? sessionManager.getUserId() : "null") +
              ", token: " + (sessionManager.getToken() != null && !sessionManager.getToken().isEmpty() ? "present" : "missing"));
        
        // Double-check session is valid
        if (sessionManager.getToken() == null || sessionManager.getToken().isEmpty()) {
            Log.e(TAG, "Token is missing despite session manager being initialized");
            sessionManager.setLoggedIn(false); // Reset login state since token is missing
        }
        
        if (!sessionManager.isLoggedIn()) {
            Snackbar.make(upvoteButton, "You must be logged in to upvote issues", Snackbar.LENGTH_LONG)
                .setAction("Login", v -> {
                    // Navigate to login screen
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                })
                .show();
            return;
        }
        
        // Show loading indicator
        View upvoteIcon = findViewById(R.id.upvoteIcon);
        if (upvoteIcon != null) {
            upvoteIcon.animate().rotation(upvoteIcon.getRotation() + 360f).setDuration(1000).start();
        }
        
        try {
            // First check if user has already upvoted this issue
            issueRepository.checkUserUpvoteStatus(issueId, new IssueRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean hasUpvoted) {
                    runOnUiThread(() -> {
                        try {
                            if (hasUpvoted) {
                                Snackbar.make(upvoteButton, "You have already upvoted this issue", Snackbar.LENGTH_SHORT).show();
                                
                                // Update UI to show it's already upvoted
                                upvoteButton.setEnabled(false);
                                upvoteButton.setAlpha(0.5f);
                                
                                // Change the upvote icon to show it's already upvoted
                                ImageView upvoteIconView = findViewById(R.id.upvoteIcon);
                                if (upvoteIconView != null) {
                                    upvoteIconView.setColorFilter(getResources().getColor(R.color.primary));
                                }
                            } else {
                                // Perform the upvote
                                performUpvote();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing upvote status", e);
                            Snackbar.make(upvoteButton, "Error processing upvote: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            // Re-enable the button
                            upvoteButton.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        try {
                            Log.e(TAG, "Error checking upvote status", error);
                            
                            // Check if it's an authentication error
                            if (error.getMessage() != null && 
                                (error.getMessage().contains("log in") || 
                                 error.getMessage().contains("logged in") ||
                                 error.getMessage().contains("authentication"))) {
                                
                                // Show login prompt
                                Snackbar.make(upvoteButton, "Please log in to upvote issues", Snackbar.LENGTH_LONG)
                                    .setAction("Login", v -> {
                                        Intent intent = new Intent(IssueDetailActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                    })
                                    .show();
                            } else {
                                // If we can't check, just try to upvote anyway
                                performUpvote();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling upvote status error", e);
                            Snackbar.make(upvoteButton, "Error processing upvote: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            // Re-enable the button
                            upvoteButton.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onAlreadyUpvoted() {
                    runOnUiThread(() -> {
                        try {
                            // Already upvoted
                            Snackbar.make(upvoteButton, "You have already upvoted this issue", Snackbar.LENGTH_SHORT).show();
                            
                            // Update UI to show it's already upvoted
                            upvoteButton.setEnabled(false);
                            upvoteButton.setAlpha(0.5f);
                            
                            // Change the upvote icon to show it's already upvoted
                            ImageView upvoteIconView = findViewById(R.id.upvoteIcon);
                            if (upvoteIconView != null) {
                                upvoteIconView.setColorFilter(getResources().getColor(R.color.primary));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling already upvoted case", e);
                            upvoteButton.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onAuthenticationError() {
                    runOnUiThread(() -> {
                        try {
                            // Show login prompt
                            Snackbar.make(upvoteButton, "Please log in to upvote issues", Snackbar.LENGTH_LONG)
                                .setAction("Login", v -> {
                                    Intent intent = new Intent(IssueDetailActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                })
                                .show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling authentication error", e);
                            upvoteButton.setEnabled(true);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during upvote process", e);
            Snackbar.make(upvoteButton, "Error during upvote: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
            // Re-enable the button
            upvoteButton.setEnabled(true);
        }
    }
    
    private void performUpvote() {
        try {
            // Show loading indicator
            upvoteButton.setEnabled(false);
            
            if (issueRepository == null) {
                issueRepository = IssueRepository.getInstance();
            }
            
            issueRepository.upvoteIssue(issueId, new IssueRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(() -> {
                        try {
                            // Refresh issue data
                            loadIssueFromApi();
                            
                            // Show success message
                            Snackbar.make(upvoteButton, "Issue upvoted successfully!", Snackbar.LENGTH_SHORT).show();
                            
                            // Disable upvote button to prevent multiple upvotes
                            upvoteButton.setEnabled(false);
                            upvoteButton.setAlpha(0.5f);
                            
                            // Change the upvote icon to show it's upvoted
                            ImageView upvoteIconView = findViewById(R.id.upvoteIcon);
                            if (upvoteIconView != null) {
                                upvoteIconView.setColorFilter(getResources().getColor(R.color.primary));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI after upvote", e);
                            // Re-enable the button
                            upvoteButton.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        try {
                            Log.e(TAG, "Error upvoting issue", error);
                            
                            // Re-enable the button
                            upvoteButton.setEnabled(true);
                            
                            // Check if it's an authentication error
                            if (error.getMessage() != null && 
                                (error.getMessage().contains("log in") || 
                                 error.getMessage().contains("logged in") ||
                                 error.getMessage().contains("authentication") ||
                                 error.getMessage().contains("session"))) {
                                
                                // Show login prompt
                                Snackbar.make(upvoteButton, "Please log in to upvote issues", Snackbar.LENGTH_LONG)
                                    .setAction("Login", v -> {
                                        Intent intent = new Intent(IssueDetailActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                    })
                                    .show();
                            } else {
                                // Other error
                                String errorMessage = error.getMessage() != null ? error.getMessage() : "Unknown error";
                                Snackbar.make(upvoteButton, "Error upvoting issue: " + errorMessage, Snackbar.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling upvote error", e);
                            Snackbar.make(upvoteButton, "Error processing upvote: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            // Re-enable the button
                            upvoteButton.setEnabled(true);
                        }
                    });
                }
                
                @Override
                public void onAlreadyUpvoted() {
                    runOnUiThread(() -> {
                        try {
                            // Already upvoted
                            Snackbar.make(upvoteButton, "You have already upvoted this issue", Snackbar.LENGTH_SHORT).show();
                            
                            // Update UI to show it's already upvoted
                            upvoteButton.setEnabled(false);
                            upvoteButton.setAlpha(0.5f);
                            
                            // Change the upvote icon to show it's already upvoted
                            ImageView upvoteIconView = findViewById(R.id.upvoteIcon);
                            if (upvoteIconView != null) {
                                upvoteIconView.setColorFilter(getResources().getColor(R.color.primary));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling already upvoted case", e);
                            upvoteButton.setEnabled(true);
                        }
                    });
                }
                
                @Override
                public void onAuthenticationError() {
                    runOnUiThread(() -> {
                        try {
                            // Show login prompt
                            Snackbar.make(upvoteButton, "Please log in to upvote issues", Snackbar.LENGTH_LONG)
                                .setAction("Login", v -> {
                                    Intent intent = new Intent(IssueDetailActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                })
                                .show();
                            
                            // Re-enable the button
                            upvoteButton.setEnabled(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling authentication error", e);
                            upvoteButton.setEnabled(true);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during performUpvote", e);
            Snackbar.make(upvoteButton, "Error during upvote: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
            // Re-enable the button
            upvoteButton.setEnabled(true);
        }
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    private void openLocationInMaps() {
        // Create a URI for Google Maps with the coordinates
        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "&z=17");
        
        // Create an Intent to open Google Maps
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // If Google Maps is not installed, open in browser
            Uri browserUri = Uri.parse("https://www.google.com/maps?q=" + latitude + "," + longitude);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            startActivity(browserIntent);
        }
    }

    private void editIssue() {
        if (currentIssue == null) {
            Toast.makeText(this, "Cannot edit issue: Issue details not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create a bundle with issue details for the bottom sheet
        Bundle args = new Bundle();
        args.putBoolean(ReportIssueBottomSheet.EXTRA_IS_EDIT_MODE, true);
        args.putString(ReportIssueBottomSheet.EXTRA_ISSUE_ID, currentIssue.getId());
        args.putString(ReportIssueBottomSheet.EXTRA_ISSUE_TITLE, currentIssue.getTitle());
        args.putString(ReportIssueBottomSheet.EXTRA_ISSUE_DESCRIPTION, currentIssue.getDescription());
        args.putString(ReportIssueBottomSheet.EXTRA_ISSUE_LOCATION, currentIssue.getLocation());
        args.putDouble(ReportIssueBottomSheet.EXTRA_ISSUE_LATITUDE, currentIssue.getLatitude());
        args.putDouble(ReportIssueBottomSheet.EXTRA_ISSUE_LONGITUDE, currentIssue.getLongitude());
        
        // Pass image URLs if available
        List<String> imageUrls = currentIssue.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            args.putString(ReportIssueBottomSheet.EXTRA_ISSUE_IMAGE_URL, imageUrls.get(0));
        }
        
        // Create and show the bottom sheet
        ReportIssueBottomSheet bottomSheet = new ReportIssueBottomSheet();
        bottomSheet.setArguments(args);
        bottomSheet.show(getSupportFragmentManager(), "ReportIssueBottomSheet");
    }
    
    private void confirmDeleteIssue() {
        if (currentIssue == null) {
            Toast.makeText(this, "Cannot delete issue: Issue details not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Issue");
        builder.setMessage("Are you sure you want to delete this issue? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteIssue();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }
    
    private void deleteIssue() {
        if (currentIssue == null || currentIssue.getId() == null) {
            Toast.makeText(this, "Cannot delete issue: Issue ID not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading indicator
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Deleting issue...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Call repository to delete issue
        issueRepository.deleteIssue(currentIssue.getId(), new IssueRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    
                    Toast.makeText(IssueDetailActivity.this, "Issue deleted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    
                    // Show error message
                    Toast.makeText(IssueDetailActivity.this, 
                        "Failed to delete issue: " + error.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onAlreadyUpvoted() {
                // Not applicable for this callback
            }
            
            @Override
            public void onAuthenticationError() {
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    
                    Toast.makeText(IssueDetailActivity.this, 
                        "Authentication error. Please login again.", 
                        Toast.LENGTH_LONG).show();
                    
                    // Navigate to login
                    Intent intent = new Intent(IssueDetailActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }

    private void checkUserUpvoteStatus() {
        try {
            if (issueId == null || issueId.isEmpty() || issueId.startsWith("local_")) {
                Log.d(TAG, "Cannot check upvote status for local/empty issue ID");
                return;
            }
            
            if (issueRepository == null) {
                issueRepository = IssueRepository.getInstance();
            }
            
            Log.d(TAG, "Checking if user has upvoted issue: " + issueId);
            upvoteButton.setEnabled(false); // Disable while checking
            
            issueRepository.checkUserUpvoteStatus(issueId, new IssueRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean hasUpvoted) {
                    runOnUiThread(() -> {
                        try {
                            upvoteButton.setEnabled(true); // Re-enable button
                            
                            if (hasUpvoted) {
                                Log.d(TAG, "User has already upvoted this issue");
                                // Update UI to show the user has already upvoted
                                upvoteButton.setEnabled(false);
                                upvoteButton.setAlpha(0.5f);
                                
                                // Change the upvote icon color
                                ImageView upvoteIconView = findViewById(R.id.upvoteIcon);
                                if (upvoteIconView != null) {
                                    upvoteIconView.setColorFilter(getResources().getColor(R.color.primary));
                                }
                            } else {
                                Log.d(TAG, "User has not upvoted this issue yet");
                                // Update UI to show the user can upvote
                                upvoteButton.setEnabled(true);
                                upvoteButton.setAlpha(1.0f);
                                
                                // Reset the upvote icon color
                                ImageView upvoteIconView = findViewById(R.id.upvoteIcon);
                                if (upvoteIconView != null) {
                                    upvoteIconView.setColorFilter(getResources().getColor(R.color.black));
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI after upvote check", e);
                            upvoteButton.setEnabled(true); // Make sure button is enabled
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        try {
                            Log.e(TAG, "Error checking upvote status", error);
                            // Enable the button anyway so user can try to upvote
                            upvoteButton.setEnabled(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling upvote status error", e);
                        }
                    });
                }
                
                @Override
                public void onAlreadyUpvoted() {
                    // This shouldn't be called during status check, but handle it just in case
                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Status check indicates user already upvoted (unexpected path)");
                            upvoteButton.setEnabled(false);
                            upvoteButton.setAlpha(0.5f);
                            
                            ImageView upvoteIconView = findViewById(R.id.upvoteIcon);
                            if (upvoteIconView != null) {
                                upvoteIconView.setColorFilter(getResources().getColor(R.color.primary));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling already upvoted case", e);
                            upvoteButton.setEnabled(true);
                        }
                    });
                }
                
                @Override
                public void onAuthenticationError() {
                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Authentication required to check upvote status");
                            // Still enable the button - login prompting will happen when they try to upvote
                            upvoteButton.setEnabled(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling authentication error", e);
                            upvoteButton.setEnabled(true);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception during upvote status check", e);
            upvoteButton.setEnabled(true); // Make sure button is enabled
        }
    }
}