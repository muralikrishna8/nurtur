package com.nurtur.tracker.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import com.nurtur.tracker.domain.model.ThemeMode

private val LightScheme = lightColorScheme(
    primary = NurturColorTokens.LightPrimary,
    onPrimary = NurturColorTokens.LightOnPrimary,
    primaryContainer = NurturColorTokens.LightPrimaryContainer,
    onPrimaryContainer = NurturColorTokens.LightOnPrimaryContainer,
    secondary = NurturColorTokens.LightSecondary,
    onSecondary = NurturColorTokens.LightOnSecondary,
    secondaryContainer = NurturColorTokens.LightPrimaryContainer,
    onSecondaryContainer = NurturColorTokens.LightOnPrimaryContainer,
    tertiary = NurturColorTokens.LightSecondary,
    onTertiary = NurturColorTokens.LightOnSecondary,
    background = NurturColorTokens.LightBackground,
    onBackground = NurturColorTokens.LightOnSurface,
    surface = NurturColorTokens.LightSurface,
    onSurface = NurturColorTokens.LightOnSurface,
    surfaceVariant = NurturColorTokens.LightSurfaceVariant,
    onSurfaceVariant = NurturColorTokens.LightOnSurfaceVariant,
    error = NurturColorTokens.LightError,
    onError = NurturColorTokens.LightOnError
)

private val DarkScheme = darkColorScheme(
    primary = NurturColorTokens.DarkPrimary,
    onPrimary = NurturColorTokens.DarkOnPrimary,
    primaryContainer = NurturColorTokens.DarkPrimaryContainer,
    onPrimaryContainer = NurturColorTokens.DarkOnPrimaryContainer,
    secondary = NurturColorTokens.DarkSecondary,
    onSecondary = NurturColorTokens.DarkOnSecondary,
    secondaryContainer = NurturColorTokens.DarkPrimaryContainer,
    onSecondaryContainer = NurturColorTokens.DarkOnPrimaryContainer,
    tertiary = NurturColorTokens.DarkSecondary,
    onTertiary = NurturColorTokens.DarkOnSecondary,
    background = NurturColorTokens.DarkBackground,
    onBackground = NurturColorTokens.DarkOnSurface,
    surface = NurturColorTokens.DarkSurface,
    onSurface = NurturColorTokens.DarkOnSurface,
    surfaceVariant = NurturColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = NurturColorTokens.DarkOnSurfaceVariant,
    error = NurturColorTokens.DarkError,
    onError = NurturColorTokens.DarkOnError
)

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
