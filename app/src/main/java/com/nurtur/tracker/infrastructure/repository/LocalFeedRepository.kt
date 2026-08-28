package com.nurtur.tracker.infrastructure.repository

import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.domain.repository.FeedRepository
import com.nurtur.tracker.infrastructure.persistence.room.FeedDao
import com.nurtur.tracker.infrastructure.persistence.toDomain
import com.nurtur.tracker.infrastructure.persistence.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalFeedRepository(
    private val feedDao: FeedDao
) : FeedRepository {
    override fun observeLatestFeed(): Flow<FeedLog?> =
        feedDao.observeLatestFeed().map { it?.toDomain() }

    override fun observeRecentFeeds(limit: Int): Flow<List<FeedLog>> =
        feedDao.observeRecentFeeds(limit).map { feeds -> feeds.map { it.toDomain() } }

    override fun observeFeedsInRange(startMillis: Long, endMillis: Long): Flow<List<FeedLog>> =
        feedDao.observeFeedsInRange(startMillis, endMillis).map { feeds -> feeds.map { it.toDomain() } }

    override fun observeAllFeeds(): Flow<List<FeedLog>> =
        feedDao.observeAllFeeds().map { feeds -> feeds.map { it.toDomain() } }

    override suspend fun insert(feedLog: FeedLog): Long = feedDao.insert(feedLog.toEntity())

    override suspend fun deleteById(id: Long) = feedDao.deleteById(id)
}
