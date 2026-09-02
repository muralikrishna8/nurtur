package com.nurtur.tracker.domain.service

import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.domain.repository.FeedAlertScheduler
import com.nurtur.tracker.domain.repository.FeedRepository
import com.nurtur.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FeedAlertCoordinatorTest {
    @Test
    fun test_handleSnooze_dismissesAndReschedulesFifteenMinutesLater() = runTest {
        // Arrange
        val now = 1_700_000_000_000L
        val settingsRepository = FakeSettingsRepository(
            SettingsState(pushNotificationsEnabled = true)
        )
        val feedRepository = FakeFeedRepository(latest = feed(endTime = now - 3_600_000L))
        val scheduler = RecordingFeedAlertScheduler()
        val coordinator = FeedAlertCoordinator(
            settingsRepository = settingsRepository,
            feedRepository = feedRepository,
            scheduler = scheduler,
            clock = { now },
            timeZone = TimeZone.getTimeZone("UTC")
        )

        // Act
        coordinator.handleSnooze()

        // Assert
        assertEquals(1, scheduler.dismissActiveAlarmCalls)
        assertEquals(
            now + SnoozePolicy.SNOOZE_DURATION_MILLIS,
            settingsRepository.settingsFlow.value.nextFeedAlertOverrideEpochMillis
        )
        assertEquals(
            listOf(now + SnoozePolicy.SNOOZE_DURATION_MILLIS),
            scheduler.scheduledTriggers
        )
    }

    @Test
    fun test_handleSkip_dismissesAndReschedulesByFeedInterval() = runTest {
        // Arrange
        val now = 1_700_000_000_000L
        val settingsRepository = FakeSettingsRepository(
            SettingsState(
                pushNotificationsEnabled = true,
                targetFeedIntervalMinutes = 180
            )
        )
        val scheduler = RecordingFeedAlertScheduler()
        val coordinator = FeedAlertCoordinator(
            settingsRepository = settingsRepository,
            feedRepository = FakeFeedRepository(latest = feed(endTime = now - 1_000L)),
            scheduler = scheduler,
            clock = { now },
            timeZone = TimeZone.getTimeZone("UTC")
        )

        // Act
        coordinator.handleSkip()

        // Assert
        assertEquals(1, scheduler.dismissActiveAlarmCalls)
        assertEquals(now + (180 * 60_000L), settingsRepository.settingsFlow.value.nextFeedAlertOverrideEpochMillis)
        assertEquals(listOf(now + (180 * 60_000L)), scheduler.scheduledTriggers)
    }

    @Test
    fun test_handleAlertFired_outsideQuietHours_usesEscalatingAudio() = runTest {
        // Arrange
        val noonUtc = epochMillisUtc(12, 0)
        val settingsRepository = FakeSettingsRepository(
            SettingsState(
                quietHoursEnabled = true,
                quietHoursStartMinutesOfDay = 22 * 60,
                quietHoursEndMinutesOfDay = 6 * 60
            )
        )
        val scheduler = RecordingFeedAlertScheduler()
        val coordinator = FeedAlertCoordinator(
            settingsRepository = settingsRepository,
            feedRepository = FakeFeedRepository(latest = null),
            scheduler = scheduler,
            clock = { noonUtc },
            timeZone = TimeZone.getTimeZone("UTC")
        )

        // Act
        coordinator.handleAlertFired()

        // Assert
        assertEquals(listOf(AlertDeliveryMode.ESCALATING_AUDIO), scheduler.notifiedModes)
    }

    @Test
    fun test_handleAlertFired_insideQuietHours_usesVibrateOnly() = runTest {
        // Arrange
        val lateNightUtc = epochMillisUtc(23, 0)
        val settingsRepository = FakeSettingsRepository(
            SettingsState(
                quietHoursEnabled = true,
                quietHoursStartMinutesOfDay = 22 * 60,
                quietHoursEndMinutesOfDay = 6 * 60
            )
        )
        val scheduler = RecordingFeedAlertScheduler()
        val coordinator = FeedAlertCoordinator(
            settingsRepository = settingsRepository,
            feedRepository = FakeFeedRepository(latest = null),
            scheduler = scheduler,
            clock = { lateNightUtc },
            timeZone = TimeZone.getTimeZone("UTC")
        )

        // Act
        coordinator.handleAlertFired()

        // Assert
        assertEquals(listOf(AlertDeliveryMode.VIBRATE_ONLY), scheduler.notifiedModes)
    }

    @Test
    fun test_rescheduleFromCurrentState_pushDisabled_cancels() = runTest {
        // Arrange
        val settingsRepository = FakeSettingsRepository(
            SettingsState(pushNotificationsEnabled = false)
        )
        val scheduler = RecordingFeedAlertScheduler()
        val coordinator = FeedAlertCoordinator(
            settingsRepository = settingsRepository,
            feedRepository = FakeFeedRepository(latest = feed(endTime = 1_000L)),
            scheduler = scheduler
        )

        // Act
        coordinator.rescheduleFromCurrentState()

        // Assert
        assertEquals(1, scheduler.cancelCalls)
        assertTrue(scheduler.scheduledTriggers.isEmpty())
    }

    @Test
    fun test_rescheduleFromCurrentState_pushEnabled_schedulesResolvedTrigger() = runTest {
        // Arrange
        val settingsRepository = FakeSettingsRepository(
            SettingsState(
                pushNotificationsEnabled = true,
                targetFeedIntervalMinutes = 120
            )
        )
        val scheduler = RecordingFeedAlertScheduler()
        val coordinator = FeedAlertCoordinator(
            settingsRepository = settingsRepository,
            feedRepository = FakeFeedRepository(latest = feed(endTime = 5_000L)),
            scheduler = scheduler
        )

        // Act
        coordinator.rescheduleFromCurrentState()

        // Assert
        assertEquals(listOf(5_000L + (120 * 60_000L)), scheduler.scheduledTriggers)
        assertEquals(0, scheduler.cancelCalls)
    }

    private fun epochMillisUtc(hourOfDay: Int, minute: Int): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(2024, Calendar.JANUARY, 1, hourOfDay, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun feed(endTime: Long): FeedLog {
        return FeedLog(
            id = 1L,
            remoteId = null,
            startTime = endTime - 1_000L,
            endTime = endTime,
            amountOffered = 120,
            amountConsumed = 100,
            milkType = "Formula",
            notes = null
        )
    }
}

private class FakeSettingsRepository(
    initial: SettingsState
) : SettingsRepository {
    override val settingsFlow = MutableStateFlow(initial)

    override suspend fun updateDefaultBottleSizeMl(value: Int) = Unit
    override suspend fun updateDefaultMilkType(value: String) = Unit
    override suspend fun updateTargetFeedIntervalMinutes(value: Int) = Unit
    override suspend fun updateThemeMode(value: ThemeMode) = Unit
    override suspend fun updatePushNotificationsEnabled(value: Boolean) = Unit
    override suspend fun updateQuietHoursEnabled(value: Boolean) = Unit
    override suspend fun updateQuietHoursStartMinutesOfDay(value: Int) = Unit
    override suspend fun updateQuietHoursEndMinutesOfDay(value: Int) = Unit
    override suspend fun updateNextFeedAlertOverrideEpochMillis(value: Long?) {
        settingsFlow.value = settingsFlow.value.copy(nextFeedAlertOverrideEpochMillis = value)
    }
}

private class FakeFeedRepository(
    private val latest: FeedLog?
) : FeedRepository {
    override fun observeLatestFeed(): Flow<FeedLog?> = flowOf(latest)
    override fun observeRecentFeeds(limit: Int): Flow<List<FeedLog>> = flowOf(emptyList())
    override fun observeFeedsInRange(startMillis: Long, endMillis: Long): Flow<List<FeedLog>> = flowOf(emptyList())
    override fun observeAllFeeds(): Flow<List<FeedLog>> = flowOf(emptyList())
    override suspend fun insert(feedLog: FeedLog): Long = 0L
    override suspend fun deleteById(id: Long) = Unit
}

private class RecordingFeedAlertScheduler : FeedAlertScheduler {
    val scheduledTriggers = mutableListOf<Long>()
    val notifiedModes = mutableListOf<AlertDeliveryMode>()
    var cancelCalls = 0
    var dismissActiveAlarmCalls = 0

    override fun schedule(triggerAtEpochMillis: Long) {
        scheduledTriggers += triggerAtEpochMillis
    }

    override fun cancel() {
        cancelCalls += 1
    }

    override fun dismissActiveAlarm() {
        dismissActiveAlarmCalls += 1
    }

    override fun notifyAlertFired(deliveryMode: AlertDeliveryMode) {
        notifiedModes += deliveryMode
    }
}
