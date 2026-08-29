package com.nurtur.tracker.domain.service

import java.time.Duration

private const val APPROACHING_WINDOW_MINUTES = 30L

enum class FeedTimerStatus {
    SAFE,
    APPROACHING,
    OVERDUE
}

object FeedTimerStatusCalculator {
    fun calculate(
        elapsed: Duration,
        targetInterval: Duration
    ): FeedTimerStatus {
        if (elapsed.isNegative) {
            return FeedTimerStatus.SAFE
        }
        val approachingStart = targetInterval.minusMinutes(APPROACHING_WINDOW_MINUTES)
        if (elapsed >= targetInterval.plusMinutes(APPROACHING_WINDOW_MINUTES)) {
            return FeedTimerStatus.OVERDUE
        }
        if (elapsed >= approachingStart) {
            return FeedTimerStatus.APPROACHING
        }
        return FeedTimerStatus.SAFE
    }
}
