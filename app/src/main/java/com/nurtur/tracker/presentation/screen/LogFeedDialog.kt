package com.nurtur.tracker.presentation.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.service.FeedMetricsCalculator
import com.nurtur.tracker.presentation.feed.FeedUiState
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
    onStartTimeChange: (Long) -> Unit,
    onEndTimeChange: (Long) -> Unit,
    onAmountOfferedChange: (String) -> Unit,
    onAmountConsumedChange: (String) -> Unit,
    onMilkTypeChange: (String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    val context = LocalContext.current
    var isMilkTypeMenuExpanded by remember { mutableStateOf(false) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Feed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                ) { Text("Pick Start Date & Time") }
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
                ) { Text("Pick End Date & Time") }
                OutlinedTextField(
                    value = uiState.amountOfferedInput,
                    onValueChange = onAmountOfferedChange,
                    label = { Text("Amount Offered (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.amountConsumedInput,
                    onValueChange = onAmountConsumedChange,
                    label = { Text("Amount Consumed (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Amount Wasted: $wasted ml", style = MaterialTheme.typography.bodyMedium)

                ExposedDropdownMenuBox(
                    expanded = isMilkTypeMenuExpanded,
                    onExpandedChange = { isMilkTypeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.milkTypeInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Milk Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMilkTypeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isMilkTypeMenuExpanded,
                        onDismissRequest = { isMilkTypeMenuExpanded = false }
                    ) {
                        listOf("Formula", "Breastmilk").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onMilkTypeChange(option)
                                    isMilkTypeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.notesInput,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.formError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.padding(8.dp)
    )
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
