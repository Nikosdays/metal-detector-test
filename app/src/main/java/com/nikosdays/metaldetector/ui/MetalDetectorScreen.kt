package com.nikosdays.metaldetector.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikosdays.metaldetector.audio.FeedbackManager
import com.nikosdays.metaldetector.sensor.MagneticSensorManager
import com.nikosdays.metaldetector.sensor.SensorReading
import com.nikosdays.metaldetector.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetalDetectorScreen(
    sensorManager: MagneticSensorManager,
    feedbackManager: FeedbackManager
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val reading = sensorManager.currentReading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Radar,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "METAL RADAR PRO",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                actions = {
                    IconButton(onClick = { sensorManager.tare() }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Калибровка",
                            tint = NeonCyan
                        )
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Selector Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = NeonGreen,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🧲 Сканер", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("📈 График", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🎯 Где чип?", fontWeight = FontWeight.Bold) }
                )
            }

            if (!sensorManager.isSensorAvailable) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚠️ Магнитный датчик не найден на этом устройстве.",
                        color = NeonRed,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                when (selectedTab) {
                    0 -> MainScannerTab(sensorManager, feedbackManager, reading)
                    1 -> OscilloscopeTab(sensorManager, reading)
                    2 -> SensorLocatorTab(reading)
                }
            }
        }
    }
}

