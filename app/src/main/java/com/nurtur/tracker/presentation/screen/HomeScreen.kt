package com.nurtur.tracker.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.service.FeedMetricsCalculator
import com.nurtur.tracker.domain.service.FeedTimerStatus
import com.nurtur.tracker.domain.service.FeedTimerStatusCalculator
import com.nurtur.tracker.presentation.feed.FeedUiState
import com.nurtur.tracker.presentation.theme.NurturColorTokens
import com.nurtur.tracker.presentation.theme.NurturDimens
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val TIMER_REFRESH_MS = 60_000L
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
    val listState = rememberLazyListState()
    val isFabExpanded = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 20

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(TIMER_REFRESH_MS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CenterAlignedTopAppBar(title = { Text("Nurtur") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = isFabExpanded,
                onClick = {
                    onStartAddFeed()
                    isDialogVisible = true
                },
                text = { Text("Log Feed") },
                icon = { Text("+") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = NurturDimens.ScreenHorizontalPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(NurturDimens.SectionSpacing)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                HeroSection(
                    currentTime = currentTime,
                    latestFeed = uiState.latestFeed,
                    targetFeedIntervalMinutes = uiState.settings.targetFeedIntervalMinutes
                )
            }
            item { SnapshotSection(uiState = uiState) }
            item { Text("Recent Feeds", style = MaterialTheme.typography.titleMedium) }
            items(uiState.recentFeeds, key = { it.id }) { feed ->
                FeedRow(
                    feed = feed,
                    onClick = {
                        onEditFeed(feed)
                        isDialogVisible = true
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(84.dp)) }
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Time since last feed", style = MaterialTheme.typography.titleMedium)
            val elapsed = latestFeed?.let { Duration.between(Instant.ofEpochMilli(it.endTime), Instant.ofEpochMilli(currentTime)) }
            val elapsedText = if (elapsed == null || elapsed.isNegative) "--" else "${elapsed.toHours()}h ${elapsed.toMinutesPart()}m"
            val timerStatus = elapsed?.let {
                FeedTimerStatusCalculator.calculate(
                    elapsed = it,
                    targetInterval = Duration.ofMinutes(targetFeedIntervalMinutes.toLong())
                )
            } ?: FeedTimerStatus.SAFE
            val timerColor = when (timerStatus) {
                FeedTimerStatus.SAFE -> MaterialTheme.colorScheme.onSurface
                FeedTimerStatus.APPROACHING -> NurturColorTokens.LightWarning
                FeedTimerStatus.OVERDUE -> if (MaterialTheme.colorScheme.background == NurturColorTokens.DarkBackground) {
                    NurturColorTokens.DarkWarning
                } else {
                    NurturColorTokens.LightWarning
                }
            }
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.displayLarge,
                color = timerColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
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
    val totalOffered = uiState.todayConsumedMl + uiState.todayWastedMl
    val consumedProgress = if (totalOffered <= 0) 0f else uiState.todayConsumedMl.toFloat() / totalOffered.toFloat()
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Daily Snapshot", style = MaterialTheme.typography.titleMedium)
                Text("Consumed: ${uiState.todayConsumedMl}ml", style = MaterialTheme.typography.bodyLarge)
                Text("Wasted milk: ${uiState.todayWastedMl}ml", style = MaterialTheme.typography.bodyLarge)
                Text("Feeds: ${uiState.todayFeedCount}", style = MaterialTheme.typography.bodyMedium)
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { consumedProgress },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = NurturColorTokens.LightSuccess,
                    modifier = Modifier
                        .padding(8.dp)
                        .sizeOrFallback()
                )
                Text("${(consumedProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FeedRow(feed: FeedLog, onClick: () -> Unit) {
    val endTimeText = timestampFormatter.format(Instant.ofEpochMilli(feed.endTime).atZone(ZoneId.systemDefault()))
    val wasted = FeedMetricsCalculator.calculateWasteMl(feed.amountOffered, feed.amountConsumed)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NurturDimens.MinTouchTarget + 24.dp)
            .clickable { onClick() }
    ) {
        ListItem(
            headlineContent = { Text("$endTimeText - ${feed.amountConsumed}ml") },
            supportingContent = { Text("$wasted ml wasted") },
            leadingContent = { Icon(Icons.Default.LocalDrink, contentDescription = null) },
            trailingContent = { Text(feed.milkType, style = MaterialTheme.typography.bodyMedium) }
        )
    }
}

private fun Modifier.sizeOrFallback(): Modifier = this.heightIn(min = 64.dp)
