package com.example.communityissuereporter.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.communityissuereporter.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * Utility class for managing UI operations and feedback
 */
public class UIUtils {
    private static final String TAG = "UIUtils";

    /**
     * Shows a styled error toast with the specified message
     * 
     * @param context The context
     * @param message The error message to display
     */
    public static void showErrorToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        View view = toast.getView();
        view.setBackgroundResource(R.drawable.error_background);
        TextView text = view.findViewById(android.R.id.message);
        if (text != null) {
            text.setTextColor(ContextCompat.getColor(context, R.color.white));
        }
        toast.show();
    }

    /**
     * Shows a styled success toast with the specified message
     * 
     * @param context The context
     * @param message The success message to display
     */
    public static void showSuccessToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        View view = toast.getView();
        view.setBackgroundResource(R.drawable.success_background);
        TextView text = view.findViewById(android.R.id.message);
        if (text != null) {
            text.setTextColor(ContextCompat.getColor(context, R.color.white));
        }
        toast.show();
    }

    /**
     * Shows a material design error snackbar
     * 
     * @param view The view to attach the snackbar to
     * @param message The error message to display
     */
    public static void showErrorSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.error_red));
        snackbar.setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
        snackbar.show();
    }

    /**
     * Shows a material design success snackbar
     * 
     * @param view The view to attach the snackbar to
     * @param message The success message to display
     */
    public static void showSuccessSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.success_green));
        snackbar.setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
        snackbar.show();
    }

    /**
     * Shows a confirmation dialog with custom styling
     * 
     * @param context The context
     * @param title The dialog title
     * @param message The dialog message
     * @param positiveButtonText The positive button text
     * @param negativeButtonText The negative button text
     * @param positiveAction The action to perform when positive button is clicked
     */
    public static void showConfirmationDialog(
            Context context,
            String title,
            String message,
            String positiveButtonText,
            String negativeButtonText,
            Runnable positiveAction) {
        
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    if (positiveAction != null) {
                        positiveAction.run();
                    }
                })
                .setNegativeButton(negativeButtonText, (dialog, which) -> dialog.dismiss())
                .setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_dialog_background))
                .show();
    }

    /**
     * Creates an error handling message with standardized formats based on error types
     * 
     * @param context The context
     * @param throwable The error that occurred
     * @return A user-friendly error message
     */
    public static String getErrorMessage(Context context, Throwable throwable) {
        if (throwable == null) {
            return context.getString(R.string.error_unknown);
        }
        
        // Get the error message based on exception type
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            return context.getString(R.string.error_unknown);
        }
        
        if (message.contains("timeout")) {
            return context.getString(R.string.error_timeout);
        } else if (message.contains("Unable to resolve host") || 
                   message.contains("No address associated with hostname")) {
            return context.getString(R.string.error_no_internet);
        } else if (message.contains("401")) {
            return context.getString(R.string.error_unauthorized);
        } else if (message.contains("403")) {
            return context.getString(R.string.error_forbidden);
        } else if (message.contains("404")) {
            return context.getString(R.string.error_not_found);
        } else if (message.contains("500")) {
            return context.getString(R.string.error_server);
        }
        
        return context.getString(R.string.error_general);
    }

    /**
     * Adds fade-in animation to a view
     * 
     * @param view The view to animate
     * @param duration The animation duration in milliseconds
     */
    public static void fadeIn(View view, int duration) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(duration);
        view.startAnimation(anim);
        view.setVisibility(View.VISIBLE);
    }

    /**
     * Adds fade-out animation to a view
     * 
     * @param view The view to animate
     * @param duration The animation duration in milliseconds
     */
    public static void fadeOut(View view, int duration) {
        AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(duration);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(anim);
    }
} 