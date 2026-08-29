package com.nurtur.tracker.domain.service

import com.nurtur.tracker.domain.model.FeedLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FeedMetricsCalculatorTest {
    @Test
    fun test_calculateWasteMl_validInput_returnsDifference() {
        val waste = FeedMetricsCalculator.calculateWasteMl(amountOffered = 120, amountConsumed = 95)
        assertEquals(25, waste)
    }

    @Test
    fun test_calculateWasteMl_consumedGreaterThanOffered_returnsZero() {
        val waste = FeedMetricsCalculator.calculateWasteMl(amountOffered = 60, amountConsumed = 80)
        assertEquals(0, waste)
    }

    @Test
    fun test_buildSevenDaySummary_validFeeds_returnsDailyAggregates() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val yesterday = LocalDate.now(zone).minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val feeds = listOf(
            FeedLog(
                id = 1,
                remoteId = null,
                startTime = today + 1_000,
                endTime = today + 2_000,
                amountOffered = 120,
                amountConsumed = 100,
                milkType = "Formula",
                notes = null
            ),
            FeedLog(
                id = 2,
                remoteId = null,
                startTime = yesterday + 2_000,
                endTime = yesterday + 3_000,
                amountOffered = 90,
                amountConsumed = 80,
                milkType = "Breastmilk",
                notes = null
            )
        )

        val result = FeedMetricsCalculator.buildSevenDaySummary(feeds, zone)

        assertEquals(7, result.size)
        assertEquals(100, result[0].consumedMl)
        assertEquals(20, result[0].wastedMl)
        assertEquals(1, result[0].feedCount)
        assertEquals(80, result[1].consumedMl)
        assertEquals(10, result[1].wastedMl)
        assertEquals(1, result[1].feedCount)
    }

    @Test
    fun test_buildSevenDaySummary_missingDays_keepsZeroValueDays() {
        val zone = ZoneId.systemDefault()
        val threeDaysAgo = LocalDate.now(zone).minusDays(3).atStartOfDay(zone).toInstant().toEpochMilli()
        val feeds = listOf(
            FeedLog(
                id = 1,
                remoteId = null,
                startTime = threeDaysAgo + 1_000,
                endTime = threeDaysAgo + 2_000,
                amountOffered = 150,
                amountConsumed = 120,
                milkType = "Formula",
                notes = null
            )
        )

        val result = FeedMetricsCalculator.buildSevenDaySummary(feeds, zone)

        assertEquals(7, result.size)
        assertEquals(0, result[0].feedCount)
        assertEquals(0, result[0].consumedMl)
        assertEquals(0, result[0].wastedMl)
        assertEquals(1, result[3].feedCount)
        assertEquals(120, result[3].consumedMl)
        assertEquals(30, result[3].wastedMl)
    }

    @Test
    fun test_buildSevenDaySummary_noFeeds_returnsSevenEmptyDays() {
        val result = FeedMetricsCalculator.buildSevenDaySummary(emptyList())

        assertEquals(7, result.size)
        assertTrue(result.all { it.feedCount == 0 && it.consumedMl == 0 && it.wastedMl == 0 })
    }
}
