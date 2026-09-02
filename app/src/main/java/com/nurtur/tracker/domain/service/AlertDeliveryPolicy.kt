package com.nurtur.tracker.domain.service

object AlertDeliveryPolicy {
    fun resolve(isQuietHoursActive: Boolean): AlertDeliveryMode {
        if (isQuietHoursActive) {
            return AlertDeliveryMode.VIBRATE_ONLY
        }
        return AlertDeliveryMode.ESCALATING_AUDIO
    }
}
