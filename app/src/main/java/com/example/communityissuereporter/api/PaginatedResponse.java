package com.example.communityissuereporter.api;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> items;
    private int totalItems;
    private int currentPage;
    private int totalPages;

    public List<T> getItems() {
        return items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasMorePages() {
        return currentPage < totalPages;
    }
} 