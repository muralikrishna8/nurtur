package com.nurtur.tracker

import android.app.Application
import androidx.room.Room
import com.nurtur.tracker.domain.repository.FeedAlertScheduler
import com.nurtur.tracker.domain.repository.FeedRepository
import com.nurtur.tracker.domain.repository.SettingsRepository
import com.nurtur.tracker.domain.service.FeedAlertCoordinator
import com.nurtur.tracker.infrastructure.alerts.AlarmManagerFeedAlertScheduler
import com.nurtur.tracker.infrastructure.persistence.room.AppDatabase
import com.nurtur.tracker.infrastructure.preferences.DataStoreSettingsRepository
import com.nurtur.tracker.infrastructure.repository.LocalFeedRepository

class NurturApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        DATABASE_NAME
    ).build()

    val feedRepository: FeedRepository = LocalFeedRepository(database.feedDao())
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(application)
    val feedAlertScheduler: FeedAlertScheduler = AlarmManagerFeedAlertScheduler(application)
    val feedAlertCoordinator: FeedAlertCoordinator = FeedAlertCoordinator(
        settingsRepository = settingsRepository,
        feedRepository = feedRepository,
        scheduler = feedAlertScheduler
    )

    companion object {
        private const val DATABASE_NAME = "nurtur_db"
    }
}
