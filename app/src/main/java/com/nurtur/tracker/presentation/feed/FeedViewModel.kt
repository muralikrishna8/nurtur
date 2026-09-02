package com.nurtur.tracker.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nurtur.tracker.domain.model.AnalyticsInsights
import com.nurtur.tracker.domain.model.DailyAnalytics
import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.domain.repository.FeedRepository
import com.nurtur.tracker.domain.repository.SettingsRepository
import com.nurtur.tracker.domain.service.FeedAlertCoordinator
import com.nurtur.tracker.domain.service.FeedMetricsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val DEFAULT_MILK_TYPE = "Formula"
private const val DEFAULT_BOTTLE_SIZE_ML = 120
private const val MIN_BOTTLE_SIZE_ML = 30
private const val MAX_BOTTLE_SIZE_ML = 500
private const val MIN_TARGET_INTERVAL_HOURS = 1
private const val MAX_TARGET_INTERVAL_HOURS = 12
private const val MINUTES_PER_HOUR = 60
private const val DEFAULT_ANALYTICS_WINDOW_DAYS = 7L
private const val QUICK_FILTER_SEVEN_DAYS = 7L
private const val QUICK_FILTER_FOURTEEN_DAYS = 14L
private const val QUICK_FILTER_THIRTY_DAYS = 30L
private const val RECENT_FEEDS_LIMIT = 30

data class FeedUiState(
    val latestFeed: FeedLog? = null,
    val todayConsumedMl: Int = 0,
    val todayWastedMl: Int = 0,
    val todayFeedCount: Int = 0,
    val recentFeeds: List<FeedLog> = emptyList(),
    val sevenDaySummary: List<DailyAnalytics> = emptyList(),
    val analyticsStartDate: LocalDate = LocalDate.now().minusDays(DEFAULT_ANALYTICS_WINDOW_DAYS - 1),
    val analyticsEndDate: LocalDate = LocalDate.now(),
    val analyticsQuickFilterDays: Long = QUICK_FILTER_SEVEN_DAYS,
    val analyticsInsights: AnalyticsInsights = AnalyticsInsights(
        averageVolumePerFeedMl = null,
        averageTimeBetweenFeedsMillis = null,
        smoothedTrendConsumedMlByDay = emptyList()
    ),
    val settings: SettingsState = SettingsState(),
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long = System.currentTimeMillis(),
    val amountOfferedInput: String = DEFAULT_BOTTLE_SIZE_ML.toString(),
    val amountConsumedInput: String = "",
    val milkTypeInput: String = DEFAULT_MILK_TYPE,
    val notesInput: String = "",
    val editingFeedId: Long? = null,
    val formError: String? = null,
    val isLogFeedDialogVisible: Boolean = false
) {
    val isEditMode: Boolean
        get() = editingFeedId != null
}

