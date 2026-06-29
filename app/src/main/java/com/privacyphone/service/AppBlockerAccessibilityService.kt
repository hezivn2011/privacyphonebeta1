package com.privacyphone.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.privacyphone.data.prefs.AppPreferences
import com.privacyphone.ui.BlockerActivity
import com.privacyphone.util.UsageStatsHelper

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var prefs: AppPreferences
    private var lastBlockedPackage = ""

    override fun onServiceConnected() {
        prefs = AppPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Skip own app and system
        if (packageName == "com.privacyphone" || packageName == "android") return

        val limitMinutes = prefs.getAppTimeLimit(packageName)
        if (limitMinutes <= 0) return

        val usedMinutes = UsageStatsHelper.getAppUsageToday(this, packageName)

        if (usedMinutes >= limitMinutes && packageName != lastBlockedPackage) {
            lastBlockedPackage = packageName
            launchBlocker(packageName, limitMinutes, usedMinutes)
        } else if (usedMinutes < limitMinutes) {
            if (lastBlockedPackage == packageName) lastBlockedPackage = ""
        }
    }

    private fun launchBlocker(packageName: String, limitMin: Int, usedMin: Long) {
        val intent = Intent(this, BlockerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("package_name", packageName)
            putExtra("limit_minutes", limitMin)
            putExtra("used_minutes", usedMin)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
