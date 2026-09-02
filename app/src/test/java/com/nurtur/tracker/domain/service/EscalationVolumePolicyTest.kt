package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EscalationVolumePolicyTest {
    @Test
    fun test_volumeFraction_atStart_isMinimum() {
        // Arrange / Act
        val result = EscalationVolumePolicy.volumeFraction(elapsedMillis = 0L)

        // Assert
        assertEquals(EscalationVolumePolicy.MINIMUM_VOLUME_FRACTION, result, 0.0001f)
    }

    @Test
    fun test_volumeFraction_atSixtySeconds_isMaximum() {
        // Arrange / Act
        val result = EscalationVolumePolicy.volumeFraction(
            elapsedMillis = EscalationVolumePolicy.ESCALATION_DURATION_MILLIS
        )

        // Assert
        assertEquals(EscalationVolumePolicy.MAXIMUM_VOLUME_FRACTION, result, 0.0001f)
    }

    @Test
    fun test_volumeFraction_beyondSixtySeconds_staysAtMaximum() {
        // Arrange / Act
        val result = EscalationVolumePolicy.volumeFraction(elapsedMillis = 90_000L)

        // Assert
        assertEquals(EscalationVolumePolicy.MAXIMUM_VOLUME_FRACTION, result, 0.0001f)
    }

    @Test
    fun test_volumeFraction_midpoint_isBetweenMinAndMax() {
        // Arrange / Act
        val result = EscalationVolumePolicy.volumeFraction(elapsedMillis = 30_000L)

        // Assert
        assertTrue(result > EscalationVolumePolicy.MINIMUM_VOLUME_FRACTION)
        assertTrue(result < EscalationVolumePolicy.MAXIMUM_VOLUME_FRACTION)
    }

    @Test
    fun test_escalationDuration_isSixtySeconds() {
        // Arrange / Act / Assert
        assertEquals(60_000L, EscalationVolumePolicy.ESCALATION_DURATION_MILLIS)
    }
}
