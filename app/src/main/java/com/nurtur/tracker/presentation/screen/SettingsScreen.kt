package com.nurtur.tracker.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.presentation.theme.NurturDimens

private const val MIN_INTERVAL_HOURS = 1f
private const val MAX_INTERVAL_HOURS = 12f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsState: SettingsState,
    onDefaultBottleSizeChange: (String) -> Unit,
    onDefaultMilkTypeChange: (String) -> Unit,
    onTargetFeedIntervalHoursChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = NurturDimens.ScreenHorizontalPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(NurturDimens.SectionSpacing)
        ) {
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Defaults", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = settingsState.defaultBottleSizeMl.toString(),
                            onValueChange = onDefaultBottleSizeChange,
                            label = { Text("Bottle Size (ml)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val options = listOf("Formula", "Breastmilk")
                            options.forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = settingsState.defaultMilkType == option,
                                    onClick = { onDefaultMilkTypeChange(option) },
                                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size
                                    )
                                ) {
                                    Text(option)
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Preferences", style = MaterialTheme.typography.titleMedium)
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val options = ThemeMode.entries
                            options.forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = settingsState.themeMode == option,
                                    onClick = { onThemeModeChange(option) },
                                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size
                                    )
                                ) {
                                    Text(option.toDisplayLabel())
                                }
                            }
                        }
                        val currentIntervalHours = (settingsState.targetFeedIntervalMinutes / 60f)
                            .coerceIn(MIN_INTERVAL_HOURS, MAX_INTERVAL_HOURS)
                        Text(
                            "Target feed interval: ${currentIntervalHours.toInt()}h",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = currentIntervalHours,
                            onValueChange = { onTargetFeedIntervalHoursChange(it.toInt().toString()) },
                            valueRange = MIN_INTERVAL_HOURS..MAX_INTERVAL_HOURS,
                            steps = (MAX_INTERVAL_HOURS - MIN_INTERVAL_HOURS).toInt() - 1
                        )
                    }
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Data", style = MaterialTheme.typography.titleMedium)
                        ListItem(
                            headlineContent = { Text("Export Data (CSV)") },
                            supportingContent = { Text("Coming soon") },
                            trailingContent = { Badge { Text("v2") } }
                        )
                        ListItem(
                            headlineContent = { Text("Delete All Data") },
                            supportingContent = { Text("Coming soon") },
                            trailingContent = { Badge { Text("v2") } }
                        )
                    }
                }
            }
        }
    }
}

private fun ThemeMode.toDisplayLabel(): String {
    return when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}
