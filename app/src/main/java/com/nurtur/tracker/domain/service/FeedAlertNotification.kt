package com.nurtur.tracker.domain.service

data class FeedAlertNotification(
    val deliveryMode: AlertDeliveryMode,
    val lastFeedEndEpochMillis: Long?
)
