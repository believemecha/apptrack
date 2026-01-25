package com.example.apptrack.data

import android.app.usage.UsageStats as AndroidUsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class UsageStatsRepository(private val context: Context) {

    private val usageStatsManager: UsageStatsManager? by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    private val packageManager: PackageManager = context.packageManager

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun getUsageStatsForDate(selectedDate: Calendar = Calendar.getInstance()): UsageStats = withContext(Dispatchers.IO) {
        // Get start and end of the selected day
        // IMPORTANT: UsageStatsManager with INTERVAL_DAILY returns data for ENTIRE daily intervals
        // that fall within the time range. So we query from midnight to next midnight to get exactly one day.
        val dayCalendar = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = dayCalendar.timeInMillis
        dayCalendar.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = dayCalendar.timeInMillis
        
        // The library returns UsageStats objects where each object contains:
        // - packageName: The app package
        // - totalTimeInForeground: Total time in milliseconds for that day (already aggregated)
        // - lastTimeUsed: Last time the app was used
        // For a single day query, we should get one UsageStats entry per app that was used that day
        
        // Query usage stats for the selected day
        // INTERVAL_DAILY returns data aggregated by day
        // For a single day query (startTime to endTime = 24 hours), we should get one entry per app
        // But the library might return multiple entries if the day spans multiple daily buckets
        val usageStatsList = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()
        
        Log.d("UsageStats", "=== Query Info ===")
        Log.d("UsageStats", "Start Time: $startTime (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(startTime))})")
        Log.d("UsageStats", "End Time: $endTime (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(endTime))})")
        Log.d("UsageStats", "Total entries returned: ${usageStatsList.size}")
        
        // IMPORTANT: With INTERVAL_DAILY, each UsageStats entry represents usage for ONE complete day
        // If we query exactly one day (midnight to next midnight), we should get one entry per app
        // However, we still aggregate in case there are multiple entries (shouldn't happen for single day)
        val aggregatedStats = mutableMapOf<String, Pair<Long, Long>>() // packageName -> (totalTime, lastTimeUsed)
        
        var totalTimeBeforeFilter = 0L
        var duplicateCount = 0
        
        // Track all time fields for comparison
        var totalTimeInForeground = 0L
        var totalTimeVisible = 0L
        var totalTimeForegroundService = 0L
        
        // Log first entry to show all available time fields
        if (usageStatsList.isNotEmpty()) {
            val firstStat = usageStatsList.first()
            Log.d("UsageStats", "=== Sample UsageStats Record Fields ===")
            Log.d("UsageStats", "Package: ${firstStat.packageName}")
            Log.d("UsageStats", "getTotalTimeInForeground(): ${firstStat.totalTimeInForeground}ms")
            Log.d("UsageStats", "getTotalTimeVisible(): ${firstStat.totalTimeVisible}ms")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d("UsageStats", "getTotalTimeForegroundServiceUsed(): ${firstStat.totalTimeForegroundServiceUsed}ms")
            }
            Log.d("UsageStats", "getLastTimeUsed(): ${firstStat.lastTimeUsed} (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(firstStat.lastTimeUsed))})")
            Log.d("UsageStats", "getLastTimeVisible(): ${firstStat.lastTimeVisible} (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(firstStat.lastTimeVisible))})")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d("UsageStats", "getLastTimeForegroundServiceUsed(): ${firstStat.lastTimeForegroundServiceUsed}")
            }
            Log.d("UsageStats", "getFirstTimeStamp(): ${firstStat.firstTimeStamp} (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(firstStat.firstTimeStamp))})")
            Log.d("UsageStats", "getLastTimeStamp(): ${firstStat.lastTimeStamp} (${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(firstStat.lastTimeStamp))})")
            Log.d("UsageStats", "=====================================")
        }
        
        usageStatsList.forEach { stat ->
            // Sum all time fields for comparison
            totalTimeInForeground += stat.totalTimeInForeground
            totalTimeVisible += stat.totalTimeVisible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                totalTimeForegroundService += stat.totalTimeForegroundServiceUsed
            }
            
            // Use totalTimeVisible for screen time (time app was actually visible on screen)
            // This is more accurate than totalTimeInForeground for "screen time" calculation
            val visibleTime = stat.totalTimeVisible
            totalTimeBeforeFilter += visibleTime
            
            // Each stat already contains the total time for that day
            // IMPORTANT: For a single day query, we should only get ONE entry per app
            // If we get duplicates, we take the MAXIMUM value (not sum) to avoid double counting
            // Digital Wellbeing likely does the same - takes one value per app per day
            val existing = aggregatedStats[stat.packageName]
            if (existing != null) {
                duplicateCount++
                // Take the MAXIMUM time, not the sum - this prevents double counting
                val maxTime = maxOf(existing.first, visibleTime)
                Log.d("UsageStats", "DUPLICATE found for ${stat.packageName}: existing=${existing.first}ms, new=${visibleTime}ms, using MAX=${maxTime}ms")
                aggregatedStats[stat.packageName] = Pair(
                    maxTime, // Use MAX, not SUM
                    maxOf(existing.second, stat.lastTimeUsed)
                )
            } else {
                // First entry for this package - use it directly
                aggregatedStats[stat.packageName] = Pair(
                    visibleTime,
                    stat.lastTimeUsed
                )
            }
        }
        
        Log.d("UsageStats", "=== Time Fields Comparison (ALL APPS, BEFORE FILTERING) ===")
        Log.d("UsageStats", "Total getTotalTimeInForeground(): ${totalTimeInForeground}ms (${totalTimeInForeground / 1000 / 60} min, ${totalTimeInForeground / 1000 / 3600.0} hours)")
        Log.d("UsageStats", "Total getTotalTimeVisible(): ${totalTimeVisible}ms (${totalTimeVisible / 1000 / 60} min, ${totalTimeVisible / 1000 / 3600.0} hours)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d("UsageStats", "Total getTotalTimeForegroundServiceUsed(): ${totalTimeForegroundService}ms (${totalTimeForegroundService / 1000 / 60} min)")
        }
        Log.d("UsageStats", "Difference (Foreground - Visible): ${totalTimeInForeground - totalTimeVisible}ms (${(totalTimeInForeground - totalTimeVisible) / 1000 / 60} min)")
        Log.d("UsageStats", "Total time before filtering (using Visible): ${totalTimeBeforeFilter}ms (${totalTimeBeforeFilter / 1000 / 60} minutes)")
        Log.d("UsageStats", "Duplicate entries found: $duplicateCount")
        Log.d("UsageStats", "Unique apps before filtering: ${aggregatedStats.size}")

        // Get all apps that have usage stats (from aggregated data)
        val appUsageList = mutableListOf<AppUsageInfo>()
        
        var excludedCount = 0
        var excludedTime = 0L
        var includedCount = 0
        var includedTime = 0L
        
        aggregatedStats.forEach { (packageName, stats) ->
            val (totalTime, lastTimeUsed) = stats
            
            // Only show apps with usage > 0
            if (totalTime <= 0) {
                excludedCount++
                return@forEach
            }
            
            // Try to get app info to get name and icon
            val appInfo = try {
                packageManager.getApplicationInfo(packageName, 0)
            } catch (e: Exception) {
                null
            }
            
            // Skip if app is not in our filtered list (hidden system apps)
            if (appInfo != null && !shouldIncludeApp(appInfo)) {
                excludedCount++
                excludedTime += totalTime
                Log.d("UsageStats", "EXCLUDED: $packageName - ${totalTime}ms (${totalTime / 1000 / 60} min)")
                return@forEach
            }
            
            includedCount++
            includedTime += totalTime
            
            val appName = try {
                if (appInfo != null) {
                    packageManager.getApplicationLabel(appInfo).toString()
                } else {
                    packageName
                }
            } catch (e: Exception) {
                packageName
            }

            val appIcon = try {
                packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }

            appUsageList.add(
                AppUsageInfo(
                    packageName = packageName,
                    appName = appName,
                    totalTimeInForeground = totalTime, // This now contains totalTimeVisible
                    lastTimeUsed = lastTimeUsed,
                    launchCount = 0, // Launch count not directly available in UsageStats
                    appIcon = appIcon
                )
            )
        }

        // Sort by usage time (descending)
        val sortedApps = appUsageList.sortedByDescending { it.totalTimeInForeground }
        
        val totalScreenTime = sortedApps.sumOf { it.totalTimeInForeground }
        
        Log.d("UsageStats", "=== Filtering Results ===")
        Log.d("UsageStats", "Excluded apps: $excludedCount (${excludedTime / 1000 / 60} minutes)")
        Log.d("UsageStats", "Included apps: $includedCount (${includedTime / 1000 / 60} minutes)")
        Log.d("UsageStats", "Final screen time (using totalTimeVisible): ${totalScreenTime}ms (${totalScreenTime / 1000 / 60} minutes, ${totalScreenTime / 1000 / 3600.0} hours)")
        Log.d("UsageStats", "")
        Log.d("UsageStats", "=== COMPARISON WITH DIGITAL WELLBEING ===")
        Log.d("UsageStats", "Compare this 'Final screen time' value with Digital Wellbeing's screen time")
        Log.d("UsageStats", "If it's still higher, Digital Wellbeing might be:")
        Log.d("UsageStats", "  1. Using totalTimeInForeground instead of totalTimeVisible")
        Log.d("UsageStats", "  2. Excluding more apps than we are")
        Log.d("UsageStats", "  3. Using a different calculation method")
        Log.d("UsageStats", "Top 5 apps:")
        sortedApps.take(5).forEachIndexed { index, app ->
            Log.d("UsageStats", "  ${index + 1}. ${app.appName} (${app.packageName}): ${app.totalTimeInForeground / 1000 / 60} min")
        }
        Log.d("UsageStats", "========================")

        com.example.apptrack.data.UsageStats(
            totalScreenTime = totalScreenTime,
            totalAppsUsed = sortedApps.size,
            mostUsedApp = sortedApps.firstOrNull(),
            apps = sortedApps
        )
    }

    private fun shouldIncludeApp(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName
        
        // Exclude ALL packages starting with "android" or "com.android" - Digital Wellbeing excludes these
        if (packageName.startsWith("android.") || packageName.startsWith("com.android.")) {
            return false
        }
        
        // Exclude system packages from other manufacturers too
        val excludedPrefixes = listOf(
            "com.samsung.android", // Samsung system apps
            "com.huawei.android", // Huawei system apps
            "com.miui", // MIUI system apps
            "com.oneplus", // OnePlus system apps
            "com.oppo", // OPPO system apps
            "com.vivo", // Vivo system apps
            "com.xiaomi", // Xiaomi system apps
        )
        
        if (excludedPrefixes.any { packageName.startsWith(it) }) {
            return false
        }
        
        // ONLY include user-installed apps (FLAG_SYSTEM == 0)
        // Digital Wellbeing typically only counts apps that users explicitly installed
        val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        
        if (!isUserApp) {
            return false
        }
        
        // Also check that the app has a launcher (can be opened by user)
        // Digital Wellbeing might exclude apps without launchers
        if (!isAppVisible(packageName)) {
            return false
        }
        
        return true
    }
    
    private fun isAppVisible(packageName: String): Boolean {
        return try {
            // Check if app has a launcher activity (visible in app drawer)
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent != null
        } catch (e: Exception) {
            false
        }
    }


    fun hasUsageStatsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } else {
            false
        }
    }
}

