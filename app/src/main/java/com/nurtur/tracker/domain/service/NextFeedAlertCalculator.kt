package com.nurtur.tracker.domain.service

private const val MILLIS_PER_MINUTE = 60_000L

object NextFeedAlertCalculator {
    fun resolve(
        lastFeedEndEpochMillis: Long?,
        targetIntervalMinutes: Int,
        overrideEpochMillis: Long?
    ): Long? {
        if (lastFeedEndEpochMillis == null) {
            return null
        }
        if (overrideEpochMillis != null) {
            return overrideEpochMillis
        }
        val safeIntervalMinutes = targetIntervalMinutes.coerceAtLeast(0)
        return lastFeedEndEpochMillis + (safeIntervalMinutes * MILLIS_PER_MINUTE)
    }
}
