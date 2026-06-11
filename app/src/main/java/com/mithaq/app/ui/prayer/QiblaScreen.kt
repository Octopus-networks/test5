package com.mithaq.app.ui.prayer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.view.Surface
import android.view.WindowManager
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mithaq.app.R
import com.mithaq.app.model.UserProfile
import com.mithaq.app.ui.components.MithaqEmptyState
import com.mithaq.app.util.AdhanScheduler
import android.hardware.GeomagneticField
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    currentUser: UserProfile,
    isArabic: Boolean,
    onOpenPrayerSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var coordinates by remember { mutableStateOf<AdhanScheduler.AdhanCoordinates?>(null) }
    var bearingToKaaba by remember { mutableStateOf(0f) }
    var distanceToKaaba by remember { mutableStateOf(0f) }
    var declination by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        val resolvedCoords = AdhanScheduler.resolveAdhanCoordinates(context, currentUser)
        if (resolvedCoords.lat != 0.0 || resolvedCoords.lng != 0.0) {
            coordinates = resolvedCoords
            val results = FloatArray(3)
            Location.distanceBetween(
                resolvedCoords.lat, resolvedCoords.lng,
                21.422487, 39.826206, // Kaaba coordinates
                results
            )
            distanceToKaaba = results[0] / 1000f // to km
            val b = results[1]
            bearingToKaaba = if (b < 0) b + 360f else b

            val geoField = GeomagneticField(
                resolvedCoords.lat.toFloat(),
                resolvedCoords.lng.toFloat(),
                0f,
                System.currentTimeMillis()
            )
            declination = geoField.declination
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedString(isArabic, R.string.prayer_hub_qibla, R.string.prayer_hub_qibla_ar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        if (coordinates == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                MithaqEmptyState(
                    title = localizedString(isArabic, R.string.prayer_hub_empty_state_title, R.string.prayer_hub_empty_state_title_ar),
                    message = localizedString(isArabic, R.string.prayer_hub_empty_state_message, R.string.prayer_hub_empty_state_message_ar),
                    icon = Icons.Default.Explore,
                    actionLabel = localizedString(isArabic, R.string.prayer_hub_setup_btn, R.string.prayer_hub_setup_btn_ar),
                    onAction = onOpenPrayerSettings
                )
            }
            return@Scaffold
        }

        QiblaCompassContent(
            bearingToKaaba = bearingToKaaba,
            distanceToKaaba = distanceToKaaba,
            declination = declination,
            isArabic = isArabic,
            modifier = modifier.padding(padding)
        )
    }
}

