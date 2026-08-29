package com.nurtur.tracker.domain.model

data class SettingsState(
    val defaultBottleSizeMl: Int = 120,
    val defaultMilkType: String = "Formula",
    val targetFeedIntervalMinutes: Int = 180,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
