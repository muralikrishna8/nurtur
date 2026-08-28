package com.nurtur.tracker.domain.model

data class FeedLog(
    val id: Long,
    val remoteId: String?,
    val startTime: Long,
    val endTime: Long,
    val amountOffered: Int,
    val amountConsumed: Int,
    val milkType: String,
    val notes: String?
)
