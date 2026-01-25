package com.example.apptrack.data

import android.graphics.drawable.Drawable

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long, // in milliseconds
    val lastTimeUsed: Long,
    val launchCount: Int,
    val appIcon: Drawable? = null
)

data class UsageStats(
    val totalScreenTime: Long, // in milliseconds
    val totalAppsUsed: Int,
    val mostUsedApp: AppUsageInfo?,
    val apps: List<AppUsageInfo>
)
