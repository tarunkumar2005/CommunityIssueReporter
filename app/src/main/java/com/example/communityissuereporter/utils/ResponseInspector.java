package com.example.communityissuereporter.utils;

import android.util.Log;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Utility class for inspecting and debugging API responses
 */
public class ResponseInspector {
    private static final String TAG = "ResponseInspector";
    
    /**
     * Logs detailed information about a Retrofit response
     */
    public static void inspectResponse(Response<?> response) {
        if (response == null) {
            Log.e(TAG, "Response is null");
            return;
        }
        
        Log.d(TAG, "Response code: " + response.code());
        Log.d(TAG, "Response message: " + response.message());
        Log.d(TAG, "Response successful: " + response.isSuccessful());
        
        if (response.headers() != null) {
            Log.d(TAG, "Headers:");
            for (String name : response.headers().names()) {
                Log.d(TAG, "  " + name + ": " + response.headers().get(name));
            }
        }
        
        if (response.body() != null) {
            Log.d(TAG, "Response has body");
            Log.d(TAG, "Body class: " + response.body().getClass().getName());
        } else {
            Log.d(TAG, "Response body is null");
        }
        
        if (!response.isSuccessful() && response.errorBody() != null) {
            try {
                String errorBody = response.errorBody().string();
                Log.e(TAG, "Error body: " + errorBody);
            } catch (Exception e) {
                Log.e(TAG, "Error reading error body", e);
            }
        }
    }
    
    /**
     * Extracts and logs the content of a response body for debugging
     */
    public static String extractResponseBodyAsString(ResponseBody responseBody) {
        if (responseBody == null) {
            return "null";
        }
        
        try {
            String content = responseBody.string();
            Log.d(TAG, "Response body content: " + content);
            return content;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting response body", e);
            return "Error extracting response body: " + e.getMessage();
        }
    }

    /**
     * Inspects a Retrofit response and logs detailed information about request URL and headers
     * @param response The Retrofit response to inspect
     * @param <T> The type of the response body
     */
    public static <T> void inspectResponseDetailed(Response<T> response) {
        try {
            Log.d(TAG, "Response URL: " + response.raw().request().url());
            Log.d(TAG, "Response Code: " + response.code());
            Log.d(TAG, "Response Message: " + response.message());
            Log.d(TAG, "Response Headers: " + response.headers());
            
            if (response.isSuccessful()) {
                Log.d(TAG, "Response Body Present: " + (response.body() != null));
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                Log.e(TAG, "Error Body: " + errorBody);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error inspecting response", e);
        }
    }
} 