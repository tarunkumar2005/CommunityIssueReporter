package com.example.communityissuereporter.api;

import java.util.Map;

public class UserProfileRequest {
    private String displayName;
    private String email;
    private String bio;
    private String location;
    private String profileImageUrl;
    private String themePreference;
    private Map<String, Boolean> notificationPreferences;

    public UserProfileRequest() {
        // Required empty constructor for serialization
    }

    public UserProfileRequest(String displayName, String email, String bio, String location, 
                              String profileImageUrl, String themePreference, 
                              Map<String, Boolean> notificationPreferences) {
        this.displayName = displayName;
        this.email = email;
        this.bio = bio;
        this.location = location;
        this.profileImageUrl = profileImageUrl;
        this.themePreference = themePreference;
        this.notificationPreferences = notificationPreferences;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public void setThemePreference(String themePreference) {
        this.themePreference = themePreference;
    }

    public Map<String, Boolean> getNotificationPreferences() {
        return notificationPreferences;
    }

    public void setNotificationPreferences(Map<String, Boolean> notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }
}
