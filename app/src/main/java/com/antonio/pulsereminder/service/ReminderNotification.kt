package com.antonio.pulsereminder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.antonio.pulsereminder.MainActivity
import com.antonio.pulsereminder.R
import com.antonio.pulsereminder.data.AlertMode
import com.antonio.pulsereminder.data.ReminderSettings
import com.antonio.pulsereminder.domain.formatDuration

object ReminderNotification {
    const val CHANNEL_ID = "pulse_reminder_session"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pulse Reminder session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active interval reminder session."
        }

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        settings: ReminderSettings,
        remainingMillis: Long,
        nextPulseInMillis: Long,
        isPaused: Boolean = false
    ): Notification {
        val contentIntent = activityIntent(context)
        val stopIntent = serviceIntent(context, ReminderServiceActions.ACTION_STOP, 1)
        val pauseOrResumeIntent = serviceIntent(
            context = context,
            action = if (isPaused) {
                ReminderServiceActions.ACTION_RESUME
            } else {
                ReminderServiceActions.ACTION_PAUSE
            },
            requestCode = 2
        )

        val statusText = if (isPaused) {
            "Paused"
        } else {
            "Next pulse in ${formatDuration(nextPulseInMillis)}"
        }
        val title = if (isPaused) "Pulse Reminder paused" else "Pulse Reminder active"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_pulse)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_pulse,
                if (isPaused) "Resume" else "Pause",
                pauseOrResumeIntent
            )
            .addAction(R.drawable.ic_pulse, "Stop", stopIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "$statusText - Remaining ${formatDuration(remainingMillis)} - " +
                            alertModeLabel(settings.alertMode)
                    )
            )

        OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_pulse)
            .setTouchIntent(contentIntent)
            .setTitle("Pulse Reminder")
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setStatus(Status.forPart(Status.TextPart(statusText)))
            .build()
            .apply(context)

        return builder.build()
    }

    private fun activityIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        return PendingIntent.getService(
            context,
            requestCode,
            Intent(context, ReminderService::class.java).apply {
                this.action = action
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alertModeLabel(alertMode: AlertMode): String {
        return when (alertMode) {
            AlertMode.VIBRATION -> "Vibration"
            AlertMode.SOUND -> "Sound"
            AlertMode.VIBRATION_AND_SOUND -> "Vibration + sound"
        }
    }
}
