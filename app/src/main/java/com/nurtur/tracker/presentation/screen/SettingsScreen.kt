package com.nurtur.tracker.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.presentation.theme.NurturDimens
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val MIN_INTERVAL_HOURS = 1f
private const val MAX_INTERVAL_HOURS = 12f
private const val MINUTES_PER_HOUR = 60
private val quietHoursTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private enum class QuietHoursPickerTarget {
    START,
    END
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsState: SettingsState,
    onDefaultBottleSizeChange: (String) -> Unit,
    onDefaultMilkTypeChange: (String) -> Unit,
    onTargetFeedIntervalHoursChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPushNotificationsEnabledChange: (Boolean) -> Unit,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    onQuietHoursStartMinutesChange: (Int) -> Unit,
    onQuietHoursEndMinutesChange: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    var quietHoursPickerTarget by remember { mutableStateOf<QuietHoursPickerTarget?>(null) }

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
                SettingsSectionCard(title = "Feeding Defaults") {
                    OutlinedTextField(
                        value = settingsState.defaultBottleSizeMl.toString(),
                        onValueChange = onDefaultBottleSizeChange,
                        label = { Text("Default Bottle Size (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Default Milk Type",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf("Formula", "Breastmilk")
                        options.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = settingsState.defaultMilkType == option,
                                onClick = { onDefaultMilkTypeChange(option) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                )
                            ) {
                                Text(if (option == "Breastmilk") "Breast" else "Formula")
                            }
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = "Reminders") {
                    val currentIntervalHours = (settingsState.targetFeedIntervalMinutes / 60f)
                        .coerceIn(MIN_INTERVAL_HOURS, MAX_INTERVAL_HOURS)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Feed Interval",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Triggers \"Overdue\" status. Every ${currentIntervalHours.toInt()}.0 hrs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = currentIntervalHours,
                            onValueChange = {
                                onTargetFeedIntervalHoursChange(it.toInt().toString())
                            },
                            valueRange = MIN_INTERVAL_HOURS..MAX_INTERVAL_HOURS,
                            steps = (MAX_INTERVAL_HOURS - MIN_INTERVAL_HOURS).toInt() - 1
                        )
                    }
                    SettingsToggleRow(
                        title = "Push Notifications",
                        checked = settingsState.pushNotificationsEnabled,
                        onCheckedChange = onPushNotificationsEnabledChange
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsToggleRow(
                            title = "Quiet Hours",
                            checked = settingsState.quietHoursEnabled,
                            onCheckedChange = onQuietHoursEnabledChange
                        )
                        if (settingsState.quietHoursEnabled) {
                            Text(
                                text = "Alarms will default to vibrate-only in this window.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuietHoursTimeField(
                                    label = "From",
                                    minutesOfDay = settingsState.quietHoursStartMinutesOfDay,
                                    onClick = {
                                        quietHoursPickerTarget = QuietHoursPickerTarget.START
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                QuietHoursTimeField(
                                    label = "To",
                                    minutesOfDay = settingsState.quietHoursEndMinutesOfDay,
                                    onClick = {
                                        quietHoursPickerTarget = QuietHoursPickerTarget.END
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = "Appearance") {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = ThemeMode.entries
                        options.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = settingsState.themeMode == option,
                                onClick = { onThemeModeChange(option) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                )
                            ) {
                                Text(option.toDisplayLabel())
                            }
                        }
                    }
                }
            }
            item {
                SettingsSectionCard(title = "Data") {
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

    quietHoursPickerTarget?.let { target ->
        val initialMinutes = when (target) {
            QuietHoursPickerTarget.START -> settingsState.quietHoursStartMinutesOfDay
            QuietHoursPickerTarget.END -> settingsState.quietHoursEndMinutesOfDay
        }
        QuietHoursTimePickerDialog(
            initialMinutesOfDay = initialMinutes,
            onDismiss = { quietHoursPickerTarget = null },
            onConfirm = { minutesOfDay ->
                when (target) {
                    QuietHoursPickerTarget.START -> onQuietHoursStartMinutesChange(minutesOfDay)
                    QuietHoursPickerTarget.END -> onQuietHoursEndMinutesChange(minutesOfDay)
                }
                quietHoursPickerTarget = null
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NurturDimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun QuietHoursTimeField(
    label: String,
    minutesOfDay: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = formatMinutesOfDay(minutesOfDay)
    Box(modifier = modifier.heightIn(min = NurturDimens.MinTouchTarget)) {
        OutlinedTextField(
            value = formattedTime,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        // TextField consumes presses; overlay keeps From/To tappable for the time picker.
        Box(
            modifier = Modifier
                .matchParentSize()
                .semantics {
                    role = Role.Button
                    contentDescription = "Edit quiet hours $label time, currently $formattedTime"
                }
                .clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietHoursTimePickerDialog(
    initialMinutesOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialMinutesOfDay / MINUTES_PER_HOUR,
        initialMinute = initialMinutesOfDay % MINUTES_PER_HOUR,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quiet hours") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm((timePickerState.hour * MINUTES_PER_HOUR) + timePickerState.minute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatMinutesOfDay(minutesOfDay: Int): String {
    val safeMinutes = minutesOfDay.coerceIn(0, (24 * MINUTES_PER_HOUR) - 1)
    return quietHoursTimeFormatter.format(
        LocalTime.of(safeMinutes / MINUTES_PER_HOUR, safeMinutes % MINUTES_PER_HOUR)
    )
}

private fun ThemeMode.toDisplayLabel(): String {
    return when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}
