package com.example.communityissuereporter;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.communityissuereporter.api.ApiClient;
import com.example.communityissuereporter.api.PaginatedResponse;
import com.example.communityissuereporter.model.Issue;
import com.example.communityissuereporter.repository.IssueRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.communityissuereporter.utils.UIUtils;
import com.example.communityissuereporter.utils.ThemeManager;

public class MainActivity extends AppCompatActivity implements IssuesAdapter.OnIssueClickListener {
    private static final String TAG = "MainActivity";
    
    private GoogleSignInClient mGoogleSignInClient;
    private SessionManager sessionManager;
    private RecyclerView issuesRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addIssueFab;
    private ImageButton profileButton;
    private BottomSheetDialog reportBottomSheet;
    private IssuesAdapter issuesAdapter;
    private List<Issue> issues = new ArrayList<>();
    private IssueRepository issueRepository;
    private int currentPage = 0; // API uses 0-indexed pages
    private int pageSize = 10;
    private boolean isLoading = false;
    private boolean hasMorePages = true; // Track if there are more pages to load
    private LinearLayout emptyStateView;
    
    // Filter and sort related fields
    private TextInputEditText searchEditText;
    private MaterialButton filterButton;
    private MaterialButton sortButton;
    private Chip statusAllChip;
    private Chip statusOpenChip;
    private Chip statusInProgressChip;
    private Chip statusResolvedChip;
    private ChipGroup statusChipGroup;
    
