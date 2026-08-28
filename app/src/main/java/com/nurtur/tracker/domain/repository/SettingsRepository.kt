package com.nurtur.tracker.domain.repository

import com.nurtur.tracker.domain.model.SettingsState
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<SettingsState>
    suspend fun updateDefaultBottleSizeMl(value: Int)
    suspend fun updateDefaultMilkType(value: String)
}
