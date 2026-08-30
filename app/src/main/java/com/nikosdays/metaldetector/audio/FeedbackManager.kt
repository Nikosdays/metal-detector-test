package com.nikosdays.metaldetector.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FeedbackManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private var feedbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled = true
    var isVibrationEnabled = true
    var sensitivityThreshold = 45f // uT delta threshold to start alarming

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun startFeedbackLoop(getDeltaValue: () -> Float) {
        feedbackJob?.cancel()
        feedbackJob = scope.launch {
            while (isActive) {
                val delta = getDeltaValue()

                if (delta > sensitivityThreshold) {
                    val excess = (delta - sensitivityThreshold).coerceIn(0f, 150f)
                    val factor = excess / 150f // 0.0 to 1.0

                    // Calculate beep interval (from 500ms down to 50ms)
                    val delayTime = (500L - (factor * 450L)).toLong().coerceAtLeast(45L)

                    if (isSoundEnabled) {
                        try {
                            val toneType = if (factor > 0.7f) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
                            toneGenerator?.startTone(toneType, 30)
                        } catch (_: Exception) {}
                    }

                    if (isVibrationEnabled && vibrator.hasVibrator()) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val amplitude = (50 + (factor * 205)).toInt().coerceIn(1, 255)
                                vibrator.vibrate(VibrationEffect.createOneShot(25, amplitude))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(25)
                            }
                        } catch (_: Exception) {}
                    }

                    delay(delayTime)
                } else {
                    // Below threshold: idle check
                    delay(120L)
                }
            }
        }
    }

    fun release() {
        feedbackJob?.cancel()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
    }
}
