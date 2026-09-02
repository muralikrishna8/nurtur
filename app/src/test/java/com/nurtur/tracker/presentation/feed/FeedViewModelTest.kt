package com.nurtur.tracker.presentation.feed

import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.domain.repository.FeedRepository
import com.nurtur.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

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

        // Act
        FeedViewModel(
            repository = feedRepository,
            settingsRepository = settingsRepository
        )

        // Assert
        assertEquals(EXPECTED_RECENT_LIMIT, feedRepository.recordedRecentLimit)
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

private class FakeSettingsRepository : SettingsRepository {
    override val settingsFlow: MutableStateFlow<SettingsState> = MutableStateFlow(SettingsState())

    override suspend fun updateDefaultBottleSizeMl(value: Int) = Unit

    override suspend fun updateDefaultMilkType(value: String) = Unit

    override suspend fun updateTargetFeedIntervalMinutes(value: Int) = Unit

    override suspend fun updateThemeMode(value: ThemeMode) = Unit

    override suspend fun updatePushNotificationsEnabled(value: Boolean) = Unit

    override suspend fun updateQuietHoursEnabled(value: Boolean) = Unit

    override suspend fun updateQuietHoursStartMinutesOfDay(value: Int) = Unit

    override suspend fun updateQuietHoursEndMinutesOfDay(value: Int) = Unit

    override suspend fun updateNextFeedAlertOverrideEpochMillis(value: Long?) = Unit
}
