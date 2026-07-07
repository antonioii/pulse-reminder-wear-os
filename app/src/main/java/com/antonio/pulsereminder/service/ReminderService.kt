package com.antonio.pulsereminder.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.antonio.pulsereminder.data.AlertMode
import com.antonio.pulsereminder.data.ReminderSettings
import com.antonio.pulsereminder.domain.ReminderState
import com.antonio.pulsereminder.feedback.SoundHelper
import com.antonio.pulsereminder.feedback.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "PulseReminder"
private const val TICK_MILLIS = 1000L

class ReminderService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var currentSettings: ReminderSettings? = null
    private var remainingMillis: Long = 0L
    private var nextPulseInMillis: Long = 0L
    private var isPaused: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ReminderServiceActions.ACTION_START -> {
                startSession(intent)
                START_STICKY
            }

            ReminderServiceActions.ACTION_PAUSE -> {
                pauseSession()
                START_STICKY
            }

            ReminderServiceActions.ACTION_RESUME -> {
                resumeSession()
                START_STICKY
            }

            ReminderServiceActions.ACTION_STOP -> {
                Log.i(TAG, "stop requested")
                stopSession(updateFinished = false)
                START_NOT_STICKY
            }

            else -> START_NOT_STICKY
        }
    }

    private fun startSession(intent: Intent) {
        val settings = intent.toReminderSettings() ?: run {
            Log.w(TAG, "start ignored: invalid settings")
            stopSelf()
            return
        }

        timerJob?.cancel()
        currentSettings = settings
        remainingMillis = settings.totalDurationMillis
        nextPulseInMillis = settings.intervalMillis.coerceAtMost(remainingMillis)
        isPaused = false

        ReminderNotification.ensureChannel(this)
        ServiceCompat.startForeground(
            this,
            ReminderNotification.NOTIFICATION_ID,
            buildNotification(settings),
            foregroundServiceType()
        )
        publishRunningState(settings)
        startTimerLoop()

        Log.i(TAG, "service started")
    }

    private fun pauseSession() {
        val settings = currentSettings ?: return
        if (isPaused) return

        timerJob?.cancel()
        isPaused = true
        ReminderSessionStore.update(
            ReminderState.Paused(
                remainingMillis = remainingMillis,
                intervalMillis = nextPulseInMillis,
                alertMode = settings.alertMode
            )
        )
        updateNotification(settings)
        Log.i(TAG, "session paused")
    }

    private fun resumeSession() {
        val settings = currentSettings ?: return
        if (!isPaused) return

        isPaused = false
        publishRunningState(settings)
        updateNotification(settings)
        startTimerLoop()
        Log.i(TAG, "session resumed")
    }

    private fun startTimerLoop() {
        val settings = currentSettings ?: return
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var lastTick = SystemClock.elapsedRealtime()

            while (isActive && remainingMillis > 0L && !isPaused) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - lastTick
                lastTick = now

                remainingMillis = (remainingMillis - elapsed).coerceAtLeast(0L)
                nextPulseInMillis = (nextPulseInMillis - elapsed).coerceAtLeast(0L)

            if (nextPulseInMillis == 0L && remainingMillis > 0L) {
                Log.i(TAG, "pulse fired")
                firePulse(settings.alertMode)
                nextPulseInMillis = settings.intervalMillis.coerceAtMost(remainingMillis)
            }

                publishRunningState(settings)
                updateNotification(settings)

                if (remainingMillis == 0L) {
                    ReminderSessionStore.update(ReminderState.Finished)
                    Log.i(TAG, "session finished")
                    stopSession(updateFinished = true)
                    return@launch
                }

                delay(TICK_MILLIS)
            }
        }
    }

    private fun publishRunningState(settings: ReminderSettings) {
        ReminderSessionStore.update(
            ReminderState.Running(
                totalDurationMillis = settings.totalDurationMillis,
                remainingMillis = remainingMillis,
                intervalMillis = settings.intervalMillis,
                nextPulseInMillis = nextPulseInMillis,
                alertMode = settings.alertMode
            )
        )
    }

    private fun firePulse(alertMode: AlertMode) {
        when (alertMode) {
            AlertMode.VIBRATION -> VibrationHelper.pulse(this)
            AlertMode.SOUND -> {
                if (!SoundHelper.pulse()) {
                    VibrationHelper.pulse(this)
                }
            }

            AlertMode.VIBRATION_AND_SOUND -> {
                VibrationHelper.pulse(this)
                SoundHelper.pulse()
            }
        }
    }

    private fun buildNotification(settings: ReminderSettings) =
        ReminderNotification.build(
            context = this,
            settings = settings,
            remainingMillis = remainingMillis,
            nextPulseInMillis = nextPulseInMillis,
            isPaused = isPaused
        )

    private fun updateNotification(settings: ReminderSettings) {
        try {
            NotificationManagerCompat.from(this).notify(
                ReminderNotification.NOTIFICATION_ID,
                buildNotification(settings)
            )
        } catch (exception: SecurityException) {
            Log.w(TAG, "notification update denied", exception)
        }
    }

    private fun stopSession(updateFinished: Boolean) {
        timerJob?.cancel()
        timerJob = null
        currentSettings = null
        remainingMillis = 0L
        nextPulseInMillis = 0L
        isPaused = false

        if (!updateFinished) {
            ReminderSessionStore.update(ReminderState.Idle)
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        ReminderSessionStore.update(ReminderState.Idle)
        Log.i(TAG, "service destroyed")
        super.onDestroy()
    }

    private fun foregroundServiceType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
    }

    private fun Intent.toReminderSettings(): ReminderSettings? {
        val totalDurationMillis = getLongExtra(
            ReminderServiceActions.EXTRA_TOTAL_DURATION_MILLIS,
            0L
        )
        val intervalMillis = getLongExtra(
            ReminderServiceActions.EXTRA_INTERVAL_MILLIS,
            0L
        )
        val alertMode = getStringExtra(ReminderServiceActions.EXTRA_ALERT_MODE)
            ?.let { runCatching { AlertMode.valueOf(it) }.getOrNull() }

        if (totalDurationMillis <= 0L || intervalMillis <= 0L || alertMode == null) {
            return null
        }

        return ReminderSettings(
            totalDurationMillis = totalDurationMillis,
            intervalMillis = intervalMillis.coerceAtMost(totalDurationMillis),
            alertMode = alertMode
        )
    }
}
