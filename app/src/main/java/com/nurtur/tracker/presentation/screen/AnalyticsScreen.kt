package com.nurtur.tracker.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.AnalyticsInsights
import com.nurtur.tracker.domain.model.DailyAnalytics
import com.nurtur.tracker.presentation.theme.NurturColorTokens
import com.nurtur.tracker.presentation.theme.NurturDimens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
private const val ML_PER_LITER = 1000.0
private val CHART_DAY_SLOT_WIDTH: Dp = 56.dp
private val CHART_LABEL_AREA_HEIGHT: Dp = 36.dp
private val CHART_LABEL_AREA_HEIGHT_EXTENDED: Dp = 48.dp
private val CHART_FADE_EDGE_WIDTH: Dp = 18.dp
private val Y_AXIS_WIDTH: Dp = 44.dp
private val QUICK_FILTER_CORNER_RADIUS = 20.dp
private val LEGEND_SWATCH_SIZE = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
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
    val contentScrollState = rememberScrollState()
    val chartMinWidth = CHART_DAY_SLOT_WIDTH * max(chartData.size, COMPACT_WINDOW_MAX_DAYS)
    val xAxisLabelAreaHeight = if (isExtendedWindow) CHART_LABEL_AREA_HEIGHT_EXTENDED else CHART_LABEL_AREA_HEIGHT
    val maxDayTotalMl = chartData.maxOfOrNull { it.consumedMl + it.wastedMl } ?: 0
    val yAxisMaxMl = computeAxisMax(maxDayTotalMl)
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val warningColor = if (isDarkTheme) NurturColorTokens.DarkWarning else NurturColorTokens.LightWarning
    val consumedBarColor = MaterialTheme.colorScheme.primary
    val wastedBarColor = warningColor
    val chartSurfaceColor = MaterialTheme.colorScheme.surface
    val chartGridLineColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
    }
    val trendLineColor = MaterialTheme.colorScheme.secondary
    val periodStats = remember(analytics) { computePeriodStats(analytics) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var selectedIndex by remember(chartData) {
        mutableIntStateOf(chartData.indexOfFirst { it.feedCount > 0 }.coerceAtLeast(0))
    }
    val selectedDay = chartData.getOrNull(selectedIndex)

    LaunchedEffect(chartData.size, isExtendedWindow) {
        if (!isExtendedWindow && chartScrollState.value != 0) {
            chartScrollState.scrollTo(0)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showDateRangePicker = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Choose custom date range"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = NurturDimens.ScreenHorizontalPadding)
                .verticalScroll(contentScrollState),
            verticalArrangement = Arrangement.spacedBy(NurturDimens.SectionSpacing)
        ) {
            QuickFilterRow(
                selectedQuickFilterDays = selectedQuickFilterDays,
                onQuickFilterSelected = onQuickFilterSelected
            )

            ChartCard(
                chartData = chartData,
                insights = insights,
                isExtendedWindow = isExtendedWindow,
                chartMinWidth = chartMinWidth,
                xAxisLabelAreaHeight = xAxisLabelAreaHeight,
                yAxisMaxMl = yAxisMaxMl,
                chartScrollState = chartScrollState,
                chartSurfaceColor = chartSurfaceColor,
                chartGridLineColor = chartGridLineColor,
                consumedBarColor = consumedBarColor,
                wastedBarColor = wastedBarColor,
                trendLineColor = trendLineColor,
                totalVolumeLabel = formatTotalVolumeLabel(periodStats.totalVolumeMl),
                selectedDay = selectedDay,
                onBarSelected = { selectedIndex = it }
            )

            PeriodStatsGrid(
                stats = periodStats,
                warningColor = warningColor
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDateRangePicker) {
        AnalyticsDateRangePickerDialog(
            selectedStartDate = selectedStartDate,
            selectedEndDate = selectedEndDate,
            selectedQuickFilterDays = selectedQuickFilterDays,
            onDismiss = { showDateRangePicker = false },
            onDateRangeSelected = onDateRangeSelected,
            onQuickFilterSelected = onQuickFilterSelected
        )
    }
}

@Composable
private fun QuickFilterRow(
    selectedQuickFilterDays: Long,
    onQuickFilterSelected: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsQuickFilterPill(
            label = "7D",
            isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_7_DAYS,
            onClick = { onQuickFilterSelected(QUICK_FILTER_LAST_7_DAYS) },
            modifier = Modifier.weight(1f)
        )
        AnalyticsQuickFilterPill(
            label = "14D",
            isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_14_DAYS,
            onClick = { onQuickFilterSelected(QUICK_FILTER_LAST_14_DAYS) },
            modifier = Modifier.weight(1f)
        )
        AnalyticsQuickFilterPill(
            label = "30D",
            isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_30_DAYS,
            onClick = { onQuickFilterSelected(QUICK_FILTER_LAST_30_DAYS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AnalyticsQuickFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(QUICK_FILTER_CORNER_RADIUS)
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (isSelected) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .heightIn(min = NurturDimens.MinTouchTarget)
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Filter $label" },
        shape = shape,
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ChartCard(
    chartData: List<DailyAnalytics>,
    insights: AnalyticsInsights,
    isExtendedWindow: Boolean,
    chartMinWidth: Dp,
    xAxisLabelAreaHeight: Dp,
    yAxisMaxMl: Int,
    chartScrollState: androidx.compose.foundation.ScrollState,
    chartSurfaceColor: Color,
    chartGridLineColor: Color,
    consumedBarColor: Color,
    wastedBarColor: Color,
    trendLineColor: Color,
    totalVolumeLabel: String,
    selectedDay: DailyAnalytics?,
    onBarSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(NurturDimens.CardCornerRadius),
        color = chartSurfaceColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Consumed vs. Wasted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = totalVolumeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    size = androidx.compose.ui.geometry.Size(
                                        fadeWidthPx,
                                        size.height
                                    )
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
                                    size = androidx.compose.ui.geometry.Size(
                                        fadeWidthPx,
                                        size.height
                                    )
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
                                    val dashPath =
                                        PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
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
                                    val barHeightRatio =
                                        if (dayTotal == 0) 0f else dayTotal.toFloat() / yAxisMaxMl.toFloat()
                                    val consumedRatio =
                                        if (dayTotal == 0) 0f else day.consumedMl.toFloat() / dayTotal.toFloat()
                                    val wastedRatio =
                                        if (dayTotal == 0) 0f else day.wastedMl.toFloat() / dayTotal.toFloat()

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
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStart = 8.dp,
                                                            topEnd = 8.dp
                                                        )
                                                    )
                                                    .clickable { onBarSelected(index) }
                                            ) {
                                                if (wastedRatio > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(wastedRatio * BAR_SECTION_WEIGHT_SCALE)
                                                            .fillMaxWidth()
                                                            .background(wastedBarColor)
                                                    )
                                                }
                                                if (consumedRatio > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(consumedRatio * BAR_SECTION_WEIGHT_SCALE)
                                                            .fillMaxWidth()
                                                            .background(consumedBarColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (trendSeries.size >= 3) {
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
                                                (size.width - spacingPx * (barsCount - 1))
                                                    .coerceAtLeast(0f) / barsCount.toFloat()
                                            val path = Path()
                                            trendSeries.forEachIndexed { index, value ->
                                                val x =
                                                    (barWidthPx * index) + (spacingPx * index) + (barWidthPx / 2f)
                                                val ratio =
                                                    (value / yAxisMaxMl.toFloat()).coerceIn(0f, 1f)
                                                val y = chartHeightPx - (ratio * chartHeightPx)
                                                if (index == 0) {
                                                    path.moveTo(x, y)
                                                } else {
                                                    path.lineTo(x, y)
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = trendLineColor,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = 4f
                                                )
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
                                    text = if (isExtendedWindow) {
                                        day.dayLabel
                                    } else {
                                        day.dayLabel.take(DAY_LABEL_LENGTH)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            ChartLegend(
                consumedColor = consumedBarColor,
                wastedColor = wastedBarColor
            )

            selectedDay?.let { day ->
                Text(
                    text = "${day.dayLabel}: Consumed ${day.consumedMl}ml, Wasted ${day.wastedMl}ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ChartLegend(
    consumedColor: Color,
    wastedColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = consumedColor, label = "Consumed")
        LegendItem(color = wastedColor, label = "Wasted")
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(LEGEND_SWATCH_SIZE)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PeriodStatsGrid(
    stats: AnalyticsPeriodStats,
    warningColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PeriodStatCard(
                label = "Avg Consumed",
                value = formatMlPerDay(stats.avgConsumedMlPerDay),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            PeriodStatCard(
                label = "Avg Wasted",
                value = formatMlPerDay(stats.avgWastedMlPerDay),
                valueColor = warningColor,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PeriodStatCard(
                label = "Total Feeds",
                value = stats.totalFeeds.toString(),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            PeriodStatCard(
                label = "Avg Feeds",
                value = formatFeedsPerDay(stats.avgFeedsPerDay),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PeriodStatCard(
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
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsDateRangePickerDialog(
    selectedStartDate: LocalDate,
    selectedEndDate: LocalDate,
    selectedQuickFilterDays: Long,
    onDismiss: () -> Unit,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onQuickFilterSelected: (Long) -> Unit
) {
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
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {}
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnalyticsQuickFilterPill(
                    label = "7D",
                    isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_7_DAYS,
                    onClick = {
                        onQuickFilterSelected(QUICK_FILTER_LAST_7_DAYS)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                AnalyticsQuickFilterPill(
                    label = "14D",
                    isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_14_DAYS,
                    onClick = {
                        onQuickFilterSelected(QUICK_FILTER_LAST_14_DAYS)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                AnalyticsQuickFilterPill(
                    label = "30D",
                    isSelected = selectedQuickFilterDays == QUICK_FILTER_LAST_30_DAYS,
                    onClick = {
                        onQuickFilterSelected(QUICK_FILTER_LAST_30_DAYS)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        val startDate = pickerState.selectedStartDateMillis?.let { utcStartMillisToLocalDate(it) }
                        val endDate = pickerState.selectedEndDateMillis?.let { utcStartMillisToLocalDate(it) }
                        if (startDate != null && endDate != null && !endDate.isBefore(startDate)) {
                            onDateRangeSelected(startDate, endDate)
                            onDismiss()
                        }
                    },
                    enabled = pickerState.selectedStartDateMillis != null &&
                        pickerState.selectedEndDateMillis != null
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

internal data class AnalyticsPeriodStats(
    val avgConsumedMlPerDay: Int,
    val avgWastedMlPerDay: Int,
    val totalFeeds: Int,
    val avgFeedsPerDay: Float,
    val totalVolumeMl: Int
)

internal fun computePeriodStats(analytics: List<DailyAnalytics>): AnalyticsPeriodStats {
    if (analytics.isEmpty()) {
        return AnalyticsPeriodStats(
            avgConsumedMlPerDay = 0,
            avgWastedMlPerDay = 0,
            totalFeeds = 0,
            avgFeedsPerDay = 0f,
            totalVolumeMl = 0
        )
    }
    val dayCount = analytics.size
    val totalConsumed = analytics.sumOf { it.consumedMl.coerceAtLeast(0) }
    val totalWasted = analytics.sumOf { it.wastedMl.coerceAtLeast(0) }
    val totalFeeds = analytics.sumOf { it.feedCount.coerceAtLeast(0) }
    return AnalyticsPeriodStats(
        avgConsumedMlPerDay = totalConsumed / dayCount,
        avgWastedMlPerDay = totalWasted / dayCount,
        totalFeeds = totalFeeds,
        avgFeedsPerDay = totalFeeds.toFloat() / dayCount.toFloat(),
        totalVolumeMl = totalConsumed + totalWasted
    )
}

internal fun formatTotalVolumeLabel(totalVolumeMl: Int): String {
    val liters = totalVolumeMl.coerceAtLeast(0) / ML_PER_LITER
    return String.format(Locale.US, "Total %.1fL", liters)
}

internal fun formatMlPerDay(mlPerDay: Int): String {
    return "${mlPerDay.coerceAtLeast(0)} ml / day"
}

internal fun formatFeedsPerDay(feedsPerDay: Float): String {
    return String.format(Locale.US, "%.1f / day", feedsPerDay.coerceAtLeast(0f))
}

private fun computeAxisMax(maxValueMl: Int): Int {
    if (maxValueMl <= 0) {
        return AXIS_STEP_ML
    }
    val roundedUp = ((maxValueMl + AXIS_STEP_ML - 1) / AXIS_STEP_ML) * AXIS_STEP_ML
    return roundedUp.coerceAtLeast(AXIS_STEP_ML)
}

private fun localDateToUtcStartMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}

private fun utcStartMillisToLocalDate(utcStartMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcStartMillis).atZone(ZoneId.of("UTC")).toLocalDate()
}
