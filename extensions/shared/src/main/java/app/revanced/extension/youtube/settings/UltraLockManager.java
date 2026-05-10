package app.revanced.extension.youtube.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;

import android.widget.EditText;

import androidx.annotation.NonNull;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

public final class UltraLockManager {
    private static final String PREFS_NAME = "revanced_ultra_lock";
    private static final String KEY_LIMITED_TIME_MS = "limited_time_ms";
    private static final String KEY_IS_LOCKED = "is_locked";

    private UltraLockManager() {
    }

    public static void promptLockDuration(@NonNull Activity activity, @NonNull Runnable onConfirmed, @NonNull Runnable onCancelled) {
        String[] options = {"Lifetime", "Limited Time"};
        new AlertDialog.Builder(activity)
                .setTitle("Choose Lock Duration")
                .setCancelable(false)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        saveDuration(activity, 0L);
                        onConfirmed.run();
                    } else {
                        promptHours(activity, onConfirmed, onCancelled);
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> onCancelled.run())
                .show();
    }

    private static void promptHours(@NonNull Activity activity, @NonNull Runnable onConfirmed, @NonNull Runnable onCancelled) {
        EditText input = new EditText(activity);
        input.setHint("Enter duration in hours");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        
        int pad = Utils.dipToPixels(20);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(activity)
                .setTitle("Limited Time Lock")
                .setMessage("How many hours should the settings be locked for?")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Lock", (dialog, which) -> {
                    try {
                        long hours = Long.parseLong(input.getText().toString().trim());
                        if (hours <= 0) {
                            Utils.showToastShort("Please enter a valid number of hours.");
                            onCancelled.run();
                            return;
                        }
                        long unlockMs = System.currentTimeMillis() + (hours * 3600L * 1000L);
                        saveDuration(activity, unlockMs);
                        onConfirmed.run();
                    } catch (Exception e) {
                        Utils.showToastShort("Invalid input.");
                        onCancelled.run();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> onCancelled.run())
                .show();
    }

    private static void saveDuration(@NonNull Activity activity, long unlockMs) {
        activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LIMITED_TIME_MS, unlockMs)
                .apply();
        Logger.printInfo(() -> "UltraLock: saveDuration unlockMs=" + unlockMs);
    }

    public static void setLockEnabled(@NonNull Activity activity, boolean enabled) {
        activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOCKED, enabled)
                .apply();
    }

    public static boolean isLocked(@NonNull Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        boolean settingEnabled = prefs.getBoolean(KEY_IS_LOCKED, false);
        Logger.printInfo(() -> "UltraLock: isLocked called. is_locked=" + settingEnabled);
        
        if (!settingEnabled) {
            return false;
        }

        long unlockMs = prefs.getLong(KEY_LIMITED_TIME_MS, 0L);
        long now = System.currentTimeMillis();
        Logger.printInfo(() -> "UltraLock: unlockMs=" + unlockMs + " now=" + now);
        
        if (unlockMs == 0L) {
            // Lifetime lock
            Logger.printInfo(() -> "UltraLock: Lifetime lock active.");
            return true;
        }

        if (now < unlockMs) {
            // Limited time lock still active
            Logger.printInfo(() -> "UltraLock: Limited time lock active. Remaining=" + ((unlockMs - now) / 60000L) + " min");
            return true;
        }

        // Lock expired
        Logger.printInfo(() -> "UltraLock: Lock expired. Disabling.");
        prefs.edit().putBoolean(KEY_IS_LOCKED, false).putLong(KEY_LIMITED_TIME_MS, 0L).apply();
        // Also sync with BaseSettings just in case UI reads from it initially
        try {
            app.revanced.extension.shared.settings.BaseSettings.ULTRA_LOCK_ENABLED.save(false);
        } catch (Exception ignored) {}
        
        return false;
    }

    public static String getLockStatusMessage(@NonNull Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        long unlockMs = prefs.getLong(KEY_LIMITED_TIME_MS, 0L);
        
        if (unlockMs == 0L) {
            return "Settings are permanently locked.";
        }
        
        long remainingMs = unlockMs - System.currentTimeMillis();
        long totalMinutes = Math.max(1L, remainingMs / (60L * 1000L));
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        
        if (hours == 0L) {
            return "Settings locked for " + minutes + " more minutes.";
        }
        return "Settings locked for " + hours + " hours and " + minutes + " minutes.";
    }
}