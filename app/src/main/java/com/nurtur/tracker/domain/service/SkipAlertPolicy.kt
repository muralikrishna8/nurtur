package com.nurtur.tracker.domain.service

private const val MILLIS_PER_MINUTE = 60_000L

object SkipAlertPolicy {
    fun rescheduleAt(nowEpochMillis: Long, targetIntervalMinutes: Int): Long {
        val safeIntervalMinutes = targetIntervalMinutes.coerceAtLeast(0)
        return nowEpochMillis + (safeIntervalMinutes * MILLIS_PER_MINUTE)
    }
}
