package com.example.communityissuereporter;

public class Issue {
    private String id;
    private String title;
    private String description;
    private String location;
    private String imageUrl;
    private String reporterName;
    private int upvotes;
    private String status;
    private long timestamp;
    private double latitude;
    private double longitude;

    public Issue(String id, String title, String description, String location, 
                String imageUrl, String reporterName, int upvotes, String status, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.imageUrl = imageUrl;
        this.reporterName = reporterName;
        this.upvotes = upvotes;
        this.status = status;
        this.timestamp = timestamp;
        this.latitude = 0;
        this.longitude = 0;
    }
    
    public Issue(String id, String title, String description, String location, 
                String imageUrl, String reporterName, int upvotes, String status, long timestamp,
                double latitude, double longitude) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.imageUrl = imageUrl;
        this.reporterName = reporterName;
        this.upvotes = upvotes;
        this.status = status;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
    public String getReporterName() { return reporterName; }
    public int getUpvotes() { return upvotes; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    
    public boolean hasValidCoordinates() {
        return latitude != 0 && longitude != 0;
    }
} 