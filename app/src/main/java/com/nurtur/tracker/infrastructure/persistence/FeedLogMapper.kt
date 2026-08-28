package com.nurtur.tracker.infrastructure.persistence

import com.nurtur.tracker.domain.model.FeedLog
import com.nurtur.tracker.infrastructure.persistence.room.FeedLogEntity

fun FeedLogEntity.toDomain(): FeedLog = FeedLog(
    id = id,
    remoteId = remoteId,
    startTime = startTime,
    endTime = endTime,
    amountOffered = amountOffered,
    amountConsumed = amountConsumed,
    milkType = milkType,
    notes = notes
)

fun FeedLog.toEntity(): FeedLogEntity = FeedLogEntity(
    id = id,
    remoteId = remoteId,
    startTime = startTime,
    endTime = endTime,
    amountOffered = amountOffered,
    amountConsumed = amountConsumed,
    milkType = milkType,
    notes = notes
)
