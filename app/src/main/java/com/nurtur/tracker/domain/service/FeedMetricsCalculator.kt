package com.nurtur.tracker.domain.service

import com.nurtur.tracker.domain.model.DailyAnalytics
import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.model.AnalyticsInsights
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FeedMetricsCalculator {
    private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    private const val MIN_VALID_CONSUMED_ML = 10
    private const val MIN_FEED_COUNT_FOR_INSIGHTS = 3

    fun calculateWasteMl(amountOffered: Int, amountConsumed: Int): Int {
        if (amountOffered < 0 || amountConsumed < 0) return 0
        return (amountOffered - amountConsumed).coerceAtLeast(0)
    }

    fun buildDailySummary(
        feeds: List<FeedLog>,
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyAnalytics> {
        if (endDate.isBefore(startDate)) {
            return emptyList()
        }
        val grouped = feeds.groupBy { feed ->
            Instant.ofEpochMilli(feed.endTime).atZone(zoneId).toLocalDate()
        }
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
        return (0..totalDays).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
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

    fun buildSevenDaySummary(
        feeds: List<FeedLog>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyAnalytics> {
        val today = LocalDate.now(zoneId)
        return buildDailySummary(
            feeds = feeds,
            startDate = today.minusDays(6),
            endDate = today,
            zoneId = zoneId
        ).asReversed()
    }

    fun buildAveragesAndTrend(
        feeds: List<FeedLog>,
        trendStartDate: LocalDate? = null,
        trendEndDate: LocalDate? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): AnalyticsInsights {
        if (feeds.size < MIN_FEED_COUNT_FOR_INSIGHTS) {
            return AnalyticsInsights(
                averageVolumePerFeedMl = null,
                averageTimeBetweenFeedsMillis = null,
                smoothedTrendConsumedMlByDay = emptyList()
            )
        }

        val qualifyingFeeds = feeds
            .filter { it.amountConsumed >= MIN_VALID_CONSUMED_ML }
            .sortedBy { it.startTime }

        val averageVolume = qualifyingFeeds
            .takeIf { it.isNotEmpty() }
            ?.map { it.amountConsumed }
            ?.average()
            ?.toInt()

        val averageIntervalMillis = qualifyingFeeds
            .zipWithNext()
            .map { (previous, next) -> (next.startTime - previous.endTime).coerceAtLeast(0L) }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toLong()

        val dailyConsumedSeries = if (trendStartDate != null && trendEndDate != null) {
            buildDailySummary(feeds, trendStartDate, trendEndDate, zoneId).map { it.consumedMl.toFloat() }
        } else {
            buildSevenDaySummary(feeds, zoneId).asReversed().map { it.consumedMl.toFloat() }
        }

        return AnalyticsInsights(
            averageVolumePerFeedMl = averageVolume,
            averageTimeBetweenFeedsMillis = averageIntervalMillis,
            smoothedTrendConsumedMlByDay = smoothMovingAverage(dailyConsumedSeries)
        )
    }

    private fun smoothMovingAverage(values: List<Float>): List<Float> {
        if (values.isEmpty()) {
            return emptyList()
        }
        if (values.size == 1) {
            return values
        }

        return values.indices.map { index ->
            val previous = values.getOrElse(index - 1) { values[index] }
            val current = values[index]
            val next = values.getOrElse(index + 1) { values[index] }
            (previous + current + next) / 3f
        }
    }
}
