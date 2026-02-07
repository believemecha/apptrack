package com.example.apptrack.ui.components

import com.example.apptrack.call.GroupedCallInfo
import java.util.Calendar
import java.util.Locale

enum class DateBucket(val label: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    ThisWeek("This week"),
    Older("Older")
}

fun dateBucketFor(timestamp: Long): DateBucket {
    val cal = Calendar.getInstance(Locale.getDefault())
    val now = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
    cal.add(Calendar.DAY_OF_YEAR, -cal.get(Calendar.DAY_OF_WEEK) + 1)
    val weekStart = cal.timeInMillis

    return when {
        timestamp >= todayStart -> DateBucket.Today
        timestamp >= yesterdayStart -> DateBucket.Yesterday
        timestamp >= weekStart -> DateBucket.ThisWeek
        else -> DateBucket.Older
    }
}

data class GroupedByDate<T>(
    val bucket: DateBucket,
    val items: List<T>
)

fun groupRecentsByDate(groupedCalls: List<GroupedCallInfo>): List<GroupedByDate<GroupedCallInfo>> {
    val byBucket = groupedCalls.groupBy { dateBucketFor(it.lastCallTimestamp) }
    val order = listOf(DateBucket.Today, DateBucket.Yesterday, DateBucket.ThisWeek, DateBucket.Older)
    return order.filter { byBucket.containsKey(it) }.map { bucket ->
        GroupedByDate(bucket, byBucket[bucket]!!.sortedByDescending { it.lastCallTimestamp })
    }
}