class FeedViewModel(
    private val repository: FeedRepository,
    private val settingsRepository: SettingsRepository,
    private val feedAlertCoordinator: FeedAlertCoordinator
) : ViewModel() {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val formState = MutableStateFlow(
        FeedUiState(
            startTimeMillis = System.currentTimeMillis(),
            endTimeMillis = System.currentTimeMillis()
        )
    )

    private val latestFeedFlow = repository.observeLatestFeed()
    private val recentFeedsFlow = repository.observeRecentFeeds(limit = RECENT_FEEDS_LIMIT)
    private val allFeedsFlow = repository.observeAllFeeds()
    private val settingsFlow = settingsRepository.settingsFlow

    init {
        viewModelScope.launch {
            // Keep OS alarms aligned with feed/settings changes.
            combine(latestFeedFlow, settingsFlow) { _, _ -> }
                .collect { rescheduleFeedAlert() }
        }
    }

    val uiState: StateFlow<FeedUiState> = combine(
        formState,
        latestFeedFlow,
        recentFeedsFlow,
        allFeedsFlow,
        settingsFlow
    ) { form, latestFeed, recentFeeds, allFeeds, settings ->
        val today = LocalDate.now(zoneId)
        val boundedStartDate = form.analyticsStartDate
        val boundedEndDate = form.analyticsEndDate.coerceAtMost(today)
        val analyticsWindowFeeds = allFeeds.filter { feed ->
            val feedDate = Instant.ofEpochMilli(feed.endTime).atZone(zoneId).toLocalDate()
            !feedDate.isBefore(boundedStartDate) && !feedDate.isAfter(boundedEndDate)
        }
        val todaysFeeds = allFeeds.filter { isToday(it.endTime) }
        val consumedToday = todaysFeeds.sumOf { it.amountConsumed.coerceAtLeast(0) }
        val wastedToday = todaysFeeds.sumOf {
            FeedMetricsCalculator.calculateWasteMl(it.amountOffered, it.amountConsumed)
        }
        form.copy(
            latestFeed = latestFeed,
            todayConsumedMl = consumedToday,
            todayWastedMl = wastedToday,
            todayFeedCount = todaysFeeds.size,
            recentFeeds = recentFeeds,
            sevenDaySummary = FeedMetricsCalculator.buildDailySummary(
                feeds = analyticsWindowFeeds,
                startDate = boundedStartDate,
                endDate = boundedEndDate,
                zoneId = zoneId
            ).asReversed(),
            analyticsStartDate = boundedStartDate,
            analyticsEndDate = boundedEndDate,
            analyticsInsights = FeedMetricsCalculator.buildAveragesAndTrend(
                feeds = analyticsWindowFeeds,
                trendStartDate = boundedStartDate,
                trendEndDate = boundedEndDate,
                zoneId = zoneId
            ),
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FeedUiState())

    fun updateAnalyticsDateRange(startDate: LocalDate, endDate: LocalDate) {
        val today = LocalDate.now(zoneId)
        val boundedStartDate = startDate.coerceAtMost(today)
        val boundedEndDate = endDate.coerceAtMost(today)
        if (boundedEndDate.isBefore(boundedStartDate)) {
            return
        }
        formState.update {
            it.copy(
                analyticsStartDate = boundedStartDate,
                analyticsEndDate = boundedEndDate,
                analyticsQuickFilterDays = 0L
            )
        }
    }

    fun applyAnalyticsQuickFilter(days: Long) {
        if (days !in listOf(QUICK_FILTER_SEVEN_DAYS, QUICK_FILTER_FOURTEEN_DAYS, QUICK_FILTER_THIRTY_DAYS)) {
            return
        }
        val today = LocalDate.now(zoneId)
        val startDate = today.minusDays(days - 1)
        formState.update {
            it.copy(
                analyticsStartDate = startDate,
                analyticsEndDate = today,
                analyticsQuickFilterDays = days
            )
        }
    }

    fun updateStartTimeMillis(value: Long) = formState.update { it.copy(startTimeMillis = value) }

    fun updateEndTimeMillis(value: Long) = formState.update { it.copy(endTimeMillis = value) }

    fun updateAmountOffered(value: String) = formState.update { it.copy(amountOfferedInput = value) }

    fun updateAmountConsumed(value: String) = formState.update { it.copy(amountConsumedInput = value) }

    fun updateMilkType(value: String) = formState.update { it.copy(milkTypeInput = value) }

    fun updateNotes(value: String) = formState.update { it.copy(notesInput = value.take(280)) }


    fun openLogFeedFromAlert() {
        startNewFeedEntry()
        formState.update { it.copy(isLogFeedDialogVisible = true) }
    }

    fun showLogFeedDialog() {
        formState.update { it.copy(isLogFeedDialogVisible = true) }
    }

    fun dismissLogFeedDialog() {
        formState.update { it.copy(isLogFeedDialogVisible = false) }
    }

    private fun rescheduleFeedAlert() {
        viewModelScope.launch {
            runCatching {
                feedAlertCoordinator.rescheduleFromCurrentState()
            }
        }
    }

    fun startNewFeedEntry() {
        val now = System.currentTimeMillis()
        val settings = uiState.value.settings
        formState.update {
            it.copy(
                startTimeMillis = now,
                endTimeMillis = now,
                amountOfferedInput = settings.defaultBottleSizeMl.toString(),
                amountConsumedInput = "",
                milkTypeInput = settings.defaultMilkType,
                notesInput = "",
                editingFeedId = null,
                formError = null
            )
        }
    }

    fun startEditingFeed(feedLog: FeedLog) {
        formState.update {
            it.copy(
                startTimeMillis = feedLog.startTime,
                endTimeMillis = feedLog.endTime,
                amountOfferedInput = feedLog.amountOffered.toString(),
                amountConsumedInput = feedLog.amountConsumed.toString(),
                milkTypeInput = feedLog.milkType,
                notesInput = feedLog.notes.orEmpty(),
                editingFeedId = feedLog.id,
                formError = null
            )
        }
    }

    fun updateDefaultBottleSizeMl(value: String) {
        val parsed = value.toIntOrNull() ?: return
        if (parsed !in MIN_BOTTLE_SIZE_ML..MAX_BOTTLE_SIZE_ML) {
            return
        }
        viewModelScope.launch {
            settingsRepository.updateDefaultBottleSizeMl(parsed)
        }
    }

    fun updateDefaultMilkType(value: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultMilkType(value)
        }
    }

    fun updateTargetFeedIntervalHours(value: String) {
        val parsedHours = value.toIntOrNull() ?: return
        if (parsedHours !in MIN_TARGET_INTERVAL_HOURS..MAX_TARGET_INTERVAL_HOURS) {
            return
        }
        viewModelScope.launch {
            settingsRepository.updateTargetFeedIntervalMinutes(parsedHours * MINUTES_PER_HOUR)
            rescheduleFeedAlert()
        }
    }

    fun updateThemeMode(value: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(value)
        }
    }

    fun updatePushNotificationsEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePushNotificationsEnabled(value)
            rescheduleFeedAlert()
        }
    }

    fun updateQuietHoursEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateQuietHoursEnabled(value)
            rescheduleFeedAlert()
        }
    }

    fun updateQuietHoursStartMinutesOfDay(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateQuietHoursStartMinutesOfDay(value)
            rescheduleFeedAlert()
        }
    }

    fun updateQuietHoursEndMinutesOfDay(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateQuietHoursEndMinutesOfDay(value)
            rescheduleFeedAlert()
        }
    }

    fun updateNextFeedAlertOverrideEpochMillis(value: Long) {
        if (value <= 0L) {
            return
        }
        viewModelScope.launch {
            settingsRepository.updateNextFeedAlertOverrideEpochMillis(value)
            rescheduleFeedAlert()
        }
    }

    fun saveFeed(): Boolean {
        val current = uiState.value
        val startTime = current.startTimeMillis
        val endTime = current.endTimeMillis
        val offered = current.amountOfferedInput.toIntOrNull()
        val consumed = current.amountConsumedInput.toIntOrNull()
        if (offered == null || consumed == null) {
            formState.update { it.copy(formError = "Please provide valid numeric values.") }
            return false
        }
        if (startTime <= 0L || endTime <= 0L || endTime < startTime) {
            formState.update { it.copy(formError = "End time must be after start time.") }
            return false
        }
        if (offered !in 1..1000 || consumed !in 0..offered) {
            formState.update { it.copy(formError = "Consumed must be between 0 and offered amount.") }
            return false
        }

        viewModelScope.launch {
            val targetId = current.editingFeedId ?: 0L
            repository.insert(
                FeedLog(
                    id = targetId,
                    remoteId = null,
                    startTime = startTime,
                    endTime = endTime,
                    amountOffered = offered,
                    amountConsumed = consumed,
                    milkType = if (current.milkTypeInput == "Breastmilk") "Breastmilk" else "Formula",
                    notes = current.notesInput.ifBlank { null }
                )
            )
            // One-shot override applies only until the next logged feed.
            settingsRepository.updateNextFeedAlertOverrideEpochMillis(null)
            rescheduleFeedAlert()
            formState.update {
                it.copy(
                    startTimeMillis = System.currentTimeMillis(),
                    endTimeMillis = System.currentTimeMillis(),
                    amountOfferedInput = uiState.value.settings.defaultBottleSizeMl.toString(),
                    amountConsumedInput = "",
                    milkTypeInput = uiState.value.settings.defaultMilkType,
                    notesInput = "",
                    editingFeedId = null,
                    formError = null
                )
            }
        }
        return true
    }

    fun deleteEditingFeed(): Boolean {
        val feedId = uiState.value.editingFeedId ?: return false
        viewModelScope.launch {
            repository.deleteById(feedId)
            settingsRepository.updateNextFeedAlertOverrideEpochMillis(null)
            rescheduleFeedAlert()
            startNewFeedEntry()
        }
        return true
    }

    fun deleteFeed(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            settingsRepository.updateNextFeedAlertOverrideEpochMillis(null)
            rescheduleFeedAlert()
        }
    }

    private fun isToday(epochMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date == LocalDate.now()
    }

    class Factory(
        private val repository: FeedRepository,
        private val settingsRepository: SettingsRepository,
        private val feedAlertCoordinator: FeedAlertCoordinator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                return FeedViewModel(
                    repository = repository,
                    settingsRepository = settingsRepository,
                    feedAlertCoordinator = feedAlertCoordinator
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
