package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedAlertSchedulePolicyTest {
    @Test
    fun test_resolveTriggerAt_pushDisabled_returnsNull() {
        // Arrange / Act
        val result = FeedAlertSchedulePolicy.resolveTriggerAt(
            pushNotificationsEnabled = false,
            lastFeedEndEpochMillis = 1_000L,
            targetIntervalMinutes = 180,
            overrideEpochMillis = null
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_resolveTriggerAt_pushEnabled_returnsCalculatedAlert() {
        // Arrange / Act
        val result = FeedAlertSchedulePolicy.resolveTriggerAt(
            pushNotificationsEnabled = true,
            lastFeedEndEpochMillis = 1_000L,
            targetIntervalMinutes = 180,
            overrideEpochMillis = null
        )

        // Assert
        assertEquals(1_000L + (180 * 60_000L), result)
    }

    @Test
    fun test_resolveTriggerAt_noLastFeed_returnsNull() {
        // Arrange / Act
        val result = FeedAlertSchedulePolicy.resolveTriggerAt(
            pushNotificationsEnabled = true,
            lastFeedEndEpochMillis = null,
            targetIntervalMinutes = 180,
            overrideEpochMillis = 99_000L
        )

        // Assert
        assertNull(result)
    }
}
