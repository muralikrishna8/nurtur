package com.nurtur.tracker.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.service.FeedMetricsCalculator
import com.nurtur.tracker.presentation.feed.FeedUiState
import com.nurtur.tracker.presentation.theme.NurturColorTokens
import com.nurtur.tracker.presentation.theme.NurturDimens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val BREASTMILK_TYPE = "Breastmilk"
private const val FORMULA_TYPE = "Formula"
private const val MAX_VOLUME_DIGITS = 4
private const val NOTES_MIN_LINES = 3
private val InputControlHeight = 50.dp
private val UtcZoneId: ZoneId = ZoneId.of("UTC")

private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
private val timeOnlyFormatter = DateTimeFormatter.ofPattern("h:mm a")

private enum class FeedDateTimeTarget {
    START,
    END
}

private enum class FeedDateTimeStep {
    DATE,
    TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFeedDialog(
    uiState: FeedUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onStartTimeChange: (Long) -> Unit,
    onEndTimeChange: (Long) -> Unit,
    onAmountOfferedChange: (String) -> Unit,
    onAmountConsumedChange: (String) -> Unit,
    onMilkTypeChange: (String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    var dateTimePickerTarget by remember { mutableStateOf<FeedDateTimeTarget?>(null) }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val warningColor = if (isDarkTheme) {
        NurturColorTokens.DarkWarning
    } else {
        NurturColorTokens.LightWarning
    }
    val inputBackgroundColor = if (isDarkTheme) {
        NurturColorTokens.DarkBackground
    } else {
        NurturColorTokens.LightBackground
    }
    val offered = uiState.amountOfferedInput.toIntOrNull() ?: 0
    val consumed = uiState.amountConsumedInput.toIntOrNull() ?: 0
    val wasted = FeedMetricsCalculator.calculateWasteMl(offered, consumed)
    val zoneId = remember { ZoneId.systemDefault() }
    val startTimeText = remember(uiState.startTimeMillis) {
        formatFeedDateTime(uiState.startTimeMillis, zoneId)
    }
    val endTimeText = remember(uiState.endTimeMillis) {
        formatFeedDateTime(uiState.endTimeMillis, zoneId)
    }
    val loggedSubtitle = remember(uiState.startTimeMillis, uiState.isEditMode) {
        if (!uiState.isEditMode) {
            null
        } else {
            formatLoggedSubtitle(uiState.startTimeMillis, zoneId)
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fieldShape = RoundedCornerShape(NurturDimens.CardCornerRadius)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = inputBackgroundColor,
        unfocusedContainerColor = inputBackgroundColor,
        disabledContainerColor = inputBackgroundColor,
        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    dateTimePickerTarget?.let { target ->
        ThemedFeedDateTimePicker(
            initialTimeMillis = when (target) {
                FeedDateTimeTarget.START -> uiState.startTimeMillis
                FeedDateTimeTarget.END -> uiState.endTimeMillis
            },
            zoneId = zoneId,
            onDismiss = { dateTimePickerTarget = null },
            onConfirm = { selectedMillis ->
                when (target) {
                    FeedDateTimeTarget.START -> onStartTimeChange(selectedMillis)
                    FeedDateTimeTarget.END -> onEndTimeChange(selectedMillis)
                }
                dateTimePickerTarget = null
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(
            topStart = NurturDimens.SheetCornerRadius,
            topEnd = NurturDimens.SheetCornerRadius
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(NurturDimens.SectionSpacing)
        ) {
            HeaderRow(
                isEditMode = uiState.isEditMode,
                loggedSubtitle = loggedSubtitle
            )

            DateTimeField(
                label = "TIME OF FEED",
                value = startTimeText,
                shape = fieldShape,
                colors = fieldColors,
                onClick = { dateTimePickerTarget = FeedDateTimeTarget.START }
            )

            DateTimeField(
                label = "TIME OF COMPLETION",
                value = endTimeText,
                shape = fieldShape,
                colors = fieldColors,
                onClick = { dateTimePickerTarget = FeedDateTimeTarget.END }
            )

            FieldLabel(text = "MILK TYPE")
            MilkTypeSelector(
                selectedMilkType = uiState.milkTypeInput,
                onMilkTypeChange = onMilkTypeChange,
                inactiveContainerColor = inputBackgroundColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VolumeNumberField(
                    label = "OFFERED (ML)",
                    value = uiState.amountOfferedInput,
                    onValueChange = { raw ->
                        onAmountOfferedChange(sanitizeVolumeInput(raw))
                    },
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
                VolumeNumberField(
                    label = "CONSUMED (ML)",
                    value = uiState.amountConsumedInput,
                    onValueChange = { raw ->
                        onAmountConsumedChange(sanitizeVolumeInput(raw))
                    },
                    shape = fieldShape,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
            }

            WastedMilkRow(wastedMl = wasted, warningColor = warningColor)

            FieldLabel(text = "NOTES (OPTIONAL)")
            OutlinedTextField(
                value = uiState.notesInput,
                onValueChange = onNotesChange,
                placeholder = {
                    Text(
                        text = "Add any notes about this feed...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                minLines = NOTES_MIN_LINES,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            uiState.formError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = NurturDimens.MinTouchTarget),
                shape = RoundedCornerShape(NurturDimens.CardCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (uiState.isEditMode) "Save Changes" else "Save Feed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.isEditMode) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = NurturDimens.MinTouchTarget)
                ) {
                    Text(
                        text = "Delete Feed Entry",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    isEditMode: Boolean,
    loggedSubtitle: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = if (isEditMode) "Edit Feed" else "Log Feed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (loggedSubtitle != null) {
            Text(
                text = loggedSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun DateTimeField(
    label: String,
    value: String,
    shape: RoundedCornerShape,
    colors: TextFieldColors,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(text = label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClick = onClick
                )
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                singleLine = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null
                    )
                },
                shape = shape,
                colors = colors,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VolumeNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    shape: RoundedCornerShape,
    colors: TextFieldColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FieldLabel(text = label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = shape,
            colors = colors,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkTypeSelector(
    selectedMilkType: String,
    onMilkTypeChange: (String) -> Unit,
    inactiveContainerColor: Color
) {
    val options = listOf(
        MilkTypeOption(
            storedValue = BREASTMILK_TYPE,
            label = "Breast",
            icon = Icons.Default.AutoAwesome
        ),
        MilkTypeOption(
            storedValue = FORMULA_TYPE,
            label = "Formula",
            icon = Icons.Default.LocalDrink
        )
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(InputControlHeight)
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selectedMilkType == option.storedValue,
                onClick = { onMilkTypeChange(option.storedValue) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null
                    )
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    inactiveContainerColor = inactiveContainerColor,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.height(InputControlHeight)
            ) {
                Text(option.label)
            }
        }
    }
}

@Composable
private fun WastedMilkRow(
    wastedMl: Int,
    warningColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Wasted Milk (auto-calculated)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$wastedMl ml",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = warningColor
        )
    }
}

private data class MilkTypeOption(
    val storedValue: String,
    val label: String,
    val icon: ImageVector
)

private fun sanitizeVolumeInput(raw: String): String {
    return raw.filter { it.isDigit() }.take(MAX_VOLUME_DIGITS)
}

private fun formatFeedDateTime(
    epochMillis: Long,
    zoneId: ZoneId
): String {
    return dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))
}

private fun formatLoggedSubtitle(epochMillis: Long, zoneId: ZoneId): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    val feedDate = dateTime.toLocalDate()
    val today = LocalDate.now(zoneId)
    val timePart = timeOnlyFormatter.format(dateTime)
    return when (feedDate) {
        today -> "Logged today at $timePart"
        today.minusDays(1) -> "Logged yesterday at $timePart"
        else -> "Logged ${dateTimeFormatter.format(dateTime)}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemedFeedDateTimePicker(
    initialTimeMillis: Long,
    zoneId: ZoneId,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var step by remember { mutableStateOf(FeedDateTimeStep.DATE) }
    val initialDateTime = remember(initialTimeMillis, zoneId) {
        Instant.ofEpochMilli(initialTimeMillis).atZone(zoneId)
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateToUtcStartMillis(initialDateTime.toLocalDate())
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute,
        is24Hour = false
    )

    when (step) {
        FeedDateTimeStep.DATE -> {
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = { step = FeedDateTimeStep.TIME },
                        enabled = datePickerState.selectedDateMillis != null
                    ) {
                        Text("Next")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors()
                )
            }
        }

        FeedDateTimeStep.TIME -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text("Select time")
                },
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
                            val selectedDateMillis = datePickerState.selectedDateMillis ?: return@TextButton
                            val selectedDate = utcStartMillisToLocalDate(selectedDateMillis)
                            val selectedMillis = selectedDate
                                .atTime(timePickerState.hour, timePickerState.minute)
                                .atZone(zoneId)
                                .toInstant()
                                .toEpochMilli()
                            onConfirm(selectedMillis)
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { step = FeedDateTimeStep.DATE }) {
                        Text("Back")
                    }
                }
            )
        }
    }
}

private fun localDateToUtcStartMillis(date: LocalDate): Long {
    return date.atStartOfDay(UtcZoneId).toInstant().toEpochMilli()
}

private fun utcStartMillisToLocalDate(utcStartMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcStartMillis).atZone(UtcZoneId).toLocalDate()
}
