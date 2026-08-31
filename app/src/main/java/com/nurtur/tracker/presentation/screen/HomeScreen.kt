package com.nurtur.tracker.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.service.FeedTimerStatus
import com.nurtur.tracker.domain.service.FeedTimerStatusCalculator
import com.nurtur.tracker.domain.service.FeedMetricsCalculator
import com.nurtur.tracker.presentation.feed.FeedUiState
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val TIMER_REFRESH_MS = 60_000L
private val feedTimerApproachingColor = Color(0xFF9C6A0C)
private val feedTimerOverdueColor = Color(0xFFB3261E)
private val timestampFormatter = DateTimeFormatter.ofPattern("MMM d, hh:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: FeedUiState,
    onStartAddFeed: () -> Unit,
    onSaveFeed: () -> Boolean,
    onDeleteFromEditor: () -> Boolean,
    onEditFeed: (FeedLog) -> Unit,
    onStartTimeChange: (Long) -> Unit,
    onEndTimeChange: (Long) -> Unit,
    onAmountOfferedChange: (String) -> Unit,
    onAmountConsumedChange: (String) -> Unit,
    onMilkTypeChange: (String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    var isDialogVisible by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(TIMER_REFRESH_MS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                onStartAddFeed()
                isDialogVisible = true
            }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroSection(
                currentTime = currentTime,
                latestFeed = uiState.latestFeed,
                targetFeedIntervalMinutes = uiState.settings.targetFeedIntervalMinutes
            )
            SnapshotSection(uiState = uiState)
            Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
            key(uiState.recentFeeds.firstOrNull()?.id, uiState.recentFeeds.size) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.recentFeeds, key = { it.id }) { feed ->
                        FeedRow(
                            feed = feed,
                            onClick = {
                                onEditFeed(feed)
                                isDialogVisible = true
                            }
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(isDialogVisible) {
        LogFeedDialog(
            uiState = uiState,
            onDismiss = { isDialogVisible = false },
            onSave = {
                val didSave = onSaveFeed()
                if (didSave) {
                    isDialogVisible = false
                }
            },
            onDelete = {
                val didDelete = onDeleteFromEditor()
                if (didDelete) {
                    isDialogVisible = false
                }
            },
            onStartTimeChange = onStartTimeChange,
            onEndTimeChange = onEndTimeChange,
            onAmountOfferedChange = onAmountOfferedChange,
            onAmountConsumedChange = onAmountConsumedChange,
            onMilkTypeChange = onMilkTypeChange,
            onNotesChange = onNotesChange
        )
    }
}

@Composable
private fun HeroSection(
    currentTime: Long,
    latestFeed: FeedLog?,
    targetFeedIntervalMinutes: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Time since last feed", style = MaterialTheme.typography.titleMedium)
            val elapsed = latestFeed?.let { Duration.between(Instant.ofEpochMilli(it.endTime), Instant.ofEpochMilli(currentTime)) }
            val elapsedText = if (elapsed == null || elapsed.isNegative) {
                "--"
            } else {
                "${elapsed.toHours()}h ${elapsed.toMinutesPart()}m"
            }
            val timerStatus = elapsed?.let {
                FeedTimerStatusCalculator.calculate(
                    elapsed = it,
                    targetInterval = Duration.ofMinutes(targetFeedIntervalMinutes.toLong())
                )
            } ?: FeedTimerStatus.SAFE
            val timerColor = when (timerStatus) {
                FeedTimerStatus.SAFE -> MaterialTheme.colorScheme.onSurface
                FeedTimerStatus.APPROACHING -> feedTimerApproachingColor
                FeedTimerStatus.OVERDUE -> feedTimerOverdueColor
            }
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = timerColor
            )
            val lastFeedTime = latestFeed?.let {
                timestampFormatter.format(Instant.ofEpochMilli(it.endTime).atZone(ZoneId.systemDefault()))
            } ?: "No feed yet"
            Text("Last feed: $lastFeedTime", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SnapshotSection(uiState: FeedUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Today's Milk", style = MaterialTheme.typography.titleSmall)
                Text("${uiState.todayConsumedMl} ml consumed")
                Text("${uiState.todayWastedMl} ml wasted")
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Feed Count", style = MaterialTheme.typography.titleSmall)
                Text("${uiState.todayFeedCount} feeds")
            }
        }
    }
}

@Composable
private fun FeedRow(feed: FeedLog, onClick: () -> Unit) {
    val endTimeText = timestampFormatter.format(Instant.ofEpochMilli(feed.endTime).atZone(ZoneId.systemDefault()))
    val wasted = FeedMetricsCalculator.calculateWasteMl(feed.amountOffered, feed.amountConsumed)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(endTimeText, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocalDrink, contentDescription = null)
                    Text(feed.milkType)
                }
            }
            Column {
                Text("${feed.amountConsumed} ml consumed")
                Text("$wasted ml wasted")
            }
        }
    }
}
