package com.nurtur.tracker.presentation.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nurtur.tracker.presentation.feed.FeedViewModel
import com.nurtur.tracker.presentation.screen.AnalyticsScreen
import com.nurtur.tracker.presentation.screen.HomeScreen
import com.nurtur.tracker.presentation.screen.SettingsScreen

enum class NurturTab(val label: String) {
    Home("Home"),
    Analytics("Analytics"),
    Settings("Settings")
}

@Composable
fun NurturApp(viewModel: FeedViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(NurturTab.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == NurturTab.Home,
                    onClick = { selectedTab = NurturTab.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(NurturTab.Home.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == NurturTab.Analytics,
                    onClick = { selectedTab = NurturTab.Analytics },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text(NurturTab.Analytics.label) }
                )
                NavigationBarItem(
                    selected = selectedTab == NurturTab.Settings,
                    onClick = { selectedTab = NurturTab.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(NurturTab.Settings.label) }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            NurturTab.Home -> HomeScreen(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onSaveFeed = viewModel::saveFeed,
                onDeleteFeed = { viewModel.deleteFeed(it) },
                onStartTimeChange = viewModel::updateStartTimeMillis,
                onEndTimeChange = viewModel::updateEndTimeMillis,
                onAmountOfferedChange = viewModel::updateAmountOffered,
                onAmountConsumedChange = viewModel::updateAmountConsumed,
                onMilkTypeChange = viewModel::updateMilkType,
                onNotesChange = viewModel::updateNotes
            )

            NurturTab.Analytics -> AnalyticsScreen(
                modifier = Modifier.padding(paddingValues),
                analytics = uiState.sevenDaySummary
            )

            NurturTab.Settings -> SettingsScreen(
                modifier = Modifier.padding(paddingValues),
                settingsState = uiState.settings,
                onDefaultBottleSizeChange = viewModel::updateDefaultBottleSizeMl,
                onDefaultMilkTypeChange = viewModel::updateDefaultMilkType
            )
        }
    }
}
