package com.nurtur.tracker.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.presentation.theme.NurturColorTokens
import com.nurtur.tracker.presentation.theme.NurturDimens
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val MIN_INTERVAL_HOURS = 1f
private const val MAX_INTERVAL_HOURS = 7f
private const val MINUTES_PER_HOUR = 60
private const val MIN_BOTTLE_SIZE_ML = 30
private const val MAX_BOTTLE_SIZE_ML = 500
private const val MAX_BOTTLE_SIZE_DIGITS = 3
private const val BREASTMILK_TYPE = "Breastmilk"
private const val FORMULA_TYPE = "Formula"
private val quietHoursTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val settingCardShape = RoundedCornerShape(NurturDimens.CardCornerRadius)
private val compactControlShape = RoundedCornerShape(12.dp)

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
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val inputBackgroundColor = if (isDarkTheme) {
        NurturColorTokens.DarkBackground
    } else {
        NurturColorTokens.LightBackground
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
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
                SettingsSectionHeader(title = "FEEDING DEFAULTS")
            }
            item {
                SettingsSettingCard {
                    SettingsLabeledControlRow(
                        title = "Default Bottle Size",
                        subtitle = "Used for quick fill of offered milk",
                        control = {
                            CompactBottleSizeField(
                                valueMl = settingsState.defaultBottleSizeMl,
                                onValueChange = onDefaultBottleSizeChange,
                                containerColor = inputBackgroundColor
                            )
                        }
                    )
                }
            }
            item {
                SettingsSettingCard {
                    SettingsLabeledControlRow(
                        title = "Default Milk Type",
                        subtitle = "Pre-selected on log sheet",
                        control = {
                            CompactMilkTypeSelector(
                                selectedMilkType = settingsState.defaultMilkType,
                                onMilkTypeChange = onDefaultMilkTypeChange,
                                inactiveContainerColor = inputBackgroundColor
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(title = "REMINDERS")
            }
            item {
                val currentIntervalHours = (settingsState.targetFeedIntervalMinutes / 60f)
                    .coerceIn(MIN_INTERVAL_HOURS, MAX_INTERVAL_HOURS)
                SettingsSettingCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsLabeledControlRow(
                            title = "Feed Interval",
                            subtitle = "Triggers \"Overdue\" status",
                            control = {
                                Text(
                                    text = "Every ${currentIntervalHours.toInt()}.0 hrs",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                        Slider(
                            value = currentIntervalHours,
                            onValueChange = {
                                onTargetFeedIntervalHoursChange(it.toInt().toString())
                            },
                            valueRange = MIN_INTERVAL_HOURS..MAX_INTERVAL_HOURS,
                            steps = (MAX_INTERVAL_HOURS - MIN_INTERVAL_HOURS).toInt() - 1,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.25f
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription =
                                        "Feed interval, currently every ${currentIntervalHours.toInt()} hours"
                                }
                        )
                    }
                }
            }
            item {
                SettingsSettingCard {
                    SettingsToggleRow(
                        title = "Push Notifications",
                        subtitle = "Get reminded when it's time to feed",
                        checked = settingsState.pushNotificationsEnabled,
                        onCheckedChange = onPushNotificationsEnabledChange
                    )
                }
            }
            item {
                SettingsSettingCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsToggleRow(
                            title = "Quiet Hours",
                            subtitle = "Alarms will only vibrate",
                            checked = settingsState.quietHoursEnabled,
                            onCheckedChange = onQuietHoursEnabledChange
                        )
                        if (settingsState.quietHoursEnabled) {
                            QuietHoursTimeRangeRow(
                                startMinutesOfDay = settingsState.quietHoursStartMinutesOfDay,
                                endMinutesOfDay = settingsState.quietHoursEndMinutesOfDay,
                                containerColor = inputBackgroundColor,
                                onStartClick = {
                                    quietHoursPickerTarget = QuietHoursPickerTarget.START
                                },
                                onEndClick = {
                                    quietHoursPickerTarget = QuietHoursPickerTarget.END
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(title = "APPEARANCE")
            }
            item {
                SettingsSettingCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "App Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        CompactThemeSelector(
                            selectedTheme = settingsState.themeMode,
                            onThemeModeChange = onThemeModeChange,
                            inactiveContainerColor = inputBackgroundColor
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
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
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsSettingCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = settingCardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsLabeledControlRow(
    title: String,
    subtitle: String?,
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NurturDimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        control()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
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
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun CompactBottleSizeField(
    valueMl: Int,
    onValueChange: (String) -> Unit,
    containerColor: Color
) {
    var isFocused by remember { mutableStateOf(false) }
    var draft by remember {
        mutableStateOf(TextFieldValue(text = valueMl.toString()))
    }

    LaunchedEffect(valueMl, isFocused) {
        if (!isFocused) {
            val committed = valueMl.toString()
            if (draft.text != committed) {
                draft = TextFieldValue(
                    text = committed,
                    selection = TextRange(committed.length)
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .clip(compactControlShape)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .widthIn(min = 72.dp)
            .semantics { contentDescription = "Default bottle size in milliliters" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        BasicTextField(
            value = draft,
            onValueChange = { incoming ->
                val sanitized = sanitizeBottleSizeDraft(incoming.text)
                draft = if (sanitized == incoming.text) {
                    incoming
                } else {
                    incoming.copy(
                        text = sanitized,
                        selection = TextRange(sanitized.length)
                    )
                }
                val parsed = sanitized.toIntOrNull()
                if (parsed != null && parsed in MIN_BOTTLE_SIZE_ML..MAX_BOTTLE_SIZE_ML) {
                    onValueChange(sanitized)
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .widthIn(min = 28.dp, max = 56.dp)
                .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    if (wasFocused && !focusState.isFocused) {
                        val parsed = draft.text.toIntOrNull()
                        if (parsed == null || parsed !in MIN_BOTTLE_SIZE_ML..MAX_BOTTLE_SIZE_ML) {
                            val committed = valueMl.toString()
                            draft = TextFieldValue(
                                text = committed,
                                selection = TextRange(committed.length)
                            )
                        } else {
                            onValueChange(parsed.toString())
                        }
                    }
                }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "ml",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun sanitizeBottleSizeDraft(raw: String): String {
    return raw.filter { it.isDigit() }.take(MAX_BOTTLE_SIZE_DIGITS)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactMilkTypeSelector(
    selectedMilkType: String,
    onMilkTypeChange: (String) -> Unit,
    inactiveContainerColor: Color
) {
    val options = listOf(
        BREASTMILK_TYPE to "Breast",
        FORMULA_TYPE to "Formula"
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.widthIn(max = 180.dp)
    ) {
        options.forEachIndexed { index, (storedValue, label) ->
            SegmentedButton(
                selected = selectedMilkType == storedValue,
                onClick = { onMilkTypeChange(storedValue) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = inactiveContainerColor,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.heightIn(min = 36.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactThemeSelector(
    selectedTheme: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    inactiveContainerColor: Color
) {
    val options = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selectedTheme == option,
                onClick = { onThemeModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = inactiveContainerColor,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.heightIn(min = 36.dp)
            ) {
                Text(
                    text = option.toDisplayLabel(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun QuietHoursTimeRangeRow(
    startMinutesOfDay: Int,
    endMinutesOfDay: Int,
    containerColor: Color,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuietHoursTimeChip(
            minutesOfDay = startMinutesOfDay,
            containerColor = containerColor,
            onClick = onStartClick,
            contentDescriptionLabel = "start",
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "to",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        QuietHoursTimeChip(
            minutesOfDay = endMinutesOfDay,
            containerColor = containerColor,
            onClick = onEndClick,
            contentDescriptionLabel = "end",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuietHoursTimeChip(
    minutesOfDay: Int,
    containerColor: Color,
    onClick: () -> Unit,
    contentDescriptionLabel: String,
    modifier: Modifier = Modifier
) {
    val formattedTime = formatMinutesOfDay(minutesOfDay)
    Box(
        modifier = modifier
            .heightIn(min = NurturDimens.MinTouchTarget)
            .clip(compactControlShape)
            .background(containerColor)
            .semantics {
                role = Role.Button
                contentDescription =
                    "Edit quiet hours $contentDescriptionLabel time, currently $formattedTime"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
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
