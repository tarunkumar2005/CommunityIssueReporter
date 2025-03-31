package com.example.communityissuereporter.api;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("loginIdentifier")
    private String loginIdentifier;
    
    @SerializedName("password")
    private String password;

    public LoginRequest(String loginIdentifier, String password) {
        this.loginIdentifier = loginIdentifier;
        this.password = password;
    }

    public String getLoginIdentifier() {
        return loginIdentifier;
    }

    public void setLoginIdentifier(String loginIdentifier) {
        this.loginIdentifier = loginIdentifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}