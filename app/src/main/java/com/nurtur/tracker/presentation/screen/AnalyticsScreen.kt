package com.nurtur.tracker.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.AnalyticsInsights
import com.nurtur.tracker.domain.model.DailyAnalytics
import com.nurtur.tracker.presentation.theme.NurturDimens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private const val BAR_SECTION_WEIGHT_SCALE = 1f
private const val DAY_LABEL_LENGTH = 3
private const val AXIS_TICK_COUNT = 4
private const val AXIS_STEP_ML = 50
private const val QUICK_FILTER_LAST_7_DAYS = 7L
private const val QUICK_FILTER_LAST_14_DAYS = 14L
private const val QUICK_FILTER_LAST_30_DAYS = 30L
private const val COMPACT_WINDOW_MAX_DAYS = 7
private const val CHART_GRID_STROKE_WIDTH = 2f
private val CHART_DAY_SLOT_WIDTH: Dp = 56.dp
private val CHART_LABEL_AREA_HEIGHT: Dp = 36.dp
private val CHART_LABEL_AREA_HEIGHT_EXTENDED: Dp = 48.dp
private val CHART_FADE_EDGE_WIDTH: Dp = 18.dp
private val Y_AXIS_WIDTH: Dp = 44.dp
private val rangeLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    analytics: List<DailyAnalytics>,
    insights: AnalyticsInsights,
    selectedStartDate: LocalDate,
    selectedEndDate: LocalDate,
    selectedQuickFilterDays: Long,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onQuickFilterSelected: (Long) -> Unit
) {
    val chartData = analytics.asReversed()
    val isExtendedWindow = chartData.size > COMPACT_WINDOW_MAX_DAYS
    val chartScrollState = rememberScrollState()
    val chartMinWidth = CHART_DAY_SLOT_WIDTH * max(chartData.size, COMPACT_WINDOW_MAX_DAYS)
    val xAxisLabelAreaHeight = if (isExtendedWindow) CHART_LABEL_AREA_HEIGHT_EXTENDED else CHART_LABEL_AREA_HEIGHT
    val maxDayTotalMl = chartData.maxOfOrNull { it.consumedMl + it.wastedMl } ?: 0
    val yAxisMaxMl = computeAxisMax(maxDayTotalMl)
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val chartSurfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = chartSurfaceColor.luminance() < 0.5f
    val chartGridLineColor = if (isDarkSurface) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        gridLineColor.copy(alpha = 0.9f)
    }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var selectedIndex by remember(chartData) {
        mutableIntStateOf(chartData.indexOfFirst { it.feedCount > 0 }.coerceAtLeast(0))
    }
    val selectedDay = chartData.getOrNull(selectedIndex)
    val averageVolumeText = insights.averageVolumePerFeedMl?.let { "${it}ml" } ?: "--"
    val averageIntervalText = insights.averageTimeBetweenFeedsMillis?.let { formatDuration(it) } ?: "--"
    LaunchedEffect(chartData.size, isExtendedWindow) {
        if (!isExtendedWindow && chartScrollState.value != 0) {
            chartScrollState.scrollTo(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { CenterAlignedTopAppBar(title = { Text("Analytics") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = NurturDimens.ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickFilterChip(
                    label = "7D",
                    isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_7_DAYS,
                    onClick = { onQuickFilterSelected(QUICK_FILTER_LAST_7_DAYS) }
                )
                QuickFilterChip(
                    label = "14D",
                    isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_14_DAYS,
                    onClick = { onQuickFilterSelected(QUICK_FILTER_LAST_14_DAYS) }
                )
                QuickFilterChip(
                    label = "30D",
                    isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_30_DAYS,
                    onClick = { onQuickFilterSelected(QUICK_FILTER_LAST_30_DAYS) }
                )
                TextButton(onClick = { showDateRangePicker = true }) {
                    Text(
                        text = buildDateRangeButtonText(selectedStartDate, selectedEndDate, selectedQuickFilterDays),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(bottom = xAxisLabelAreaHeight),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        for (tick in AXIS_TICK_COUNT downTo 0) {
                            val valueMl = yAxisMaxMl * tick / AXIS_TICK_COUNT
                            Text(
                                text = "${valueMl}ml",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(Y_AXIS_WIDTH)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (isExtendedWindow) {
                                    Modifier.horizontalScroll(chartScrollState)
                                } else {
                                    Modifier
                                }
                            )
                            .drawWithContent {
                                drawContent()
                                if (!isExtendedWindow) {
                                    return@drawWithContent
                                }
                                val fadeWidthPx = CHART_FADE_EDGE_WIDTH.toPx()
                                val canScrollLeft = chartScrollState.value > 0
                                val canScrollRight = chartScrollState.value < chartScrollState.maxValue
                                if (canScrollLeft) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(chartSurfaceColor, Color.Transparent),
                                            startX = 0f,
                                            endX = fadeWidthPx
                                        ),
                                        topLeft = Offset.Zero,
                                        size = androidx.compose.ui.geometry.Size(fadeWidthPx, size.height)
                                    )
                                }
                                if (canScrollRight) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, chartSurfaceColor),
                                            startX = size.width - fadeWidthPx,
                                            endX = size.width
                                        ),
                                        topLeft = Offset(x = size.width - fadeWidthPx, y = 0f),
                                        size = androidx.compose.ui.geometry.Size(fadeWidthPx, size.height)
                                    )
                                }
                            }
                            .padding(start = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .then(
                                    if (isExtendedWindow) {
                                        Modifier.width(chartMinWidth)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }
                                )
                        ) {
                            val trendSeries = chartData.mapIndexedNotNull { index, _ ->
                                insights.smoothedTrendConsumedMlByDay.getOrNull(index)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp - xAxisLabelAreaHeight)
                                    .drawBehind {
                                        val dashPath = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                        val segmentHeight = size.height / AXIS_TICK_COUNT.toFloat()
                                        for (tick in 0..AXIS_TICK_COUNT) {
                                            val y = tick * segmentHeight
                                            drawLine(
                                                color = chartGridLineColor,
                                                start = Offset(0f, y),
                                                end = Offset(size.width, y),
                                                pathEffect = dashPath,
                                                strokeWidth = CHART_GRID_STROKE_WIDTH
                                            )
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    chartData.forEachIndexed { index, day ->
                                        val dayTotal = day.consumedMl + day.wastedMl
                                        val barHeightRatio = if (dayTotal == 0) 0f else dayTotal.toFloat() / yAxisMaxMl.toFloat()
                                        val consumedRatio = if (dayTotal == 0) 0f else day.consumedMl.toFloat() / dayTotal.toFloat()
                                        val wastedRatio = if (dayTotal == 0) 0f else day.wastedMl.toFloat() / dayTotal.toFloat()

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            if (barHeightRatio > 0f) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.7f)
                                                        .fillMaxHeight(barHeightRatio)
                                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                        .clickable { selectedIndex = index }
                                                ) {
                                                    if (wastedRatio > 0f) {
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(wastedRatio * BAR_SECTION_WEIGHT_SCALE)
                                                                .fillMaxWidth()
                                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                                        )
                                                    }
                                                    if (consumedRatio > 0f) {
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(consumedRatio * BAR_SECTION_WEIGHT_SCALE)
                                                                .fillMaxWidth()
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (trendSeries.size >= 3) {
                                    val trendColor: Color = MaterialTheme.colorScheme.tertiary
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawBehind {
                                                val chartHeightPx = size.height
                                                if (chartHeightPx <= 0f) {
                                                    return@drawBehind
                                                }
                                                val barsCount = chartData.size.coerceAtLeast(1)
                                                val spacingPx = 8.dp.toPx()
                                                val barWidthPx =
                                                    (size.width - spacingPx * (barsCount - 1)).coerceAtLeast(0f) / barsCount.toFloat()
                                                val path = Path()
                                                trendSeries.forEachIndexed { index, value ->
                                                    val x = (barWidthPx * index) + (spacingPx * index) + (barWidthPx / 2f)
                                                    val ratio = (value / yAxisMaxMl.toFloat()).coerceIn(0f, 1f)
                                                    val y = chartHeightPx - (ratio * chartHeightPx)
                                                    if (index == 0) {
                                                        path.moveTo(x, y)
                                                    } else {
                                                        path.lineTo(x, y)
                                                    }
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = trendColor,
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                                )
                                            }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(xAxisLabelAreaHeight),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                chartData.forEach { day ->
                                    Text(
                                        text = if (isExtendedWindow) day.dayLabel else day.dayLabel.take(DAY_LABEL_LENGTH),
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = if (isExtendedWindow) 2 else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                selectedDay?.let { day ->
                    Text(
                        text = "${day.dayLabel}: Consumed ${day.consumedMl}ml, Wasted ${day.wastedMl}ml",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Average Volume per Feed: $averageVolumeText",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Average Time Between Feeds: $averageIntervalText",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    }
    if (showDateRangePicker) {
        val zoneId = ZoneId.systemDefault()
        val todayMillisUtc = localDateToUtcStartMillis(LocalDate.now(zoneId))
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = localDateToUtcStartMillis(selectedStartDate),
            initialSelectedEndDateMillis = localDateToUtcStartMillis(selectedEndDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillisUtc
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {},
            dismissButton = {}
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    QuickFilterChip(
                        label = "Last 7 Days",
                        isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_7_DAYS,
                        onClick = {
                            onQuickFilterSelected(QUICK_FILTER_LAST_7_DAYS)
                            showDateRangePicker = false
                        }
                    )
                    QuickFilterChip(
                        label = "Last 14 Days",
                        isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_14_DAYS,
                        onClick = {
                            onQuickFilterSelected(QUICK_FILTER_LAST_14_DAYS)
                            showDateRangePicker = false
                        }
                    )
                    QuickFilterChip(
                        label = "Last 30 Days",
                        isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_30_DAYS,
                        onClick = {
                            onQuickFilterSelected(QUICK_FILTER_LAST_30_DAYS)
                            showDateRangePicker = false
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val startDate = pickerState.selectedStartDateMillis?.let { utcStartMillisToLocalDate(it) }
                            val endDate = pickerState.selectedEndDateMillis?.let { utcStartMillisToLocalDate(it) }
                            if (startDate != null && endDate != null && !endDate.isBefore(startDate)) {
                                onDateRangeSelected(startDate, endDate)
                                showDateRangePicker = false
                            }
                        },
                        enabled = pickerState.selectedStartDateMillis != null && pickerState.selectedEndDateMillis != null
                    ) {
                        Text("Apply")
                    }
                }
                DateRangePicker(
                    state = pickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors()
                )
            }
        }
    }
}

@Composable
private fun QuickFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) }
    )
}

private fun computeAxisMax(maxValueMl: Int): Int {
    if (maxValueMl <= 0) {
        return AXIS_STEP_ML
    }
    val roundedUp = ((maxValueMl + AXIS_STEP_ML - 1) / AXIS_STEP_ML) * AXIS_STEP_ML
    return roundedUp.coerceAtLeast(AXIS_STEP_ML)
}

private fun formatDuration(durationMillis: Long): String {
    if (durationMillis <= 0L) {
        return "0m"
    }
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    if (hours == 0L) {
        return "${minutes}m"
    }
    if (minutes == 0L) {
        return "${hours}h"
    }
    return String.format(Locale.US, "%dh %02dm", hours, minutes)
}

private fun buildDateRangeButtonText(
    selectedStartDate: LocalDate,
    selectedEndDate: LocalDate,
    selectedQuickFilterDays: Long
): String {
    return when (selectedQuickFilterDays) {
        QUICK_FILTER_LAST_7_DAYS -> "Last 7 Days"
        QUICK_FILTER_LAST_14_DAYS -> "Last 14 Days"
        QUICK_FILTER_LAST_30_DAYS -> "Last 30 Days"
        else -> "${rangeLabelFormatter.format(selectedStartDate)} - ${rangeLabelFormatter.format(selectedEndDate)}"
    }
}

private fun localDateToUtcStartMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}

private fun utcStartMillisToLocalDate(utcStartMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcStartMillis).atZone(ZoneId.of("UTC")).toLocalDate()
}
