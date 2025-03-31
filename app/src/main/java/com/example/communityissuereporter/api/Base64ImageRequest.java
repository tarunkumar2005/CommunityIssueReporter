package com.example.communityissuereporter.api;

public class Base64ImageRequest {
    private String base64Image;
    private String userId;
    private String filename;
    private String type;

    public Base64ImageRequest() {
        // Required empty constructor for serialization
    }

    public Base64ImageRequest(String base64Image, String userId, String filename, String type) {
        this.base64Image = base64Image;
        this.userId = userId;
        this.filename = filename;
        this.type = type;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
} 