package com.nikosdays.metaldetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nikosdays.metaldetector.audio.FeedbackManager
import com.nikosdays.metaldetector.sensor.MagneticSensorManager
import com.nikosdays.metaldetector.ui.MetalDetectorScreen
import com.nikosdays.metaldetector.ui.theme.MetalDetectorTheme

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: MagneticSensorManager
    private lateinit var feedbackManager: FeedbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = MagneticSensorManager(this)
        feedbackManager = FeedbackManager(this)

        feedbackManager.startFeedbackLoop {
            sensorManager.currentReading.delta
        }

        setContent {
            MetalDetectorTheme {
                MetalDetectorScreen(
                    sensorManager = sensorManager,
                    feedbackManager = feedbackManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.start()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        feedbackManager.release()
    }
}
