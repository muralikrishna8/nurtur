package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextFeedAlertCalculatorTest {
    @Test
    fun test_resolve_noLastFeed_returnsNull() {
        // Arrange / Act
        val result = NextFeedAlertCalculator.resolve(
            lastFeedEndEpochMillis = null,
            targetIntervalMinutes = 180,
            overrideEpochMillis = null
        )

        // Assert
        assertNull(result)
    }

    @Test
    fun test_resolve_withLastFeedAndNoOverride_addsGlobalInterval() {
        // Arrange
        val lastFeedEnd = 1_700_000_000_000L

        // Act
        val result = NextFeedAlertCalculator.resolve(
            lastFeedEndEpochMillis = lastFeedEnd,
            targetIntervalMinutes = 180,
            overrideEpochMillis = null
        )

        // Assert
        assertEquals(lastFeedEnd + 180L * 60_000L, result)
    }

    @Test
    fun test_resolve_withOverride_prefersOverrideOverGlobalInterval() {
        // Arrange
        val lastFeedEnd = 1_700_000_000_000L
        val override = lastFeedEnd + 90L * 60_000L

        // Act
        val result = NextFeedAlertCalculator.resolve(
            lastFeedEndEpochMillis = lastFeedEnd,
            targetIntervalMinutes = 180,
            overrideEpochMillis = override
        )

        // Assert
        assertEquals(override, result)
    }

    @Test
    fun test_resolve_overrideIgnoredWhenNoLastFeed() {
        // Arrange / Act
        val result = NextFeedAlertCalculator.resolve(
            lastFeedEndEpochMillis = null,
            targetIntervalMinutes = 180,
            overrideEpochMillis = 1_700_000_000_000L
        )

        // Assert
        assertNull(result)
    }
}
