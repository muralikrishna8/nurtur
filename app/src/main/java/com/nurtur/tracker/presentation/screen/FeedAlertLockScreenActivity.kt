package com.nurtur.tracker.presentation.screen

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.nurtur.tracker.MainActivity
import com.nurtur.tracker.NurturApplication
import com.nurtur.tracker.R
import com.nurtur.tracker.domain.service.FeedAlertActions
import com.nurtur.tracker.domain.service.FeedAlertCoordinator
import com.nurtur.tracker.presentation.theme.NurturDimens
import com.nurtur.tracker.presentation.theme.NurturTheme
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class FeedAlertLockScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenDisplay()
        activeInstance = WeakReference(this)
        val alertBody = intent.getStringExtra(EXTRA_ALERT_BODY)
            ?: getString(R.string.feed_alert_body_unknown)
        setContent {
            NurturTheme {
                FeedAlertLockScreenContent(
                    alertBody = alertBody,
                    onStartFeed = { launchStartFeed() },
                    onSnooze = { runCoordinatorAction { it.handleSnooze() } },
                    onSkip = { runCoordinatorAction { it.handleSkip() } }
                )
            }
        }
    }

    override fun onDestroy() {
        if (activeInstance?.get() === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    private fun configureLockScreenDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private fun runCoordinatorAction(action: suspend (FeedAlertCoordinator) -> Unit) {
        val app = application as? NurturApplication ?: return
        lifecycleScope.launch {
            action(app.container.feedAlertCoordinator)
            finish()
        }
    }

    private fun launchStartFeed() {
        val startFeedIntent = Intent(this, MainActivity::class.java).apply {
            this.action = FeedAlertActions.ACTION_START_FEED
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isKeyguardLocked == true) {
            keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        startActivity(startFeedIntent)
                        finish()
                    }

                    override fun onDismissCancelled() {
                        finish()
                    }

                    override fun onDismissError() {
                        startActivity(startFeedIntent)
                        finish()
                    }
                }
            )
            return
        }
        startActivity(startFeedIntent)
        finish()
    }

    companion object {
        const val EXTRA_ALERT_BODY = "extra_alert_body"

        private var activeInstance: WeakReference<FeedAlertLockScreenActivity>? = null

        fun finishIfShowing() {
            val activity = activeInstance?.get() ?: return
            activity.runOnUiThread { activity.finish() }
        }
    }
}

@Composable
private fun FeedAlertLockScreenContent(
    alertBody: String,
    onStartFeed: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    var actionsEnabled by remember { mutableStateOf(true) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = NurturDimens.ScreenHorizontalPadding)
                .padding(vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.feed_alert_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = alertBody,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            LockScreenActionButton(
                label = stringResource(R.string.feed_alert_action_start_feed),
                enabled = actionsEnabled,
                onClick = {
                    actionsEnabled = false
                    onStartFeed()
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            LockScreenActionButton(
                label = stringResource(R.string.feed_alert_action_snooze),
                enabled = actionsEnabled,
                outlined = true,
                onClick = {
                    actionsEnabled = false
                    onSnooze()
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            LockScreenActionButton(
                label = stringResource(R.string.feed_alert_action_skip),
                enabled = actionsEnabled,
                outlined = true,
                onClick = {
                    actionsEnabled = false
                    onSkip()
                }
            )
        }
    }
}

@Composable
private fun LockScreenActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = NurturDimens.MinTouchTarget)
    if (outlined) {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            Text(text = label)
        }
        return
    }
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text = label)
    }
}
