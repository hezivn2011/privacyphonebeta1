package com.privacyphone.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.privacyphone.model.AppInfo
import java.util.Calendar

object UsageStatsHelper {

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsageMinutes(context: Context): Long {
        if (!hasUsagePermission(context)) return 0L
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startTime = cal.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, System.currentTimeMillis()
        )

        val totalMs = stats
            .filter { it.packageName != "com.privacyphone" }
            .sumOf { it.totalTimeInForeground }

        return totalMs / 60000L
    }

    fun getAppUsageToday(context: Context, packageName: String): Long {
        if (!hasUsagePermission(context)) return 0L
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startTime = cal.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, System.currentTimeMillis()
        )
        val appStat = stats.find { it.packageName == packageName }
        return (appStat?.totalTimeInForeground ?: 0L) / 60000L
    }

    fun getWeeklyUsage(context: Context): List<Long> {
        if (!hasUsagePermission(context)) return List(7) { 0L }
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val result = mutableListOf<Long>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            val end = cal.timeInMillis

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, start, end
            )
            val total = stats.filter { it.packageName != "com.privacyphone" }
                .sumOf { it.totalTimeInForeground } / 60000L
            result.add(total)
        }
        return result
    }

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return installedApps
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { it.packageName != "com.privacyphone" }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    usedTodayMinutes = getAppUsageToday(context, appInfo.packageName)
                )
            }
            .sortedByDescending { it.usedTodayMinutes }
    }

    fun formatMinutes(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}'" else "${m}'"
    }

    fun formatMinutesShort(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h${m}'" else "${m}m"
    }
}
