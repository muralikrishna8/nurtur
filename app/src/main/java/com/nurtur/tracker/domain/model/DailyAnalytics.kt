package com.nurtur.tracker.domain.model

data class DailyAnalytics(
    val dayLabel: String,
    val consumedMl: Int,
    val wastedMl: Int,
    val feedCount: Int
)
