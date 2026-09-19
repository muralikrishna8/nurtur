package com.nurtur.tracker.domain.service

data class NextAlertCountdown(
    val hours: Int,
    val minutes: Int
)

object NextAlertCountdownCalculator {
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MINUTES_PER_HOUR = 60

    fun calculate(
        targetEpochMillis: Long,
        nowEpochMillis: Long
    ): NextAlertCountdown {
        val remainingMillis = (targetEpochMillis - nowEpochMillis).coerceAtLeast(0L)
        val totalMinutes = (remainingMillis / MILLIS_PER_MINUTE).toInt()
        return NextAlertCountdown(
            hours = totalMinutes / MINUTES_PER_HOUR,
            minutes = totalMinutes % MINUTES_PER_HOUR
        )
    }
}
