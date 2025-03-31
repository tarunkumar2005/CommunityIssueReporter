package com.example.communityissuereporter;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.example.communityissuereporter.api.ApiClient;
import com.example.communityissuereporter.utils.ThemeManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IssueReporterApplication extends Application {
    private static final String TAG = "IssueReporterApp";
    private static IssueReporterApplication instance;
    private SessionManager sessionManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "Application starting");
        
        try {
            // Initialize SessionManager
            sessionManager = new SessionManager(getApplicationContext());
            
            // Validate session on startup
            sessionManager.validateSession();
            
            // Initialize API client
            ApiClient.init(sessionManager);
            
            // Apply saved theme settings
            ThemeManager.applyTheme(this);
            
            // Log session info
            if (sessionManager.isLoggedIn()) {
                Log.d(TAG, "User is logged in at app startup");
                Log.d(TAG, "Session info - username: " + sessionManager.getUsername() + 
                      ", userId: " + sessionManager.getUserId() + 
                      ", token: " + (sessionManager.getToken() != null && !sessionManager.getToken().isEmpty() ? "present" : "missing"));
            } else {
                Log.d(TAG, "User is not logged in at app startup");
            }
            
            // For debugging - log API information and test connectivity
            if (sessionManager.isLoggedIn()) {
                Log.d(TAG, "Testing API connection");
                Log.d(TAG, "API Base URL: " + ApiClient.getBaseUrl());
            } else {
                Log.d(TAG, "User is not logged in, skipping API tests");
            }
        } catch (Exception e) {
            // Catch any unexpected exceptions during application initialization
            Log.e(TAG, "Error during application initialization", e);
        }
    }
    
    public static IssueReporterApplication getInstance() {
        return instance;
    }
    
    public SessionManager getSessionManager() {
        // Ensure we never return a null SessionManager
        if (sessionManager == null) {
            Log.e(TAG, "SessionManager was null when requested! Creating new instance.");
            sessionManager = new SessionManager(getApplicationContext());
            ApiClient.init(sessionManager);
        }
        return sessionManager;
    }
    
    /**
     * Helper method to ensure all components are using the same SessionManager
     * and the ApiClient is properly initialized
     */
    public void ensureProperSessionManagement() {
        try {
            if (sessionManager != null) {
                // Validate the session to ensure consistency
                sessionManager.validateSession();
                
                Log.d(TAG, "Ensuring proper session management - isLoggedIn: " + sessionManager.isLoggedIn() + 
                      ", userId: " + (sessionManager.getUserId() != null ? sessionManager.getUserId() : "null") +
                      ", token: " + (sessionManager.getToken() != null && !sessionManager.getToken().isEmpty() ? "present" : "missing"));
                
                // Re-initialize ApiClient with our session manager
                ApiClient.init(sessionManager);
            } else {
                Log.e(TAG, "SessionManager is null in ensureProperSessionManagement!");
                sessionManager = new SessionManager(getApplicationContext());
                sessionManager.validateSession();
                ApiClient.init(sessionManager);
            }
        } catch (Exception e) {
            // Catch any unexpected exceptions during session management
            Log.e(TAG, "Error ensuring proper session management", e);
            
            // Try to recover by creating a new session manager
            try {
                sessionManager = new SessionManager(getApplicationContext());
                ApiClient.init(sessionManager);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to recover from session management error", ex);
            }
        }
    }
    
    /**
     * Test method for image upload and issue creation
     * Note: This is commented out by default since it creates real issues in the database
     */
    private void testImageUploadAndIssueCreation() {
        try {
            // Create a test image file
            File cacheDir = getCacheDir();
            File testImage = new File(cacheDir, "test_image.jpg");
            
            if (!testImage.exists()) {
                // Create a simple test image
                boolean created = createTestImage(testImage);
                if (!created) {
                    Log.e(TAG, "Failed to create test image");
                    return;
                }
            }
            
            Log.d(TAG, "Test image path: " + testImage.getAbsolutePath());
            
            // Test the API
            // ApiTestUtil.testCreateIssueWithImage(testImage.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error in test image upload", e);
        }
    }
    
    /**
     * Helper method to create a simple test image
     */
    private boolean createTestImage(File outFile) {
        try {
            // Create a very simple Bitmap and save it to the file
            Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            // Draw something on it
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            canvas.drawRect(50, 50, 150, 150, paint);
            
            // Add some text
            paint.setColor(Color.WHITE);
            paint.setTextSize(20);
            canvas.drawText("Test Image", 60, 100, paint);
            
            // Save to file
            FileOutputStream fos = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating test image", e);
            return false;
        }
    }
} 