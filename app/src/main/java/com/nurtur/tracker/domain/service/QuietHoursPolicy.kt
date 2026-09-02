package com.nurtur.tracker.domain.service

private const val MINUTES_PER_DAY = 24 * 60

object QuietHoursPolicy {
    fun isActive(
        enabled: Boolean,
        minutesOfDay: Int,
        startMinutesOfDay: Int,
        endMinutesOfDay: Int
    ): Boolean {
        if (!enabled) {
            return false
        }
        val start = startMinutesOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
        val end = endMinutesOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
        if (start == end) {
            return false
        }
        val current = minutesOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
        if (start < end) {
            return current in start until end
        }
        return current >= start || current < end
    }
}
