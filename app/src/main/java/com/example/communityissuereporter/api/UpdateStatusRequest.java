package com.example.communityissuereporter.api;

public class UpdateStatusRequest {
    private String status;
    private String comment;

    public UpdateStatusRequest(String status, String comment) {
        this.status = status;
        this.comment = comment;
    }

    // Getters
    public String getStatus() { return status; }
    public String getComment() { return comment; }
} 