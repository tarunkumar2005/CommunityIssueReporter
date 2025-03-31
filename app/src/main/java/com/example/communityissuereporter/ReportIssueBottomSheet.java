package com.example.communityissuereporter;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.communityissuereporter.api.ApiClient;
import com.example.communityissuereporter.api.ImageUploadResponse;
import com.example.communityissuereporter.model.Issue;
import com.example.communityissuereporter.repository.IssueRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportIssueBottomSheet extends BottomSheetDialogFragment {
    private static final String TAG = "ReportIssueSheet";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES  // For Android 13+
    };
    
    private static final String[] LOCATION_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    // Constants for edit mode
    public static final String EXTRA_IS_EDIT_MODE = "is_edit_mode";
    public static final String EXTRA_ISSUE_ID = "issue_id";
    public static final String EXTRA_ISSUE_TITLE = "issue_title";
    public static final String EXTRA_ISSUE_DESCRIPTION = "issue_description";
    public static final String EXTRA_ISSUE_LOCATION = "issue_location";
    public static final String EXTRA_ISSUE_IMAGE_URL = "issue_image_url";
    public static final String EXTRA_ISSUE_LATITUDE = "issue_latitude";
    public static final String EXTRA_ISSUE_LONGITUDE = "issue_longitude";
    
    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText locationInput;
    private TextInputLayout locationInputLayout;
    private ImageView previewImage;
    private LinearLayout uploadPrompt;
    private ImageButton changePhotoButton;
    private MaterialButton reportButton;
    private TextView currentLocationText;
    private Uri selectedImageUri;
    private View imageUploadContainer;
    private FusedLocationProviderClient fusedLocationClient;
    
    private SessionManager sessionManager;
    private double currentLatitude;
    private double currentLongitude;

    // Add repository instance
    private IssueRepository issueRepository;

    private boolean isEditMode = false;
    private String issueId;
    private String existingImageUrl;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        permissions -> {
            boolean allGranted = true;
            for (Boolean isGranted : permissions.values()) {
                allGranted = allGranted && isGranted;
            }
            if (allGranted) {
                openImagePicker();
            } else {
                Toast.makeText(requireContext(), "Permission required to select image", Toast.LENGTH_SHORT).show();
            }
        }
    );
    
    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        permissions -> {
            boolean allGranted = true;
            for (Boolean isGranted : permissions.values()) {
                allGranted = allGranted && isGranted;
            }
            if (allGranted) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    );

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                displaySelectedImage();
            }
        }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.report_issue_bottom_sheet, container, false);
        
        // Initialize SessionManager
        sessionManager = new SessionManager(requireContext());

        // Initialize API client with session manager
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "User is logged in, username: " + sessionManager.getUsername() + 
                  ", uid: " + sessionManager.getUid() +
                  ", token: " + (sessionManager.getToken() != null && !sessionManager.getToken().isEmpty() ? "present" : "missing"));
            ApiClient.init(sessionManager);
        } else {
            Log.d(TAG, "User is not logged in in ReportIssueBottomSheet");
        }
        
        // Initialize repository
        issueRepository = IssueRepository.getInstance();
        
        // Initialize location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Initialize views
        titleInput = view.findViewById(R.id.titleInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        locationInput = view.findViewById(R.id.locationInput);
        locationInputLayout = (TextInputLayout) locationInput.getParent().getParent();
        previewImage = view.findViewById(R.id.previewImage);
        uploadPrompt = view.findViewById(R.id.uploadPrompt);
        changePhotoButton = view.findViewById(R.id.changePhotoButton);
        reportButton = view.findViewById(R.id.reportButton);
        currentLocationText = view.findViewById(R.id.currentLocationText);
        ImageButton closeButton = view.findViewById(R.id.closeButton);
        imageUploadContainer = view.findViewById(R.id.imageUploadContainer);

        // Setup click listeners
        View.OnClickListener imagePickerClickListener = v -> checkPermissionAndPickImage();
        imageUploadContainer.setOnClickListener(imagePickerClickListener);
        uploadPrompt.setOnClickListener(imagePickerClickListener);
        changePhotoButton.setOnClickListener(imagePickerClickListener);
        closeButton.setOnClickListener(v -> dismiss());
        reportButton.setOnClickListener(v -> submitIssue());
        
        // Setup location click listener
        currentLocationText.setOnClickListener(v -> checkLocationPermissionAndGetLocation());
        if (locationInputLayout != null) {
            locationInputLayout.setEndIconOnClickListener(v -> checkLocationPermissionAndGetLocation());
        }

        // Setup text watchers
        setupTextWatchers();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Check if we're in edit mode
        Bundle args = getArguments();
        if (args != null) {
            isEditMode = args.getBoolean(EXTRA_IS_EDIT_MODE, false);
            if (isEditMode) {
                // We're editing an existing issue
                issueId = args.getString(EXTRA_ISSUE_ID);
                String title = args.getString(EXTRA_ISSUE_TITLE);
                String description = args.getString(EXTRA_ISSUE_DESCRIPTION);
                String location = args.getString(EXTRA_ISSUE_LOCATION);
                existingImageUrl = args.getString(EXTRA_ISSUE_IMAGE_URL);
                currentLatitude = args.getDouble(EXTRA_ISSUE_LATITUDE, 0);
                currentLongitude = args.getDouble(EXTRA_ISSUE_LONGITUDE, 0);
                
                // Populate fields with existing data
                titleInput.setText(title);
                descriptionInput.setText(description);
                locationInput.setText(location);
                
                // Load existing image if available
                if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                    loadExistingImage(existingImageUrl);
                }
                
                // Update button text
                reportButton.setText("Update Issue");
            }
        }
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
            }
        };

        titleInput.addTextChangedListener(watcher);
        descriptionInput.addTextChangedListener(watcher);
        locationInput.addTextChangedListener(watcher);
    }

    private void checkPermissionAndPickImage() {
        // Check if we have the required permissions
        boolean allPermissionsGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                // For Android 13+, we need to check for READ_MEDIA_IMAGES
                if (android.os.Build.VERSION.SDK_INT >= 33 && 
                    permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Skip READ_EXTERNAL_STORAGE check on Android 13+
                    continue;
                }
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            openImagePicker();
        } else {
            // Request permissions
            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            try {
                // Load and display the selected image
                Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(previewImage);
                
                // Update UI
                previewImage.setVisibility(View.VISIBLE);
                uploadPrompt.setVisibility(View.GONE);
                changePhotoButton.setVisibility(View.VISIBLE);
                
                // Enable report button if all fields are valid
                validateInputs();
            } catch (Exception e) {
                Log.e(TAG, "Error displaying selected image", e);
                Toast.makeText(requireContext(), "Failed to load selected image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkLocationPermissionAndGetLocation() {
        boolean allPermissionsGranted = true;
        for (String permission : LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            getCurrentLocation();
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSIONS);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Show loading indicator
        currentLocationText.setText("Getting location...");
        
        // Get current location with high accuracy
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
            @Override
            public boolean isCancellationRequested() {
                return false;
            }

            @Override
            public CancellationToken onCanceledRequested(OnTokenCanceledListener onTokenCanceledListener) {
                return this;
            }
        }).addOnSuccessListener(location -> {
            if (location != null) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                
                // Get address from coordinates
                getAddressFromLocation(currentLatitude, currentLongitude);
            } else {
                currentLocationText.setText("Use current location");
                Toast.makeText(requireContext(), "Failed to get current location", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            currentLocationText.setText("Use current location");
            Toast.makeText(requireContext(), "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                
                // Get the thoroughfare (street)
                String thoroughfare = address.getThoroughfare();
                if (thoroughfare != null) {
                    sb.append(thoroughfare);
                }
                
                // Get the locality (city)
                String locality = address.getLocality();
                if (locality != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(locality);
                }
                
                // Get the admin area (state)
                String adminArea = address.getAdminArea();
                if (adminArea != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(adminArea);
                }
                
                // Get the country
                String country = address.getCountryName();
                if (country != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(country);
                }
                
                String addressText = sb.toString();
                if (!addressText.isEmpty()) {
                    locationInput.setText(addressText);
                    currentLocationText.setText("Location updated");
                } else {
                    locationInput.setText(latitude + ", " + longitude);
                    currentLocationText.setText("Location updated (coordinates only)");
                }
            } else {
                locationInput.setText(latitude + ", " + longitude);
                currentLocationText.setText("Location updated (coordinates only)");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address from location", e);
            locationInput.setText(latitude + ", " + longitude);
            currentLocationText.setText("Location updated (coordinates only)");
        }
    }

    private boolean validateInputs() {
        boolean isValid = !titleInput.getText().toString().trim().isEmpty() &&
                         !descriptionInput.getText().toString().trim().isEmpty() &&
                         !locationInput.getText().toString().trim().isEmpty();
        
        reportButton.setEnabled(isValid);
        return isValid;
    }

    private void submitIssue() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Get input values
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();

        // Show loading state
        setLoadingState(true);

        // Check if we're in edit mode
        if (isEditMode) {
            updateExistingIssue(title, description, location);
        } else {
            createNewIssue(title, description, location);
        }
    }

    private void createNewIssue(String title, String description, String location) {
        // Create new issue with or without image
        if (selectedImageUri != null) {
            // Get file path from URI
            String imagePath = getImagePathFromUri(selectedImageUri);
            if (imagePath != null) {
                // Create issue with image
                issueRepository.createIssue(
                    title, description, location, currentLatitude, currentLongitude, imagePath,
                    new IssueRepository.RepositoryCallback<Issue>() {
                        @Override
                        public void onSuccess(Issue result) {
                            requireActivity().runOnUiThread(() -> {
                                setLoadingState(false);
                                showSuccessAndDismiss("Issue reported successfully!");
                                
                                // Refresh the main activity's issue list
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).loadIssues(true);
                                }
                            });
                        }

                        @Override
                        public void onError(Throwable error) {
                            requireActivity().runOnUiThread(() -> {
                                setLoadingState(false);
                                showError("Failed to report issue: " + error.getMessage());
                                
                                // Fallback to local save if API fails
                                saveIssueLocally(title, description, location, selectedImageUri.toString());
                            });
                        }
                        
                        @Override
                        public void onAlreadyUpvoted() {
                            // Not applicable for this callback
                        }
                        
                        @Override
                        public void onAuthenticationError() {
                            requireActivity().runOnUiThread(() -> {
                                setLoadingState(false);
                                showError("Authentication error. Please log in again.");
                                
                                // Navigate to login
                                if (getActivity() != null) {
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    startActivity(intent);
                                    if (getDialog() != null) getDialog().dismiss();
                                }
                            });
                        }
                    });
            } else {
                setLoadingState(false);
                showError("Failed to process selected image");
            }
        } else {
            // Create issue without image
            issueRepository.createIssue(
                title, description, location, currentLatitude, currentLongitude, null,
                new IssueRepository.RepositoryCallback<Issue>() {
                    @Override
                    public void onSuccess(Issue result) {
                        requireActivity().runOnUiThread(() -> {
                            setLoadingState(false);
                            showSuccessAndDismiss("Issue reported successfully!");
                            
                            // Refresh the main activity's issue list
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).loadIssues(true);
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable error) {
                        requireActivity().runOnUiThread(() -> {
                            setLoadingState(false);
                            showError("Failed to report issue: " + error.getMessage());
                            
                            // Fallback to local save if API fails
                            saveIssueLocally(title, description, location, null);
                        });
                    }
                    
                    @Override
                    public void onAlreadyUpvoted() {
                        // Not applicable for this callback
                    }
                    
                    @Override
                    public void onAuthenticationError() {
                        requireActivity().runOnUiThread(() -> {
                            setLoadingState(false);
                            showError("Authentication error. Please log in again.");
                            
                            // Navigate to login
                            if (getActivity() != null) {
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                startActivity(intent);
                                if (getDialog() != null) getDialog().dismiss();
                            }
                        });
                    }
                });
        }
    }
    
    private void updateExistingIssue(String title, String description, String location) {
        // Prepare image URLs list
        List<String> imageUrls = new ArrayList<>();
        
        // If we have an existing image URL and no new image selected, use the existing one
        if (existingImageUrl != null && !existingImageUrl.isEmpty() && selectedImageUri == null) {
            imageUrls.add(existingImageUrl);
            performUpdate(title, description, location, imageUrls);
        } 
        // If we have a new image selected, upload it first
        else if (selectedImageUri != null) {
            String imagePath = getImagePathFromUri(selectedImageUri);
            if (imagePath != null) {
                // Upload the new image first
                issueRepository.uploadImage(imagePath, new IssueRepository.RepositoryCallback<ImageUploadResponse>() {
                    @Override
                    public void onSuccess(ImageUploadResponse result) {
                        String newImageUrl = result.getImageUrl();
                        if (newImageUrl != null && !newImageUrl.isEmpty()) {
                            imageUrls.add(newImageUrl);
                        }
                        performUpdate(title, description, location, imageUrls);
                    }

                    @Override
                    public void onError(Throwable error) {
                        requireActivity().runOnUiThread(() -> {
                            setLoadingState(false);
                            showError("Failed to upload image: " + error.getMessage());
                        });
                    }
                    
                    @Override
                    public void onAlreadyUpvoted() {
                        // Not applicable for this callback
                    }
                    
                    @Override
                    public void onAuthenticationError() {
                        requireActivity().runOnUiThread(() -> {
                            setLoadingState(false);
                            showError("Authentication error. Please log in again.");
                            
                            // Navigate to login
                            if (getActivity() != null) {
                                Intent intent = new Intent(getActivity(), LoginActivity.class);
                                startActivity(intent);
                                if (getDialog() != null) getDialog().dismiss();
                            }
                        });
                    }
                });
            } else {
                setLoadingState(false);
                showError("Failed to process selected image");
            }
        } 
        // If no images at all, just update without images
        else {
            performUpdate(title, description, location, imageUrls);
        }
    }
    
    private void performUpdate(String title, String description, String location, List<String> imageUrls) {
        issueRepository.updateIssue(
            issueId, title, description, location, currentLatitude, currentLongitude, imageUrls,
            new IssueRepository.RepositoryCallback<Issue>() {
                @Override
                public void onSuccess(Issue result) {
                    requireActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        showSuccessAndDismiss("Issue updated successfully!");
                        
                        // Refresh the main activity's issue list
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).loadIssues(true);
                        }
                    });
                }

                @Override
                public void onError(Throwable error) {
                    requireActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        showError("Failed to update issue: " + error.getMessage());
                    });
                }
                
                @Override
                public void onAlreadyUpvoted() {
                    // Not applicable for this callback
                }
                
                @Override
                public void onAuthenticationError() {
                    requireActivity().runOnUiThread(() -> {
                        setLoadingState(false);
                        showError("Authentication error. Please log in again.");
                        
                        // Navigate to login
                        if (getActivity() != null) {
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            startActivity(intent);
                            if (getDialog() != null) getDialog().dismiss();
                        }
                    });
                }
            });
    }

    private void setLoadingState(boolean isLoading) {
        reportButton.setEnabled(!isLoading);
        reportButton.setText(isLoading ? "Loading..." : isEditMode ? "Update Issue" : "Report Issue");
    }

    private void showSuccessAndDismiss(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private String getImagePathFromUri(Uri uri) {
        try {
            // Create a temporary file in the app's cache directory
            File outputDir = requireContext().getCacheDir();
            File outputFile = File.createTempFile("image", ".jpg", outputDir);
            
            // Copy the content from the URI to the file
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(outputFile);
            
            byte[] buffer = new byte[4 * 1024]; // 4k buffer
            int read;
            
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error getting file path from URI", e);
            return null;
        }
    }
    
    /**
     * Converts an image to base64 format for uploading
     * @param imagePath Path to the image file
     * @return Base64 encoded string of the image
     */
    private String convertImageToBase64(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: " + imagePath);
                return null;
            }
            
            // Load and resize the bitmap to reduce memory usage
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2; // Reduces image dimensions by half
            
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from file");
                return null;
            }
            
            // Compress the bitmap to a byte array
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Convert to base64
            String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
            
            Log.d(TAG, "Converted image to base64, length: " + base64Image.length() + " chars");
            
            // Clean up
            bitmap.recycle();
            baos.close();
            
            return base64Image;
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to base64", e);
            return null;
        }
    }
    
    // Keep the local save method as a fallback
    private void saveIssueLocally(String title, String description, String location, String imageUriString) {
        // Use class-level SessionManager
        if (sessionManager == null) {
            Log.e(TAG, "SessionManager is null when trying to save issue locally!");
            // Create a new one as fallback
            sessionManager = new SessionManager(requireContext());
        }
        
        String username = sessionManager.getUsername();
        String userId = sessionManager.getUserId();
        long currentTime = System.currentTimeMillis();
        
        // Create image URL list
        List<String> imageUrls = new ArrayList<>();
        if (imageUriString != null && !imageUriString.isEmpty()) {
            imageUrls.add(imageUriString);
        }
        
        // Create a new issue object with the local image URI and coordinates
        Issue newIssue = new Issue(
            "local_" + currentTime, // Generate a local ID
            title,
            description,
            location,
            imageUrls,
            userId != null ? userId : "local_user_id",
            username != null ? username : "@local_user",
            0, // Initial upvotes
            "OPEN", // Initial status
            currentTime, // Created timestamp
            currentTime, // Updated timestamp
            currentLatitude, // Latitude
            currentLongitude // Longitude
        );
        
        // Add the new issue to the list in MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).addNewIssue(newIssue);
            Toast.makeText(requireContext(), "Issue reported successfully", Toast.LENGTH_SHORT).show();
            dismiss();
        } else {
            reportButton.setEnabled(true);
            Toast.makeText(requireContext(), "Failed to report issue", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExistingImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
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
                    .centerCrop()
                    .into(previewImage);
            } else if (imageUrl.startsWith("http")) {
                // It's already a full URL, use it as is
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.img)
                    .error(R.drawable.img)
                    .centerCrop()
                    .into(previewImage);
            } else {
                // Handle other types of URLs or fallback to placeholder
                Log.w(TAG, "Unrecognized URL format: " + imageUrl);
                previewImage.setImageResource(R.drawable.img);
            }
            
            // Update UI to show the image
            previewImage.setVisibility(View.VISIBLE);
            uploadPrompt.setVisibility(View.GONE);
            changePhotoButton.setVisibility(View.VISIBLE);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            previewImage.setImageResource(R.drawable.img);
            previewImage.setVisibility(View.VISIBLE);
            uploadPrompt.setVisibility(View.GONE);
            changePhotoButton.setVisibility(View.VISIBLE);
        }
    }
}