package com.antonio.pulsereminder.service

object ReminderServiceActions {
    const val ACTION_START = "com.antonio.pulsereminder.action.START"
    const val ACTION_PAUSE = "com.antonio.pulsereminder.action.PAUSE"
    const val ACTION_RESUME = "com.antonio.pulsereminder.action.RESUME"
    const val ACTION_STOP = "com.antonio.pulsereminder.action.STOP"

    const val EXTRA_TOTAL_DURATION_MILLIS = "extra_total_duration_millis"
    const val EXTRA_INTERVAL_MILLIS = "extra_interval_millis"
    const val EXTRA_ALERT_MODE = "extra_alert_mode"
}
