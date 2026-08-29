package com.nurtur.tracker.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode

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
    var isMilkTypeMenuExpanded by remember { mutableStateOf(false) }
    var isThemeMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = settingsState.defaultBottleSizeMl.toString(),
            onValueChange = onDefaultBottleSizeChange,
            label = { Text("Default bottle size (ml)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = (settingsState.targetFeedIntervalMinutes / 60).toString(),
            onValueChange = onTargetFeedIntervalHoursChange,
            label = { Text("Target feed interval (hours)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(
            expanded = isMilkTypeMenuExpanded,
            onExpandedChange = { isMilkTypeMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = settingsState.defaultMilkType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Default milk type") },
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
                            onDefaultMilkTypeChange(option)
                            isMilkTypeMenuExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = isThemeMenuExpanded,
            onExpandedChange = { isThemeMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = settingsState.themeMode.toDisplayLabel(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Theme") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isThemeMenuExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isThemeMenuExpanded,
                onDismissRequest = { isThemeMenuExpanded = false }
            ) {
                ThemeMode.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toDisplayLabel()) },
                        onClick = {
                            onThemeModeChange(option)
                            isThemeMenuExpanded = false
                        }
                    )
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
