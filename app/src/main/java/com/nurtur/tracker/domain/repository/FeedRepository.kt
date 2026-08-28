package com.nurtur.tracker.domain.repository

import com.nurtur.tracker.domain.model.FeedLog
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun observeLatestFeed(): Flow<FeedLog?>
    fun observeRecentFeeds(limit: Int): Flow<List<FeedLog>>
    fun observeFeedsInRange(startMillis: Long, endMillis: Long): Flow<List<FeedLog>>
    fun observeAllFeeds(): Flow<List<FeedLog>>
    suspend fun insert(feedLog: FeedLog): Long
    suspend fun deleteById(id: Long)
}
