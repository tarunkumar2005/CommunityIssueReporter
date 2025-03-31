package com.example.communityissuereporter.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class User {
    @SerializedName("uid")
    private String uid;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("display_name")
    private String displayName;
    
    @SerializedName("role")
    private String role;
    
    @SerializedName("location")
    private String location;
    
    @SerializedName("bio")
    private String bio;
    
    @SerializedName("profile_image_url")
    private String profileImageUrl;
    
    @SerializedName("account_creation_date")
    private long accountCreationDate;
    
    @SerializedName("last_login")
    private long lastLogin;
    
    @SerializedName("notification_preferences")
    private Map<String, Boolean> notificationPreferences;

    @SerializedName("theme_preference")
    private String themePreference;
    
    @SerializedName("stats")
    private UserStats stats;

    // Default constructor
    public User() {
        this.notificationPreferences = new HashMap<>();
        this.notificationPreferences.put("ownIssues", true);
        this.notificationPreferences.put("communityActivity", false);
        this.themePreference = "light";
        this.stats = new UserStats();
    }

    // Constructor with essential fields
    public User(String uid, String username, String email) {
        this();
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.role = "USER";
        this.accountCreationDate = System.currentTimeMillis();
        this.lastLogin = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getAccountCreationDate() {
        return accountCreationDate;
    }

    public void setAccountCreationDate(long accountCreationDate) {
        this.accountCreationDate = accountCreationDate;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Map<String, Boolean> getNotificationPreferences() {
        return notificationPreferences;
    }

    public void setNotificationPreferences(Map<String, Boolean> notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }

    public UserStats getStats() {
        return stats;
    }

    public void setStats(UserStats stats) {
        this.stats = stats;
    }

    // Inner class for user statistics
    public static class UserStats {
        @SerializedName("issues_reported")
        private int issuesReported;

        @SerializedName("upvotes_received")
        private int upvotesReceived;

        @SerializedName("resolved_issues")
        private int resolvedIssues;

        public UserStats() {
            this.issuesReported = 0;
            this.upvotesReceived = 0;
            this.resolvedIssues = 0;
        }

        public int getIssuesReported() {
            return issuesReported;
        }

        public void setIssuesReported(int issuesReported) {
            this.issuesReported = issuesReported;
        }

        public int getUpvotesReceived() {
            return upvotesReceived;
        }

        public void setUpvotesReceived(int upvotesReceived) {
            this.upvotesReceived = upvotesReceived;
        }

        public int getResolvedIssues() {
            return resolvedIssues;
        }

        public void setResolvedIssues(int resolvedIssues) {
            this.resolvedIssues = resolvedIssues;
        }

        public void incrementIssuesReported() {
            this.issuesReported++;
        }

        public void incrementUpvotesReceived() {
            this.upvotesReceived++;
        }

        public void incrementResolvedIssues() {
            this.resolvedIssues++;
        }
    }
}
