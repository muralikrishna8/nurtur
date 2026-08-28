package com.nurtur.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.room.Room
import com.nurtur.tracker.data.local.AppDatabase
import com.nurtur.tracker.data.preferences.SettingsPreferences
import com.nurtur.tracker.data.repository.LocalFeedRepository
import com.nurtur.tracker.ui.NurturApp
import com.nurtur.tracker.ui.theme.NurturTheme
import com.nurtur.tracker.ui.viewmodel.FeedViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FeedViewModel by viewModels {
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "nurtur_db"
        ).build()
        FeedViewModel.Factory(
            repository = LocalFeedRepository(database.feedDao()),
            settingsPreferences = SettingsPreferences(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NurturTheme {
                NurturApp(viewModel = viewModel)
            }
        }
    }
}