@Composable
fun MainScannerTab(
    sensorManager: MagneticSensorManager,
    feedbackManager: FeedbackManager,
    reading: SensorReading
) {
    var soundEnabled by remember { mutableStateOf(feedbackManager.isSoundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(feedbackManager.isVibrationEnabled) }
    var sensitivity by remember { mutableFloatStateOf(feedbackManager.sensitivityThreshold) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Main Dial Gauge
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AnalogGauge(deltaValue = reading.delta, confidence = reading.confidence)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${reading.delta.toInt()}",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = getConfidenceColor(reading.confidence)
                )
                Text(
                    text = "Δ μT (микротеслы)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "База: ${reading.magnitude.toInt()} μT",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }

        // Metal Probability & Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ВЕРОЯТНОСТЬ МЕТАЛЛА:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        "${reading.confidence}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = getConfidenceColor(reading.confidence)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confidence Progress Bar
                LinearProgressIndicator(
                    progress = { reading.confidence / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = getConfidenceColor(reading.confidence),
                    trackColor = Color(0xFF1F293D)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    reading.statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = getConfidenceColor(reading.confidence),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Ближайшая сторона: ${reading.dominantAxis}",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }

        // 3D Axis mini readouts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AxisMiniCard(label = "X (Бок)", value = reading.x, modifier = Modifier.weight(1f))
            AxisMiniCard(label = "Y (Верх)", value = reading.y, modifier = Modifier.weight(1f))
            AxisMiniCard(label = "Z (Глубина)", value = reading.z, modifier = Modifier.weight(1f))
        }

        // Controls and Tare Action
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tare Button
            Button(
                onClick = { sensorManager.tare() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ОБНУЛИТЬ ФОН (ТАРА)", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sound, Vibrate, Sensitivity row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sound Toggle
                FilterChip(
                    selected = soundEnabled,
                    onClick = {
                        soundEnabled = !soundEnabled
                        feedbackManager.isSoundEnabled = soundEnabled
                    },
                    label = { Text(if (soundEnabled) "Звук: ВКЛ" else "Звук: ВЫКЛ", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Vibration Toggle
                FilterChip(
                    selected = vibrationEnabled,
                    onClick = {
                        vibrationEnabled = !vibrationEnabled
                        feedbackManager.isVibrationEnabled = vibrationEnabled
                    },
                    label = { Text(if (vibrationEnabled) "Вибро: ВКЛ" else "Вибро: ВЫКЛ", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            if (vibrationEnabled) Icons.Default.Vibration else Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            // Sensitivity slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Порог:", fontSize = 11.sp, color = Color.Gray)
                Slider(
                    value = sensitivity,
                    onValueChange = {
                        sensitivity = it
                        feedbackManager.sensitivityThreshold = it
                    },
                    valueRange = 15f..120f,
                    modifier = Modifier.weight(1f)
                )
                Text("${sensitivity.toInt()} μT", fontSize = 11.sp, color = NeonGreen)
            }
        }
    }
}

@Composable
fun OscilloscopeTab(sensorManager: MagneticSensorManager, reading: SensorReading) {
    val history = sensorManager.history

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Grid lines
                    val gridColor = Color(0xFF1E293B)
                    for (i in 1..4) {
                        val y = height * (i / 5f)
                        drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1.dp.toPx())
                    }
                    for (i in 1..6) {
                        val x = width * (i / 7f)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1.dp.toPx())
                    }

                    if (history.size > 1) {
                        val maxVal = (history.maxOrNull() ?: 100f).coerceAtLeast(80f)
                        val stepX = width / (history.size - 1)
                        val path = Path()

                        history.forEachIndexed { index, value ->
                            val normalizedY = height - ((value / maxVal) * height).coerceIn(0f, height)
                            val x = index * stepX
                            if (index == 0) {
                                path.moveTo(x, normalizedY)
                            } else {
                                path.lineTo(x, normalizedY)
                            }
                        }

                        drawPath(
                            path = path,
                            color = NeonGreen,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                Text(
                    "ОСЦИЛЛОГРАФ ПОЛЯ (РЕАЛЬНОЕ ВРЕМЯ)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📈 Показатели графика:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Текущий всплеск: ${reading.delta.toInt()} μT", color = NeonGreen, fontSize = 13.sp)
                Text("• Пик за сессию: ${(history.maxOrNull() ?: 0f).toInt()} μT", color = NeonYellow, fontSize = 13.sp)
                Text("• Стабильность сигнала: ${if (reading.delta < 8f) "Высокая (Шум минимален)" else "Обнаружены магнитные флуктуации"}", color = Color.LightGray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SensorLocatorTab(reading: SensorReading) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "🎯 Как найти точную точку датчика:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "1. Возьми небольшую железную монету или ключ.\n2. Медленно води ей по задней крышке телефона.\n3. В месте, где значение ниже подскакивает до максимума — физически распаян чип магнитометра на плате!",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )
            }
        }

        // Phone Silhouette with responsive heat spot
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF131A29))
                .border(2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Camera module mockup
            Box(
                modifier = Modifier
                    .size(48.dp, 60.dp)
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B))
            )

            // Dynamic Sensor Heat Pulse
            val pulseSize by animateFloatAsState(
                targetValue = (reading.confidence * 2f).coerceIn(30f, 120f),
                animationSpec = tween(150), label = "pulse"
            )

            Box(
                modifier = Modifier
                    .size(pulseSize.dp)
                    .clip(CircleShape)
                    .background(getConfidenceColor(reading.confidence).copy(alpha = 0.4f))
            )

            Text(
                "${reading.delta.toInt()} μT",
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Text(
            "Текущий отклик чипа: ${reading.statusText}",
            fontSize = 13.sp,
            color = getConfidenceColor(reading.confidence),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AnalogGauge(deltaValue: Float, confidence: Int) {
    val animatedAngle by animateFloatAsState(
        targetValue = ((deltaValue / 150f).coerceIn(0f, 1f) * 240f) - 120f,
        animationSpec = tween(100), label = "needleAngle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f - 16.dp.toPx()

        // Background Track Arc
        drawArc(
            color = Color(0xFF1A2338),
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
        )

        // Active Colored Arc (Gradient from Green -> Yellow -> Red)
        drawArc(
            brush = Brush.sweepGradient(
                listOf(NeonGreen, NeonYellow, NeonOrange, NeonRed)
            ),
            startAngle = 150f,
            sweepAngle = ((deltaValue / 150f).coerceIn(0f, 1f) * 240f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
        )

        // Gauge needle pointing from center
        rotate(animatedAngle, pivot = center) {
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(center.x, center.y - radius + 10.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center hub
        drawCircle(color = getConfidenceColor(confidence), radius = 10.dp.toPx(), center = center)
        drawCircle(color = DarkBackground, radius = 5.dp.toPx(), center = center)
    }
}

@Composable
fun AxisMiniCard(label: String, value: Float, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${value.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan
            )
        }
    }
}

fun getConfidenceColor(confidence: Int): Color {
    return when {
        confidence < 20 -> NeonGreen
        confidence < 50 -> NeonYellow
        confidence < 80 -> NeonOrange
        else -> NeonRed
    }
}
