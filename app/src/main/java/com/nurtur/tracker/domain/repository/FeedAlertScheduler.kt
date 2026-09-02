package com.nurtur.tracker.domain.repository

import com.nurtur.tracker.domain.service.AlertDeliveryMode

interface FeedAlertScheduler {
    fun schedule(triggerAtEpochMillis: Long)

    fun cancel()

    fun dismissActiveAlarm()

    fun notifyAlertFired(deliveryMode: AlertDeliveryMode)
}
