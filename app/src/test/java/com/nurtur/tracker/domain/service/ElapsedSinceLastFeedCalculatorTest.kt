package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ElapsedSinceLastFeedCalculatorTest {
    @Test
    fun test_calculate_missingLastFeed_returnsNull() {
        val result = ElapsedSinceLastFeedCalculator.calculate(
            lastFeedEndEpochMillis = null,
            nowEpochMillis = 1_700_000_000_000L
        )

        assertNull(result)
    }

    @Test
    fun test_calculate_wholeHours_returnsHoursOnlyMinutes() {
        val now = 1_700_000_000_000L
        val lastFeedEnd = now - (3L * 60L * 60_000L)

        val result = ElapsedSinceLastFeedCalculator.calculate(
            lastFeedEndEpochMillis = lastFeedEnd,
            nowEpochMillis = now
        )

        assertEquals(ElapsedSinceLastFeed(hours = 3, minutes = 0), result)
    }

    @Test
    fun test_calculate_hoursAndMinutes_returnsBothParts() {
        val now = 1_700_000_000_000L
        val lastFeedEnd = now - (2L * 60L * 60_000L) - (15L * 60_000L)

        val result = ElapsedSinceLastFeedCalculator.calculate(
            lastFeedEndEpochMillis = lastFeedEnd,
            nowEpochMillis = now
        )

        assertEquals(ElapsedSinceLastFeed(hours = 2, minutes = 15), result)
    }

    @Test
    fun test_calculate_underOneHour_returnsMinutesOnly() {
        val now = 1_700_000_000_000L
        val lastFeedEnd = now - (45L * 60_000L)

        val result = ElapsedSinceLastFeedCalculator.calculate(
            lastFeedEndEpochMillis = lastFeedEnd,
            nowEpochMillis = now
        )

        assertEquals(ElapsedSinceLastFeed(hours = 0, minutes = 45), result)
    }

    @Test
    fun test_calculate_futureLastFeed_returnsZero() {
        val now = 1_700_000_000_000L

        val result = ElapsedSinceLastFeedCalculator.calculate(
            lastFeedEndEpochMillis = now + 60_000L,
            nowEpochMillis = now
        )

        assertEquals(ElapsedSinceLastFeed(hours = 0, minutes = 0), result)
    }
}
