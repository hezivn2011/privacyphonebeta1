package com.privacyphone.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var timeLimitMinutes: Int = 60,
    var usedTodayMinutes: Long = 0L,
    var isEnabled: Boolean = true
)

data class MediaItem(
    val path: String,
    val type: MediaType,
    val sensitivityScore: Float,
    val label: String
)

enum class MediaType { IMAGE, VIDEO }

data class UsageData(
    val packageName: String,
    val appName: String,
    val totalMinutes: Long,
    val dayData: List<Long> // last 7 days usage in minutes
)
