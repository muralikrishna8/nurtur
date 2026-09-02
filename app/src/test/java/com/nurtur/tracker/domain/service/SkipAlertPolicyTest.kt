package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class SkipAlertPolicyTest {
    @Test
    fun test_rescheduleAt_addsTargetIntervalFromNow() {
        // Arrange
        val nowEpochMillis = 1_700_000_000_000L

        // Act
        val result = SkipAlertPolicy.rescheduleAt(
            nowEpochMillis = nowEpochMillis,
            targetIntervalMinutes = 180
        )

        // Assert
        assertEquals(nowEpochMillis + (180 * 60_000L), result)
    }

    @Test
    fun test_rescheduleAt_negativeInterval_clampsToZeroOffset() {
        // Arrange
        val nowEpochMillis = 1_700_000_000_000L

        // Act
        val result = SkipAlertPolicy.rescheduleAt(
            nowEpochMillis = nowEpochMillis,
            targetIntervalMinutes = -5
        )

        // Assert
        assertEquals(nowEpochMillis, result)
    }
}
