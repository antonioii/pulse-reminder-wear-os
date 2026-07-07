package com.antonio.pulsereminder.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.antonio.pulsereminder.data.AlertMode
import com.antonio.pulsereminder.domain.formatDuration
import kotlinx.coroutines.launch

@Composable
fun ActiveSessionScreen(
    remainingMillis: Long,
    nextPulseInMillis: Long,
    alertMode: AlertMode,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onRotaryScrollEvent {
                coroutineScope.launch {
                    scrollState.scrollBy(it.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (isPaused) "Paused" else "Session active",
            color = Color(0xFF72D6C9),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = formatDuration(remainingMillis),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = if (isPaused) {
                "Next pulse: paused"
            } else {
                "Next pulse: ${formatDuration(nextPulseInMillis)}"
            },
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = alertModeLabel(alertMode),
            color = Color(0xFFB8C3C9),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )

        Button(
            onClick = onPauseResume,
            modifier = Modifier.fillMaxWidth(0.72f)
        ) {
            Text(if (isPaused) "Resume" else "Pause")
        }

        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(0.72f)
        ) {
            Text("Stop")
        }
    }
}

private fun alertModeLabel(alertMode: AlertMode): String {
    return when (alertMode) {
        AlertMode.VIBRATION -> "Vibration only"
        AlertMode.SOUND -> "Sound only"
        AlertMode.VIBRATION_AND_SOUND -> "Vibration + sound"
    }
}
