package com.nurtur.tracker.infrastructure.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nurtur.tracker.NurturApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val app = context.applicationContext as? NurturApplication ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.feedAlertCoordinator.rescheduleFromCurrentState()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
