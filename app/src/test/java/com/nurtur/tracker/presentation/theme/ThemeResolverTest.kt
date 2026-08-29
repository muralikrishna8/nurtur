package com.nurtur.tracker.presentation.theme

import com.nurtur.tracker.domain.model.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {

    @Test
    fun resolveDarkTheme_systemModeAndSystemDark_returnsTrue() {
        val resolved = resolveDarkTheme(ThemeMode.SYSTEM, isSystemDarkTheme = true)
        assertTrue(resolved)
    }

    @Test
    fun resolveDarkTheme_systemModeAndSystemLight_returnsFalse() {
        val resolved = resolveDarkTheme(ThemeMode.SYSTEM, isSystemDarkTheme = false)
        assertFalse(resolved)
    }

    @Test
    fun resolveDarkTheme_lightMode_ignoresSystemTheme() {
        val resolved = resolveDarkTheme(ThemeMode.LIGHT, isSystemDarkTheme = true)
        assertFalse(resolved)
    }

    @Test
    fun resolveDarkTheme_darkMode_ignoresSystemTheme() {
        val resolved = resolveDarkTheme(ThemeMode.DARK, isSystemDarkTheme = false)
        assertTrue(resolved)
    }
}
