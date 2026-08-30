package com.nikosdays.metaldetector.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class SensorReading(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f,
    val delta: Float = 0f,
    val confidence: Int = 0,
    val statusText: String = "Калибровка...",
    val dominantAxis: String = "Z"
)

class MagneticSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    var isSensorAvailable by mutableStateOf(magneticSensor != null)
        private set

    var currentReading by mutableStateOf(SensorReading())
        private set

    // History of magnitude deltas for the oscilloscope graph (max 60 points)
    val history = mutableStateListOf<Float>()

    private var baseX = 0f
    private var baseY = 0f
    private var baseZ = 0f
    private var isCalibrated = false

    // EMA smoothing factor (0.0 to 1.0) for cleaner display
    private var smoothX = 0f
    private var smoothY = 0f
    private var smoothZ = 0f
    private val alpha = 0.25f

    fun start() {
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun tare() {
        baseX = smoothX
        baseY = smoothY
        baseZ = smoothZ
        isCalibrated = true
    }

    fun resetTare() {
        baseX = 0f
        baseY = 0f
        baseZ = 0f
        isCalibrated = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return

        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        // Low-pass filter for smooth gauge motion
        smoothX += alpha * (rawX - smoothX)
        smoothY += alpha * (rawY - smoothY)
        smoothZ += alpha * (rawZ - smoothZ)

        val totalMagnitude = sqrt(smoothX * smoothX + smoothY * smoothY + smoothZ * smoothZ)

        if (!isCalibrated) {
            baseX = smoothX
            baseY = smoothY
            baseZ = smoothZ
            isCalibrated = true
        }

        // Calculate 3D delta from tare baseline
        val dx = smoothX - baseX
        val dy = smoothY - baseY
        val dz = smoothZ - baseZ
        val delta = sqrt(dx * dx + dy * dy + dz * dz)

        // Calculate confidence percentage on realistic physical scale
        val confidence = when {
            delta < 6f -> (delta / 6f * 8f).roundToInt()
            delta < 20f -> 8 + ((delta - 6f) / 14f * 27f).roundToInt() // 8% - 35%
            delta < 55f -> 35 + ((delta - 20f) / 35f * 40f).roundToInt() // 35% - 75%
            delta < 120f -> 75 + ((delta - 55f) / 65f * 20f).roundToInt() // 75% - 95%
            else -> 99
        }.coerceIn(0, 99)

        val statusText = when {
            confidence < 15 -> "Фон в норме (нет металла)"
            confidence < 45 -> "Слабая аномалия / Небольшой металл"
            confidence < 75 -> "Обнаружен металл / Проводка"
            else -> "⚠️ МАССИВНЫЙ МЕТАЛЛ / МАГНИТ!"
        }

        val dominantAxis = when {
            Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz) -> if (dx > 0) "Справа (+X)" else "Слева (-X)"
            Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > Math.abs(dz) -> if (dy > 0) "Сверху (+Y)" else "Снизу (-Y)"
            else -> if (dz > 0) "С лицевой стороны (+Z)" else "Сзади (-Z)"
        }

        currentReading = SensorReading(
            x = smoothX,
            y = smoothY,
            z = smoothZ,
            magnitude = totalMagnitude,
            delta = delta,
            confidence = confidence,
            statusText = statusText,
            dominantAxis = dominantAxis
        )

        // Update history buffer
        if (history.size > 50) {
            history.removeAt(0)
        }
        history.add(delta)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
