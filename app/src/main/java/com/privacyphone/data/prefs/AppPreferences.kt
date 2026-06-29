package com.privacyphone.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "privacy_phone_prefs"
        private const val KEY_PIN = "user_pin"
        private const val KEY_SETUP_DONE = "setup_done"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER = "security_answer"
        private const val KEY_SENSITIVE_COUNT = "sensitive_count"
        private const val KEY_BLOCKED_TODAY = "blocked_today"
        private const val KEY_SCAN_ENABLED = "scan_enabled"
        private const val KEY_APP_LIMITS = "app_limits"
        private const val KEY_AI_EVALUATE = "ai_evaluate_cache"
    }

    var pin: String
        get() = prefs.getString(KEY_PIN, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PIN, value) }

    var isSetupDone: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_SETUP_DONE, value) }

    var securityQuestion: String
        get() = prefs.getString(KEY_SECURITY_QUESTION, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SECURITY_QUESTION, value) }

    var securityAnswer: String
        get() = prefs.getString(KEY_SECURITY_ANSWER, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SECURITY_ANSWER, value) }

    var sensitiveCount: Int
        get() = prefs.getInt(KEY_SENSITIVE_COUNT, 0)
        set(value) = prefs.edit { putInt(KEY_SENSITIVE_COUNT, value) }

    var blockedToday: Int
        get() = prefs.getInt(KEY_BLOCKED_TODAY, 0)
        set(value) = prefs.edit { putInt(KEY_BLOCKED_TODAY, value) }

    var scanEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCAN_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SCAN_ENABLED, value) }

    var aiEvaluateCache: String
        get() = prefs.getString(KEY_AI_EVALUATE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_AI_EVALUATE, value) }

    fun setAppTimeLimit(packageName: String, minutes: Int) {
        prefs.edit { putInt("limit_$packageName", minutes) }
    }

    fun getAppTimeLimit(packageName: String): Int {
        return prefs.getInt("limit_$packageName", 60) // default 60 min
    }

    fun removeAppTimeLimit(packageName: String) {
        prefs.edit { remove("limit_$packageName") }
    }

    fun getAllLimitedApps(): Set<String> {
        return prefs.all.keys
            .filter { it.startsWith("limit_") }
            .map { it.removePrefix("limit_") }
            .toSet()
    }
}
