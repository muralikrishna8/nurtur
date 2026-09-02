package com.nurtur.tracker.domain.service

object ExactAlarmCapability {
    private const val ANDROID_12_API = 31

    fun shouldUseExactAlarm(sdkInt: Int, canScheduleExactAlarms: Boolean): Boolean {
        if (sdkInt < ANDROID_12_API) {
            return true
        }
        return canScheduleExactAlarms
    }
}
