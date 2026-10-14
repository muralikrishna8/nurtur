package com.nurtur.tracker.presentation.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsumedDurationLabelTest {

    @Test
    fun test_formatConsumedDurationLabel_eighteenMinutes_matchesMockup() {
        // Arrange
        val startEpochMillis = 1_000L
        val endEpochMillis = startEpochMillis + 18L * 60_000L

        // Act
        val label = formatConsumedDurationLabel(startEpochMillis, endEpochMillis)

        // Assert
        assertEquals("18 min", label)
    }

    @Test
    fun test_formatConsumedDurationLabel_oneMinute_usesSingularCompactForm() {
        // Arrange
        val startEpochMillis = 0L
        val endEpochMillis = 60_000L

        // Act
        val label = formatConsumedDurationLabel(startEpochMillis, endEpochMillis)

        // Assert
        assertEquals("1 min", label)
    }

    @Test
    fun test_formatConsumedDurationLabel_underOneMinute_showsSubMinuteLabel() {
        // Arrange
        val startEpochMillis = 5_000L
        val endEpochMillis = startEpochMillis + 59_000L

        // Act
        val label = formatConsumedDurationLabel(startEpochMillis, endEpochMillis)

        // Assert
        assertEquals("< 1 min", label)
    }

    @Test
    fun test_formatConsumedDurationLabel_endBeforeStart_clampsToSubMinute() {
        // Arrange
        val startEpochMillis = 120_000L
        val endEpochMillis = 10_000L

        // Act
        val label = formatConsumedDurationLabel(startEpochMillis, endEpochMillis)

        // Assert
        assertEquals("< 1 min", label)
    }

    @Test
    fun test_formatConsumedDurationLabel_exactHours_omitsZeroMinutes() {
        // Arrange
        val startEpochMillis = 0L
        val endEpochMillis = 2L * 60L * 60_000L

        // Act
        val label = formatConsumedDurationLabel(startEpochMillis, endEpochMillis)

        // Assert
        assertEquals("2 hr", label)
    }

    @Test
    fun test_formatConsumedDurationLabel_hoursAndMinutes_includesBothParts() {
        // Arrange
        val startEpochMillis = 0L
        val endEpochMillis = (90L * 60_000L) + 59_000L

        // Act
        val label = formatConsumedDurationLabel(startEpochMillis, endEpochMillis)

        // Assert
        assertEquals("1 hr 30 min", label)
    }
}
