package com.nurtur.tracker.infrastructure.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nurtur.tracker.NurturApplication
import com.nurtur.tracker.domain.service.FeedAlertActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FeedAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext as? NurturApplication ?: return
        val coordinator = app.container.feedAlertCoordinator
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    FeedAlertActions.ACTION_FIRE -> coordinator.handleAlertFired()
                    FeedAlertActions.ACTION_SNOOZE -> coordinator.handleSnooze()
                    FeedAlertActions.ACTION_SKIP -> coordinator.handleSkip()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
