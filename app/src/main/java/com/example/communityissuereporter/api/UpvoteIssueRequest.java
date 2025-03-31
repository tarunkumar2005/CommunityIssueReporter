package com.example.communityissuereporter.api;

/**
 * Request model for upvoting an issue.
 * This matches the backend UpvoteIssueRequest DTO.
 */
public class UpvoteIssueRequest {
    private String userId;

    public UpvoteIssueRequest() {
    }

    public UpvoteIssueRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
} 