@Composable
private fun QiblaCompassContent(
    bearingToKaaba: Float,
    distanceToKaaba: Float,
    declination: Float,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var needsCalibration by remember { mutableStateOf(false) }
    // Budget devices often ship without a magnetometer; without it (or a rotation
    // vector) the dial can never rotate, so tell the user instead of showing a dead
    // compass.
    var hasCompassSensor by remember { mutableStateOf(true) }
    
    // Sensor values
    var rawAzimuth by remember { mutableStateOf(0f) }
    
    // Smooth rotation unwrap logic to prevent 360-0 snapping
    var rotationValue by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationValue,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "compassRotation"
    )

    // Haptic state edge trigger
    var wasAligned by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
        val windowManager = ContextCompat.getSystemService(context, WindowManager::class.java)
        
        var hasRotationVector = false
        val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var accelValues = FloatArray(3)
        var magValues = FloatArray(3)
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rMatrix = FloatArray(9)
                var success = false

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    // Some Samsung sensors report 5 values; getRotationMatrixFromVector
                    // throws unless the vector is truncated to 4.
                    val vector = if (event.values.size > 4) event.values.copyOf(4) else event.values
                    SensorManager.getRotationMatrixFromVector(rMatrix, vector)
                    success = true
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        accelValues = event.values.clone()
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        magValues = event.values.clone()
                    }
                    if (accelValues.isNotEmpty() && magValues.isNotEmpty()) {
                        success = SensorManager.getRotationMatrix(rMatrix, null, accelValues, magValues)
                    }
                }

                if (success) {
                    val displayRotation = windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
                    val mappedMatrix = FloatArray(9)
                    
                    when (displayRotation) {
                        Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(rMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, mappedMatrix)
                        Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mappedMatrix)
                        Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, mappedMatrix)
                        Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, mappedMatrix)
                        else -> SensorManager.remapCoordinateSystem(rMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, mappedMatrix)
                    }

                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(mappedMatrix, orientation)
                    var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    
                    // Apply magnetic declination to get true north
                    azimuthDeg += declination
                    if (azimuthDeg < 0) azimuthDeg += 360f
                    if (azimuthDeg >= 360f) azimuthDeg -= 360f
                    
                    rawAzimuth = azimuthDeg
                    
                    // Unwrap for smooth animation
                    var diff = azimuthDeg - (rotationValue % 360f)
                    if (diff > 180f) diff -= 360f
                    if (diff < -180f) diff += 360f
                    rotationValue += diff
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    needsCalibration = accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW || 
                                       accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                }
            }
        }

        hasCompassSensor = rotationVectorSensor != null || (accelSensor != null && magSensor != null)

        if (rotationVectorSensor != null) {
            hasRotationVector = true
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Determine alignment
    val diff = abs(rawAzimuth - bearingToKaaba)
    val angularDiff = min(diff, 360f - diff)
    val isAligned = angularDiff <= 3f

    // Edge-triggered haptic
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            wasAligned = true
        } else if (!isAligned && wasAligned) {
            wasAligned = false
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        if (!hasCompassSensor) {
            Text(
                text = localizedString(isArabic, R.string.qibla_no_sensor, R.string.qibla_no_sensor_ar),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Compass Display
        val compassColor = if (isAligned) Color(0xFFF2CA50) else MaterialTheme.colorScheme.onSurfaceVariant
        val needleColor = if (isAligned) Color(0xFFF2CA50) else MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2
                
                // Draw outer ring
                drawCircle(
                    color = compassColor.copy(alpha = 0.2f),
                    radius = radius,
                    style = Stroke(width = 8.dp.toPx())
                )

                // Rotate the dial to reflect true north
                rotate(degrees = -animatedRotation, pivot = center) {
                    // Ticks and Cardinals
                    for (i in 0 until 360 step 15) {
                        val angleRad = Math.toRadians((i - 90).toDouble())
                        val isCardinal = i % 90 == 0
                        val lineLength = if (isCardinal) 16.dp.toPx() else 8.dp.toPx()
                        
                        val startX = center.x + (radius - lineLength) * cos(angleRad).toFloat()
                        val startY = center.y + (radius - lineLength) * sin(angleRad).toFloat()
                        val endX = center.x + radius * cos(angleRad).toFloat()
                        val endY = center.y + radius * sin(angleRad).toFloat()
                        
                        drawLine(
                            color = compassColor.copy(alpha = if (isCardinal) 0.8f else 0.4f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isCardinal) 4.dp.toPx() else 2.dp.toPx()
                        )

                        if (isCardinal) {
                            val letter = when (i) {
                                0 -> if (isArabic) "ش" else "N"
                                90 -> if (isArabic) "ق" else "E"
                                180 -> if (isArabic) "ج" else "S"
                                270 -> if (isArabic) "غ" else "W"
                                else -> ""
                            }
                            val textLayoutResult = textMeasurer.measure(
                                text = letter,
                                style = TextStyle(
                                    color = compassColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val textRadius = radius - 36.dp.toPx()
                            val textX = center.x + textRadius * cos(angleRad).toFloat() - textLayoutResult.size.width / 2
                            val textY = center.y + textRadius * sin(angleRad).toFloat() - textLayoutResult.size.height / 2
                            
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(textX, textY)
                            )
                        }
                    }

                    // Kaaba Marker
                    val kaabaRad = Math.toRadians((bearingToKaaba - 90).toDouble())
                    val kaabaX = center.x + (radius - 8.dp.toPx()) * cos(kaabaRad).toFloat()
                    val kaabaY = center.y + (radius - 8.dp.toPx()) * sin(kaabaRad).toFloat()
                    
                    val kaabaPath = Path().apply {
                        val markerSize = 12.dp.toPx()
                        moveTo(kaabaX, kaabaY - markerSize)
                        lineTo(kaabaX + markerSize, kaabaY)
                        lineTo(kaabaX, kaabaY + markerSize)
                        lineTo(kaabaX - markerSize, kaabaY)
                        close()
                    }
                    drawPath(path = kaabaPath, color = Color(0xFFF2CA50))
                }

                // Fixed Center Needle Pointing Up
                val needlePath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.7f)
                    lineTo(center.x + 12.dp.toPx(), center.y + 20.dp.toPx())
                    lineTo(center.x, center.y + 10.dp.toPx())
                    lineTo(center.x - 12.dp.toPx(), center.y + 20.dp.toPx())
                    close()
                }
                drawPath(path = needlePath, color = needleColor)
                drawCircle(color = needleColor, radius = 6.dp.toPx(), center = center)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Text
        if (isAligned) {
            Text(
                text = localizedString(isArabic, R.string.qibla_facing_correct, R.string.qibla_facing_correct_ar),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFF2CA50),
                fontWeight = FontWeight.Bold
            )
        } else {
            Spacer(modifier = Modifier.height(28.dp)) // Maintain layout height
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            val distLabel = String.format(Locale.US, "%,.0f", distanceToKaaba)
            val bearLabel = String.format(Locale.US, "%.0f°", bearingToKaaba)
            Text(
                text = if (isArabic) "اتجاه القبلة: $bearLabel — المسافة: $distLabel كم" 
                       else "Qibla: $bearLabel — Distance: $distLabel km",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (needsCalibration) {
            Text(
                text = localizedString(isArabic, R.string.qibla_calibrate, R.string.qibla_calibrate_ar),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun localizedString(isArabic: Boolean, englishResId: Int, arabicResId: Int): String {
    return stringResource(id = if (isArabic) arabicResId else englishResId)
}
