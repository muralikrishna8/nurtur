package com.nurtur.tracker.infrastructure.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.model.ThemeMode
import com.nurtur.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nurtur_settings")

private const val MIN_BOTTLE_SIZE_ML = 30
private const val MAX_BOTTLE_SIZE_ML = 500
private const val MIN_INTERVAL_MINUTES = 30
private const val MAX_INTERVAL_MINUTES = 720
private const val MINUTES_PER_DAY = 24 * 60
private const val CLEARED_OVERRIDE_SENTINEL = -1L

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    private object Keys {
        val defaultBottleSizeMl = intPreferencesKey("default_bottle_size_ml")
        val defaultMilkType = stringPreferencesKey("default_milk_type")
        val targetFeedIntervalMinutes = intPreferencesKey("target_feed_interval_minutes")
        val themeMode = stringPreferencesKey("theme_mode")
        val pushNotificationsEnabled = booleanPreferencesKey("push_notifications_enabled")
        val quietHoursEnabled = booleanPreferencesKey("quiet_hours_enabled")
        val quietHoursStartMinutesOfDay = intPreferencesKey("quiet_hours_start_minutes")
        val quietHoursEndMinutesOfDay = intPreferencesKey("quiet_hours_end_minutes")
        val nextFeedAlertOverrideEpochMillis = longPreferencesKey("next_feed_alert_override_epoch_millis")
    }

    override val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { preferences ->
        val storedOverride = preferences[Keys.nextFeedAlertOverrideEpochMillis]
        SettingsState(
            defaultBottleSizeMl = preferences[Keys.defaultBottleSizeMl] ?: 120,
            defaultMilkType = preferences[Keys.defaultMilkType] ?: "Formula",
            targetFeedIntervalMinutes = preferences[Keys.targetFeedIntervalMinutes] ?: 180,
            themeMode = ThemeMode.fromStoredValue(preferences[Keys.themeMode]),
            pushNotificationsEnabled = preferences[Keys.pushNotificationsEnabled] ?: true,
            quietHoursEnabled = preferences[Keys.quietHoursEnabled] ?: true,
            quietHoursStartMinutesOfDay = preferences[Keys.quietHoursStartMinutesOfDay]
                ?: SettingsState.DEFAULT_QUIET_HOURS_START_MINUTES,
            quietHoursEndMinutesOfDay = preferences[Keys.quietHoursEndMinutesOfDay]
                ?: SettingsState.DEFAULT_QUIET_HOURS_END_MINUTES,
            nextFeedAlertOverrideEpochMillis = storedOverride?.takeIf { it >= 0L }
        )
    }

    override suspend fun updateDefaultBottleSizeMl(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.defaultBottleSizeMl] = value.coerceIn(MIN_BOTTLE_SIZE_ML, MAX_BOTTLE_SIZE_ML)
        }
    }

    override suspend fun updateDefaultMilkType(value: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.defaultMilkType] = if (value == "Breastmilk") value else "Formula"
        }
    }

    override suspend fun updateTargetFeedIntervalMinutes(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.targetFeedIntervalMinutes] =
                value.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
        }
    }

    override suspend fun updateThemeMode(value: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.themeMode] = value.name
        }
    }

    override suspend fun updatePushNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.pushNotificationsEnabled] = value
        }
    }

    override suspend fun updateQuietHoursEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.quietHoursEnabled] = value
        }
    }

    override suspend fun updateQuietHoursStartMinutesOfDay(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.quietHoursStartMinutesOfDay] =
                value.coerceIn(0, MINUTES_PER_DAY - 1)
        }
    }

    override suspend fun updateQuietHoursEndMinutesOfDay(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.quietHoursEndMinutesOfDay] =
                value.coerceIn(0, MINUTES_PER_DAY - 1)
        }
    }

    override suspend fun updateNextFeedAlertOverrideEpochMillis(value: Long?) {
        context.dataStore.edit { preferences ->
            preferences[Keys.nextFeedAlertOverrideEpochMillis] = value ?: CLEARED_OVERRIDE_SENTINEL
        }
    }
}
