package com.example.communityissuereporter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "LoginSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_UID = "uid";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        
        // Validate session on initialization
        validateSession();
    }
    
    /**
     * Validates the current session to ensure consistency
     * If isLoggedIn is true but token or uid is missing, resets the login state
     */
    public void validateSession() {
        boolean isLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);
        String token = pref.getString(KEY_TOKEN, null);
        String uid = pref.getString(KEY_UID, null);
        
        Log.d(TAG, "Validating session - isLoggedIn: " + isLoggedIn + 
              ", token: " + (token != null && !token.isEmpty() ? "present" : "missing") + 
              ", uid: " + (uid != null && !uid.isEmpty() ? "present" : "missing"));
        
        if (isLoggedIn && (token == null || token.isEmpty() || uid == null || uid.isEmpty())) {
            Log.e(TAG, "Invalid session state detected: logged in but missing token or user ID");
            // Reset the session to a consistent state
            logout();
        }
    }

    public void setLoggedIn(boolean isLoggedIn) {
        Log.d(TAG, "Setting login state to: " + isLoggedIn);
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        
        // If setting to logged out, clear sensitive data
        if (!isLoggedIn) {
            editor.remove(KEY_TOKEN);
            editor.remove(KEY_UID);
        }
        
        editor.apply();
    }

    public void saveUserDetails(String username, String email, String token, String uid) {
        // Validate inputs to prevent crashes
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Cannot save user details: Token is null or empty");
            return;
        }
        
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "Cannot save user details: User ID is null or empty");
            return;
        }
        
        Log.d(TAG, "Saving user details - username: " + (username != null ? username : "null") + 
              ", email: " + (email != null ? email : "null") + 
              ", token: " + (token != null && !token.isEmpty() ? "present" : "missing") + 
              ", uid: " + uid);
        
        // Use safe values for null inputs
        editor.putString(KEY_USERNAME, username != null ? username : "");
        editor.putString(KEY_EMAIL, email != null ? email : "");
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_UID, uid);
        
        // Also update the login state
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        
        // Use apply() for better performance
        editor.apply();
        
        // Log the successful save
        Log.d(TAG, "User details saved successfully");
    }
    
    /**
     * Updates just the auth token
     */
    public void updateToken(String token) {
        // Validate token
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Cannot update token: Token is null or empty");
            return;
        }
        
        Log.d(TAG, "Updating token: present");
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    /**
     * Updates just the username and email
     */
    public void updateUserInfo(String username, String email) {
        Log.d(TAG, "Updating user info - username: " + (username != null ? username : "null") + 
              ", email: " + (email != null ? email : "null"));
        
        // Validate inputs
        if (username == null || username.isEmpty()) {
            Log.e(TAG, "Cannot update user info: Username is null or empty");
            return;
        }
        
        editor.putString(KEY_USERNAME, username);
        
        if (email != null) {
            editor.putString(KEY_EMAIL, email);
        }
        
        editor.apply();
        Log.d(TAG, "User info updated successfully");
    }

    public boolean isLoggedIn() {
        boolean loggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);
        
        // Double-check that we have a token and uid if logged in
        if (loggedIn) {
            String token = getToken();
            String uid = getUid();
            
            if (token == null || token.isEmpty() || uid == null || uid.isEmpty()) {
                Log.e(TAG, "isLoggedIn: User marked as logged in but token or uid is missing!");
                // Fix the inconsistent state
                setLoggedIn(false);
                return false;
            }
        }
        
        return loggedIn;
    }

    public String getUsername() {
        String username = pref.getString(KEY_USERNAME, null);
        return username != null ? username : "";
    }

    public String getEmail() {
        String email = pref.getString(KEY_EMAIL, null);
        return email != null ? email : "";
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public String getUid() {
        return pref.getString(KEY_UID, null);
    }

    public String getUserId() {
        return getUid();
    }

    public void logout() {
        Log.d(TAG, "Logging out user");
        editor.clear();
        editor.apply();
    }
}