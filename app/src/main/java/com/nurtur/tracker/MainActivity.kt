package com.nurtur.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.room.Room
import com.nurtur.tracker.infrastructure.persistence.room.AppDatabase
import com.nurtur.tracker.infrastructure.preferences.DataStoreSettingsRepository
import com.nurtur.tracker.infrastructure.repository.LocalFeedRepository
import com.nurtur.tracker.presentation.app.NurturApp
import com.nurtur.tracker.presentation.feed.FeedViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FeedViewModel by viewModels {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "nurtur_db"
        ).build()
        FeedViewModel.Factory(
            repository = LocalFeedRepository(database.feedDao()),
            settingsRepository = DataStoreSettingsRepository(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NurturApp(viewModel = viewModel)
        }
    }
}
