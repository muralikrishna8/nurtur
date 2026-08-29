package com.nurtur.tracker.domain.model

data class AnalyticsInsights(
    val averageVolumePerFeedMl: Int?,
    val averageTimeBetweenFeedsMillis: Long?,
    val smoothedTrendConsumedMlByDay: List<Float>
)
