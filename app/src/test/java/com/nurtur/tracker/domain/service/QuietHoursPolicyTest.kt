package com.nurtur.tracker.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursPolicyTest {
    @Test
    fun test_isActive_disabled_returnsFalse() {
        // Arrange / Act
        val result = QuietHoursPolicy.isActive(
            enabled = false,
            minutesOfDay = 23 * 60,
            startMinutesOfDay = 22 * 60,
            endMinutesOfDay = 6 * 60
        )

        // Assert
        assertFalse(result)
    }

    @Test
    fun test_isActive_sameStartAndEnd_returnsFalse() {
        // Arrange / Act
        val result = QuietHoursPolicy.isActive(
            enabled = true,
            minutesOfDay = 12 * 60,
            startMinutesOfDay = 10 * 60,
            endMinutesOfDay = 10 * 60
        )

        // Assert
        assertFalse(result)
    }

    @Test
    fun test_isActive_overnightWindow_insideLateNight_returnsTrue() {
        // Arrange / Act
        val result = QuietHoursPolicy.isActive(
            enabled = true,
            minutesOfDay = 23 * 60,
            startMinutesOfDay = 22 * 60,
            endMinutesOfDay = 6 * 60
        )

        // Assert
        assertTrue(result)
    }

    @Test
    fun test_isActive_overnightWindow_insideEarlyMorning_returnsTrue() {
        // Arrange / Act
        val result = QuietHoursPolicy.isActive(
            enabled = true,
            minutesOfDay = 5 * 60,
            startMinutesOfDay = 22 * 60,
            endMinutesOfDay = 6 * 60
        )

        // Assert
        assertTrue(result)
    }

    @Test
    fun test_isActive_overnightWindow_outside_returnsFalse() {
        // Arrange / Act
        val result = QuietHoursPolicy.isActive(
            enabled = true,
            minutesOfDay = 12 * 60,
            startMinutesOfDay = 22 * 60,
            endMinutesOfDay = 6 * 60
        )

        // Assert
        assertFalse(result)
    }

    @Test
    fun test_isActive_sameDayWindow_inside_returnsTrue() {
        // Arrange / Act
        val result = QuietHoursPolicy.isActive(
            enabled = true,
            minutesOfDay = 14 * 60,
            startMinutesOfDay = 13 * 60,
            endMinutesOfDay = 15 * 60
        )

        // Assert
        assertTrue(result)
    }

    @Test
    fun test_isActive_sameDayWindow_outside_returnsFalse() {
        // Arrange / Act
        val result = QuietHoursPolicy.isActive(
            enabled = true,
            minutesOfDay = 16 * 60,
            startMinutesOfDay = 13 * 60,
            endMinutesOfDay = 15 * 60
        )

        // Assert
        assertFalse(result)
    }
}
