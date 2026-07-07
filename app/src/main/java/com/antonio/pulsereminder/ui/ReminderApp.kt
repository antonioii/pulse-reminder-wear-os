package com.antonio.pulsereminder.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import com.antonio.pulsereminder.data.AlertMode
import com.antonio.pulsereminder.data.ReminderSettings
import com.antonio.pulsereminder.data.SettingsRepository
import com.antonio.pulsereminder.domain.ReminderState
import com.antonio.pulsereminder.service.ReminderService
import com.antonio.pulsereminder.service.ReminderServiceActions
import com.antonio.pulsereminder.service.ReminderSessionStore
import kotlinx.coroutines.launch

@Composable
fun ReminderApp(settingsRepository: SettingsRepository?) {
    val context = LocalContext.current
    val sessionState by ReminderSessionStore.state.collectAsStateWithLifecycle()
    var pendingStartSettings by remember { mutableStateOf<ReminderSettings?>(null) }
    var optimisticSettings by remember { mutableStateOf<ReminderSettings?>(null) }
    var notificationPermissionMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val defaultSettings = remember {
        ReminderSettings(
            totalDurationMillis = 5 * 60 * 1000L,
            intervalMillis = 60 * 1000L,
            alertMode = AlertMode.VIBRATION
        )
    }
    val savedSettings by settingsRepository?.settings?.collectAsStateWithLifecycle(defaultSettings)
        ?: remember { mutableStateOf(defaultSettings) }

    fun startSession(settings: ReminderSettings) {
        if (settingsRepository != null) {
            coroutineScope.launch {
                settingsRepository.saveSettings(settings)
            }
        }

        optimisticSettings = settings
        context.sendPulseReminderAction(ReminderServiceActions.ACTION_START, settings)
        notificationPermissionMessage = null
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            val settings = pendingStartSettings
            pendingStartSettings = null
            if (granted && settings != null) {
                startSession(settings)
            } else {
                notificationPermissionMessage =
                    "Allow notifications to keep sessions visible in the background."
            }
        }
    )

    MaterialTheme {
        when (val state = sessionState) {
            ReminderState.Idle,
            ReminderState.Finished -> {
                val pendingDisplay = optimisticSettings
                if (pendingDisplay == null) {
                    SetupScreen(
                        initialSettings = savedSettings,
                        permissionMessage = notificationPermissionMessage,
                        modifier = Modifier.background(Color.Black),
                        onRequestNotificationPermission = {
                            if (context.needsNotificationPermission()) {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                notificationPermissionMessage =
                                    "Notifications are already allowed."
                            }
                        },
                        onStart = { settings ->
                            val normalizedSettings = settings.copy(
                                intervalMillis = settings.intervalMillis.coerceAtMost(
                                    settings.totalDurationMillis
                                )
                            )

                            if (context.needsNotificationPermission()) {
                                pendingStartSettings = normalizedSettings
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                startSession(normalizedSettings)
                            }
                        }
                    )
                } else {
                    ActiveSessionScreen(
                        remainingMillis = pendingDisplay.totalDurationMillis,
                        nextPulseInMillis = pendingDisplay.intervalMillis,
                        alertMode = pendingDisplay.alertMode,
                        isPaused = false,
                        modifier = Modifier.background(Color.Black),
                        onPauseResume = {
                            context.sendPulseReminderAction(ReminderServiceActions.ACTION_PAUSE)
                        },
                        onStop = {
                            optimisticSettings = null
                            context.sendPulseReminderAction(ReminderServiceActions.ACTION_STOP)
                        }
                    )
                }
            }

            is ReminderState.Running -> {
                optimisticSettings = null
                ActiveSessionScreen(
                    remainingMillis = state.remainingMillis,
                    nextPulseInMillis = state.nextPulseInMillis,
                    alertMode = state.alertMode,
                    isPaused = false,
                    modifier = Modifier.background(Color.Black),
                    onPauseResume = {
                        context.sendPulseReminderAction(ReminderServiceActions.ACTION_PAUSE)
                    },
                    onStop = {
                        context.sendPulseReminderAction(ReminderServiceActions.ACTION_STOP)
                    }
                )
            }

            is ReminderState.Paused -> {
                optimisticSettings = null
                ActiveSessionScreen(
                    remainingMillis = state.remainingMillis,
                    nextPulseInMillis = state.intervalMillis,
                    alertMode = state.alertMode,
                    isPaused = true,
                    modifier = Modifier.background(Color.Black),
                    onPauseResume = {
                        context.sendPulseReminderAction(ReminderServiceActions.ACTION_RESUME)
                    },
                    onStop = {
                        context.sendPulseReminderAction(ReminderServiceActions.ACTION_STOP)
                    }
                )
            }
        }
    }
}

private fun Context.needsNotificationPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
}

private fun Context.sendPulseReminderAction(
    actionName: String,
    settings: ReminderSettings? = null
) {
    val intent = Intent(this, ReminderService::class.java).apply {
        action = actionName
        settings?.let {
            putExtra(
                ReminderServiceActions.EXTRA_TOTAL_DURATION_MILLIS,
                it.totalDurationMillis
            )
            putExtra(ReminderServiceActions.EXTRA_INTERVAL_MILLIS, it.intervalMillis)
            putExtra(ReminderServiceActions.EXTRA_ALERT_MODE, it.alertMode.name)
        }
    }

    if (actionName == ReminderServiceActions.ACTION_START) {
        ContextCompat.startForegroundService(this, intent)
    } else {
        startService(intent)
    }
}
