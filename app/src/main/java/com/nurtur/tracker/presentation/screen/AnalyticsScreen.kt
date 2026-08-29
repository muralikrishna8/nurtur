package com.nurtur.tracker.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nurtur.tracker.domain.model.DailyAnalytics

private const val BAR_SECTION_WEIGHT_SCALE = 1f
private const val DAY_LABEL_LENGTH = 3
private const val AXIS_TICK_COUNT = 4
private const val AXIS_STEP_ML = 50
private val Y_AXIS_WIDTH: Dp = 44.dp

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    analytics: List<DailyAnalytics>
) {
    val chartData = analytics.take(7).asReversed()
    val maxDayTotalMl = chartData.maxOfOrNull { it.consumedMl + it.wastedMl } ?: 0
    val yAxisMaxMl = computeAxisMax(maxDayTotalMl)
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    var selectedIndex by remember(chartData) {
        mutableIntStateOf(chartData.indexOfFirst { it.feedCount > 0 }.coerceAtLeast(0))
    }
    val selectedDay = chartData.getOrNull(selectedIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Last 7 Days", style = MaterialTheme.typography.titleLarge)
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
                            .padding(bottom = 24.dp),
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

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .drawBehind {
                                val dashPath = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                val segmentHeight = size.height / AXIS_TICK_COUNT.toFloat()
                                for (tick in 0..AXIS_TICK_COUNT) {
                                    val y = tick * segmentHeight
                                    drawLine(
                                        color = gridLineColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        pathEffect = dashPath,
                                        strokeWidth = 1.5f
                                    )
                                }
                            }
                            .padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartData.forEachIndexed { index, day ->
                            val dayTotal = day.consumedMl + day.wastedMl
                            val barHeightRatio = if (dayTotal == 0) 0f else dayTotal.toFloat() / yAxisMaxMl.toFloat()
                            val consumedRatio = if (dayTotal == 0) 0f else day.consumedMl.toFloat() / dayTotal.toFloat()
                            val wastedRatio = if (dayTotal == 0) 0f else day.wastedMl.toFloat() / dayTotal.toFloat()

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
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
                                Text(
                                    text = day.dayLabel.take(DAY_LABEL_LENGTH),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                )
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
            }
        }
    }
}

private fun computeAxisMax(maxValueMl: Int): Int {
    if (maxValueMl <= 0) {
        return AXIS_STEP_ML
    }
    val roundedUp = ((maxValueMl + AXIS_STEP_ML - 1) / AXIS_STEP_ML) * AXIS_STEP_ML
    return roundedUp.coerceAtLeast(AXIS_STEP_ML)
}
