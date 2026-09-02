package com.nurtur.tracker.infrastructure.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nurtur.tracker.domain.repository.FeedAlertScheduler
import com.nurtur.tracker.domain.service.AlertDeliveryMode
import com.nurtur.tracker.domain.service.FeedAlertActions

class AlarmManagerFeedAlertScheduler(
    private val context: Context
) : FeedAlertScheduler {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(triggerAtEpochMillis: Long) {
        FeedAlertNotifier.ensureChannels(context)
        val pendingIntent = firePendingIntent()
        val triggerAt = triggerAtEpochMillis.coerceAtLeast(System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
            return
        }
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    override fun cancel() {
        alarmManager.cancel(firePendingIntent())
        dismissActiveAlarm()
    }

    override fun dismissActiveAlarm() {
        FeedAlertNotifier.cancel(context)
        context.stopService(Intent(context, EscalatingAlarmService::class.java))
    }

    override fun notifyAlertFired(deliveryMode: AlertDeliveryMode) {
        FeedAlertNotifier.showFeedAlert(context, deliveryMode)
        if (deliveryMode != AlertDeliveryMode.ESCALATING_AUDIO) {
            return
        }
        val serviceIntent = Intent(context, EscalatingAlarmService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
            return
        }
        context.startService(serviceIntent)
    }

    private fun firePendingIntent(): PendingIntent {
        val intent = Intent(context, FeedAlertReceiver::class.java).apply {
            action = FeedAlertActions.ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_FIRE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_FIRE = 500
    }
}
