package com.example.communityissuereporter.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class to handle date formatting throughout the app
 */
public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    
    /**
     * Returns a relative time span string for the given timestamp
     * (e.g., "2 days ago", "5 minutes ago", "just now")
     *
     * @param timestamp The timestamp in milliseconds
     * @return A human-readable relative time string
     */
    public static String getRelativeTimeSpan(long timestamp) {
        try {
            long now = System.currentTimeMillis();
            
            // Use Android's DateUtils for relative time formatting without import
            return android.text.format.DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    now,
                    android.text.format.DateUtils.MINUTE_IN_MILLIS,
                    android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString();
        } catch (Exception e) {
            // Fallback to regular date format if there's an error
            return formatDate(timestamp);
        }
    }
    
    /**
     * Formats a timestamp into a date string (e.g., "Apr 15, 2023")
     *
     * @param timestamp The timestamp in milliseconds
     * @return A formatted date string
     */
    public static String formatDate(long timestamp) {
        try {
            return DATE_FORMAT.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown date";
        }
    }
    
    /**
     * Formats a timestamp into a date and time string (e.g., "Apr 15, 2023 14:30")
     *
     * @param timestamp The timestamp in milliseconds
     * @return A formatted date and time string
     */
    public static String formatDateTime(long timestamp) {
        try {
            return DATE_TIME_FORMAT.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown date";
        }
    }
    
    /**
     * Formats a Date object into a string using the specified pattern
     *
     * @param date The Date object to format
     * @param pattern The pattern to use for formatting (e.g., "MMMM yyyy")
     * @return A formatted date string
     */
    public static String formatDateToString(Date date, String pattern) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());
            return formatter.format(date);
        } catch (Exception e) {
            return "Unknown date";
        }
    }
    
    /**
     * Checks if a timestamp is today
     *
     * @param timestamp The timestamp in milliseconds
     * @return True if the timestamp is from today, false otherwise
     */
    public static boolean isToday(long timestamp) {
        return android.text.format.DateUtils.isToday(timestamp);
    }
} 