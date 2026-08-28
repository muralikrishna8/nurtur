package com.nurtur.tracker.data.repository

import com.nurtur.tracker.data.local.FeedLogEntity
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun observeLatestFeed(): Flow<FeedLogEntity?>
    fun observeRecentFeeds(limit: Int): Flow<List<FeedLogEntity>>
    fun observeFeedsInRange(startMillis: Long, endMillis: Long): Flow<List<FeedLogEntity>>
    fun observeAllFeeds(): Flow<List<FeedLogEntity>>
    suspend fun insert(feedLog: FeedLogEntity): Long
    suspend fun deleteById(id: Long)
}
