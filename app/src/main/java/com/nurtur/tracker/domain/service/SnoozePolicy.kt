package com.nurtur.tracker.domain.service

private const val SNOOZE_MINUTES = 15
private const val MILLIS_PER_MINUTE = 60_000L

object SnoozePolicy {
    const val SNOOZE_DURATION_MILLIS: Long = SNOOZE_MINUTES * MILLIS_PER_MINUTE

    fun rescheduleAt(nowEpochMillis: Long): Long {
        return nowEpochMillis + SNOOZE_DURATION_MILLIS
    }
}
