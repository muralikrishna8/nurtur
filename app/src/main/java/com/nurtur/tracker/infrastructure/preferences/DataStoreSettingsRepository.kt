package com.nurtur.tracker.infrastructure.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nurtur.tracker.domain.model.SettingsState
import com.nurtur.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nurtur_settings")

class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    private object Keys {
        val defaultBottleSizeMl = intPreferencesKey("default_bottle_size_ml")
        val defaultMilkType = stringPreferencesKey("default_milk_type")
        val targetFeedIntervalMinutes = intPreferencesKey("target_feed_interval_minutes")
    }

    override val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { preferences ->
        SettingsState(
            defaultBottleSizeMl = preferences[Keys.defaultBottleSizeMl] ?: 120,
            defaultMilkType = preferences[Keys.defaultMilkType] ?: "Formula",
            targetFeedIntervalMinutes = preferences[Keys.targetFeedIntervalMinutes] ?: 180
        )
    }

    override suspend fun updateDefaultBottleSizeMl(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.defaultBottleSizeMl] = value.coerceIn(30, 500)
        }
    }

    override suspend fun updateDefaultMilkType(value: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.defaultMilkType] = if (value == "Breastmilk") value else "Formula"
        }
    }

    override suspend fun updateTargetFeedIntervalMinutes(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.targetFeedIntervalMinutes] = value.coerceIn(30, 720)
        }
    }
}
