package com.nurtur.tracker.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import com.nurtur.tracker.domain.model.ThemeMode

private val LightScheme = lightColorScheme()
private val DarkScheme = darkColorScheme()

@Composable
fun NurturTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = resolveDarkTheme(themeMode, isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content
    )
}

fun resolveDarkTheme(themeMode: ThemeMode, isSystemDarkTheme: Boolean): Boolean {
    return when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
