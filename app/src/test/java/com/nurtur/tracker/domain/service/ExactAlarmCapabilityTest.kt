package com.nurtur.tracker.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactAlarmCapabilityTest {
    @Test
    fun test_shouldUseExactAlarm_preAndroid12_returnsTrue() {
        // Arrange / Act
        val result = ExactAlarmCapability.shouldUseExactAlarm(
            sdkInt = 30,
            canScheduleExactAlarms = false
        )

        // Assert
        assertTrue(result)
    }

    @Test
    fun test_shouldUseExactAlarm_android12WithoutGrant_returnsFalse() {
        // Arrange / Act
        val result = ExactAlarmCapability.shouldUseExactAlarm(
            sdkInt = 31,
            canScheduleExactAlarms = false
        )

        // Assert
        assertFalse(result)
    }

    @Test
    fun test_shouldUseExactAlarm_android12WithGrant_returnsTrue() {
        // Arrange / Act
        val result = ExactAlarmCapability.shouldUseExactAlarm(
            sdkInt = 31,
            canScheduleExactAlarms = true
        )

        // Assert
        assertTrue(result)
    }
}
