package com.nurtur.tracker.domain.service

object FeedAlertSchedulePolicy {
    fun resolveTriggerAt(
        pushNotificationsEnabled: Boolean,
        lastFeedEndEpochMillis: Long?,
        targetIntervalMinutes: Int,
        overrideEpochMillis: Long?
    ): Long? {
        if (!pushNotificationsEnabled) {
            return null
        }
        return NextFeedAlertCalculator.resolve(
            lastFeedEndEpochMillis = lastFeedEndEpochMillis,
            targetIntervalMinutes = targetIntervalMinutes,
            overrideEpochMillis = overrideEpochMillis
        )
    }
}
