package com.example.communityissuereporter.repository;

import android.util.Log;

import com.example.communityissuereporter.api.ApiClient;
import com.example.communityissuereporter.api.ApiResponse;
import com.example.communityissuereporter.api.UserProfileRequest;
import com.example.communityissuereporter.model.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static UserRepository instance;

    private UserRepository() {
        // Private constructor to enforce singleton pattern
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public interface UserProfileCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface DeleteAccountCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public void getUserProfile(String uid, UserProfileCallback callback) {
        Log.d(TAG, "Getting user profile for uid: " + uid);
        
        ApiClient.getClient().getUserProfile(uid).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Log.d(TAG, "User profile retrieved successfully");
                    callback.onSuccess(response.body().getData());
                } else {
                    String errorMessage = "Failed to retrieve user profile";
                    if (response.errorBody() != null) {
                        errorMessage += ": " + response.code();
                    }
                    Log.e(TAG, errorMessage);
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }

    public void updateUserProfile(String uid, UserProfileRequest request, UserProfileCallback callback) {
        Log.d(TAG, "Updating user profile for uid: " + uid);
        
        ApiClient.getClient().updateUserProfile(uid, request).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Log.d(TAG, "User profile updated successfully");
                    callback.onSuccess(response.body().getData());
                } else {
                    String errorMessage = "Failed to update user profile";
                    if (response.errorBody() != null) {
                        errorMessage += ": " + response.code();
                    }
                    Log.e(TAG, errorMessage);
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }

    public void updateNotificationPreferences(String uid, Map<String, Boolean> preferences, UserProfileCallback callback) {
        Log.d(TAG, "Updating notification preferences for uid: " + uid);
        
        ApiClient.getClient().updateNotificationPreferences(uid, preferences).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Log.d(TAG, "Notification preferences updated successfully");
                    callback.onSuccess(response.body().getData());
                } else {
                    String errorMessage = "Failed to update notification preferences";
                    if (response.errorBody() != null) {
                        errorMessage += ": " + response.code();
                    }
                    Log.e(TAG, errorMessage);
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }

    public void deleteUserAccount(String uid, DeleteAccountCallback callback) {
        Log.d(TAG, "Deleting user account for uid: " + uid);
        
        ApiClient.getClient().deleteUserAccount(uid).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "User account deleted successfully");
                    callback.onSuccess();
                } else {
                    String errorMessage = "Failed to delete user account";
                    if (response.errorBody() != null) {
                        errorMessage += ": " + response.code();
                    }
                    Log.e(TAG, errorMessage);
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }
}
