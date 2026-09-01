package com.nurtur.tracker.presentation.screen

import com.nurtur.tracker.domain.model.DailyAnalytics
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsPeriodStatsTest {

    @Test
    fun test_computePeriodStats_emptyList_returnsZeros() {
        // Arrange
        val analytics = emptyList<DailyAnalytics>()

        // Act
        val result = computePeriodStats(analytics)

        // Assert
        assertEquals(0, result.avgConsumedMlPerDay)
        assertEquals(0, result.avgWastedMlPerDay)
        assertEquals(0, result.totalFeeds)
        assertEquals(0f, result.avgFeedsPerDay)
        assertEquals(0, result.totalVolumeMl)
    }

    @Test
    fun test_computePeriodStats_sevenDayWindow_averagesAndTotals() {
        // Arrange
        val analytics = listOf(
            DailyAnalytics(dayLabel = "Mon", consumedMl = 100, wastedMl = 10, feedCount = 3),
            DailyAnalytics(dayLabel = "Tue", consumedMl = 200, wastedMl = 20, feedCount = 5),
            DailyAnalytics(dayLabel = "Wed", consumedMl = 0, wastedMl = 0, feedCount = 0),
            DailyAnalytics(dayLabel = "Thu", consumedMl = 140, wastedMl = 14, feedCount = 4),
            DailyAnalytics(dayLabel = "Fri", consumedMl = 160, wastedMl = 16, feedCount = 4),
            DailyAnalytics(dayLabel = "Sat", consumedMl = 120, wastedMl = 12, feedCount = 3),
            DailyAnalytics(dayLabel = "Sun", consumedMl = 180, wastedMl = 18, feedCount = 5)
        )

        // Act
        val result = computePeriodStats(analytics)

        // Assert
        assertEquals(128, result.avgConsumedMlPerDay)
        assertEquals(12, result.avgWastedMlPerDay)
        assertEquals(24, result.totalFeeds)
        assertEquals(24f / 7f, result.avgFeedsPerDay)
        assertEquals(990, result.totalVolumeMl)
    }

    @Test
    fun test_formatTotalVolumeLabel_convertsMlToLiters() {
        // Arrange
        val totalVolumeMl = 3200

        // Act
        val result = formatTotalVolumeLabel(totalVolumeMl)

        // Assert
        assertEquals("Total 3.2L", result)
    }

    @Test
    fun test_formatMlPerDay_and_formatFeedsPerDay() {
        // Arrange / Act / Assert
        assertEquals("142 ml / day", formatMlPerDay(142))
        assertEquals("4.0 / day", formatFeedsPerDay(4.0f))
        assertEquals("4.4 / day", formatFeedsPerDay(4.4f))
    }
}
