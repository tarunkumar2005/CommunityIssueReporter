package com.example.communityissuereporter.api;

import android.os.StrictMode;
import android.util.Log;

import com.example.communityissuereporter.BuildConfig;
import com.example.communityissuereporter.model.Issue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    private static RetrofitClient instance = null;
    private ApiService apiService;
    private Retrofit retrofit;
    private final String baseUrl;
    private final Executor callbackExecutor;

    private RetrofitClient() {
        // Store the base URL for debugging purposes
        this.baseUrl = BuildConfig.API_BASE_URL;
        
        // Create an executor for callbacks to avoid main thread work
        this.callbackExecutor = Executors.newSingleThreadExecutor();
        
        // Create logging interceptor with detailed logs
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> 
            Log.d(TAG, message));
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Configure OkHttpClient with longer timeouts and logging
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // Create a custom Gson instance with the Issue deserializer
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Issue.class, new IssueDeserializer())
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .callbackExecutor(callbackExecutor)
                .build();

        apiService = retrofit.create(ApiService.class);
        
        Log.d(TAG, "Initialized with base URL: " + baseUrl);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }
    
    // Method to get the base URL for debugging
    public String getBaseUrl() {
        return baseUrl;
    }
    
    // Method to reset the instance (useful for testing or when changing base URL)
    public static void resetInstance() {
        instance = null;
    }
}