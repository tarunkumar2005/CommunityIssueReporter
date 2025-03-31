package com.example.communityissuereporter.api;

import com.google.gson.annotations.SerializedName;

public class GoogleAuthRequest {
    @SerializedName("idToken")
    private String idToken;
    
    @SerializedName("email")
    private String email;

    public GoogleAuthRequest(String idToken, String email) {
        this.idToken = idToken;
        this.email = email;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}