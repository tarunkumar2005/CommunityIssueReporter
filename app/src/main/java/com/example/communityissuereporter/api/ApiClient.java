package com.example.communityissuereporter.api;

import android.util.Log;

import com.example.communityissuereporter.BuildConfig;
import com.example.communityissuereporter.SessionManager;
import com.example.communityissuereporter.model.Issue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ApiClient {
    private static final String TAG = "ApiClient";
    // Use the API base URL from BuildConfig
    private static final String BASE_URL = BuildConfig.API_BASE_URL;
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;
    private static SessionManager sessionManager = null;

    public static void init(SessionManager manager) {
        Log.d(TAG, "Initializing ApiClient with SessionManager");
        if (manager == null) {
            Log.e(TAG, "Warning: Null SessionManager provided to ApiClient.init()");
            return;
        } else {
            Log.d(TAG, "SessionManager isLoggedIn: " + manager.isLoggedIn() + 
                  ", userId: " + (manager.getUserId() != null ? manager.getUserId() : "null") +
                  ", token: " + (manager.getToken() != null ? "present" : "null"));
        }
        
        // Store the session manager
        sessionManager = manager;
        
        // Reset the client to ensure it picks up the new session
        reset();
    }

    public static SessionManager getSessionManager() {
        if (sessionManager == null) {
            Log.e(TAG, "getSessionManager called but sessionManager is null!");
        }
        return sessionManager;
    }

    public static String getBaseUrl() {
        // Ensure the base URL ends with a slash for proper URL construction
        if (!BASE_URL.endsWith("/")) {
            return BASE_URL + "/";
        }
        return BASE_URL;
    }

    public static ApiService getClient() {
        if (apiService == null) {
            // Create logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> 
                Log.d(TAG, message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Create OkHttp Client
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    // Print the request URL for debugging
                    Log.d(TAG, "Making request to: " + chain.request().url());
                    
                    // Get the latest token directly from the session manager
                    // This ensures we always use the most up-to-date token
                    String token = null;
                    if (sessionManager != null) {
                        token = sessionManager.getToken();
                        Log.d(TAG, "Using token from session manager: " + (token != null && !token.isEmpty() ? "YES" : "NO"));
                        
                        if (token == null || token.isEmpty()) {
                            Log.e(TAG, "Token is missing despite having a session manager!");
                            if (sessionManager.isLoggedIn()) {
                                Log.e(TAG, "User is marked as logged in but token is missing!");
                            }
                        }
                    } else {
                        Log.e(TAG, "Session manager is null when making API request!");
                    }
                    
                    if (token != null && !token.isEmpty()) {
                        Log.d(TAG, "Adding Authorization header with token");
                        return chain.proceed(
                            chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer " + token)
                                .build()
                        );
                    } else {
                        Log.d(TAG, "No token available, proceeding without Authorization header");
                        return chain.proceed(chain.request());
                    }
                });

            // Create Gson with proper date handling
            Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                .registerTypeAdapter(Issue.class, new IssueDeserializer())
                .create();

            // Create Retrofit instance
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();

            apiService = retrofit.create(ApiService.class);
            
            Log.d(TAG, "ApiClient initialized with base URL: " + BASE_URL);
        }
        return apiService;
    }
    
    // Method to reset the API client instance
    public static void reset() {
        retrofit = null;
        apiService = null;
        Log.d(TAG, "ApiClient reset");
    }
}