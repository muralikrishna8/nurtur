package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class NextAlertCountdownCalculatorTest {
    @Test
    fun test_calculate_wholeHours_returnsHoursOnlyMinutes() {
        val now = 1_700_000_000_000L
        val target = now + (3L * 60L * 60_000L)

        val result = NextAlertCountdownCalculator.calculate(
            targetEpochMillis = target,
            nowEpochMillis = now
        )

        assertEquals(NextAlertCountdown(hours = 3, minutes = 0), result)
    }

    @Test
    fun test_calculate_hoursAndMinutes_returnsBothParts() {
        val now = 1_700_000_000_000L
        val target = now + (2L * 60L * 60_000L) + (15L * 60_000L)

        val result = NextAlertCountdownCalculator.calculate(
            targetEpochMillis = target,
            nowEpochMillis = now
        )

        assertEquals(NextAlertCountdown(hours = 2, minutes = 15), result)
    }

    @Test
    fun test_calculate_underOneHour_returnsMinutesOnly() {
        val now = 1_700_000_000_000L
        val target = now + (45L * 60_000L)

        val result = NextAlertCountdownCalculator.calculate(
            targetEpochMillis = target,
            nowEpochMillis = now
        )

        assertEquals(NextAlertCountdown(hours = 0, minutes = 45), result)
    }

    @Test
    fun test_calculate_targetInPast_returnsZero() {
        val now = 1_700_000_000_000L
        val target = now - 60_000L

        val result = NextAlertCountdownCalculator.calculate(
            targetEpochMillis = target,
            nowEpochMillis = now
        )

        assertEquals(NextAlertCountdown(hours = 0, minutes = 0), result)
    }
}
