package com.antonio.pulsereminder.domain

import com.antonio.pulsereminder.data.AlertMode

sealed interface ReminderState {
    data object Idle : ReminderState

    data class Running(
        val totalDurationMillis: Long,
        val remainingMillis: Long,
        val intervalMillis: Long,
        val nextPulseInMillis: Long,
        val alertMode: AlertMode
    ) : ReminderState

    data class Paused(
        val remainingMillis: Long,
        val intervalMillis: Long,
        val alertMode: AlertMode
    ) : ReminderState

    data object Finished : ReminderState
}
