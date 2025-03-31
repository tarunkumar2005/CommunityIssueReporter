package com.example.communityissuereporter.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Issue {
    private String id;
    private String title;
    private String description;
    private String location;
    private List<String> imageUrls;
    private String reporterUid;
    private String reporterName;
    private String status;
    private int upvotes;
    
    @SerializedName("createdAt")
    private long createdAt;
    
    @SerializedName("updatedAt")
    private long updatedAt;
    
    private double latitude;
    private double longitude;

    public Issue() {
        // Empty constructor for Gson deserialization
    }

    public Issue(String id, String title, String description, String location, 
                List<String> imageUrls, String reporterUid, String reporterName, 
                int upvotes, String status, long createdAt, long updatedAt,
                double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.imageUrls = imageUrls;
        this.reporterUid = reporterUid;
        this.reporterName = reporterName;
        this.upvotes = upvotes;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getId() { 
        return id; 
    }
    
    public String getTitle() { 
        return title; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public String getLocation() { 
        return location; 
    }
    
    public List<String> getImageUrls() { 
        return imageUrls; 
    }
    
    public String getReporterUid() { 
        return reporterUid; 
    }
    
    public String getReporterName() { 
        return reporterName; 
    }
    
    public int getUpvotes() { 
        return upvotes; 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    public long getTimestamp() { 
        return createdAt; 
    }
    
    public long getCreatedAt() { 
        return createdAt; 
    }
    
    public long getUpdatedAt() { 
        return updatedAt; 
    }
    
    public double getLatitude() { 
        return latitude; 
    }
    
    public double getLongitude() { 
        return longitude; 
    }
    
    public boolean hasValidCoordinates() {
        return latitude != 0 && longitude != 0;
    }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setReporterUid(String reporterUid) { this.reporterUid = reporterUid; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public void setStatus(String status) { this.status = status; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
} 