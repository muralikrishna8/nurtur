package com.nurtur.tracker.infrastructure.alerts

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
import com.nurtur.tracker.domain.service.FeedAlertActions

object FeedAlertNotifier {
    const val CHANNEL_ID_ESCALATING = "feed_alerts_escalating"
    const val CHANNEL_ID_QUIET = "feed_alerts_quiet"
    const val NOTIFICATION_ID = 41001

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val escalating = NotificationChannel(
            CHANNEL_ID_ESCALATING,
            context.getString(R.string.feed_alert_channel_escalating_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.feed_alert_channel_escalating_description)
            enableVibration(true)
            setSound(null, null)
        }
        val quiet = NotificationChannel(
            CHANNEL_ID_QUIET,
            context.getString(R.string.feed_alert_channel_quiet_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.feed_alert_channel_quiet_description)
            enableVibration(true)
            setSound(null, null)
        }
        manager.createNotificationChannel(escalating)
        manager.createNotificationChannel(quiet)
    }

    fun showFeedAlert(context: Context, deliveryMode: AlertDeliveryMode) {
        ensureChannels(context)
        val channelId = if (deliveryMode == AlertDeliveryMode.VIBRATE_ONLY) {
            CHANNEL_ID_QUIET
        } else {
            CHANNEL_ID_ESCALATING
        }
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
            .setContentText(context.getString(R.string.feed_alert_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.feed_alert_action_start_feed), startFeedIntent)
            .addAction(0, context.getString(R.string.feed_alert_action_snooze), snoozeIntent)
            .addAction(0, context.getString(R.string.feed_alert_action_skip), skipIntent)
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

    private const val REQUEST_CONTENT = 501
    private const val REQUEST_START_FEED = 502
    private const val REQUEST_SNOOZE = 503
    private const val REQUEST_SKIP = 504
    private val QUIET_VIBRATE_PATTERN = longArrayOf(0L, 200L, 120L, 200L)
    private val ESCALATING_VIBRATE_PATTERN = longArrayOf(0L, 350L, 180L, 350L)
}
