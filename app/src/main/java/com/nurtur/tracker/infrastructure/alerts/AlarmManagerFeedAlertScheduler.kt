package com.nurtur.tracker.infrastructure.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nurtur.tracker.domain.repository.FeedAlertScheduler
import com.nurtur.tracker.domain.service.AlertDeliveryMode
import com.nurtur.tracker.domain.service.ExactAlarmCapability
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
        val useExact = ExactAlarmCapability.shouldUseExactAlarm(
            sdkInt = Build.VERSION.SDK_INT,
            canScheduleExactAlarms = canScheduleExactAlarms()
        )
        try {
            if (useExact) {
                scheduleExact(triggerAt, pendingIntent)
                return
            }
        } catch (error: SecurityException) {
            // Android 12+ can deny exact alarms; keep the app alive with inexact fallback.
            Log.w(TAG, "Exact alarm scheduling denied; falling back to inexact alarm.", error)
        }
        scheduleInexact(triggerAt, pendingIntent)
    }

    override fun cancel() {
        try {
            alarmManager.cancel(firePendingIntent())
        } catch (error: SecurityException) {
            Log.w(TAG, "Canceling alarm failed.", error)
        }
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

    private fun scheduleExact(triggerAt: Long, pendingIntent: PendingIntent) {
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

    private fun scheduleInexact(triggerAt: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
            return
        }
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        return alarmManager.canScheduleExactAlarms()
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
        private const val TAG = "FeedAlertScheduler"
    }
}
