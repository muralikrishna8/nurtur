package com.nurtur.tracker.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenAlertCapabilityTest {
    @Test
    fun test_shouldUseFullScreenIntent_preAndroid14_returnsTrue() {
        val result = FullScreenAlertCapability.shouldUseFullScreenIntent(
            sdkInt = 33,
            canUseFullScreenIntent = false
        )

        assertTrue(result)
    }

    @Test
    fun test_shouldUseFullScreenIntent_android14WithoutGrant_returnsFalse() {
        val result = FullScreenAlertCapability.shouldUseFullScreenIntent(
            sdkInt = 34,
            canUseFullScreenIntent = false
        )

        assertFalse(result)
    }

    @Test
    fun test_shouldUseFullScreenIntent_android14WithGrant_returnsTrue() {
        val result = FullScreenAlertCapability.shouldUseFullScreenIntent(
            sdkInt = 34,
            canUseFullScreenIntent = true
        )

        assertTrue(result)
    }
}
