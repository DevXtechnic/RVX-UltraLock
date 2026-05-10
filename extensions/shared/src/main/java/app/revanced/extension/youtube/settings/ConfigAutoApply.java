package app.revanced.extension.youtube.settings;

import android.content.Context;
import android.content.SharedPreferences;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;

/**
 * Enforces the minimalist "focus" configuration while UltraLock is active.
 *
 * Called from {@link YouTubeActivityHook#initialize(android.app.Activity)} before
 * {@link UltraLockManager#gateSettingsAccess} so settings are locked-in on every
 * cold start, even after a phone reboot or cache clear.
 *
 * IMPORTANT: This class writes directly to the "revanced" SharedPreferences
 * using a single batched editor.apply() to avoid:
 *   1. Triggering Settings class static initializer before context is ready
 *   2. Flooding the binder with 60+ individual write transactions
 *      (which causes "Too many Binders sent to SYSTEM" kill on Android 9)
 */
public final class ConfigAutoApply {

    private static final String REVANCED_PREFS = "revanced";
    private static final String PREFS_NAME = "revanced_config_auto_apply";
    private static final String KEY_INITIALIZED = "initialized";
    private static boolean hasChanges = false;

    private ConfigAutoApply() {}

    /**
     * Injection point — called from {@link YouTubeActivityHook#initialize}.
     *
     * @param context  any non-null Context (the Activity is fine)
     */
    public static void enforceIfLocked(Context context) {
        try {
            if (!BaseSettings.ULTRA_LOCK_ENABLED.get()) {
                return;
            }
            applyMinimalistConfig(context);
        } catch (Exception ex) {
            Logger.printException(() -> "ConfigAutoApply failure", ex);
        }
    }

    private static void setBool(SharedPreferences prefs, SharedPreferences.Editor editor, String key, boolean value) {
        if (prefs.getBoolean(key, !value) != value) {
            editor.putBoolean(key, value);
            hasChanges = true;
        }
    }

    private static void setStr(SharedPreferences prefs, SharedPreferences.Editor editor, String key, String value) {
        if (!value.equals(prefs.getString(key, null))) {
            editor.putString(key, value);
            hasChanges = true;
        }
    }

    // -------------------------------------------------------------------------
    // Core enforcement — batched, idempotent, safe to call every launch
    // -------------------------------------------------------------------------

    private static void applyMinimalistConfig(Context context) {
        Logger.printDebug(() -> "ConfigAutoApply: bypass applied (user requested lock-only mode)");
        // Settings are no longer forced onto the user.
    }

}
