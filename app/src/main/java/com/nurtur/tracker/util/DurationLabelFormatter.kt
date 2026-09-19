package com.nurtur.tracker.util

import android.content.Context
import com.nurtur.tracker.R

object DurationLabelFormatter {
    fun format(context: Context, hours: Int, minutes: Int): String {
        if (hours <= 0 && minutes <= 0) {
            return context.getString(R.string.duration_less_than_one_minute)
        }
        val hoursLabel = hours.takeIf { it > 0 }?.let {
            context.resources.getQuantityString(R.plurals.duration_hours, it, it)
        }
        val minutesLabel = minutes.takeIf { it > 0 }?.let {
            context.resources.getQuantityString(R.plurals.duration_minutes, it, it)
        }
        return listOfNotNull(hoursLabel, minutesLabel).joinToString(" ")
    }
}
