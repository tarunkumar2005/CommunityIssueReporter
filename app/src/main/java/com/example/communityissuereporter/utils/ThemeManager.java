package com.example.communityissuereporter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class to manage theme settings across the app
 */
public class ThemeManager {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    /**
     * Apply the saved theme or default to system theme
     * @param context Application context
     */
    public static void applyTheme(Context context) {
        String themeMode = getThemeMode(context);
        applyTheme(themeMode);
    }

    /**
     * Apply a specific theme mode
     * @param themeMode The theme mode to apply (light, dark, or system)
     */
    public static void applyTheme(String themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
        }
    }

    /**
     * Save the user's theme preference
     * @param context Application context
     * @param themeMode The theme mode to save (light, dark, or system)
     */
    public static void saveThemeMode(Context context, String themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME_MODE, themeMode).apply();
    }

    /**
     * Get the currently saved theme mode
     * @param context Application context
     * @return The current theme mode (defaults to system if not set)
     */
    public static String getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME_MODE, THEME_SYSTEM);
    }

    /**
     * Check if the current theme is in dark mode
     * @param context Application context
     * @return true if in dark mode, false otherwise
     */
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & 
                Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
} 