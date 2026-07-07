package com.antonio.pulsereminder.data

data class ReminderSettings(
    val totalDurationMillis: Long,
    val intervalMillis: Long,
    val alertMode: AlertMode
)

enum class AlertMode {
    VIBRATION,
    SOUND,
    VIBRATION_AND_SOUND
}
