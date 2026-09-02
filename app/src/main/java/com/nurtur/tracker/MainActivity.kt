package com.nurtur.tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.nurtur.tracker.domain.service.FeedAlertActions
import com.nurtur.tracker.presentation.app.NurturApp
import com.nurtur.tracker.presentation.feed.FeedViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FeedViewModel by viewModels {
        val container = (application as NurturApplication).container
        FeedViewModel.Factory(
            repository = container.feedRepository,
            settingsRepository = container.settingsRepository,
            feedAlertCoordinator = container.feedAlertCoordinator
        )
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handleIncomingIntent(intent)
        setContent {
            NurturApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != FeedAlertActions.ACTION_START_FEED) {
            return
        }
        viewModel.openLogFeedFromAlert()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
