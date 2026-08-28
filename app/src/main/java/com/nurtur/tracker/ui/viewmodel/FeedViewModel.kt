package com.nurtur.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nurtur.tracker.data.local.FeedLogEntity
import com.nurtur.tracker.data.preferences.SettingsPreferences
import com.nurtur.tracker.data.repository.FeedRepository
import com.nurtur.tracker.domain.model.DailyAnalytics
import com.nurtur.tracker.domain.model.SettingsState
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

data class FeedUiState(
    val latestFeed: FeedLogEntity? = null,
    val todayConsumedMl: Int = 0,
    val todayWastedMl: Int = 0,
    val todayFeedCount: Int = 0,
    val recentFeeds: List<FeedLogEntity> = emptyList(),
    val sevenDaySummary: List<DailyAnalytics> = emptyList(),
    val settings: SettingsState = SettingsState(),
    val startTimeInput: String = "",
    val endTimeInput: String = "",
    val amountOfferedInput: String = DEFAULT_BOTTLE_SIZE_ML.toString(),
    val amountConsumedInput: String = "",
    val milkTypeInput: String = DEFAULT_MILK_TYPE,
    val notesInput: String = "",
    val formError: String? = null
)

class FeedViewModel(
    private val repository: FeedRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {
    private val formState = MutableStateFlow(
        FeedUiState(
            startTimeInput = System.currentTimeMillis().toString(),
            endTimeInput = System.currentTimeMillis().toString()
        )
    )

    private val latestFeedFlow = repository.observeLatestFeed()
    private val recentFeedsFlow = repository.observeRecentFeeds(limit = 5)
    private val allFeedsFlow = repository.observeAllFeeds()
    private val settingsFlow = settingsPreferences.settingsFlow

    val uiState: StateFlow<FeedUiState> = combine(
        formState,
        latestFeedFlow,
        recentFeedsFlow,
        allFeedsFlow,
        settingsFlow
    ) { form, latestFeed, recentFeeds, allFeeds, settings ->
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
            sevenDaySummary = FeedMetricsCalculator.buildSevenDaySummary(allFeeds),
            settings = settings,
            amountOfferedInput = if (form.amountOfferedInput.isBlank()) {
                settings.defaultBottleSizeMl.toString()
            } else {
                form.amountOfferedInput
            },
            milkTypeInput = if (form.milkTypeInput.isBlank()) {
                settings.defaultMilkType
            } else {
                form.milkTypeInput
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    fun updateStartTime(value: String) = formState.update { it.copy(startTimeInput = value) }

    fun updateEndTime(value: String) = formState.update { it.copy(endTimeInput = value) }

    fun updateAmountOffered(value: String) = formState.update { it.copy(amountOfferedInput = value) }

    fun updateAmountConsumed(value: String) = formState.update { it.copy(amountConsumedInput = value) }

    fun updateMilkType(value: String) = formState.update { it.copy(milkTypeInput = value) }

    fun updateNotes(value: String) = formState.update { it.copy(notesInput = value.take(280)) }

    fun updateDefaultBottleSizeMl(value: String) {
        val parsed = value.toIntOrNull() ?: return
        viewModelScope.launch {
            settingsPreferences.updateDefaultBottleSizeMl(parsed)
        }
    }

    fun updateDefaultMilkType(value: String) {
        viewModelScope.launch {
            settingsPreferences.updateDefaultMilkType(value)
        }
    }

    fun saveFeed() {
        val current = uiState.value
        val startTime = current.startTimeInput.toLongOrNull()
        val endTime = current.endTimeInput.toLongOrNull()
        val offered = current.amountOfferedInput.toIntOrNull()
        val consumed = current.amountConsumedInput.toIntOrNull()
        if (startTime == null || endTime == null || offered == null || consumed == null) {
            formState.update { it.copy(formError = "Please provide valid numeric values.") }
            return
        }
        if (startTime <= 0L || endTime <= 0L || endTime < startTime) {
            formState.update { it.copy(formError = "End time must be after start time.") }
            return
        }
        if (offered !in 1..1000 || consumed !in 0..offered) {
            formState.update { it.copy(formError = "Consumed must be between 0 and offered amount.") }
            return
        }

        viewModelScope.launch {
            repository.insert(
                FeedLogEntity(
                    remoteId = null,
                    startTime = startTime,
                    endTime = endTime,
                    amountOffered = offered,
                    amountConsumed = consumed,
                    milkType = if (current.milkTypeInput == "Breastmilk") "Breastmilk" else "Formula",
                    notes = current.notesInput.ifBlank { null }
                )
            )
            formState.update {
                it.copy(
                    startTimeInput = System.currentTimeMillis().toString(),
                    endTimeInput = System.currentTimeMillis().toString(),
                    amountOfferedInput = uiState.value.settings.defaultBottleSizeMl.toString(),
                    amountConsumedInput = "",
                    milkTypeInput = uiState.value.settings.defaultMilkType,
                    notesInput = "",
                    formError = null
                )
            }
        }
    }

    fun deleteFeed(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    private fun isToday(epochMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date == LocalDate.now()
    }

    class Factory(
        private val repository: FeedRepository,
        private val settingsPreferences: SettingsPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                return FeedViewModel(repository, settingsPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
