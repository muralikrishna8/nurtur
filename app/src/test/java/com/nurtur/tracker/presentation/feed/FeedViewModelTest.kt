package com.nurtur.tracker.presentation.feed

import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.domain.service.FeedCsvCodec
import com.nurtur.tracker.domain.service.FeedCsvExportRequest
import com.nurtur.tracker.domain.repository.FeedAlertScheduler
import com.nurtur.tracker.domain.repository.FeedRepository
import com.nurtur.tracker.domain.repository.SettingsRepository
import com.nurtur.tracker.domain.service.FeedAlertCoordinator
import com.nurtur.tracker.domain.service.FeedAlertNotification
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
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val EXPECTED_RECENT_LIMIT = 30

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun test_exportFeedHistory_usesStoredFeeds() = runTest {
        // Arrange
        val feed = sampleFeed()
        val feedRepository = RecordingFeedRepository()
        feedRepository.allFeeds = listOf(feed)
        val viewModel = newViewModel(feedRepository)
        advanceUntilIdle()
        val expected = FeedCsvCodec.export(
            FeedCsvExportRequest(
                feeds = listOf(feed),
                zoneId = ZoneId.systemDefault(),
                today = LocalDate.now()
            )
        )

        // Act
        val document = viewModel.exportFeedHistory()

        // Assert
        assertEquals(expected.fileName, document.fileName)
        assertEquals(expected.contents, document.contents)
    }

    @Test
    fun test_importFeedHistory_insertsNewFeeds() = runTest {
        // Arrange
        val feed = sampleFeed()
        val csv = FeedCsvCodec.export(
            FeedCsvExportRequest(
                feeds = listOf(feed),
                zoneId = ZoneId.of("UTC"),
                today = LocalDate.of(2026, 10, 14)
            )
        ).contents
        val feedRepository = RecordingFeedRepository()
        val viewModel = newViewModel(feedRepository)
        advanceUntilIdle()

        // Act
        val imported = viewModel.importFeedHistory(csv)
        advanceUntilIdle()

        // Assert
        assertTrue(imported)
        val inserted = feedRepository.insertedBatches.single().single()
        assertEquals(0L, inserted.id)
        assertEquals(null, inserted.remoteId)
        assertEquals(feed.startTime, inserted.startTime)
        assertEquals(feed.endTime, inserted.endTime)
        assertEquals(feed.amountConsumed, inserted.amountConsumed)
        assertEquals(feed.milkType, inserted.milkType)
        assertEquals(feed.notes, inserted.notes)
    }

    @Test
    fun test_importFeedHistory_skipsFeedsAlreadyStored() = runTest {
        // Arrange
        val feed = sampleFeed()
        val csv = FeedCsvCodec.export(
            FeedCsvExportRequest(
                feeds = listOf(feed),
                zoneId = ZoneId.of("UTC"),
                today = LocalDate.of(2026, 10, 14)
            )
        ).contents
        val feedRepository = RecordingFeedRepository()
        feedRepository.allFeeds = listOf(feed)
        val viewModel = newViewModel(feedRepository)
        advanceUntilIdle()

        // Act
        val imported = viewModel.importFeedHistory(csv)

        // Assert
        assertTrue(imported)
        assertTrue(feedRepository.insertedBatches.isEmpty())
    }

    @Test
    fun test_importFeedHistory_rejectsInvalidCsvWithoutInserting() = runTest {
        // Arrange
        val feedRepository = RecordingFeedRepository()
        val viewModel = newViewModel(feedRepository)
        advanceUntilIdle()

        // Act
        val imported = viewModel.importFeedHistory("not,a,nurtur,file\n1,2,3\n")

        // Assert
        assertFalse(imported)
        assertTrue(feedRepository.insertedBatches.isEmpty())
    }

    private fun newViewModel(feedRepository: RecordingFeedRepository): FeedViewModel {
        val settingsRepository = FakeSettingsRepository()
        return FeedViewModel(
            repository = feedRepository,
            settingsRepository = settingsRepository,
            feedAlertCoordinator = FeedAlertCoordinator(
                settingsRepository = settingsRepository,
                feedRepository = feedRepository,
                scheduler = NoOpFeedAlertScheduler()
            )
        )
    }

    private fun sampleFeed(): FeedLog {
        return FeedLog(
            id = 4L,
            remoteId = "remote-should-not-export",
            startTime = 1_700_000_000_000L,
            endTime = 1_700_000_060_000L,
            amountOffered = 120,
            amountConsumed = 90,
            milkType = "Breastmilk",
            notes = "sleepy, but fine"
        )
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
    var allFeeds: List<FeedLog> = emptyList()
    val insertedBatches = mutableListOf<List<FeedLog>>()

    override fun observeAllFeeds(): Flow<List<FeedLog>> = flowOf(allFeeds)
    override suspend fun insert(feedLog: FeedLog): Long = 0L
    override suspend fun insertAll(feedLogs: List<FeedLog>) {
        insertedBatches += feedLogs
    }
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
    override fun notifyAlertFired(notification: FeedAlertNotification) = Unit
}
