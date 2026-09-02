package com.nurtur.tracker.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertDeliveryPolicyTest {
    @Test
    fun test_resolve_quietHoursActive_returnsVibrateOnly() {
        // Arrange / Act
        val result = AlertDeliveryPolicy.resolve(isQuietHoursActive = true)

        // Assert
        assertEquals(AlertDeliveryMode.VIBRATE_ONLY, result)
    }

    @Test
    fun test_resolve_quietHoursInactive_returnsEscalatingAudio() {
        // Arrange / Act
        val result = AlertDeliveryPolicy.resolve(isQuietHoursActive = false)

        // Assert
        assertEquals(AlertDeliveryMode.ESCALATING_AUDIO, result)
    }
}
