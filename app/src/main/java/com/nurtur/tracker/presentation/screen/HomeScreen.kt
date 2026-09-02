package com.nurtur.tracker.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.service.FeedMetricsCalculator
import com.nurtur.tracker.domain.service.FeedTimerStatus
import com.nurtur.tracker.domain.service.FeedTimerStatusCalculator
import com.nurtur.tracker.domain.service.NextFeedAlertCalculator
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Edit
import com.nurtur.tracker.presentation.feed.FeedUiState
import com.nurtur.tracker.presentation.theme.NurturColorTokens
import com.nurtur.tracker.presentation.theme.NurturDimens
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val TIMER_REFRESH_MS = 60_000L
private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
private const val BREASTMILK_TYPE = "Breastmilk"
private val timeOnlyFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val dayTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

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
    onNotesChange: (String) -> Unit,
    onNextFeedAlertOverrideChange: (Long) -> Unit,
    onShowLogFeedDialog: () -> Unit,
    onDismissLogFeedDialog: () -> Unit
) {
    var isNextAlertEditorVisible by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val listState = rememberLazyListState()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val successColor =
        if (isDarkTheme) NurturColorTokens.DarkSuccess else NurturColorTokens.LightSuccess
    val warningColor =
        if (isDarkTheme) NurturColorTokens.DarkWarning else NurturColorTokens.LightWarning
    val nextAlertEpochMillis = remember(
        uiState.latestFeed?.endTime,
        uiState.settings.targetFeedIntervalMinutes,
        uiState.settings.nextFeedAlertOverrideEpochMillis
    ) {
        NextFeedAlertCalculator.resolve(
            lastFeedEndEpochMillis = uiState.latestFeed?.endTime,
            targetIntervalMinutes = uiState.settings.targetFeedIntervalMinutes,
            overrideEpochMillis = uiState.settings.nextFeedAlertOverrideEpochMillis
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(TIMER_REFRESH_MS)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nurtur",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Profile placeholder for visual parity */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onStartAddFeed()
                    onShowLogFeedDialog()
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                ),
                modifier = Modifier.semantics { contentDescription = "Log a new feed" }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = NurturDimens.ScreenHorizontalPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(NurturDimens.SectionSpacing)
        ) {
            item {
                HeroSection(
                    currentTime = currentTime,
                    latestFeed = uiState.latestFeed,
                    targetFeedIntervalMinutes = uiState.settings.targetFeedIntervalMinutes,
                    successColor = successColor,
                    warningColor = warningColor,
                    isDarkTheme = isDarkTheme
                )
            }
            if (nextAlertEpochMillis != null) {
                item {
                    NextAlertChip(
                        nextAlertEpochMillis = nextAlertEpochMillis,
                        onEditClick = { isNextAlertEditorVisible = true }
                    )
                }
            }
            item {
                SnapshotSection(
                    uiState = uiState,
                    warningColor = warningColor
                )
            }
            item {
                Text(
                    text = "Recent Feeds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.recentFeeds, key = { it.id }) { feed ->
                FeedRow(
                    feed = feed,
                    successColor = successColor,
                    warningColor = warningColor,
                    onClick = {
                        onEditFeed(feed)
                        onShowLogFeedDialog()
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }

    AnimatedVisibility(uiState.isLogFeedDialogVisible) {
        LogFeedDialog(
            uiState = uiState,
            onDismiss = { onDismissLogFeedDialog() },
            onSave = {
                val didSave = onSaveFeed()
                if (didSave) {
                    onDismissLogFeedDialog()
                }
            },
            onDelete = {
                val didDelete = onDeleteFromEditor()
                if (didDelete) {
                    onDismissLogFeedDialog()
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

    if (isNextAlertEditorVisible && nextAlertEpochMillis != null) {
        NextAlertTimePickerDialog(
            initialEpochMillis = nextAlertEpochMillis,
            minimumEpochMillis = uiState.latestFeed?.endTime ?: currentTime,
            onDismiss = { isNextAlertEditorVisible = false },
            onConfirm = { selectedEpochMillis ->
                onNextFeedAlertOverrideChange(selectedEpochMillis)
                isNextAlertEditorVisible = false
            }
        )
    }
}

@Composable
private fun HeroSection(
    currentTime: Long,
    latestFeed: FeedLog?,
    targetFeedIntervalMinutes: Int,
    successColor: Color,
    warningColor: Color,
    isDarkTheme: Boolean
) {
    val elapsed = latestFeed?.let {
        Duration.between(Instant.ofEpochMilli(it.endTime), Instant.ofEpochMilli(currentTime))
    }
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
    val badgeLabel = when (timerStatus) {
        FeedTimerStatus.SAFE -> "On track"
        FeedTimerStatus.APPROACHING -> "Approaching"
        FeedTimerStatus.OVERDUE -> "Overdue"
    }
    val badgeBackground = when (timerStatus) {
        FeedTimerStatus.SAFE -> successColor
        FeedTimerStatus.APPROACHING,
        FeedTimerStatus.OVERDUE -> warningColor
    }
    val badgeContent = when {
        timerStatus == FeedTimerStatus.SAFE && !isDarkTheme -> Color.White
        timerStatus == FeedTimerStatus.SAFE && isDarkTheme -> NurturColorTokens.DarkOnPrimary
        else -> NurturColorTokens.DarkOnPrimary
    }
    val helperText = when (timerStatus) {
        FeedTimerStatus.SAFE -> null
        FeedTimerStatus.APPROACHING,
        FeedTimerStatus.OVERDUE -> "Gentle warning"
    }
    val lastFeedSubtext = latestFeed?.let {
        "Last feed was at ${formatRelativeFeedTime(it.endTime)}"
    } ?: "No feed logged yet"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "TIME SINCE LAST FEED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
                StatusBadge(
                    label = badgeLabel,
                    background = badgeBackground,
                    contentColor = badgeContent
                )
            }
            Text(
                text = elapsedText,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            Text(
                text = lastFeedSubtext,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            helperText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = warningColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    background: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SnapshotSection(
    uiState: FeedUiState,
    warningColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Daily Snapshot",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SnapshotMetricCard(
                label = "Consumed",
                value = "${uiState.todayConsumedMl} ml",
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            SnapshotMetricCard(
                label = "Wasted",
                value = "${uiState.todayWastedMl} ml",
                valueColor = warningColor,
                modifier = Modifier.weight(1f)
            )
            SnapshotMetricCard(
                label = "Feeds",
                value = "${uiState.todayFeedCount} count",
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SnapshotMetricCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(NurturDimens.CardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeedRow(
    feed: FeedLog,
    successColor: Color,
    warningColor: Color,
    onClick: () -> Unit
) {
    val wasted = FeedMetricsCalculator.calculateWasteMl(feed.amountOffered, feed.amountConsumed)
    val isBreastmilk = feed.milkType.equals(BREASTMILK_TYPE, ignoreCase = true)
    val milkLabel = if (isBreastmilk) "Breast Milk" else "Formula Milk"
    val feedIcon: ImageVector =
        if (isBreastmilk) Icons.Default.AutoAwesome else Icons.Default.LocalDrink
    val statusText = if (wasted == 0) "Clean feed" else "$wasted ml wasted"
    val statusColor = if (wasted == 0) successColor else warningColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(NurturDimens.CardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feedIcon,
                    contentDescription = milkLabel,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = milkLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatRelativeFeedTime(feed.endTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${feed.amountConsumed} ml",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (wasted > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(warningColor)
                        )
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatRelativeFeedTime(epochMillis: Long): String {
    val zoneId = ZoneId.systemDefault()
    val feedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    val feedDate = feedDateTime.toLocalDate()
    val today = LocalDate.now(zoneId)
    val timePart = timeOnlyFormatter.format(feedDateTime)
    return when (feedDate) {
        today -> "Today, $timePart"
        today.minusDays(1) -> "Yesterday, $timePart"
        else -> dayTimeFormatter.format(feedDateTime)
    }
}


@Composable
private fun NextAlertChip(
    nextAlertEpochMillis: Long,
    onEditClick: () -> Unit
) {
    val formattedTime = timeOnlyFormatter.format(
        Instant.ofEpochMilli(nextAlertEpochMillis).atZone(ZoneId.systemDefault())
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NurturDimens.MinTouchTarget)
            .semantics {
                contentDescription = "Next feed alert set for $formattedTime"
                liveRegion = LiveRegionMode.Polite
            },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Next alert: $formattedTime",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "EDIT",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextAlertTimePickerDialog(
    initialEpochMillis: Long,
    minimumEpochMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val zoneId = ZoneId.systemDefault()
    val initialDateTime = remember(initialEpochMillis) {
        Instant.ofEpochMilli(initialEpochMillis).atZone(zoneId)
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Next feed alert") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        resolveOverrideEpochMillis(
                            selectedHour = timePickerState.hour,
                            selectedMinute = timePickerState.minute,
                            baseEpochMillis = initialEpochMillis,
                            minimumEpochMillis = minimumEpochMillis,
                            zoneId = zoneId
                        )
                    )
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

internal fun resolveOverrideEpochMillis(
    selectedHour: Int,
    selectedMinute: Int,
    baseEpochMillis: Long,
    minimumEpochMillis: Long,
    zoneId: ZoneId
): Long {
    val baseDate = Instant.ofEpochMilli(baseEpochMillis).atZone(zoneId).toLocalDate()
    var candidate = baseDate
        .atTime(selectedHour, selectedMinute)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
    while (candidate <= minimumEpochMillis) {
        candidate += MILLIS_PER_DAY
    }
    return candidate
}

