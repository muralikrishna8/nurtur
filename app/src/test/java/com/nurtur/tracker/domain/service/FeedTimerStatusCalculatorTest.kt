package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class FeedTimerStatusCalculatorTest {
    @Test
    fun test_calculate_elapsedFarFromTarget_returnsSafe() {
        val result = FeedTimerStatusCalculator.calculate(
            elapsed = Duration.ofMinutes(60),
            targetInterval = Duration.ofMinutes(180)
        )

        assertEquals(FeedTimerStatus.SAFE, result)
    }

    @Test
    fun test_calculate_elapsedWithinThirtyMinutesOfTarget_returnsApproaching() {
        val result = FeedTimerStatusCalculator.calculate(
            elapsed = Duration.ofMinutes(160),
            targetInterval = Duration.ofMinutes(180)
        )

        assertEquals(FeedTimerStatus.APPROACHING, result)
    }

    @Test
    fun test_calculate_elapsedThirtyMinutesPastTarget_returnsOverdue() {
        val result = FeedTimerStatusCalculator.calculate(
            elapsed = Duration.ofMinutes(210),
            targetInterval = Duration.ofMinutes(180)
        )

        assertEquals(FeedTimerStatus.OVERDUE, result)
    }
}
