package com.antonio.pulsereminder.feedback

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

private const val TAG = "PulseReminder"
private const val TONE_DURATION_MILLIS = 140
private const val TONE_GAP_MILLIS = 200L
private const val TONE_RELEASE_DELAY_MILLIS = 620L
private const val TONE_VOLUME_PERCENT = 90

object SoundHelper {
    fun pulse(): Boolean {
        return playDoubleBeep(AudioManager.STREAM_ALARM) ||
            playDoubleBeep(AudioManager.STREAM_NOTIFICATION)
    }

    private fun playDoubleBeep(streamType: Int): Boolean {
        return try {
            val toneGenerator = ToneGenerator(streamType, TONE_VOLUME_PERCENT)
            val handler = Handler(Looper.getMainLooper())
            val started = toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MILLIS)

            if (started) {
                handler.postDelayed(
                    {
                        toneGenerator.startTone(
                            ToneGenerator.TONE_PROP_BEEP,
                            TONE_DURATION_MILLIS
                        )
                    },
                    TONE_DURATION_MILLIS + TONE_GAP_MILLIS
                )
            } else {
                Log.w(TAG, "sound pulse failed to start on stream $streamType")
            }

            handler.postDelayed(
                { toneGenerator.release() },
                TONE_RELEASE_DELAY_MILLIS
            )

            started
        } catch (exception: RuntimeException) {
            Log.w(TAG, "sound pulse failed on stream $streamType", exception)
            false
        }
    }
}
