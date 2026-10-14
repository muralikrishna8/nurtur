package com.nurtur.tracker.domain.service

object FullScreenAlertCapability {
    private const val ANDROID_14_API = 34

    fun shouldUseFullScreenIntent(sdkInt: Int, canUseFullScreenIntent: Boolean): Boolean {
        if (sdkInt < ANDROID_14_API) {
            return true
        }
        return canUseFullScreenIntent
    }
}
