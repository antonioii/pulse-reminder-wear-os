package com.antonio.pulsereminder.service

import com.antonio.pulsereminder.domain.ReminderState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ReminderSessionStore {
    private val mutableState = MutableStateFlow<ReminderState>(ReminderState.Idle)
    val state: StateFlow<ReminderState> = mutableState.asStateFlow()

    fun update(state: ReminderState) {
        mutableState.value = state
    }
}
