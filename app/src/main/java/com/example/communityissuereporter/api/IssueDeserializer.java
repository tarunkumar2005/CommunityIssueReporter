package com.example.communityissuereporter.api;

import android.util.Log;

import com.example.communityissuereporter.model.Issue;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Custom deserializer for Issue to handle date fields properly
 */
public class IssueDeserializer implements JsonDeserializer<Issue> {
    private static final String TAG = "IssueDeserializer";
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
    
    @Override
    public Issue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();
            
            Issue issue = new Issue();
            
            // Handle simple string fields
            if (jsonObject.has("id")) {
                issue.setId(getAsStringOrNull(jsonObject, "id"));
            }
            
            if (jsonObject.has("title")) {
                issue.setTitle(getAsStringOrNull(jsonObject, "title"));
            }
            
            if (jsonObject.has("description")) {
                issue.setDescription(getAsStringOrNull(jsonObject, "description"));
            }
            
            if (jsonObject.has("location")) {
                issue.setLocation(getAsStringOrNull(jsonObject, "location"));
            }
            
            if (jsonObject.has("reporterUid")) {
                issue.setReporterUid(getAsStringOrNull(jsonObject, "reporterUid"));
            }
            
            if (jsonObject.has("reporterName")) {
                issue.setReporterName(getAsStringOrNull(jsonObject, "reporterName"));
            }
            
            if (jsonObject.has("status")) {
                issue.setStatus(getAsStringOrNull(jsonObject, "status"));
            }
            
            // Handle numeric fields
            if (jsonObject.has("upvotes")) {
                try {
                    issue.setUpvotes(jsonObject.get("upvotes").getAsInt());
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing upvotes", e);
                    issue.setUpvotes(0);
                }
            }
            
            if (jsonObject.has("latitude")) {
                try {
                    issue.setLatitude(jsonObject.get("latitude").getAsDouble());
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing latitude", e);
                    issue.setLatitude(0);
                }
            }
            
            if (jsonObject.has("longitude")) {
                try {
                    issue.setLongitude(jsonObject.get("longitude").getAsDouble());
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing longitude", e);
                    issue.setLongitude(0);
                }
            }
            
            // Handle date fields
            if (jsonObject.has("createdAt")) {
                issue.setCreatedAt(parseDate(jsonObject.get("createdAt")));
            }
            
            if (jsonObject.has("updatedAt")) {
                issue.setUpdatedAt(parseDate(jsonObject.get("updatedAt")));
            }
            
            // Handle image URLs
            if (jsonObject.has("imageUrls") && !jsonObject.get("imageUrls").isJsonNull()) {
                List<String> imageUrls = new ArrayList<>();
                JsonElement imageUrlsElement = jsonObject.get("imageUrls");
                
                if (imageUrlsElement.isJsonArray()) {
                    for (JsonElement urlElement : imageUrlsElement.getAsJsonArray()) {
                        if (!urlElement.isJsonNull()) {
                            imageUrls.add(urlElement.getAsString());
                        }
                    }
                }
                
                issue.setImageUrls(imageUrls);
            } else {
                issue.setImageUrls(new ArrayList<>());
            }
            
            return issue;
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing Issue", e);
            throw new JsonParseException("Error deserializing Issue: " + e.getMessage());
        }
    }
    
    private String getAsStringOrNull(JsonObject jsonObject, String fieldName) {
        JsonElement element = jsonObject.get(fieldName);
        return (element != null && !element.isJsonNull()) ? element.getAsString() : null;
    }
    
    private long parseDate(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return 0;
        }
        
        try {
            // First try to parse as long (timestamp)
            return element.getAsLong();
        } catch (NumberFormatException e) {
            // If that fails, try to parse as ISO date string
            try {
                String dateStr = element.getAsString();
                Date date = ISO_FORMAT.parse(dateStr);
                return date != null ? date.getTime() : 0;
            } catch (ParseException pe) {
                Log.e(TAG, "Error parsing date: " + element, pe);
                return 0;
            }
        }
    }
} 