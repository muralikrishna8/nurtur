package com.nurtur.tracker.infrastructure.alerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nurtur.tracker.MainActivity
import com.nurtur.tracker.R
import com.nurtur.tracker.domain.service.AlertDeliveryMode
import com.nurtur.tracker.domain.service.ElapsedSinceLastFeedCalculator
import com.nurtur.tracker.domain.service.FeedAlertActions
import com.nurtur.tracker.domain.service.FeedAlertNotification
import com.nurtur.tracker.domain.service.FullScreenAlertCapability
import com.nurtur.tracker.presentation.screen.FeedAlertLockScreenActivity
import com.nurtur.tracker.util.DurationLabelFormatter

object FeedAlertNotifier {
    const val CHANNEL_ID_ESCALATING = "feed_alerts_escalating"
    const val CHANNEL_ID_QUIET = "feed_alerts_quiet_wakeup"
    const val NOTIFICATION_ID = 41001
    private const val LEGACY_CHANNEL_ID_QUIET = "feed_alerts_quiet"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID_QUIET)
        val escalating = NotificationChannel(
            CHANNEL_ID_ESCALATING,
            context.getString(R.string.feed_alert_channel_escalating_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.feed_alert_channel_escalating_description)
            enableVibration(true)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val quiet = NotificationChannel(
            CHANNEL_ID_QUIET,
            context.getString(R.string.feed_alert_channel_quiet_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.feed_alert_channel_quiet_description)
            enableVibration(true)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(escalating)
        manager.createNotificationChannel(quiet)
    }

    fun showFeedAlert(context: Context, notification: FeedAlertNotification) {
        ensureChannels(context)
        val deliveryMode = notification.deliveryMode
        val channelId = if (deliveryMode == AlertDeliveryMode.VIBRATE_ONLY) {
            CHANNEL_ID_QUIET
        } else {
            CHANNEL_ID_ESCALATING
        }
        val alertBody = alertBody(context, notification.lastFeedEndEpochMillis)
        val contentIntent = activityPendingIntent(context, REQUEST_CONTENT, action = null)
        val startFeedIntent = activityPendingIntent(
            context,
            REQUEST_START_FEED,
            FeedAlertActions.ACTION_START_FEED
        )
        val snoozeIntent = broadcastPendingIntent(
            context,
            REQUEST_SNOOZE,
            FeedAlertActions.ACTION_SNOOZE
        )
        val skipIntent = broadcastPendingIntent(
            context,
            REQUEST_SKIP,
            FeedAlertActions.ACTION_SKIP
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.feed_alert_title))
            .setContentText(alertBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(lockScreenUiAction(context.getString(R.string.feed_alert_action_start_feed), startFeedIntent))
            .addAction(backgroundLockScreenAction(context.getString(R.string.feed_alert_action_snooze), snoozeIntent))
            .addAction(backgroundLockScreenAction(context.getString(R.string.feed_alert_action_skip), skipIntent))
        attachFullScreenIntentIfAllowed(context, builder, alertBody)
        if (deliveryMode == AlertDeliveryMode.VIBRATE_ONLY) {
            builder.setVibrate(QUIET_VIBRATE_PATTERN)
            builder.setSilent(true)
        } else {
            builder.setVibrate(ESCALATING_VIBRATE_PATTERN)
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        FeedAlertLockScreenActivity.finishIfShowing()
    }

    private fun attachFullScreenIntentIfAllowed(
        context: Context,
        builder: NotificationCompat.Builder,
        alertBody: String
    ) {
        val canUseFullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
        } else {
            true
        }
        if (!FullScreenAlertCapability.shouldUseFullScreenIntent(
                sdkInt = Build.VERSION.SDK_INT,
                canUseFullScreenIntent = canUseFullScreenIntent
            )
        ) {
            return
        }
        val fullScreenIntent = Intent(context, FeedAlertLockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(FeedAlertLockScreenActivity.EXTRA_ALERT_BODY, alertBody)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_FULL_SCREEN,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setFullScreenIntent(pendingIntent, true)
    }

    private fun lockScreenUiAction(label: String, pendingIntent: PendingIntent): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(0, label, pendingIntent).build()
    }

    private fun backgroundLockScreenAction(
        label: String,
        pendingIntent: PendingIntent
    ): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(0, label, pendingIntent)
            .setShowsUserInterface(false)
            .build()
    }

    private fun activityPendingIntent(
        context: Context,
        requestCode: Int,
        action: String?
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            if (action != null) {
                this.action = action
            }
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun broadcastPendingIntent(
        context: Context,
        requestCode: Int,
        action: String
    ): PendingIntent {
        val intent = Intent(context, FeedAlertReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alertBody(context: Context, lastFeedEndEpochMillis: Long?): String {
        val elapsed = ElapsedSinceLastFeedCalculator.calculate(
            lastFeedEndEpochMillis = lastFeedEndEpochMillis,
            nowEpochMillis = System.currentTimeMillis()
        )
        if (elapsed == null) {
            return context.getString(R.string.feed_alert_body_unknown)
        }
        val durationLabel = DurationLabelFormatter.format(context, elapsed.hours, elapsed.minutes)
        return context.getString(R.string.feed_alert_body, durationLabel)
    }

    private const val REQUEST_CONTENT = 501
    private const val REQUEST_START_FEED = 502
    private const val REQUEST_SNOOZE = 503
    private const val REQUEST_SKIP = 504
    private const val REQUEST_FULL_SCREEN = 505
    private val QUIET_VIBRATE_PATTERN = longArrayOf(0L, 200L, 120L, 200L)
    private val ESCALATING_VIBRATE_PATTERN = longArrayOf(0L, 350L, 180L, 350L)
}
