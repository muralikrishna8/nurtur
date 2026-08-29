package com.nurtur.tracker.domain.service

import com.nurtur.tracker.domain.model.DailyAnalytics
import com.nurtur.tracker.domain.model.FeedLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FeedMetricsCalculator {
    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    fun calculateWasteMl(amountOffered: Int, amountConsumed: Int): Int {
        if (amountOffered < 0 || amountConsumed < 0) return 0
        return (amountOffered - amountConsumed).coerceAtLeast(0)
    }

    fun buildSevenDaySummary(
        feeds: List<FeedLog>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyAnalytics> {
        val grouped = feeds.groupBy { feed ->
            Instant.ofEpochMilli(feed.endTime).atZone(zoneId).toLocalDate()
        }
        val today = LocalDate.now(zoneId)
        return (0L..6L).map { dayOffset ->
            val date = today.minusDays(dayOffset)
            val dayFeeds = grouped[date].orEmpty()
            val consumed = dayFeeds.sumOf { it.amountConsumed.coerceAtLeast(0) }
            val wasted = dayFeeds.sumOf {
                calculateWasteMl(it.amountOffered, it.amountConsumed)
            }
            DailyAnalytics(
                dayLabel = dayFormatter.format(date),
                consumedMl = consumed,
                wastedMl = wasted,
                feedCount = dayFeeds.size
            )
        }
    }
}
