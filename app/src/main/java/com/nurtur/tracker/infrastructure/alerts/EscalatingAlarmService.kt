package com.nurtur.tracker.infrastructure.alerts

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.nurtur.tracker.R
import com.nurtur.tracker.domain.service.EscalationVolumePolicy

class EscalatingAlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var startedAtEpochMillis: Long = 0L
    private var isRunning = false

    private val escalateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) {
                return
            }
            val elapsed = System.currentTimeMillis() - startedAtEpochMillis
            val volume = EscalationVolumePolicy.volumeFraction(elapsed)
            mediaPlayer?.setVolume(volume, volume)
            if (elapsed >= EscalationVolumePolicy.ESCALATION_DURATION_MILLIS) {
                return
            }
            handler.postDelayed(this, VOLUME_TICK_MILLIS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FeedAlertNotifier.ensureChannels(this)
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())
        if (isRunning) {
            return START_NOT_STICKY
        }
        isRunning = true
        startedAtEpochMillis = System.currentTimeMillis()
        startSubtleVibration()
        startEscalatingChime()
        handler.post(escalateRunnable)
        handler.postDelayed(
            { stopSelfSafely() },
            EscalationVolumePolicy.ESCALATION_DURATION_MILLIS
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releasePlayback()
        super.onDestroy()
    }

    private fun startSubtleVibration() {
        val vibrator = resolveVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(INITIAL_VIBRATE_PATTERN, -1))
            return
        }
        @Suppress("DEPRECATION")
        vibrator.vibrate(INITIAL_VIBRATE_PATTERN, -1)
    }

    private fun startEscalatingChime() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(applicationContext, uri)
            player.isLooping = true
            val startVolume = EscalationVolumePolicy.MINIMUM_VOLUME_FRACTION
            player.setVolume(startVolume, startVolume)
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (_: Exception) {
            player.release()
            mediaPlayer = null
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FeedAlertNotifier.CHANNEL_ID_ESCALATING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.feed_alert_title))
            .setContentText(getString(R.string.feed_alert_escalating_status))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun resolveVibrator(): Vibrator? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java) ?: return null
            return manager.defaultVibrator
        }
        @Suppress("DEPRECATION")
        return getSystemService(VIBRATOR_SERVICE) as? Vibrator
    }

    private fun stopSelfSafely() {
        releasePlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayback() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.run {
            stop()
            release()
        }
        mediaPlayer = null
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 41002
        private const val VOLUME_TICK_MILLIS = 2_000L
        private val INITIAL_VIBRATE_PATTERN = longArrayOf(0L, 250L, 150L, 250L)
    }
}
