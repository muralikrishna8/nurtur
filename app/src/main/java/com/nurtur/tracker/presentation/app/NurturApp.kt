package com.nurtur.tracker.presentation.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nurtur.tracker.presentation.feed.FeedViewModel
import com.nurtur.tracker.presentation.screen.AnalyticsScreen
import com.nurtur.tracker.presentation.screen.HomeScreen
import com.nurtur.tracker.presentation.screen.SettingsScreen
import com.nurtur.tracker.presentation.theme.NurturTheme

enum class NurturTab(val label: String) {
    Home("Home"),
    Analytics("Analytics"),
    Settings("Settings")
}

@Composable
fun NurturApp(viewModel: FeedViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(NurturTab.Home) }

    NurturTheme(themeMode = uiState.settings.themeMode) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == NurturTab.Home,
                        onClick = { selectedTab = NurturTab.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text(NurturTab.Home.label) },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == NurturTab.Analytics,
                        onClick = { selectedTab = NurturTab.Analytics },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                        label = { Text(NurturTab.Analytics.label) },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == NurturTab.Settings,
                        onClick = { selectedTab = NurturTab.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text(NurturTab.Settings.label) },
                        colors = navItemColors
                    )
                }
            }
        ) { paddingValues ->
            when (selectedTab) {
                NurturTab.Home -> HomeScreen(
                    modifier = Modifier.padding(paddingValues),
                    uiState = uiState,
                    onStartAddFeed = {
                        viewModel.startNewFeedEntry()
                        viewModel.showLogFeedDialog()
                    },
                    onSaveFeed = viewModel::saveFeed,
                    onDeleteFromEditor = viewModel::deleteEditingFeed,
                    onEditFeed = { feed ->
                        viewModel.startEditingFeed(feed)
                        viewModel.showLogFeedDialog()
                    },
                    onStartTimeChange = viewModel::updateStartTimeMillis,
                    onEndTimeChange = viewModel::updateEndTimeMillis,
                    onAmountOfferedChange = viewModel::updateAmountOffered,
                    onAmountConsumedChange = viewModel::updateAmountConsumed,
                    onMilkTypeChange = viewModel::updateMilkType,
                    onNotesChange = viewModel::updateNotes,
                    onNextFeedAlertOverrideChange = viewModel::updateNextFeedAlertOverrideEpochMillis,
                    onShowLogFeedDialog = viewModel::showLogFeedDialog,
                    onDismissLogFeedDialog = viewModel::dismissLogFeedDialog
                )

                NurturTab.Analytics -> AnalyticsScreen(
                    modifier = Modifier.padding(paddingValues),
                    analytics = uiState.sevenDaySummary,
                    insights = uiState.analyticsInsights,
                    selectedStartDate = uiState.analyticsStartDate,
                    selectedEndDate = uiState.analyticsEndDate,
                    selectedQuickFilterDays = uiState.analyticsQuickFilterDays,
                    onDateRangeSelected = viewModel::updateAnalyticsDateRange,
                    onQuickFilterSelected = viewModel::applyAnalyticsQuickFilter
                )

                NurturTab.Settings -> SettingsScreen(
                    modifier = Modifier.padding(paddingValues),
                    settingsState = uiState.settings,
                    onDefaultBottleSizeChange = viewModel::updateDefaultBottleSizeMl,
                    onDefaultMilkTypeChange = viewModel::updateDefaultMilkType,
                    onTargetFeedIntervalHoursChange = viewModel::updateTargetFeedIntervalHours,
                    onThemeModeChange = { selectedMode -> viewModel.updateThemeMode(selectedMode) },
                    onPushNotificationsEnabledChange = viewModel::updatePushNotificationsEnabled,
                    onQuietHoursEnabledChange = viewModel::updateQuietHoursEnabled,
                    onQuietHoursStartMinutesChange = viewModel::updateQuietHoursStartMinutesOfDay,
                    onQuietHoursEndMinutesChange = viewModel::updateQuietHoursEndMinutesOfDay
                )
            }
        }
    }
}
