package com.example.communityissuereporter.api;

import java.util.List;

public class CreateIssueRequest {
    private String title;
    private String description;
    private String location;
    private double latitude;
    private double longitude;
    private List<String> imageUrls;
    private String reporterUid;
    private String reporterName;

    public CreateIssueRequest(String title, String description, String location, 
                            double latitude, double longitude, List<String> imageUrls) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrls = imageUrls;
    }

    public CreateIssueRequest(String title, String description, String location, 
                            double latitude, double longitude, List<String> imageUrls,
                            String reporterUid, String reporterName) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrls = imageUrls;
        this.reporterUid = reporterUid;
        this.reporterName = reporterName;
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getReporterUid() { return reporterUid; }
    public String getReporterName() { return reporterName; }

    // Setters
    public void setReporterUid(String reporterUid) { this.reporterUid = reporterUid; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
} 