package com.example.communityissuereporter.api;

/**
 * Request class for adding an image to an issue
 */
public class UploadImageRequest {
    private String issueId;
    private String imageUrl;
    private String uploadedBy;

    public UploadImageRequest(String issueId, String imageUrl, String uploadedBy) {
        this.issueId = issueId;
        this.imageUrl = imageUrl;
        this.uploadedBy = uploadedBy;
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
