package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozePolicyTest {
    @Test
    fun test_rescheduleAt_addsExactlyFifteenMinutes() {
        // Arrange
        val nowEpochMillis = 1_700_000_000_000L

        // Act
        val result = SnoozePolicy.rescheduleAt(nowEpochMillis)

        // Assert
        assertEquals(nowEpochMillis + SnoozePolicy.SNOOZE_DURATION_MILLIS, result)
    }

    @Test
    fun test_snoozeDuration_isFifteenMinutes() {
        // Arrange / Act / Assert
        assertEquals(15 * 60_000L, SnoozePolicy.SNOOZE_DURATION_MILLIS)
    }
}
