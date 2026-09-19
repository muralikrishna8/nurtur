package com.nurtur.tracker.domain.service

data class ElapsedSinceLastFeed(
    val hours: Int,
    val minutes: Int
)

object ElapsedSinceLastFeedCalculator {
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MINUTES_PER_HOUR = 60

    fun calculate(
        lastFeedEndEpochMillis: Long?,
        nowEpochMillis: Long
    ): ElapsedSinceLastFeed? {
        if (lastFeedEndEpochMillis == null) {
            return null
        }
        val elapsedMillis = (nowEpochMillis - lastFeedEndEpochMillis).coerceAtLeast(0L)
        val totalMinutes = (elapsedMillis / MILLIS_PER_MINUTE).toInt()
        return ElapsedSinceLastFeed(
            hours = totalMinutes / MINUTES_PER_HOUR,
            minutes = totalMinutes % MINUTES_PER_HOUR
        )
    }
}
