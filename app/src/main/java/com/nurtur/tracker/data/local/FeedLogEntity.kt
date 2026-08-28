package com.nurtur.tracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_logs")
data class FeedLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val remoteId: String?,
    val startTime: Long,
    val endTime: Long,
    val amountOffered: Int,
    val amountConsumed: Int,
    val milkType: String = "Formula",
    val notes: String?
)
