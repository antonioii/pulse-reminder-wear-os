package com.antonio.pulsereminder.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

private const val TAG = "PulseReminder"
private const val PULSE_DURATION_MILLIS = 160L
private const val PULSE_GAP_MILLIS = 200L

object VibrationHelper {
    fun pulse(context: Context) {
        try {
            val vibrator = context.systemVibrator() ?: run {
                Log.w(TAG, "vibration unavailable: no vibrator service")
                return
            }

            if (!vibrator.hasVibrator()) {
                Log.w(TAG, "vibration unavailable: device reports no vibrator")
                return
            }

            val effect = VibrationEffect.createWaveform(
                longArrayOf(
                    0L,
                    PULSE_DURATION_MILLIS,
                    PULSE_GAP_MILLIS,
                    PULSE_DURATION_MILLIS
                ),
                intArrayOf(
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE
                ),
                -1
            )
            vibrator.vibrate(effect)
        } catch (exception: SecurityException) {
            Log.w(TAG, "vibration denied", exception)
        } catch (exception: RuntimeException) {
            Log.w(TAG, "vibration failed", exception)
        }
    }

    private fun Context.systemVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
