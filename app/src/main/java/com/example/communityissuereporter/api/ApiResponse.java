package com.example.communityissuereporter.api;

public class ApiResponse<T> {
    private T data;
    private String status;
    private String message;

    public ApiResponse() {
        // Required empty constructor for serialization
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Utility method to check success status
    public boolean isSuccess() {
        return "success".equals(status);
    }
} 