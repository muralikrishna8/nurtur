package com.nurtur.tracker.domain.service

import com.nurtur.tracker.data.local.FeedLogEntity
import org.junit.Assert.assertEquals
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
            FeedLogEntity(
                id = 1,
                remoteId = null,
                startTime = today + 1_000,
                endTime = today + 2_000,
                amountOffered = 120,
                amountConsumed = 100,
                milkType = "Formula",
                notes = null
            ),
            FeedLogEntity(
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

        assertEquals(2, result.size)
        assertEquals(100, result[0].consumedMl)
        assertEquals(20, result[0].wastedMl)
        assertEquals(1, result[0].feedCount)
    }
}
