package com.nurtur.tracker.data.repository

import com.nurtur.tracker.data.local.FeedDao
import com.nurtur.tracker.data.local.FeedLogEntity
import kotlinx.coroutines.flow.Flow

class LocalFeedRepository(
    private val feedDao: FeedDao
) : FeedRepository {
    override fun observeLatestFeed(): Flow<FeedLogEntity?> = feedDao.observeLatestFeed()

    override fun observeRecentFeeds(limit: Int): Flow<List<FeedLogEntity>> = feedDao.observeRecentFeeds(limit)

    override fun observeFeedsInRange(startMillis: Long, endMillis: Long): Flow<List<FeedLogEntity>> =
        feedDao.observeFeedsInRange(startMillis, endMillis)

    override fun observeAllFeeds(): Flow<List<FeedLogEntity>> = feedDao.observeAllFeeds()

    override suspend fun insert(feedLog: FeedLogEntity): Long = feedDao.insert(feedLog)

    override suspend fun deleteById(id: Long) = feedDao.deleteById(id)
}
