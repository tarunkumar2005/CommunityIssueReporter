package com.example.communityissuereporter.repository;

import android.util.Log;

import com.example.communityissuereporter.api.ApiClient;
import com.example.communityissuereporter.api.ApiResponse;
import com.example.communityissuereporter.api.CreateIssueRequest;
import com.example.communityissuereporter.api.ImageUploadResponse;
import com.example.communityissuereporter.api.PaginatedResponse;
import com.example.communityissuereporter.api.UpdateStatusRequest;
import com.example.communityissuereporter.api.UpvoteIssueRequest;
import com.example.communityissuereporter.model.Issue;
import com.example.communityissuereporter.utils.ResponseInspector;
import com.example.communityissuereporter.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class IssueRepository {
    private static final String TAG = "IssueRepository";
    private static IssueRepository instance;

    private IssueRepository() {}

    public static IssueRepository getInstance() {
        if (instance == null) {
            instance = new IssueRepository();
        }
        return instance;
    }

    public void getIssues(int page, int size, String status, String location, 
                         String reporterUid, String searchTerm, Integer minUpvotes,
                         String startDate, String endDate, String sort, 
                         RepositoryCallback<PaginatedResponse<Issue>> callback) {
        // Ensure page is non-negative and convert to 1-indexed (backend API expects 1-indexed pages)
        int apiPage = Math.max(0, page) + 1;
        Log.d(TAG, "Getting issues: page=" + page + " (API page=" + apiPage + 
              "), size=" + size + 
              ", status=" + status + 
              ", location=" + location + 
              ", reporterUid=" + reporterUid + 
              ", searchTerm=" + searchTerm + 
              ", minUpvotes=" + minUpvotes + 
              ", startDate=" + startDate + 
              ", endDate=" + endDate + 
              ", sort=" + sort);
        
        ApiClient.getClient().getIssues(apiPage, size, status, location, reporterUid, 
                                      searchTerm, minUpvotes, startDate, endDate, sort)
            .enqueue(new Callback<ApiResponse<PaginatedResponse<Issue>>>() {
                @Override
                public void onResponse(Call<ApiResponse<PaginatedResponse<Issue>>> call, 
                                    Response<ApiResponse<PaginatedResponse<Issue>>> response) {
                    // Log detailed response information for debugging
                    ResponseInspector.inspectResponseDetailed(response);
                    
                    if (response.isSuccessful()) {
                        Log.d(TAG, "API call successful: " + response.code());
                        ApiResponse<PaginatedResponse<Issue>> apiResponse = response.body();
                        
                        if (apiResponse != null) {
                            // Log the status and message for debugging
                            Log.d(TAG, "API response status: " + apiResponse.getStatus() + ", message: " + apiResponse.getMessage());
                            
                            if (apiResponse.isSuccess()) {
                                Log.d(TAG, "API returned success status");
                                PaginatedResponse<Issue> paginatedResponse = apiResponse.getData();
                                
                                if (paginatedResponse != null) {
                                    List<Issue> items = paginatedResponse.getItems();
                                    Log.d(TAG, "Received " + (items != null ? items.size() : 0) + " issues");
                                    
                                    if (items != null) {
                                        for (int i = 0; i < items.size(); i++) {
                                            Issue issue = items.get(i);
                                            Log.d(TAG, "Issue " + i + ": ID=" + issue.getId() + 
                                                  ", Title=" + issue.getTitle() + 
                                                  ", Status=" + issue.getStatus());
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "Paginated response is null");
                                }
                                
                                callback.onSuccess(apiResponse.getData());
                            } else {
                                String message = apiResponse.getMessage();
                                Log.e(TAG, "API returned failure status: " + message);
                                callback.onError(new Exception("Failed to fetch issues: " + message));
                            }
                        } else {
                            Log.e(TAG, "API response body is null");
                            callback.onError(new Exception("Failed to fetch issues: API response body is null"));
                        }
                    } else {
                        Log.e(TAG, "API call failed with code: " + response.code());
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Error body: " + errorBody);
                            callback.onError(new Exception("API Error: " + errorBody));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error body", e);
                            callback.onError(new Exception("Failed to fetch issues: HTTP " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<PaginatedResponse<Issue>>> call, Throwable t) {
                    Log.e(TAG, "Network error when fetching issues", t);
                    callback.onError(t);
                }
            });
    }

    // Keep the old method for backward compatibility
    public void getIssues(int page, int size, String status, String sort, 
                         RepositoryCallback<PaginatedResponse<Issue>> callback) {
        getIssues(page, size, status, null, null, null, null, null, null, sort, callback);
    }

    public void createIssue(String title, String description, String location,
                          double latitude, double longitude, String imagePath,
                          RepositoryCallback<Issue> callback) {
        Log.d(TAG, "Creating issue: " + title + ", with image: " + (imagePath != null));
        
        if (imagePath != null) {
            // First upload the image, then create the issue with the image URL
            Log.d(TAG, "Uploading image first: " + imagePath);
            
            uploadImage(imagePath, new RepositoryCallback<ImageUploadResponse>() {
                @Override
                public void onSuccess(ImageUploadResponse result) {
                    String imageUrl = result.getImageUrl();
                    Log.d(TAG, "Image uploaded successfully, URL: " + imageUrl);
                    
                    // Now create the issue with the image URL
                    createIssueWithImage(title, description, location, latitude, longitude, imageUrl, callback);
                }

                @Override
                public void onError(Throwable error) {
                    Log.e(TAG, "Failed to upload image", error);
                    
                    // Show a more specific error message
                    callback.onError(error);
                }
                
                @Override
                public void onAlreadyUpvoted() {
                    // Not applicable for image upload
                }
                
                @Override
                public void onAuthenticationError() {
                    Log.e(TAG, "Authentication error when uploading image");
                    callback.onError(new Exception("Authentication error: Please log in again"));
                }
            });
        } else {
            // Create issue without image
            Log.d(TAG, "Creating issue without image");
            createIssueWithImage(title, description, location, latitude, longitude, null, callback);
        }
    }

    public void createIssueWithImage(String title, String description, String location,
                                    double latitude, double longitude, String imageUrl,
                                    RepositoryCallback<Issue> callback) {
        Log.d(TAG, "Creating issue with image URL: " + imageUrl);
        
        // Get user info from SessionManager
        SessionManager sessionManager = ApiClient.getSessionManager();
        String reporterUid = sessionManager != null ? sessionManager.getUserId() : null;
        String reporterName = sessionManager != null ? sessionManager.getUsername() : null;
        
        List<String> imageUrls = imageUrl != null ? Collections.singletonList(imageUrl) : Collections.emptyList();
        CreateIssueRequest request = new CreateIssueRequest(
            title, description, location, latitude, longitude, imageUrls, reporterUid, reporterName);

        // Log the request details
        Log.d(TAG, "Issue creation request: title=" + title + 
              ", description length=" + (description != null ? description.length() : 0) + 
              ", location=" + location + 
              ", coordinates=(" + latitude + "," + longitude + ")" +
              ", imageUrls=" + imageUrls +
              ", reporterUid=" + reporterUid +
              ", reporterName=" + reporterName);

        ApiClient.getClient().createIssue(request)
            .enqueue(new Callback<ApiResponse<Issue>>() {
                @Override
                public void onResponse(Call<ApiResponse<Issue>> call, Response<ApiResponse<Issue>> response) {
                    Log.d(TAG, "Issue creation response code: " + response.code());
                    
                    if (response.isSuccessful()) {
                        ApiResponse<Issue> apiResponse = response.body();
                        
                        if (apiResponse != null && apiResponse.isSuccess()) {
                            Log.d(TAG, "Issue created successfully");
                            
                            Issue createdIssue = apiResponse.getData();
                            if (createdIssue != null) {
                                Log.d(TAG, "Created issue ID: " + createdIssue.getId());
                                callback.onSuccess(createdIssue);
                            } else {
                                Log.e(TAG, "Created issue is null in response");
                                callback.onError(new Exception("Created issue is null in response"));
                            }
                        } else {
                            String errorMsg = apiResponse != null ? apiResponse.getMessage() : "Unknown error";
                            Log.e(TAG, "Failed to create issue: " + errorMsg);
                            callback.onError(new Exception("Failed to create issue: " + errorMsg));
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Issue creation failed with error: " + errorBody);
                            callback.onError(new Exception("Failed to create issue: " + errorBody));
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            callback.onError(new Exception("Failed to create issue: HTTP " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Issue>> call, Throwable t) {
                    Log.e(TAG, "Issue creation network request failed", t);
                    callback.onError(new Exception("Network error creating issue: " + t.getMessage(), t));
                }
            });
    }

    public void updateIssueStatus(String issueId, String status, String comment,
                                RepositoryCallback<Issue> callback) {
        Log.d(TAG, "Updating issue status: " + issueId + " to " + status);
        
        UpdateStatusRequest request = new UpdateStatusRequest(status, comment);
        ApiClient.getClient().updateIssueStatus(issueId, request)
            .enqueue(new Callback<ApiResponse<Issue>>() {
                @Override
                public void onResponse(Call<ApiResponse<Issue>> call, Response<ApiResponse<Issue>> response) {
                    if (response.isSuccessful()) {
                        ApiResponse<Issue> apiResponse = response.body();
                        if (apiResponse != null) {
                            Log.d(TAG, "API response status: " + apiResponse.getStatus() + ", message: " + apiResponse.getMessage());
                            
                            if (apiResponse.isSuccess()) {
                                Log.d(TAG, "Status updated successfully");
                                callback.onSuccess(apiResponse.getData());
                            } else {
                                String message = apiResponse.getMessage();
                                Log.e(TAG, "Failed to update status: " + message);
                                callback.onError(new Exception("Failed to update issue status: " + message));
                            }
                        } else {
                            Log.e(TAG, "API response body is null");
                            callback.onError(new Exception("Failed to update issue status: API response body is null"));
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Status update failed with HTTP " + response.code() + ": " + errorBody);
                            callback.onError(new Exception("Failed to update issue status: " + errorBody));
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            callback.onError(new Exception("Failed to update issue status: HTTP " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Issue>> call, Throwable t) {
                    Log.e(TAG, "Status update failed", t);
                    callback.onError(t);
                }
            });
    }

    public void upvoteIssue(String issueId, RepositoryCallback<Boolean> callback) {
        Log.d(TAG, "Upvoting issue: " + issueId);
        
        // Get the session manager from the API client
        SessionManager sessionManager = ApiClient.getSessionManager();
        
        // Better debugging for session issues
        if (sessionManager == null) {
            Log.e(TAG, "Cannot upvote - SessionManager is null - API client not initialized properly");
            callback.onError(new Exception("Internal error: Session not initialized. Please restart the app."));
            return;
        }
        
        if (!sessionManager.isLoggedIn()) {
            Log.e(TAG, "Cannot upvote - User is not logged in according to SessionManager");
            callback.onError(new Exception("You must be logged in to upvote"));
            return;
        }
        
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot upvote - User ID not available despite isLoggedIn=true");
            callback.onError(new Exception("You must be logged in to upvote"));
            return;
        }
        
        // Log the token for debugging
        String token = sessionManager.getToken();
        Log.d(TAG, "User authentication - isLoggedIn: " + sessionManager.isLoggedIn() + 
              ", userId: " + userId + 
              ", token: " + (token != null && !token.isEmpty() ? "present" : "missing"));
        
        // Ensure we have a token
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Cannot upvote - Token is missing despite user being logged in");
            callback.onError(new Exception("Session error: Please log out and log in again"));
            return;
        }
        
        // Create the request with the user ID
        UpvoteIssueRequest request = new UpvoteIssueRequest(userId);
        Log.d(TAG, "Created upvote request with userId: " + userId);
        
        // Make the API call to upvote the issue
        ApiClient.getClient().upvoteIssue(issueId, request)
            .enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful()) {
                        ApiResponse<Void> apiResponse = response.body();
                        if (apiResponse != null) {
                            Log.d(TAG, "API response status: " + apiResponse.getStatus() + ", message: " + apiResponse.getMessage());
                            
                            if (apiResponse.isSuccess()) {
                                Log.d(TAG, "Issue upvoted successfully");
                                callback.onSuccess(true);
                            } else {
                                String message = apiResponse.getMessage();
                                Log.e(TAG, "Failed to upvote issue: " + message);
                                
                                // Handle specific error cases
                                if (message != null && message.contains("already upvoted")) {
                                    callback.onAlreadyUpvoted();
                                } else if (message != null && message.contains("authentication")) {
                                    callback.onAuthenticationError();
                                } else {
                                    callback.onError(new Exception("Failed to upvote issue: " + message));
                                }
                            }
                        } else {
                            Log.e(TAG, "API response body is null");
                            callback.onError(new Exception("Failed to upvote issue: API response body is null"));
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Upvote failed with HTTP " + response.code() + ": " + errorBody);
                            callback.onError(new Exception("Failed to upvote issue: " + errorBody));
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            callback.onError(new Exception("Failed to upvote issue: HTTP " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Log.e(TAG, "Upvote failed", t);
                    callback.onError(t);
                }
            });
    }

    public void getIssueDetails(String issueId, RepositoryCallback<Issue> callback) {
        Log.d(TAG, "Getting issue details: " + issueId);
        
        ApiClient.getClient().getIssueDetails(issueId)
            .enqueue(new Callback<ApiResponse<Issue>>() {
                @Override
                public void onResponse(Call<ApiResponse<Issue>> call, Response<ApiResponse<Issue>> response) {
                    if (response.isSuccessful()) {
                        ApiResponse<Issue> apiResponse = response.body();
                        if (apiResponse != null) {
                            Log.d(TAG, "API response status: " + apiResponse.getStatus() + ", message: " + apiResponse.getMessage());
                            
                            if (apiResponse.isSuccess()) {
                                Log.d(TAG, "Issue details retrieved successfully");
                                callback.onSuccess(apiResponse.getData());
                            } else {
                                String message = apiResponse.getMessage();
                                Log.e(TAG, "Failed to get issue details: " + message);
                                callback.onError(new Exception("Failed to get issue details: " + message));
                            }
                        } else {
                            Log.e(TAG, "API response body is null");
                            callback.onError(new Exception("Failed to get issue details: API response body is null"));
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Get issue details failed with HTTP " + response.code() + ": " + errorBody);
                            callback.onError(new Exception("Failed to get issue details: " + errorBody));
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            callback.onError(new Exception("Failed to get issue details: HTTP " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Issue>> call, Throwable t) {
                    Log.e(TAG, "Get details failed", t);
                    callback.onError(t);
                }
            });
    }

    public void uploadImage(String imagePath, RepositoryCallback<ImageUploadResponse> callback) {
        Log.d(TAG, "Uploading image from path: " + imagePath + ", file exists: " + 
            (new File(imagePath).exists()) + ", file size: " + new File(imagePath).length() + " bytes");
        
        // Create a request body from the file
        File file = new File(imagePath);
        if (!file.exists()) {
            Log.e(TAG, "Image file does not exist: " + imagePath);
            callback.onError(new Exception("Image file does not exist"));
            return;
        }
        
        if (file.length() > 10 * 1024 * 1024) {
            Log.e(TAG, "Image file too large: " + file.length() + " bytes");
            callback.onError(new Exception("Image file too large (max 10MB)"));
            return;
        }
        
        try {
            // Try to get the content type
            String contentType = URLConnection.guessContentTypeFromName(file.getName());
            if (contentType == null) {
                contentType = "image/jpeg"; // Default to JPEG if we can't determine
            }
            Log.d(TAG, "Image content type: " + contentType);
            
            // Check if the content type is an image
            if (!contentType.startsWith("image/")) {
                Log.e(TAG, "Not an image file: " + contentType);
                callback.onError(new Exception("Not a valid image file"));
                return;
            }
            
            // Create the request body
            RequestBody requestFile = RequestBody.create(MediaType.parse(contentType), file);
            
            // Create the MultipartBody.Part
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            
            // Make the API call
            Log.d(TAG, "Making image upload API call to: " + ApiClient.getBaseUrl() + "api/images/upload");
            ApiClient.getClient().uploadImage(imagePart)
                .enqueue(new Callback<ApiResponse<ImageUploadResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ImageUploadResponse>> call, Response<ApiResponse<ImageUploadResponse>> response) {
                        Log.d(TAG, "Image upload response code: " + response.code());
                        
                        if (response.isSuccessful()) {
                            ApiResponse<ImageUploadResponse> apiResponse = response.body();
                            Log.d(TAG, "Image upload response body: " + (apiResponse != null ? 
                                "success=" + apiResponse.isSuccess() + ", message=" + apiResponse.getMessage() : "null"));
                            
                            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                                Log.d(TAG, "Image upload successful, URL: " + apiResponse.getData().getImageUrl());
                                callback.onSuccess(apiResponse.getData());
                            } else {
                                String errorMsg = apiResponse != null ? apiResponse.getMessage() : "Unknown error";
                                Log.e(TAG, "Image upload failed: " + errorMsg);
                                callback.onError(new Exception("Failed to upload image: " + errorMsg));
                            }
                        } else {
                            try {
                                String errorBody = response.errorBody() != null ? 
                                    response.errorBody().string() : "Unknown error";
                                Log.e(TAG, "Image upload failed with HTTP " + response.code() + ": " + errorBody);
                                
                                // Try to parse the error body as JSON to get more details
                                try {
                                    JSONObject errorJson = new JSONObject(errorBody);
                                    String message = errorJson.optString("message", "Unknown error");
                                    Log.e(TAG, "Parsed error message: " + message);
                                    callback.onError(new Exception("Failed to upload image: " + message));
                                } catch (JSONException e) {
                                    // Not JSON or parse error
                                    callback.onError(new Exception("Failed to upload image: " + errorBody));
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error body", e);
                                callback.onError(new Exception("Failed to upload image: HTTP " + response.code()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ImageUploadResponse>> call, Throwable t) {
                        Log.e(TAG, "Image upload network request failed", t);
                        callback.onError(new Exception("Network error uploading image: " + t.getMessage(), t));
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing image upload", e);
            callback.onError(new Exception("Error preparing image upload: " + e.getMessage(), e));
        }
    }
    
    /**
     * Uploads an image as base64 string
     */
    public void uploadBase64Image(String base64Image, RepositoryCallback<ImageUploadResponse> callback) {
        if (base64Image == null || base64Image.isEmpty()) {
            Log.e(TAG, "Base64 image is null or empty");
            callback.onError(new Exception("Base64 image is null or empty"));
            return;
        }
        
        Log.d(TAG, "Uploading base64 image, length: " + base64Image.length() + " chars");
        
        try {
            // Create request body
            String userId = ApiClient.getSessionManager() != null ? 
                ApiClient.getSessionManager().getUserId() : "";
            com.example.communityissuereporter.api.Base64ImageRequest request = 
                new com.example.communityissuereporter.api.Base64ImageRequest(
                    base64Image, 
                    userId,
                    "issue_image_" + System.currentTimeMillis() + ".jpg",
                    "issue"
                );
            
            // Make the API call
            Log.d(TAG, "Making base64 image upload API call");
            ApiClient.getClient().uploadBase64Image(request)
                .enqueue(new Callback<ApiResponse<ImageUploadResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ImageUploadResponse>> call, 
                                       Response<ApiResponse<ImageUploadResponse>> response) {
                        Log.d(TAG, "Base64 image upload response code: " + response.code());
                        
                        if (response.isSuccessful()) {
                            ApiResponse<ImageUploadResponse> apiResponse = response.body();
                            
                            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                                Log.d(TAG, "Base64 image upload successful, URL: " + apiResponse.getData().getImageUrl());
                                callback.onSuccess(apiResponse.getData());
                            } else {
                                String errorMsg = apiResponse != null ? apiResponse.getMessage() : "Unknown error";
                                Log.e(TAG, "Base64 image upload failed: " + errorMsg);
                                callback.onError(new Exception("Failed to upload base64 image: " + errorMsg));
                            }
                        } else {
                            try {
                                String errorBody = response.errorBody() != null ? 
                                    response.errorBody().string() : "Unknown error";
                                Log.e(TAG, "Base64 image upload failed with HTTP " + response.code() + ": " + errorBody);
                                callback.onError(new Exception("Failed to upload base64 image: " + errorBody));
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error body", e);
                                callback.onError(new Exception("Failed to upload base64 image: HTTP " + response.code()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ImageUploadResponse>> call, Throwable t) {
                        Log.e(TAG, "Base64 image upload network request failed", t);
                        callback.onError(new Exception("Network error uploading base64 image: " + t.getMessage(), t));
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing base64 image upload", e);
            callback.onError(new Exception("Error preparing base64 image upload: " + e.getMessage(), e));
        }
    }

    /**
     * Checks if the current user has already upvoted a specific issue
     */
    public void checkUserUpvoteStatus(String issueId, RepositoryCallback<Boolean> callback) {
        Log.d(TAG, "Checking if user has upvoted issue: " + issueId);
        
        // Get SessionManager
        SessionManager sessionManager = ApiClient.getSessionManager();
        if (sessionManager == null) {
            Log.e(TAG, "Cannot check upvote - SessionManager is null");
            callback.onError(new Exception("Internal error: Session not initialized. Please restart the app."));
            return;
        }
        
        if (!sessionManager.isLoggedIn()) {
            Log.e(TAG, "Cannot check upvote - User is not logged in according to SessionManager");
            callback.onAuthenticationError();
            return;
        }
        
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot check upvote - User ID not available despite isLoggedIn=true");
            callback.onAuthenticationError();
            return;
        }
        
        // Log the token for debugging
        String token = sessionManager.getToken();
        Log.d(TAG, "User authentication - isLoggedIn: " + sessionManager.isLoggedIn() + 
              ", userId: " + userId + 
              ", token: " + (token != null && !token.isEmpty() ? "present" : "missing"));
        
        ApiClient.getClient().checkUserUpvote(issueId, userId)
            .enqueue(new Callback<ApiResponse<Boolean>>() {
                @Override
                public void onResponse(Call<ApiResponse<Boolean>> call, Response<ApiResponse<Boolean>> response) {
                    if (response.isSuccessful()) {
                        ApiResponse<Boolean> apiResponse = response.body();
                        if (apiResponse != null) {
                            Log.d(TAG, "API response status: " + apiResponse.getStatus() + ", message: " + apiResponse.getMessage());
                            
                            if (apiResponse.isSuccess()) {
                                Log.d(TAG, "User upvote check successful");
                                // Check if the data is null and provide a default value
                                Boolean hasUpvoted = apiResponse.getData();
                                if (hasUpvoted == null) {
                                    Log.w(TAG, "Upvote check returned null data, defaulting to false");
                                    hasUpvoted = false;
                                }
                                callback.onSuccess(hasUpvoted);
                            } else {
                                String message = apiResponse.getMessage();
                                Log.e(TAG, "Failed to check upvote status: " + message);
                                
                                // Check for authentication issues
                                if (message != null && 
                                    (message.contains("auth") || 
                                     message.contains("login") || 
                                     message.contains("token"))) {
                                    callback.onAuthenticationError();
                                } else {
                                    callback.onError(new Exception("Failed to check upvote status: " + message));
                                }
                            }
                        } else {
                            Log.e(TAG, "API response body is null");
                            callback.onError(new Exception("Failed to check upvote status: API response body is null"));
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Check upvote status failed with HTTP " + response.code() + ": " + errorBody);
                            
                            // Check for authentication issues based on HTTP status code
                            if (response.code() == 401 || response.code() == 403) {
                                callback.onAuthenticationError();
                            } else {
                                callback.onError(new Exception("Failed to check upvote status: " + errorBody));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            callback.onError(new Exception("Failed to check upvote status: HTTP " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Boolean>> call, Throwable t) {
                    Log.e(TAG, "Check upvote status failed", t);
                    callback.onError(t);
                }
            });
    }

    public void updateIssue(String issueId, String title, String description, String location,
                          double latitude, double longitude, List<String> imageUrls,
                          RepositoryCallback<Issue> callback) {
        Log.d(TAG, "Updating issue: " + issueId);
        Log.d(TAG, "Update details - Title: " + title + ", Description: " + description + 
              ", Location: " + location + ", Lat/Long: " + latitude + "/" + longitude + 
              ", ImageUrls: " + (imageUrls != null ? imageUrls.toString() : "null"));
        
        // Get user info from SessionManager
        SessionManager sessionManager = ApiClient.getSessionManager();
        String reporterUid = sessionManager != null ? sessionManager.getUserId() : null;
        String reporterName = sessionManager != null ? sessionManager.getUsername() : null;
        
        Log.d(TAG, "Update with user - UID: " + reporterUid + ", Name: " + reporterName);
        
        CreateIssueRequest request = new CreateIssueRequest(
            title, description, location, latitude, longitude, imageUrls, reporterUid, reporterName);

        Log.d(TAG, "Making API call to update issue with endpoint: api/issues/" + issueId);
        ApiClient.getClient().updateIssue(issueId, request)
            .enqueue(new Callback<ApiResponse<Issue>>() {
                @Override
                public void onResponse(Call<ApiResponse<Issue>> call, Response<ApiResponse<Issue>> response) {
                    Log.d(TAG, "Issue update response code: " + response.code());
                    
                    if (response.isSuccessful()) {
                        ApiResponse<Issue> apiResponse = response.body();
                        
                        if (apiResponse != null && apiResponse.isSuccess()) {
                            Log.d(TAG, "Issue updated successfully");
                            callback.onSuccess(apiResponse.getData());
                        } else {
                            String errorMsg = apiResponse != null ? apiResponse.getMessage() : "Unknown error";
                            Log.e(TAG, "Failed to update issue: " + errorMsg);
                            callback.onError(new Exception("Failed to update issue: " + errorMsg));
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Issue update failed with error: " + errorBody);
                            callback.onError(new Exception("Failed to update issue: " + errorBody));
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            callback.onError(new Exception("Failed to update issue: HTTP " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Issue>> call, Throwable t) {
                    Log.e(TAG, "Issue update network request failed", t);
                    callback.onError(new Exception("Network error updating issue: " + t.getMessage(), t));
                }
            });
    }
    
    public void deleteIssue(String issueId, RepositoryCallback<Void> callback) {
        Log.d(TAG, "Deleting issue: " + issueId);
        
        // Get user info from SessionManager
        SessionManager sessionManager = ApiClient.getSessionManager();
        String userId = sessionManager != null ? sessionManager.getUserId() : null;
        
        if (userId == null) {
            Log.e(TAG, "Cannot delete issue: user ID is null");
            callback.onError(new Exception("User ID is required to delete an issue"));
            return;
        }

        ApiClient.getClient().deleteIssue(issueId, userId)
            .enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    Log.d(TAG, "Issue deletion response code: " + response.code());
                    
                    if (response.isSuccessful()) {
                        ApiResponse<Void> apiResponse = response.body();
                        
                        if (apiResponse != null && apiResponse.isSuccess()) {
                            Log.d(TAG, "Issue deleted successfully");
                            callback.onSuccess(null);
                        } else {
                            String errorMsg = apiResponse != null ? apiResponse.getMessage() : "Unknown error";
                            Log.e(TAG, "Failed to delete issue: " + errorMsg);
                            callback.onError(new Exception("Failed to delete issue: " + errorMsg));
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Issue deletion failed with error: " + errorBody);
                            callback.onError(new Exception("Failed to delete issue: " + errorBody));
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            callback.onError(new Exception("Failed to delete issue: HTTP " + response.code()));
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Log.e(TAG, "Issue deletion network request failed", t);
                    callback.onError(new Exception("Network error deleting issue: " + t.getMessage(), t));
                }
            });
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Throwable error);
        void onAlreadyUpvoted();
        void onAuthenticationError();
    }
}