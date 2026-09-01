package com.nurtur.tracker.presentation.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.service.FeedMetricsCalculator
import com.nurtur.tracker.presentation.feed.FeedUiState
import com.nurtur.tracker.presentation.theme.NurturDimens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

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
    val context = LocalContext.current
    val offered = uiState.amountOfferedInput.toIntOrNull() ?: 0
    val consumed = uiState.amountConsumedInput.toIntOrNull() ?: 0
    val wasted = FeedMetricsCalculator.calculateWasteMl(offered, consumed)
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a") }
    val startTimeText = remember(uiState.startTimeMillis) {
        formatter.format(Instant.ofEpochMilli(uiState.startTimeMillis).atZone(ZoneId.systemDefault()))
    }
    val endTimeText = remember(uiState.endTimeMillis) {
        formatter.format(Instant.ofEpochMilli(uiState.endTimeMillis).atZone(ZoneId.systemDefault()))
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (uiState.isEditMode) "Edit Feed" else "Log Feed",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            OutlinedTextField(
                value = startTimeText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Time") },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = {
                    showDateTimePicker(
                        context = context,
                        initialTimeMillis = uiState.startTimeMillis,
                        onDateTimeSelected = onStartTimeChange
                    )
                }
            ) {
                Text("Pick Start Date & Time")
            }
            OutlinedTextField(
                value = endTimeText,
                onValueChange = {},
                readOnly = true,
                label = { Text("End Time") },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = {
                    showDateTimePicker(
                        context = context,
                        initialTimeMillis = uiState.endTimeMillis,
                        onDateTimeSelected = onEndTimeChange
                    )
                }
            ) {
                Text("Pick End Date & Time")
            }
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val types = listOf("Breastmilk", "Formula")
                types.forEachIndexed { index, milkType ->
                    SegmentedButton(
                        selected = uiState.milkTypeInput == milkType,
                        onClick = { onMilkTypeChange(milkType) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = types.size
                        )
                    ) {
                        Text(milkType)
                    }
                }
            }
            OutlinedTextField(
                value = uiState.amountOfferedInput,
                onValueChange = onAmountOfferedChange,
                label = { Text("Offered (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.amountConsumedInput,
                onValueChange = onAmountConsumedChange,
                label = { Text("Consumed (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Wasted milk: $wasted ml", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = uiState.notesInput,
                onValueChange = onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            uiState.formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(if (uiState.isEditMode) "Save Changes" else "Save Feed")
            }
            if (uiState.isEditMode) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = NurturDimens.ScreenHorizontalPadding)
                ) {
                    Text("Delete Feed", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun showDateTimePicker(
    context: android.content.Context,
    initialTimeMillis: Long,
    onDateTimeSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTimeMillis }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    pickedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    pickedDate.set(Calendar.MINUTE, minute)
                    pickedDate.set(Calendar.SECOND, 0)
                    pickedDate.set(Calendar.MILLISECOND, 0)
                    onDateTimeSelected(pickedDate.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
