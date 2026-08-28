package com.nurtur.tracker.ui.screens

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.service.FeedMetricsCalculator
import com.nurtur.tracker.ui.viewmodel.FeedUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFeedDialog(
    uiState: FeedUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onAmountOfferedChange: (String) -> Unit,
    onAmountConsumedChange: (String) -> Unit,
    onMilkTypeChange: (String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    var isMilkTypeMenuExpanded by remember { mutableStateOf(false) }
    val offered = uiState.amountOfferedInput.toIntOrNull() ?: 0
    val consumed = uiState.amountConsumedInput.toIntOrNull() ?: 0
    val wasted = FeedMetricsCalculator.calculateWasteMl(offered, consumed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Feed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.startTimeInput,
                    onValueChange = onStartTimeChange,
                    label = { Text("Start Time (epoch ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.endTimeInput,
                    onValueChange = onEndTimeChange,
                    label = { Text("End Time (epoch ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
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
