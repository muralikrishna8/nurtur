package com.nurtur.tracker.domain.service

import com.nurtur.tracker.domain.repository.FeedAlertScheduler
import com.nurtur.tracker.domain.repository.FeedRepository
import com.nurtur.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone

class FeedAlertCoordinator(
    private val settingsRepository: SettingsRepository,
    private val feedRepository: FeedRepository,
    private val scheduler: FeedAlertScheduler,
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeZone: TimeZone = TimeZone.getDefault()
) {
    suspend fun rescheduleFromCurrentState() {
        val settings = settingsRepository.settingsFlow.first()
        val latestFeed = feedRepository.observeLatestFeed().first()
        val triggerAt = FeedAlertSchedulePolicy.resolveTriggerAt(
            pushNotificationsEnabled = settings.pushNotificationsEnabled,
            lastFeedEndEpochMillis = latestFeed?.endTime,
            targetIntervalMinutes = settings.targetFeedIntervalMinutes,
            overrideEpochMillis = settings.nextFeedAlertOverrideEpochMillis
        )
        if (triggerAt == null) {
            scheduler.cancel()
            return
        }
        scheduler.schedule(triggerAt)
    }

    suspend fun handleAlertFired() {
        val settings = settingsRepository.settingsFlow.first()
        val now = clock()
        val quietHoursActive = QuietHoursPolicy.isActive(
            enabled = settings.quietHoursEnabled,
            minutesOfDay = minutesOfDay(now),
            startMinutesOfDay = settings.quietHoursStartMinutesOfDay,
            endMinutesOfDay = settings.quietHoursEndMinutesOfDay
        )
        val deliveryMode = AlertDeliveryPolicy.resolve(isQuietHoursActive = quietHoursActive)
        scheduler.notifyAlertFired(deliveryMode)
    }

    suspend fun handleSnooze() {
        val snoozeUntil = SnoozePolicy.rescheduleAt(clock())
        scheduler.dismissActiveAlarm()
        settingsRepository.updateNextFeedAlertOverrideEpochMillis(snoozeUntil)
        rescheduleFromCurrentState()
    }

    suspend fun handleSkip() {
        val settings = settingsRepository.settingsFlow.first()
        val skipUntil = SkipAlertPolicy.rescheduleAt(
            nowEpochMillis = clock(),
            targetIntervalMinutes = settings.targetFeedIntervalMinutes
        )
        scheduler.dismissActiveAlarm()
        settingsRepository.updateNextFeedAlertOverrideEpochMillis(skipUntil)
        rescheduleFromCurrentState()
    }

    private fun minutesOfDay(epochMillis: Long): Int {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = epochMillis
        return (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE)
    }
}
