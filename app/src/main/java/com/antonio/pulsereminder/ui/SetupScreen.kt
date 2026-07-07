package com.antonio.pulsereminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.antonio.pulsereminder.data.ReminderSettings
import kotlinx.coroutines.launch

private enum class DurationField {
    HOURS,
    MINUTES
}

private enum class IntervalField {
    HOURS,
    MINUTES,
    SECONDS
}

private const val SECOND = 1000L
private const val MINUTE = 60 * SECOND
private const val HOUR = 60 * MINUTE

private val alertChoices = listOf(
    "Vibration" to AlertMode.VIBRATION,
    "Sound" to AlertMode.SOUND,
    "Both" to AlertMode.VIBRATION_AND_SOUND
)

@Composable
fun SetupScreen(
    initialSettings: ReminderSettings,
    onStart: (ReminderSettings) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
    permissionMessage: String? = null
) {
    var durationMillis by remember(initialSettings) {
        mutableLongStateOf(initialSettings.totalDurationMillis.coerceIn(MINUTE, 24 * HOUR))
    }
    var intervalMillis by remember(initialSettings) {
        mutableLongStateOf(initialSettings.intervalMillis.coerceIn(5 * SECOND, durationMillis))
    }
    var durationField by remember { mutableStateOf(DurationField.MINUTES) }
    var intervalField by remember { mutableStateOf(IntervalField.MINUTES) }
    var selectedAlertMode by remember(initialSettings) {
        mutableStateOf(initialSettings.alertMode)
    }
    var showInfo by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showInfo) {
        if (!showInfo) {
            focusRequester.requestFocus()
        }
    }

    if (showInfo) {
        InfoScreen(
            modifier = modifier,
            onBack = { showInfo = false }
        )
        return
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
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Pulse Reminder",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )

        TimeAdjustBlock(
            title = "Duration",
            value = formatDurationFields(durationMillis),
            selectedIndex = durationField.ordinal,
            onSelect = { durationField = DurationField.entries[it] },
            onMinus = {
                durationMillis = (durationMillis - durationField.stepMillis())
                    .coerceIn(MINUTE, 24 * HOUR)
                intervalMillis = intervalMillis.coerceAtMost(durationMillis)
            },
            onPlus = {
                durationMillis = (durationMillis + durationField.stepMillis())
                    .coerceIn(MINUTE, 24 * HOUR)
            }
        )

        TimeAdjustBlock(
            title = "Interval",
            value = formatIntervalFields(intervalMillis),
            selectedIndex = intervalField.ordinal,
            onSelect = { intervalField = IntervalField.entries[it] },
            onMinus = {
                intervalMillis = (intervalMillis - intervalField.stepMillis())
                    .coerceIn(5 * SECOND, durationMillis)
            },
            onPlus = {
                intervalMillis = (intervalMillis + intervalField.stepMillis())
                    .coerceIn(5 * SECOND, durationMillis)
            }
        )

        AlertBlock(
            selected = selectedAlertMode,
            onSelected = { selectedAlertMode = it }
        )

        permissionMessage?.let {
            Text(
                text = it,
                color = Color(0xFFFFC857),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                onStart(
                    ReminderSettings(
                        totalDurationMillis = durationMillis,
                        intervalMillis = intervalMillis.coerceAtMost(durationMillis),
                        alertMode = selectedAlertMode
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(0.72f)
        ) {
            Text(
                text = "Start",
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onRequestNotificationPermission,
            modifier = Modifier.fillMaxWidth(0.72f)
        ) {
            Text(
                text = "Settings",
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = { showInfo = true },
            modifier = Modifier.fillMaxWidth(0.72f)
        ) {
            Text(
                text = "Info",
                textAlign = TextAlign.Center
            )
        }

    }
}

@Composable
private fun TimeAdjustBlock(
    title: String,
    value: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF11161A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF263139), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF72D6C9),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundAdjustButton(label = "-", onClick = onMinus)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                value.forEachIndexed { index, part ->
                    TimeField(
                        label = part,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) }
                    )
                    if (index < value.lastIndex) {
                        Text(
                            text = ":",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            RoundAdjustButton(label = "+", onClick = onPlus)
        }
    }
}

@Composable
private fun RoundAdjustButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color(0xFF72D6C9), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.Black,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun TimeField(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Color(0xFF72D6C9) else Color(0xFF171B1F),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (selected) Color(0xFF72D6C9) else Color(0xFF38434A),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun AlertBlock(
    selected: AlertMode,
    onSelected: (AlertMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF11161A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF263139), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "Alert",
            color = Color(0xFF72D6C9),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
        Column(
            modifier = Modifier.fillMaxWidth(0.82f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alertChoices.forEach { (label, mode) ->
                ChoicePill(
                    label = label,
                    selected = mode == selected,
                    onClick = { onSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun InfoScreen(
    modifier: Modifier,
    onBack: () -> Unit
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
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "How it works",
            color = Color(0xFF72D6C9),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Duration sets how long the reminder session runs.",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Interval sets how often a pulse should happen.",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Alert chooses vibration, sound, or both.",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.72f)
        ) {
            Text(
                text = "Back",
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) Color(0xFF72D6C9) else Color(0xFF171B1F)
    val contentColor = if (selected) Color.Black else Color.White
    val borderColor = if (selected) Color(0xFF72D6C9) else Color(0xFF2F363B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = contentColor,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun DurationField.stepMillis(): Long {
    return when (this) {
        DurationField.HOURS -> HOUR
        DurationField.MINUTES -> MINUTE
    }
}

private fun IntervalField.stepMillis(): Long {
    return when (this) {
        IntervalField.HOURS -> HOUR
        IntervalField.MINUTES -> MINUTE
        IntervalField.SECONDS -> 5 * SECOND
    }
}

private fun formatDurationFields(millis: Long): List<String> {
    val totalMinutes = millis / MINUTE
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return listOf("%02d".format(hours), "%02d".format(minutes))
}

private fun formatIntervalFields(millis: Long): List<String> {
    val totalSeconds = millis / SECOND
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return listOf("%02d".format(hours), "%02d".format(minutes), "%02d".format(seconds))
}