    // Filter parameters
    private String currentStatus = null;
    private String currentLocation = null;
    private String currentSearchTerm = null;
    private Integer currentMinUpvotes = null;
    private String currentStartDate = null;
    private String currentEndDate = null;
    private String currentSort = "createdAt,desc"; // Default sort: newest first

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "MainActivity created");

        // Get SessionManager from Application
        IssueReporterApplication app = (IssueReporterApplication) getApplication();
        sessionManager = app.getSessionManager();
        app.ensureProperSessionManagement();

        // Check if user is logged in, if not, go to login screen
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, navigating to login screen");
            navigateToLogin();
            return;
        }
        
        Log.d(TAG, "User is logged in, username: " + sessionManager.getUsername() + 
              ", uid: " + sessionManager.getUid() +
              ", token: " + (sessionManager.getToken() != null && !sessionManager.getToken().isEmpty() ? "present" : "missing"));

        // Initialize API client with session manager
        ApiClient.init(sessionManager);
        
        // Initialize repository
        issueRepository = IssueRepository.getInstance();

        // Initialize Google Sign In client for sign out
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize views
        initializeViews();
        setupRecyclerView();
        setupListeners();

        // Load data from API
        loadIssues(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Re-check login status and re-initialize API client in case session has changed
        if (sessionManager == null) {
            IssueReporterApplication app = (IssueReporterApplication) getApplication();
            sessionManager = app.getSessionManager();
        }
        
        // Ensure session management is proper
        ((IssueReporterApplication) getApplication()).ensureProperSessionManagement();
        
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "User no longer logged in (detected in onResume), navigating to login screen");
            navigateToLogin();
            return;
        }
        
        // Ensure API client has current session
        ApiClient.init(sessionManager);
        
        // Refresh issues list when returning to the activity
        currentPage = 0;
        hasMorePages = true;
        loadIssues(true);
    }

    private void initializeViews() {
        issuesRecyclerView = findViewById(R.id.issuesRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        addIssueFab = findViewById(R.id.addIssueFab);
        profileButton = findViewById(R.id.profileButton);
        emptyStateView = findViewById(R.id.emptyStateView);
        
        // Initialize filter and sort views
        searchEditText = findViewById(R.id.searchEditText);
        filterButton = findViewById(R.id.filterButton);
        sortButton = findViewById(R.id.sortButton);
        statusChipGroup = findViewById(R.id.statusChipGroup);
        statusAllChip = findViewById(R.id.statusAllChip);
        statusOpenChip = findViewById(R.id.statusOpenChip);
        statusInProgressChip = findViewById(R.id.statusInProgressChip);
        statusResolvedChip = findViewById(R.id.statusResolvedChip);
        
        if (emptyStateView == null) {
            Log.w(TAG, "Empty state view not found in layout");
        }
    }

    private void setupRecyclerView() {
        issuesAdapter = new IssuesAdapter(this);
        issuesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        issuesRecyclerView.setAdapter(issuesAdapter);
        
        // Add scroll listener for pagination
        issuesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                
                // Load more when we're near the end of the list
                if (!isLoading && hasMorePages && 
                    (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 5) && 
                    firstVisibleItemPosition >= 0) {
                    // Load next page
                    loadIssues(false);
                }
            }
        });
    }

    private void setupListeners() {
        // Profile button click
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // FAB click - show report issue bottom sheet
        addIssueFab.setOnClickListener(v -> showReportIssueBottomSheet());

        // Empty state report button - add null check to prevent crashes
        View emptyStateButton = findViewById(R.id.emptyStateButton);
        if (emptyStateButton != null) {
            emptyStateButton.setOnClickListener(v -> showReportIssueBottomSheet());
        }

        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Reset to first page when refreshing
            currentPage = 0;
            hasMorePages = true;
            loadIssues(true);
        });
        
        // Setup filter and sort listeners
        setupFilterAndSortListeners();
    }
    
    private void setupFilterAndSortListeners() {
        // Search functionality
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentSearchTerm = searchEditText.getText().toString().trim();
                if (currentSearchTerm.isEmpty()) {
                    currentSearchTerm = null;
                }
                // Reset to first page and reload
                currentPage = 0;
                hasMorePages = true;
                loadIssues(true);
                
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                return true;
            }
            return false;
        });
        
        // Clear button in search field
        TextInputLayout searchInputLayout = findViewById(R.id.searchInputLayout);
        searchInputLayout.setEndIconOnClickListener(v -> {
            searchEditText.setText("");
            currentSearchTerm = null;
            currentPage = 0;
            hasMorePages = true;
            loadIssues(true);
            
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        });
        
        // Status filter chips
        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // If no chip is selected, select "All" by default
                statusAllChip.setChecked(true);
                currentStatus = null;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.statusAllChip) {
                    currentStatus = null;
                } else if (checkedId == R.id.statusOpenChip) {
                    currentStatus = "OPEN";
                } else if (checkedId == R.id.statusInProgressChip) {
                    currentStatus = "IN_PROGRESS";
                } else if (checkedId == R.id.statusResolvedChip) {
                    currentStatus = "RESOLVED";
                }
            }
            
            // Reset to first page and reload
            currentPage = 0;
            hasMorePages = true;
            loadIssues(true);
        });
        
        // Filter button - show advanced filter dialog
        filterButton.setOnClickListener(v -> showAdvancedFilterDialog());
        
        // Sort button - show sort options
        sortButton.setOnClickListener(v -> showSortOptionsMenu());
    }
    
    private void showAdvancedFilterDialog() {
        // Create a dialog for advanced filtering
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_advanced_filter, null);
        builder.setView(dialogView);
        
        // Get references to dialog views
        TextInputEditText locationEditText = dialogView.findViewById(R.id.locationEditText);
        TextInputEditText minUpvotesEditText = dialogView.findViewById(R.id.minUpvotesEditText);
        TextInputEditText startDateEditText = dialogView.findViewById(R.id.startDateEditText);
        TextInputEditText endDateEditText = dialogView.findViewById(R.id.endDateEditText);
        MaterialButton resetFiltersButton = dialogView.findViewById(R.id.resetFiltersButton);
        MaterialButton applyFiltersButton = dialogView.findViewById(R.id.applyFiltersButton);
        
        // Set current values if any
        if (currentLocation != null) {
            locationEditText.setText(currentLocation);
        }
        if (currentMinUpvotes != null) {
            minUpvotesEditText.setText(String.valueOf(currentMinUpvotes));
        }
        if (currentStartDate != null) {
            startDateEditText.setText(currentStartDate);
        }
        if (currentEndDate != null) {
            endDateEditText.setText(currentEndDate);
        }
        
        // Setup date pickers
        setupDatePicker(startDateEditText);
        setupDatePicker(endDateEditText);
        
        // Create the dialog
        AlertDialog dialog = builder.create();
        
        // Set click listeners for the buttons
        resetFiltersButton.setOnClickListener(v -> {
            locationEditText.setText("");
            minUpvotesEditText.setText("");
            startDateEditText.setText("");
            endDateEditText.setText("");
        });
        
        applyFiltersButton.setOnClickListener(v -> {
            // Get values from dialog
            currentLocation = locationEditText.getText().toString().trim();
            if (currentLocation.isEmpty()) currentLocation = null;
            
            String minUpvotesStr = minUpvotesEditText.getText().toString().trim();
            currentMinUpvotes = minUpvotesStr.isEmpty() ? null : Integer.parseInt(minUpvotesStr);
            
            currentStartDate = startDateEditText.getText().toString().trim();
            if (currentStartDate.isEmpty()) currentStartDate = null;
            
            currentEndDate = endDateEditText.getText().toString().trim();
            if (currentEndDate.isEmpty()) currentEndDate = null;
            
            // Reset to first page and reload
            currentPage = 0;
            hasMorePages = true;
            loadIssues(true);
            
            // Update filter button text to indicate active filters
            updateFilterButtonText();
            
            // Dismiss the dialog
            dialog.dismiss();
        });
        
        // Show the dialog
        dialog.show();
    }
    
    private void updateFilterButtonText() {
        int activeFilterCount = 0;
        if (currentLocation != null) activeFilterCount++;
        if (currentMinUpvotes != null) activeFilterCount++;
        if (currentStartDate != null || currentEndDate != null) activeFilterCount++;
        
        if (activeFilterCount > 0) {
            filterButton.setText(String.format("Filters (%d)", activeFilterCount));
        } else {
            filterButton.setText("Advanced Filter");
        }
    }
    
    private void setupDatePicker(TextInputEditText dateEditText) {
        dateEditText.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format date as YYYY-MM-DD
                        String formattedDate = String.format(Locale.US, "%04d-%02d-%02d", 
                                selectedYear, selectedMonth + 1, selectedDay);
                        dateEditText.setText(formattedDate);
                    },
                    year, month, day);
            datePickerDialog.show();
        });
    }
    
    private void showSortOptionsMenu() {
        PopupMenu popupMenu = new PopupMenu(this, sortButton);
        popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());
        
        // Set the current sort option as checked
        MenuItem currentMenuItem = null;
        if ("createdAt,desc".equals(currentSort)) {
            currentMenuItem = popupMenu.getMenu().findItem(R.id.sort_newest);
        } else if ("createdAt,asc".equals(currentSort)) {
            currentMenuItem = popupMenu.getMenu().findItem(R.id.sort_oldest);
        } else if ("upvotes,desc".equals(currentSort)) {
            currentMenuItem = popupMenu.getMenu().findItem(R.id.sort_most_upvoted);
        } else if ("upvotes,asc".equals(currentSort)) {
            currentMenuItem = popupMenu.getMenu().findItem(R.id.sort_least_upvoted);
        } else if ("updatedAt,desc".equals(currentSort)) {
            currentMenuItem = popupMenu.getMenu().findItem(R.id.sort_recently_updated);
        }
        
        if (currentMenuItem != null) {
            currentMenuItem.setChecked(true);
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            String sortText = "Sort: ";
            String previousSort = currentSort;
            
            if (itemId == R.id.sort_newest) {
                currentSort = "createdAt,desc";
                sortText += "Newest";
            } else if (itemId == R.id.sort_oldest) {
                currentSort = "createdAt,asc";
                sortText += "Oldest";
            } else if (itemId == R.id.sort_most_upvoted) {
                currentSort = "upvotes,desc";
                sortText += "Most Upvoted";
            } else if (itemId == R.id.sort_least_upvoted) {
                currentSort = "upvotes,asc";
                sortText += "Least Upvoted";
            } else if (itemId == R.id.sort_recently_updated) {
                currentSort = "updatedAt,desc";
                sortText += "Recently Updated";
            }
            
            // Update sort button text
            sortButton.setText(sortText);
            
            // Update the adapter's sort order
            issuesAdapter.setSortOrder(currentSort);
            
            // Only reload if the sort order changed
            if (!previousSort.equals(currentSort)) {
                // Reset to first page and reload
                currentPage = 0;
                hasMorePages = true;
                loadIssues(true);
            }
            
            return true;
        });
        
        popupMenu.show();
    }

    public void loadIssues(boolean refreshList) {
        if (isLoading) return;
        isLoading = true;
        
        // Show loading indicator
        if (refreshList) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        // Clear list if refreshing
        if (refreshList) {
            issues.clear();
            issuesAdapter.notifyDataSetChanged();
        }
        
        // Get the current user from session
        String reporterUid = null;  // Set to user ID if you want to filter by reporter
        
        Log.d(TAG, "Loading issues with filters - Status: " + currentStatus + 
              ", Location: " + currentLocation + 
              ", Search: " + currentSearchTerm + 
              ", MinUpvotes: " + currentMinUpvotes + 
              ", DateRange: " + currentStartDate + " to " + currentEndDate + 
              ", Sort: " + currentSort);
        
        // Make sure adapter knows the current sort order
        issuesAdapter.setSortOrder(currentSort);
        
        // Call API to get issues with filters
        try {
            issueRepository.getIssues(
                    currentPage, 
                    pageSize, 
                    currentStatus, 
                    currentLocation, 
                    reporterUid, 
                    currentSearchTerm, 
                    currentMinUpvotes, 
                    currentStartDate, 
                    currentEndDate, 
                    currentSort,
                    new IssueRepository.RepositoryCallback<PaginatedResponse<Issue>>() {
                        @Override
                        public void onSuccess(final PaginatedResponse<Issue> result) {
                            runOnUiThread(() -> {
                                try {
                                    isLoading = false;
                                    
                                    // Update data in the adapter - with null safety
                                    if (refreshList) {
                                        issues.clear();
                                    }
                                    
                                    if (result != null && result.getItems() != null) {
                                        issues.addAll(result.getItems());
                                    }
                                    
                                    if (issuesAdapter != null) {
                                        issuesAdapter.setIssues(issues);
                                    }
                                    
                                    // Check if there are more pages - with null safety
                                    if (result != null) {
                                        hasMorePages = result.hasMorePages();
                                        
                                        // Update the current page
                                        if (!issues.isEmpty()) {
                                            try {
                                                currentPage = result.getCurrentPage() + 1;
                                            } catch (Exception e) {
                                                // Fallback if there's an issue with the page calculation
                                                currentPage++;
                                                Log.e(TAG, "Error calculating next page", e);
                                            }
                                        }
                                    }
                                    
                                    // Show empty state if no issues - with null safety
                                    updateEmptyState();
                                    
                                    // Hide loading indicator
                                    if (swipeRefreshLayout != null) {
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                } catch (Exception e) {
                                    // Catch any unexpected exceptions to prevent crashes
                                    Log.e(TAG, "Error processing API result", e);
                                    if (swipeRefreshLayout != null) {
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                    UIUtils.showErrorSnackbar(swipeRefreshLayout, 
                                            "An unexpected error occurred. Please try again.");
                                }
                            });
                        }
                        
                        @Override
                        public void onError(final Throwable error) {
                            runOnUiThread(() -> {
                                isLoading = false;
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                                
                                // Show error message using our enhanced error handling
                                UIUtils.showErrorSnackbar(swipeRefreshLayout, 
                                        UIUtils.getErrorMessage(MainActivity.this, error));
                                
                                Log.e(TAG, "Error loading issues", error);
                                
                                // If we have no issues, show dummy data as fallback
                                if (issues.isEmpty()) {
                                    loadDummyData();
                                }
                                
                                // Update empty state
                                updateEmptyState();
                            });
                        }
                        
                        @Override
                        public void onAlreadyUpvoted() {
                            // Not applicable for getting issues list
                        }
                        
                        @Override
                        public void onAuthenticationError() {
                            runOnUiThread(() -> {
                                isLoading = false;
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                                
                                // Show login required message
                                UIUtils.showErrorSnackbar(swipeRefreshLayout, 
                                        "Authentication error. Please login again.");
                                
                                // Navigate to login screen
                                navigateToLogin();
                            });
                        }
                    });
        } catch (Exception e) {
            // Handle any exceptions from the repository call
            isLoading = false;
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Log.e(TAG, "Error calling repository", e);
            UIUtils.showErrorSnackbar(swipeRefreshLayout, "Error connecting to server. Please try again.");
            
            // If we have no issues, show dummy data as fallback
            if (issues.isEmpty()) {
                loadDummyData();
            }
            
            // Update empty state
            updateEmptyState();
        }
    }

    // Fallback method to load dummy data in case API fails
    private void loadDummyData() {
        issues = new ArrayList<>();
        
        // Create dummy image URLs
        List<String> nyImages = new ArrayList<>();
        nyImages.add("dummy_image_url");
        
        List<String> laImages = new ArrayList<>();
        laImages.add("dummy_image_url");
        
        List<String> chicagoImages = new ArrayList<>();
        chicagoImages.add("dummy_image_url");
        
        List<String> sfImages = new ArrayList<>();
        sfImages.add("dummy_image_url");
        
        long currentTime = System.currentTimeMillis();
        
        // Example with coordinates for New York City
        issues.add(new Issue(
            "dummy_1",
            "Broken Street Light",
            "The street light at the corner of Main St. has been flickering for the past week, creating safety concerns for pedestrians at night. This is particularly dangerous during the evening rush hour when many people are walking home from work or heading to nearby restaurants.",
            "123 Main Street, City",
            nyImages,
            "dummy_user_1",
            "@john_doe",
            42,
            "OPEN",
            currentTime,
            currentTime,
            40.7128, // Latitude (NYC)
            -74.0060 // Longitude (NYC)
        ));

        // Example with coordinates for Los Angeles
        issues.add(new Issue(
            "dummy_2",
            "Pothole Needs Fixing",
            "Large pothole in the middle of the road causing traffic and potential damage to vehicles. This has been an issue for several weeks now and is getting worse with each rainfall. Multiple vehicles have reported damage.",
            "456 Oak Avenue",
            laImages,
            "dummy_user_2",
            "@jane_smith",
            28,
            "OPEN",
            currentTime - 86400000, // 1 day ago
            currentTime - 86400000,
            34.0522, // Latitude (LA)
            -118.2437 // Longitude (LA)
        ));

        // Example with coordinates for Chicago
        issues.add(new Issue(
            "dummy_3",
            "Overflowing Garbage Bin",
            "The public garbage bin near the park hasn't been collected in days. It's overflowing and attracting pests. The situation is becoming a health hazard for children playing in the nearby playground area.",
            "Central Park, West Entrance",
            chicagoImages,
            "dummy_user_3",
            "@mike_wilson",
            15,
            "OPEN",
            currentTime - 172800000, // 2 days ago
            currentTime - 172800000,
            41.8781, // Latitude (Chicago)
            -87.6298 // Longitude (Chicago)
        ));

        // Example with coordinates for San Francisco
        issues.add(new Issue(
            "dummy_4",
            "Graffiti on Public Building",
            "Someone has vandalized the community center wall with inappropriate graffiti. This needs to be cleaned up as soon as possible as it's visible from the main road and gives a bad impression of our neighborhood.",
            "789 Community Ave",
            sfImages,
            "dummy_user_4",
            "@sarah_parker",
            33,
            "OPEN",
            currentTime - 259200000, // 3 days ago
            currentTime - 259200000,
            37.7749, // Latitude (SF)
            -122.4194 // Longitude (SF)
        ));

        issuesAdapter.setIssues(issues);
        
        // Show a message to the user
        Snackbar.make(issuesRecyclerView, 
            "Couldn't connect to server. Showing sample data.", 
            Snackbar.LENGTH_LONG).show();
    }

    private void showReportIssueBottomSheet() {
        ReportIssueBottomSheet reportIssueBottomSheet = new ReportIssueBottomSheet();
        reportIssueBottomSheet.show(getSupportFragmentManager(), "ReportIssueBottomSheet");
    }

    @Override
    public void onIssueClick(Issue issue) {
        Intent intent = new Intent(this, IssueDetailActivity.class);
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_ID, issue.getId());
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_TITLE, issue.getTitle());
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_DESC, issue.getDescription());
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_LOCATION, issue.getLocation());
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_REPORTER, issue.getReporterName());
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_UPVOTES, issue.getUpvotes());
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_STATUS, issue.getStatus());
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_TIMESTAMP, issue.getTimestamp());
        
        // Handle the case where we might have multiple image URLs
        List<String> imageUrls = issue.getImageUrls();
        String imageUrl = imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null;
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_IMAGE_URL, imageUrl);
        
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_LATITUDE, issue.getLatitude());
        intent.putExtra(IssueDetailActivity.EXTRA_ISSUE_LONGITUDE, issue.getLongitude());
        startActivity(intent);
    }

    @Override
    public void onUpvoteClick(Issue issue) {
        // Validate inputs to prevent crashes
        if (issue == null) {
            Log.e(TAG, "Cannot upvote null issue");
            return;
        }
        
        // Check if user is logged in
        if (sessionManager == null) {
            Log.e(TAG, "SessionManager is null in onUpvoteClick!");
            try {
                sessionManager = new SessionManager(this);
                ApiClient.init(sessionManager);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing SessionManager", e);
                UIUtils.showErrorSnackbar(swipeRefreshLayout, "Error with session. Please log out and login again.");
                return;
            }
        }
        
        Log.d(TAG, "onUpvoteClick: User login status - isLoggedIn: " + sessionManager.isLoggedIn() + 
              ", userId: " + (sessionManager.getUserId() != null ? sessionManager.getUserId() : "null") +
              ", token: " + (sessionManager.getToken() != null && !sessionManager.getToken().isEmpty() ? "present" : "missing"));
        
        if (!sessionManager.isLoggedIn()) {
            UIUtils.showErrorSnackbar(swipeRefreshLayout, "You must be logged in to upvote issues");
            return;
        }
        
        // Ensure the API client is using our session
        ApiClient.init(sessionManager);
        
        // First check if user has already upvoted this issue
        try {
            String issueId = issue.getId();
            if (issueId == null || issueId.isEmpty()) {
                UIUtils.showErrorSnackbar(swipeRefreshLayout, "Invalid issue. Please try again.");
                return;
            }
            
            issueRepository.checkUserUpvoteStatus(issueId, new IssueRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean hasUpvoted) {
                    runOnUiThread(() -> {
                        if (hasUpvoted) {
                            UIUtils.showErrorSnackbar(swipeRefreshLayout, "You have already upvoted this issue");
                        } else {
                            // If not upvoted, proceed with upvote
                            performUpvote(issue);
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        // Show error message using our enhanced error handling
                        UIUtils.showErrorSnackbar(swipeRefreshLayout, UIUtils.getErrorMessage(MainActivity.this, error));
                        Log.e(TAG, "Error checking if user has upvoted", error);
                    });
                }
                
                @Override
                public void onAlreadyUpvoted() {
                    runOnUiThread(() -> {
                        UIUtils.showErrorSnackbar(swipeRefreshLayout, "You have already upvoted this issue");
                    });
                }
                
                @Override
                public void onAuthenticationError() {
                    runOnUiThread(() -> {
                        UIUtils.showErrorSnackbar(swipeRefreshLayout, "Please log in to upvote issues");
                        // Navigate to login screen
                        navigateToLogin();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in upvote process", e);
            UIUtils.showErrorSnackbar(swipeRefreshLayout, "Error checking upvote status. Please try again.");
        }
    }
    
    private void performUpvote(Issue issue) {
        if (issue == null || issue.getId() == null) {
            Log.e(TAG, "Cannot perform upvote on null issue or issue with null ID");
            return;
        }
        
        Log.d(TAG, "performUpvote: User login status - isLoggedIn: " + sessionManager.isLoggedIn() + 
              ", userId: " + (sessionManager.getUserId() != null ? sessionManager.getUserId() : "null") +
              ", token: " + (sessionManager.getToken() != null && !sessionManager.getToken().isEmpty() ? "present" : "missing"));
        
        // Show a loading indicator
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Upvoting issue...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Ensure the API client is using our session
        ApiClient.init(sessionManager);
        
        try {
            issueRepository.upvoteIssue(issue.getId(), new IssueRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            
                            // Refresh the issues list to get the updated upvote count
                            loadIssues(true);
                            
                            // Show success message
                            UIUtils.showSuccessSnackbar(swipeRefreshLayout, "Upvoted successfully!");
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling upvote success", e);
                            UIUtils.showErrorSnackbar(swipeRefreshLayout, "Error updating UI after upvote");
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            
                            // Show error message using our enhanced error handling
                            UIUtils.showErrorSnackbar(swipeRefreshLayout, UIUtils.getErrorMessage(MainActivity.this, error));
                            Log.e(TAG, "Error upvoting issue", error);
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling upvote error callback", e);
                        }
                    });
                }
                
                @Override
                public void onAlreadyUpvoted() {
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            
                            UIUtils.showErrorSnackbar(swipeRefreshLayout, "You have already upvoted this issue");
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling already upvoted case", e);
                        }
                    });
                }
                
                @Override
                public void onAuthenticationError() {
                    runOnUiThread(() -> {
                        try {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            
                            UIUtils.showErrorSnackbar(swipeRefreshLayout, "Please log in to upvote issues");
                            // Navigate to login screen
                            navigateToLogin();
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling authentication error", e);
                        }
                    });
                }
            });
        } catch (Exception e) {
            // Ensure dialog is dismissed and error is shown
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Log.e(TAG, "Exception calling upvote API", e);
            UIUtils.showErrorSnackbar(swipeRefreshLayout, "Error connecting to server. Please try again.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_logout) {
            logout();
            return true;
        } else if (itemId == R.id.action_theme) {
            showThemeOptionsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showThemeOptionsDialog() {
        String[] themeOptions = {"Light", "Dark", "System Default"};
        
        // Determine which option is currently selected
        String currentTheme = ThemeManager.getThemeMode(this);
        int selectedIndex = 0;
        switch (currentTheme) {
            case ThemeManager.THEME_LIGHT:
                selectedIndex = 0;
                break;
            case ThemeManager.THEME_DARK:
                selectedIndex = 1;
                break;
            case ThemeManager.THEME_SYSTEM:
                selectedIndex = 2;
                break;
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themeOptions, selectedIndex, (dialog, which) -> {
                    String selectedTheme;
                    switch (which) {
                        case 0:
                            selectedTheme = ThemeManager.THEME_LIGHT;
                            break;
                        case 1:
                            selectedTheme = ThemeManager.THEME_DARK;
                            break;
                        case 2:
                        default:
                            selectedTheme = ThemeManager.THEME_SYSTEM;
                            break;
                    }
                    
                    // Save theme preference
                    ThemeManager.saveThemeMode(MainActivity.this, selectedTheme);
                    
                    // Apply the new theme
                    ThemeManager.applyTheme(selectedTheme);
                    
                    // Close dialog
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void logout() {
        // Sign out from Google
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Clear session
            sessionManager.logout();
            
            // Navigate back to login
            navigateToLogin();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Method to add a new issue locally
    public void addNewIssue(Issue newIssue) {
        // Add to the beginning of the list
        issues.add(0, newIssue);
        
        // Update the adapter
        issuesAdapter.setIssues(issues);
        
        // Scroll to top to show the new issue
        issuesRecyclerView.smoothScrollToPosition(0);
    }

    // Add a utility method to update the empty state visibility safely
    private void updateEmptyState() {
        try {
            if (issues == null || issues.isEmpty()) {
                if (emptyStateView != null) {
                    emptyStateView.setVisibility(View.VISIBLE);
                }
                if (issuesRecyclerView != null) {
                    issuesRecyclerView.setVisibility(View.GONE);
                }
            } else {
                if (emptyStateView != null) {
                    emptyStateView.setVisibility(View.GONE);
                }
                if (issuesRecyclerView != null) {
                    issuesRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating empty state", e);
        }
    }
}