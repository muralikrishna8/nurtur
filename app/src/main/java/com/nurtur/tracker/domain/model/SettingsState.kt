package com.nurtur.tracker.domain.model

data class SettingsState(
    val defaultBottleSizeMl: Int = 120,
    val defaultMilkType: String = "Formula",
    val targetFeedIntervalMinutes: Int = 180,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pushNotificationsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStartMinutesOfDay: Int = DEFAULT_QUIET_HOURS_START_MINUTES,
    val quietHoursEndMinutesOfDay: Int = DEFAULT_QUIET_HOURS_END_MINUTES,
    val nextFeedAlertOverrideEpochMillis: Long? = null
) {
    companion object {
        const val DEFAULT_QUIET_HOURS_START_MINUTES = 22 * 60
        const val DEFAULT_QUIET_HOURS_END_MINUTES = 6 * 60
    }
}
