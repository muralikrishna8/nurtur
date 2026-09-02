package com.nurtur.tracker.presentation.feed

import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.domain.repository.FeedAlertScheduler
import com.nurtur.tracker.domain.repository.FeedRepository
import com.nurtur.tracker.domain.repository.SettingsRepository
import com.nurtur.tracker.domain.service.AlertDeliveryMode
import com.nurtur.tracker.domain.service.FeedAlertCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)

private const val EXPECTED_RECENT_LIMIT = 30

class FeedViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_init_observeRecentFeeds_usesThirtyEntryLimit() = runTest {
        // Arrange
        val feedRepository = RecordingFeedRepository()
        val settingsRepository = FakeSettingsRepository()
        val coordinator = FeedAlertCoordinator(
            settingsRepository = settingsRepository,
            feedRepository = feedRepository,
            scheduler = NoOpFeedAlertScheduler()
        )

        // Act
        FeedViewModel(
            repository = feedRepository,
            settingsRepository = settingsRepository,
            feedAlertCoordinator = coordinator
        )

        // Assert
        assertEquals(EXPECTED_RECENT_LIMIT, feedRepository.recordedRecentLimit)
    }

    @Test
    fun test_openLogFeedFromAlert_primesFormAndShowsDialog() = runTest {
        // Arrange
        val feedRepository = RecordingFeedRepository()
        val settingsRepository = FakeSettingsRepository(
            SettingsState(defaultBottleSizeMl = 150, defaultMilkType = "Breastmilk")
        )
        val coordinator = FeedAlertCoordinator(
            settingsRepository = settingsRepository,
            feedRepository = feedRepository,
            scheduler = NoOpFeedAlertScheduler()
        )
        val viewModel = FeedViewModel(
            repository = feedRepository,
            settingsRepository = settingsRepository,
            feedAlertCoordinator = coordinator
        )
        advanceUntilIdle()

        // Act
        viewModel.openLogFeedFromAlert()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.isLogFeedDialogVisible)
        assertEquals("150", state.amountOfferedInput)
        assertEquals("Breastmilk", state.milkTypeInput)
        assertEquals(null, state.editingFeedId)
    }

    @Test
    fun test_updateDefaultBottleSizeMl_persistsValidValue() = runTest {
        // Arrange
        val feedRepository = RecordingFeedRepository()
        val settingsRepository = FakeSettingsRepository(
            SettingsState(defaultBottleSizeMl = 120)
        )
        val viewModel = FeedViewModel(
            repository = feedRepository,
            settingsRepository = settingsRepository,
            feedAlertCoordinator = FeedAlertCoordinator(
                settingsRepository = settingsRepository,
                feedRepository = feedRepository,
                scheduler = NoOpFeedAlertScheduler()
            )
        )
        advanceUntilIdle()

        // Act
        viewModel.updateDefaultBottleSizeMl("150")
        advanceUntilIdle()

        // Assert
        assertEquals(150, settingsRepository.settingsFlow.value.defaultBottleSizeMl)
    }

    @Test
    fun test_updateDefaultBottleSizeMl_ignoresEmptyAndBelowMinimum() = runTest {
        // Arrange
        val feedRepository = RecordingFeedRepository()
        val settingsRepository = FakeSettingsRepository(
            SettingsState(defaultBottleSizeMl = 120)
        )
        val viewModel = FeedViewModel(
            repository = feedRepository,
            settingsRepository = settingsRepository,
            feedAlertCoordinator = FeedAlertCoordinator(
                settingsRepository = settingsRepository,
                feedRepository = feedRepository,
                scheduler = NoOpFeedAlertScheduler()
            )
        )
        advanceUntilIdle()

        // Act
        viewModel.updateDefaultBottleSizeMl("")
        viewModel.updateDefaultBottleSizeMl("15")
        advanceUntilIdle()

        // Assert
        assertEquals(120, settingsRepository.settingsFlow.value.defaultBottleSizeMl)
    }
}

private class RecordingFeedRepository : FeedRepository {
    var recordedRecentLimit: Int? = null

    override fun observeLatestFeed(): Flow<FeedLog?> = flowOf(null)
    override fun observeRecentFeeds(limit: Int): Flow<List<FeedLog>> {
        recordedRecentLimit = limit
        return flowOf(emptyList())
    }
    override fun observeFeedsInRange(startMillis: Long, endMillis: Long): Flow<List<FeedLog>> = flowOf(emptyList())
    override fun observeAllFeeds(): Flow<List<FeedLog>> = flowOf(emptyList())
    override suspend fun insert(feedLog: FeedLog): Long = 0L
    override suspend fun deleteById(id: Long) = Unit
}

private class FakeSettingsRepository(
    initial: SettingsState = SettingsState()
) : SettingsRepository {
    override val settingsFlow = MutableStateFlow(initial)
    override suspend fun updateDefaultBottleSizeMl(value: Int) {
        settingsFlow.value = settingsFlow.value.copy(defaultBottleSizeMl = value)
    }
    override suspend fun updateDefaultMilkType(value: String) = Unit
    override suspend fun updateTargetFeedIntervalMinutes(value: Int) = Unit
    override suspend fun updateThemeMode(value: ThemeMode) = Unit
    override suspend fun updatePushNotificationsEnabled(value: Boolean) = Unit
    override suspend fun updateQuietHoursEnabled(value: Boolean) = Unit
    override suspend fun updateQuietHoursStartMinutesOfDay(value: Int) = Unit
    override suspend fun updateQuietHoursEndMinutesOfDay(value: Int) = Unit
    override suspend fun updateNextFeedAlertOverrideEpochMillis(value: Long?) = Unit
}

private class NoOpFeedAlertScheduler : FeedAlertScheduler {
    override fun schedule(triggerAtEpochMillis: Long) = Unit
    override fun cancel() = Unit
    override fun dismissActiveAlarm() = Unit
    override fun notifyAlertFired(deliveryMode: AlertDeliveryMode) = Unit
}
