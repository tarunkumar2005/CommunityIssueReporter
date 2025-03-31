package com.example.communityissuereporter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.communityissuereporter.api.ApiClient;
import com.example.communityissuereporter.model.Issue;
import com.example.communityissuereporter.utils.DateUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class IssuesAdapter extends RecyclerView.Adapter<IssuesAdapter.IssueViewHolder> {
    private static final String TAG = "IssuesAdapter";
    
    private List<Issue> issues;
    private final Context context;
    private final OnIssueClickListener listener;
    private String currentSortOrder = "createdAt,desc"; // Default sort order

    public interface OnIssueClickListener {
        void onIssueClick(Issue issue);
        void onUpvoteClick(Issue issue);
    }

    public IssuesAdapter(Context context) {
        this.context = context;
        this.issues = new ArrayList<>();
        
        // Ensure context implements the listener interface
        if (context instanceof OnIssueClickListener) {
            this.listener = (OnIssueClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnIssueClickListener");
        }
    }
    
    public void setIssues(List<Issue> issues) {
        if (issues == null) {
            Log.w(TAG, "Attempted to set null issues list");
            this.issues = new ArrayList<>();
        } else {
            this.issues = new ArrayList<>(issues); // Create a new copy to avoid reference issues
        }
        
        // Sort the list based on current sort order
        sortIssues();
        
        // Notify adapter of data change
        notifyDataSetChanged();
    }
    
    public void setSortOrder(String sortOrder) {
        if (sortOrder != null && !sortOrder.equals(this.currentSortOrder)) {
            this.currentSortOrder = sortOrder;
            sortIssues();
            notifyDataSetChanged();
        }
    }
    
    private void sortIssues() {
        if (issues == null || issues.isEmpty()) {
            return;
        }
        
        try {
            // Parse the sort order (field,direction)
            final String[] sortParts = currentSortOrder.split(",");
            final String field = sortParts[0];
            final boolean ascending = sortParts.length > 1 && "asc".equals(sortParts[1]);
            
            // Create a comparator based on the sort field
            Comparator<Issue> comparator = null;
            
            switch (field) {
                case "createdAt":
                    comparator = (issue1, issue2) -> {
                        long time1 = issue1 != null ? issue1.getTimestamp() : 0;
                        long time2 = issue2 != null ? issue2.getTimestamp() : 0;
                        return ascending ? Long.compare(time1, time2) : Long.compare(time2, time1);
                    };
                    break;
                    
                case "updatedAt":
                    comparator = (issue1, issue2) -> {
                        // Safely get updated at time, falling back to created time if necessary
                        long time1 = issue1 != null ? 
                            (issue1.getUpdatedAt() > 0 ? issue1.getUpdatedAt() : issue1.getTimestamp()) : 0;
                        long time2 = issue2 != null ? 
                            (issue2.getUpdatedAt() > 0 ? issue2.getUpdatedAt() : issue2.getTimestamp()) : 0;
                        return ascending ? Long.compare(time1, time2) : Long.compare(time2, time1);
                    };
                    break;
                    
                case "upvotes":
                    comparator = (issue1, issue2) -> {
                        int votes1 = issue1 != null ? issue1.getUpvotes() : 0;
                        int votes2 = issue2 != null ? issue2.getUpvotes() : 0;
                        return ascending ? Integer.compare(votes1, votes2) : Integer.compare(votes2, votes1);
                    };
                    break;
                    
                default:
                    // Default to sorting by created date
                    comparator = (issue1, issue2) -> {
                        long time1 = issue1 != null ? issue1.getTimestamp() : 0;
                        long time2 = issue2 != null ? issue2.getTimestamp() : 0;
                        return Long.compare(time2, time1); // Default to newest first
                    };
                    break;
            }
            
            // Apply the sort
            if (comparator != null) {
                Collections.sort(issues, comparator);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sorting issues", e);
        }
    }

    @NonNull
    @Override
    public IssueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.issue_card_item, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IssueViewHolder holder, int position) {
        try {
            // Check if position is valid and issues list is not null
            if (position < 0 || position >= issues.size() || issues.get(position) == null) {
                Log.e(TAG, "Invalid position or null issue at position: " + position);
                return;
            }
            
        Issue issue = issues.get(position);
        Log.d(TAG, "Binding issue: " + issue.getId() + ", title: " + issue.getTitle());
        
            // Set title with null check
            if (holder.issueTitle != null) {
                holder.issueTitle.setText(issue.getTitle());
            }
            
            // Set description with null check
            if (holder.issueDescription != null) {
                String description = issue.getDescription();
                holder.issueDescription.setText(description != null ? description : "");
            }
            
            // Set location with null check
            if (holder.issueLocation != null) {
                String location = issue.getLocation();
                holder.issueLocation.setText(location != null ? location : "Location not specified");
            }
            
            // Set reporter name with null check
            if (holder.reporterName != null) {
                String reporterName = issue.getReporterName();
                holder.reporterName.setText(reporterName != null ? reporterName : "Unknown Reporter");
            }
            
            // Set date with null check and format
            if (holder.issueDate != null) {
                long timestamp = issue.getTimestamp();
                if (timestamp > 0) {
                    holder.issueDate.setText(DateUtils.getRelativeTimeSpan(timestamp));
                } else {
                    holder.issueDate.setText("Unknown date");
                }
            }
            
            // Set upvote count with null check
            if (holder.upvoteButton != null) {
                holder.upvoteButton.setText(String.valueOf(issue.getUpvotes()));
            }
            
            // Set status badge
            if (holder.statusBadge != null) {
                holder.statusBadge.setText(issue.getStatus());
                // Set background color based on status
                int colorResId;
                switch (issue.getStatus()) {
                    case "OPEN":
                        colorResId = R.color.status_open;
                break;
                    case "IN_PROGRESS":
                        colorResId = R.color.status_in_progress;
                break;
                    case "RESOLVED":
                        colorResId = R.color.status_resolved;
                break;
            default:
                        colorResId = R.color.status_default;
                break;
        }
        
                // Get color from resources and apply to drawable
                int statusColor = ContextCompat.getColor(context, colorResId);
                
                try {
                    // Create a new drawable with the specified color
                    GradientDrawable shape = new GradientDrawable();
                    
                    // Try to get the dimension, use fallback if not available
                    float cornerRadius;
                    try {
                        cornerRadius = context.getResources().getDimension(R.dimen.status_badge_corner_radius);
                    } catch (Exception e) {
                        // Fallback to 16dp converted to pixels
                        final float scale = context.getResources().getDisplayMetrics().density;
                        cornerRadius = 16 * scale;
                    }
                    
                    shape.setCornerRadius(cornerRadius);
                    shape.setColor(statusColor);
                    
                    // Set the drawable as background
                    ViewCompat.setBackground(holder.statusBadge, shape);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting status badge", e);
                    // Ultimate fallback - just set the color
                    holder.statusBadge.setBackgroundColor(statusColor);
                }
            }

            // Load image if available and if the ImageView exists in the layout
            if (holder.issueImage != null) {
                List<String> imageUrls = issue.getImageUrls();
                if (imageUrls != null && !imageUrls.isEmpty() && !TextUtils.isEmpty(imageUrls.get(0))) {
                    // Make the image view visible
                    holder.issueImage.setVisibility(View.VISIBLE);
                    
                    // Load the first image with Glide
                    try {
                        RequestOptions requestOptions = new RequestOptions()
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.error_image)
                                .diskCacheStrategy(DiskCacheStrategy.ALL);
                        
                        Glide.with(context)
                                .load(imageUrls.get(0))
                                .apply(requestOptions)
                                .into(holder.issueImage);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading image with Glide", e);
                        // Show error image
                        holder.issueImage.setImageResource(R.drawable.error_image);
                    }
                } else {
                    // If no image URL, hide the image view
                    holder.issueImage.setVisibility(View.GONE);
                }
            }
            
            // Set click listeners
            // For the entire item
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < issues.size()) {
                        listener.onIssueClick(issues.get(adapterPosition));
                    }
                }
            });
            
            // For the upvote button
            if (holder.upvoteButton != null) {
                holder.upvoteButton.setOnClickListener(v -> {
                    if (listener != null) {
                        int adapterPosition = holder.getAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < issues.size()) {
                            listener.onUpvoteClick(issues.get(adapterPosition));
                        }
                    }
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder", e);
        }
    }
    
    @Override
    public int getItemCount() {
        return issues != null ? issues.size() : 0;
    }

    static class IssueViewHolder extends RecyclerView.ViewHolder {
        final TextView issueTitle;
        final TextView issueDescription;
        final TextView issueLocation;
        final TextView reporterName;
        final TextView issueDate;
        final TextView statusBadge;
        final MaterialButton upvoteButton;
        final ImageButton issueMenuButton;
        final ImageView issueImage;
        
        IssueViewHolder(@NonNull View itemView) {
            super(itemView);
            issueTitle = itemView.findViewById(R.id.issueTitle);
            issueDescription = itemView.findViewById(R.id.issueDescription);
            issueLocation = itemView.findViewById(R.id.issueLocation);
            reporterName = itemView.findViewById(R.id.reporterName);
            issueDate = itemView.findViewById(R.id.issueDate);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            upvoteButton = itemView.findViewById(R.id.upvoteButton);
            issueMenuButton = itemView.findViewById(R.id.issueMenuButton);
            
            // The image view might not exist in older layouts
            ImageView imageView = itemView.findViewById(R.id.issueImage);
            issueImage = imageView; // This might be null if not in layout
        }
    }
}