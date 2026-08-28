package com.nurtur.tracker.infrastructure.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedLogEntity: FeedLogEntity): Long

    @Query("DELETE FROM feed_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM feed_logs ORDER BY endTime DESC LIMIT 1")
    fun observeLatestFeed(): Flow<FeedLogEntity?>

    @Query("SELECT * FROM feed_logs ORDER BY endTime DESC LIMIT :limit")
    fun observeRecentFeeds(limit: Int): Flow<List<FeedLogEntity>>

    @Query("SELECT * FROM feed_logs WHERE endTime BETWEEN :startMillis AND :endMillis ORDER BY endTime DESC")
    fun observeFeedsInRange(startMillis: Long, endMillis: Long): Flow<List<FeedLogEntity>>

    @Query("SELECT * FROM feed_logs ORDER BY endTime DESC")
    fun observeAllFeeds(): Flow<List<FeedLogEntity>>
}
