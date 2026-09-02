package com.nurtur.tracker.domain.service

object EscalationVolumePolicy {
    const val ESCALATION_DURATION_MILLIS: Long = 60_000L
    const val MINIMUM_VOLUME_FRACTION: Float = 0.05f
    const val MAXIMUM_VOLUME_FRACTION: Float = 1.0f

    fun volumeFraction(elapsedMillis: Long): Float {
        if (elapsedMillis <= 0L) {
            return MINIMUM_VOLUME_FRACTION
        }
        if (elapsedMillis >= ESCALATION_DURATION_MILLIS) {
            return MAXIMUM_VOLUME_FRACTION
        }
        val progress = elapsedMillis.toFloat() / ESCALATION_DURATION_MILLIS.toFloat()
        val span = MAXIMUM_VOLUME_FRACTION - MINIMUM_VOLUME_FRACTION
        return MINIMUM_VOLUME_FRACTION + (span * progress)
    }
}
