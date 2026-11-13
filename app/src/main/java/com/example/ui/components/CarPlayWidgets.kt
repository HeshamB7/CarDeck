package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.DriverAssistUtils
import com.example.model.CarWidgetType
import com.example.model.DashboardWidgetItem
import com.example.model.LauncherPreferences
import com.example.model.MediaPlaybackState
import com.example.model.QuickContact
import com.example.model.SpeedUnit
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.ui.theme.CarDisplayFontFamily
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

val AppleWidgetCornerRadius = 24.dp

@Composable
fun CarPlayWidgetRenderer(
    item: DashboardWidgetItem,
    preferences: LauncherPreferences,
    telemetry: TelemetryState,
    systemStatus: SystemStatusState,
    mediaState: MediaPlaybackState,
    isEditMode: Boolean,
    isSelectedForSwap: Boolean,
    isHighwayFocusActive: Boolean,
    isBreakReminderDue: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onDelete: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onTapInEditMode: () -> Unit,
    onSearchNavigate: (String) -> Unit,
    onOpenFullMap: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrevious: () -> Unit,
    onOpenMediaApp: () -> Unit,
    onGrantMediaAccess: () -> Unit,
    onGrantLocation: () -> Unit,
    onGrantCalendar: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenHotspot: () -> Unit,
    onOpenSplitApps: () -> Unit,
    onToggleHighwayFocus: () -> Unit,
    onParkedPinClick: () -> Unit,
    onClearParkedPin: () -> Unit,
    onResetTripTimer: () -> Unit,
    onRefreshWeather: () -> Unit,
    onDialContact: (QuickContact) -> Unit,
    onEditQuickContact: (QuickContact) -> Unit,
    modifier: Modifier = Modifier
) {
    WidgetEditableWrapper(
        widgetId = item.id,
        isEditMode = isEditMode,
        isSelectedForSwap = isSelectedForSwap,
        canMoveEarlier = canMoveEarlier,
        canMoveLater = canMoveLater,
        onDelete = onDelete,
        onMoveEarlier = onMoveEarlier,
        onMoveLater = onMoveLater,
        onTapToSwap = onTapInEditMode,
        modifier = modifier
    ) {
        val widgetRadiusDp = 24f
        val hasParkedPin = preferences.parkedLatitude != null && preferences.parkedLongitude != null
        val parkedAgeMins = preferences.parkedTimestampMs?.let { ts ->
            DriverAssistUtils.formatParkedAgeMinutes(ts, System.currentTimeMillis())
        }

        when (item.type) {
            CarWidgetType.NAVIGATION_MAP -> NavigationMapCard(
                telemetry = telemetry,
                navProvider = preferences.navProvider,
                homeAddress = preferences.homeAddress,
                workAddress = preferences.workAddress,
                speedLimitKmh = preferences.speedLimitKmh,
                speedGuardEnabled = preferences.speedGuardEnabled,
                speedUnit = preferences.speedUnit,
                cornerRadiusDp = widgetRadiusDp,
                cardAlpha = preferences.cardAlpha,
                hasParkedPin = hasParkedPin,
                parkedAgeMinutes = parkedAgeMins,
                onSearchNavigate = onSearchNavigate,
                onOpenFullMap = onOpenFullMap,
                onParkedPinClick = onParkedPinClick,
                onClearParkedPin = onClearParkedPin,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.MINI_MAP -> MiniMapWidgetCard(
                telemetry = telemetry,
                speedLimitKmh = preferences.speedLimitKmh,
                speedUnit = preferences.speedUnit,
                cardAlpha = preferences.cardAlpha,
                onOpenFullMap = onOpenFullMap,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.TURN_SPEED -> TurnGuidanceSpeedCard(
                telemetry = telemetry,
                speedUnit = preferences.speedUnit,
                speedLimitKmh = preferences.speedLimitKmh,
                speedGuardEnabled = preferences.speedGuardEnabled,
                cornerRadiusDp = widgetRadiusDp,
                cardAlpha = preferences.cardAlpha,
                onGrantLocation = onGrantLocation,
                onOpenMap = onOpenFullMap,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.MEDIA_PLAYER -> UniversalMediaCard(
                mediaState = mediaState,
                cornerRadiusDp = widgetRadiusDp,
                cardAlpha = preferences.cardAlpha,
                onPlayPause = onMediaPlayPause,
                onNext = onMediaNext,
                onPrevious = onMediaPrevious,
                onOpenMediaApp = onOpenMediaApp,
                onGrantMediaAccess = onGrantMediaAccess,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.CALENDAR_AGENDA -> SmartStackCard(
                telemetry = telemetry,
                systemStatus = systemStatus,
                isHighwayFocusActive = isHighwayFocusActive,
                isBreakReminderDue = isBreakReminderDue,
                cornerRadiusDp = widgetRadiusDp,
                cardAlpha = preferences.cardAlpha,
                onGrantCalendar = onGrantCalendar,
                onOpenBluetooth = onOpenBluetooth,
                onOpenWifi = onOpenWifi,
                onToggleHighwayFocus = onToggleHighwayFocus,
                onOpenSplitApps = onOpenSplitApps,
                onFindRestStop = { onSearchNavigate("coffee rest stop") },
                onResetTripTimer = onResetTripTimer,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.WEATHER -> WeatherWidgetCard(
                telemetry = telemetry,
                speedUnit = preferences.speedUnit,
                cardAlpha = preferences.cardAlpha,
                onGrantLocation = onGrantLocation,
                onRefresh = onRefreshWeather,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.DIGITAL_CLOCK -> DigitalClockWidgetCard(
                telemetry = telemetry,
                cardAlpha = preferences.cardAlpha,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.ANALOG_CLOCK -> AnalogClockWidgetCard(
                cardAlpha = preferences.cardAlpha,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.SPEED_CONTACTS -> QuickDialContactsWidgetCard(
                contacts = preferences.quickContacts,
                cardAlpha = preferences.cardAlpha,
                isEditMode = isEditMode,
                onDialContact = onDialContact,
                onEditContact = onEditQuickContact,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.VEHICLE_BATTERY -> VehicleBatteryWidgetCard(
                telemetry = telemetry,
                systemStatus = systemStatus,
                hasParkedPin = hasParkedPin,
                cardAlpha = preferences.cardAlpha,
                onOpenBluetooth = onOpenBluetooth,
                onOpenWifi = onOpenWifi,
                onParkedPinClick = onParkedPinClick,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.QUICK_CONTROLS -> QuickVehicleControlsWidgetCard(
                systemStatus = systemStatus,
                isHighwayFocusActive = isHighwayFocusActive,
                cardAlpha = preferences.cardAlpha,
                onOpenBluetooth = onOpenBluetooth,
                onOpenWifi = onOpenWifi,
                onOpenHotspot = onOpenHotspot,
                onOpenSplitApps = onOpenSplitApps,
                onToggleHighwayFocus = onToggleHighwayFocus,
                modifier = Modifier.fillMaxSize()
            )
            CarWidgetType.TRIP_ODOMETER -> TripOdometerWidgetCard(
                telemetry = telemetry,
                speedUnit = preferences.speedUnit,
                cardAlpha = preferences.cardAlpha,
                onGrantLocation = onGrantLocation,
                onResetTrip = onResetTripTimer,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun WidgetEditableWrapper(
    widgetId: String,
    isEditMode: Boolean,
    isSelectedForSwap: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onDelete: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onTapToSwap: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = LocalCarTokens.current
    val jiggleTransition = rememberInfiniteTransition(label = "jiggle_$widgetId")
    val jiggleAngle by jiggleTransition.animateFloat(
        initialValue = -0.65f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 155),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggle_angle"
    )

    Box(
        modifier = modifier
            .rotate(if (isEditMode && !isSelectedForSwap) jiggleAngle else 0f)
    ) {
        content()

        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppleWidgetCornerRadius))
                    .border(
                        width = if (isSelectedForSwap) 2.5.dp else 1.25.dp,
                        color = if (isSelectedForSwap) tokens.accentBlue else tokens.primaryText.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(AppleWidgetCornerRadius)
                    )
                    .background(
                        if (isSelectedForSwap) tokens.accentBlue.copy(alpha = 0.14f)
                        else Color.Black.copy(alpha = 0.08f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTapToSwap
                    )
            )

            // Top-left Apple-style circular remove (-) badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(28.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(tokens.alertRed)
                    .testTag("widget_delete_$widgetId")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDelete
                    ),
                contentAlignment = Alignment.Center
            ) {
                CarPlaySymbol(
                    type = CarPlaySymbolType.MINUS,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Bottom floating frosted reorder bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(tokens.cardBackground.copy(alpha = 0.92f))
                    .border(0.75.dp, tokens.cardBorder, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (canMoveEarlier) tokens.pillBackground else Color.Transparent
                        )
                        .clickable(
                            enabled = canMoveEarlier,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMoveEarlier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CarPlaySymbol(
                        type = CarPlaySymbolType.ARROW_LEFT,
                        tint = if (canMoveEarlier) tokens.primaryText else tokens.secondaryText.copy(alpha = 0.35f),
                        modifier = Modifier.size(13.dp)
                    )
                }

                CarPlayText(
                    text = if (isSelectedForSwap) stringResource(R.string.widget_tap_swap_target)
                    else stringResource(R.string.widget_tap_to_move),
                    style = LocalCarPlayTypography.current.micro,
                    color = if (isSelectedForSwap) tokens.accentBlue else tokens.secondaryText
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (canMoveLater) tokens.pillBackground else Color.Transparent
                        )
                        .clickable(
                            enabled = canMoveLater,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMoveLater
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CarPlaySymbol(
                        type = CarPlaySymbolType.ARROW_RIGHT,
                        tint = if (canMoveLater) tokens.primaryText else tokens.secondaryText.copy(alpha = 0.35f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniMapWidgetCard(
    telemetry: TelemetryState,
    speedLimitKmh: Int,
    speedUnit: SpeedUnit,
    cardAlpha: Float,
    onOpenFullMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val isNight = tokens.isNight
    val displayLimit = if (speedUnit == SpeedUnit.MPH) {
        (speedLimitKmh * 0.621371f).roundToInt()
    } else speedLimitKmh

    CarPlayGlassCard(
        cornerRadius = AppleWidgetCornerRadius,
        cardAlpha = cardAlpha,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onOpenFullMap
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val bgColor = if (isNight) Color(0xFF181C24) else Color(0xFFF4F2EA)
                        val parkColor = if (isNight) Color(0xFF182C22) else Color(0xFFD2EAD0)
                        val roadColor = if (isNight) Color(0xFF2D3442) else Color.White
                        val roadWidth = 3.dp.toPx()
                        val route = Path().apply {
                            moveTo(w * 0.50f, h * 0.88f)
                            lineTo(w * 0.50f, h * 0.44f)
                            quadraticBezierTo(w * 0.50f, h * 0.24f, w * 0.76f, h * 0.16f)
                        }
                        val routeCasing = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
                        val routeStroke = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
                        val c = Offset(w * 0.50f, h * 0.54f)
                        val arrow = Path().apply {
                            moveTo(c.x, c.y - 4.5.dp.toPx())
                            lineTo(c.x + 3.5.dp.toPx(), c.y + 3.5.dp.toPx())
                            lineTo(c.x, c.y + 1.2.dp.toPx())
                            lineTo(c.x - 3.5.dp.toPx(), c.y + 3.5.dp.toPx())
                            close()
                        }
                        val r1 = 19.dp.toPx()
                        val r2 = 10.dp.toPx()
                        val r3 = 7.5.dp.toPx()

                        onDrawBehind {
                            drawRect(bgColor)
                            drawRect(parkColor, Offset(w * 0.06f, h * 0.14f), Size(w * 0.30f, h * 0.28f))
                            drawLine(roadColor, Offset(0f, h * 0.34f), Offset(w, h * 0.34f), strokeWidth = roadWidth)
                            drawLine(roadColor, Offset(0f, h * 0.68f), Offset(w, h * 0.68f), strokeWidth = roadWidth)
                            drawLine(roadColor, Offset(w * 0.32f, 0f), Offset(w * 0.32f, h), strokeWidth = roadWidth)
                            drawLine(roadColor, Offset(w * 0.72f, 0f), Offset(w * 0.72f, h), strokeWidth = roadWidth)
                            drawPath(route, Color(0xFF0051B3), style = routeCasing)
                            drawPath(route, tokens.accentBlue, style = routeStroke)
                            drawCircle(tokens.accentBlue.copy(alpha = 0.22f), radius = r1, center = c)
                            drawCircle(Color.White, radius = r2, center = c)
                            drawCircle(tokens.accentBlue, radius = r3, center = c)
                            rotate(degrees = telemetry.bearingDegrees, pivot = c) {
                                drawPath(arrow, Color.White)
                            }
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tokens.cardBackground.copy(alpha = 0.90f))
                        .border(0.75.dp, tokens.cardBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.hasGpsFix) tokens.positiveGreen else tokens.warningOrange)
                    )
                    CarPlayText(
                        text = stringResource(R.string.widget_mini_map),
                        style = type.micro,
                        color = tokens.primaryText
                    )
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White)
                        .border(1.2.dp, Color(0xFF1C1C1E), RoundedCornerShape(7.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CarPlayText("LIMIT", type.micro.copy(fontSize = 7.sp, lineHeight = 8.sp), Color(0xFF1C1C1E))
                    CarPlayText(displayLimit.toString(), type.controlLabel.copy(fontWeight = FontWeight.Black), Color.Black)
                }
            }
        }
    }
}

@Composable
fun WeatherWidgetCard(
    telemetry: TelemetryState,
    speedUnit: SpeedUnit,
    cardAlpha: Float,
    onGrantLocation: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val weather = telemetry.weather
    val useFahrenheit = speedUnit == SpeedUnit.MPH

    fun formatTemp(c: Float): String {
        val v = if (useFahrenheit) (c * 9f / 5f) + 32f else c
        return "${v.roundToInt()}°"
    }

    CarPlayGlassCard(
        cornerRadius = AppleWidgetCornerRadius,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (tokens.isNight) {
                            listOf(Color(0x6611243E), Color(0x440D1B2A))
                        } else {
                            listOf(Color(0x55388BFD), Color(0x2268B5FF))
                        }
                    )
                )
                .padding(14.dp)
        ) {
            val isCompact = maxHeight < 150.dp
            if (!telemetry.hasLocationPermission) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CarPlaySymbol(CarPlaySymbolType.SUN, tokens.warningOrange, Modifier.size(22.dp))
                        CarPlayText(stringResource(R.string.widget_weather), type.rowTitle, tokens.primaryText)
                    }
                    CarPlayText(
                        text = stringResource(R.string.weather_location_needed),
                        style = type.bodySecondary,
                        color = tokens.secondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    CarPlayButton(onClick = onGrantLocation) {
                        CarPlayText(stringResource(R.string.action_grant), type.controlLabel, Color.White)
                    }
                }
            } else if (!weather.hasData) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CarPlayText(stringResource(R.string.widget_weather), type.rowTitle, tokens.primaryText)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(tokens.pillBackground)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onRefresh
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CarPlaySymbol(CarPlaySymbolType.REFRESH, tokens.accentBlue, Modifier.size(15.dp))
                        }
                    }
                    CarPlayText(
                        text = if (weather.isLoading) stringResource(R.string.weather_loading)
                        else stringResource(R.string.weather_waiting_gps),
                        style = type.bodySecondary,
                        color = tokens.secondaryText
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            CarPlayText(
                                text = formatTemp(weather.temperatureC),
                                style = type.displaySpeed.copy(
                                    fontSize = if (isCompact) 36.sp else 46.sp,
                                    lineHeight = if (isCompact) 40.sp else 50.sp
                                ),
                                color = tokens.primaryText
                            )
                            CarPlayText(
                                text = weatherCodeDescription(weather.weatherCode),
                                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = tokens.secondaryText
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val iconType = when {
                                weather.weatherCode >= 51 -> CarPlaySymbolType.WEATHER_RAIN
                                weather.weatherCode in 1..48 -> CarPlaySymbolType.WEATHER_CLOUD
                                !weather.isDaylight -> CarPlaySymbolType.MOON
                                else -> CarPlaySymbolType.SUN
                            }
                            val iconColor = when (iconType) {
                                CarPlaySymbolType.SUN -> tokens.warningOrange
                                CarPlaySymbolType.WEATHER_RAIN -> tokens.accentBlue
                                else -> tokens.primaryText
                            }
                            CarPlaySymbol(iconType, iconColor, Modifier.size(30.dp))
                            CarPlayText(
                                text = "H:${formatTemp(weather.highTempC)}  L:${formatTemp(weather.lowTempC)}",
                                style = type.micro.copy(fontFeatureSettings = "tnum"),
                                color = tokens.secondaryText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherStatChip(
                            label = stringResource(R.string.weather_wind),
                            value = "${weather.windSpeedKmh.roundToInt()} km/h"
                        )
                        WeatherStatChip(
                            label = stringResource(R.string.weather_precip),
                            value = "${weather.precipChancePercent}%"
                        )
                        WeatherStatChip(
                            label = stringResource(R.string.weather_feels_like),
                            value = formatTemp(weather.apparentTempC)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherStatChip(label: String, value: String) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tokens.pillBackground.copy(alpha = 0.65f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        CarPlayText(label, type.micro, tokens.secondaryText)
        CarPlayText(value, type.caption.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), tokens.primaryText)
    }
}

@Composable
fun DigitalClockWidgetCard(
    telemetry: TelemetryState,
    cardAlpha: Float,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.US) }
    val amPmFmt = remember { SimpleDateFormat("a", Locale.US) }
    val dateLineFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val date = remember(nowMs) { Date(nowMs) }
    val timeText = remember(nowMs) { timeFmt.format(date) }
    val amPm = remember(nowMs) { amPmFmt.format(date) }
    val dateLine = remember(nowMs) { dateLineFmt.format(date) }
    val elapsedMin = ((nowMs - telemetry.tripStartTimeMs).coerceAtLeast(0L) / 60_000L)

    CarPlayGlassCard(
        cornerRadius = AppleWidgetCornerRadius,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            val isCompact = maxHeight < 150.dp
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarPlayText(
                        text = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT),
                        style = type.micro,
                        color = tokens.accentBlue
                    )
                    CarPlayText(
                        text = amPm,
                        style = type.caption.copy(fontWeight = FontWeight.Bold),
                        color = tokens.secondaryText
                    )
                }

                CarPlayText(
                    text = timeText,
                    style = type.displaySpeed.copy(
                        fontSize = if (isCompact) 42.sp else 54.sp,
                        lineHeight = if (isCompact) 44.sp else 56.sp
                    ),
                    color = tokens.primaryText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarPlayText(
                        text = dateLine,
                        style = type.bodySecondary.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    CarPlayText(
                        text = "${elapsedMin}m",
                        style = type.micro.copy(fontFeatureSettings = "tnum"),
                        color = tokens.positiveGreen
                    )
                }
            }
        }
    }
}

@Composable
fun AnalogClockWidgetCard(
    cardAlpha: Float,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val textMeasurer = rememberTextMeasurer()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val cal = remember { Calendar.getInstance() }.apply { timeInMillis = nowMs }
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val cityCode = remember {
        TimeZone.getDefault().id.substringAfterLast('/').replace('_', ' ')
    }

    CarPlayGlassCard(
        cornerRadius = AppleWidgetCornerRadius,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val dialSize = min(maxWidth.value, maxHeight.value * 0.84f).dp.coerceIn(96.dp, 196.dp)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val faceColor = if (tokens.isNight) Color(0xFF141416) else Color(0xFFFDFDFD)
                    val tickColor = if (tokens.isNight) Color.White else Color(0xFF1C1C1E)
                    val handColor = if (tokens.isNight) Color.White else Color(0xFF111111)
                    val secondColor = tokens.warningOrange

                    Box(
                        modifier = Modifier
                            .size(dialSize)
                            .drawWithCache {
                                val r = size.minDimension / 2f
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val borderStroke = Stroke(width = 1.2.dp.toPx())
                                val hourTickW = 2.dp.toPx()
                                val minTickW = 1.dp.toPx()
                                val numStyle = TextStyle(
                                    fontFamily = CarDisplayFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (r * 0.17f).toSp(),
                                    color = tickColor
                                )

                                val ticks = (0 until 60).map { i ->
                                    val angleRad = (i * 6.0 - 90.0) * PI / 180.0
                                    val isHour = i % 5 == 0
                                    val inner = r * (if (isHour) 0.84f else 0.90f)
                                    val outer = r * 0.95f
                                    Triple(
                                        Offset(c.x + (cos(angleRad) * inner).toFloat(), c.y + (sin(angleRad) * inner).toFloat()),
                                        Offset(c.x + (cos(angleRad) * outer).toFloat(), c.y + (sin(angleRad) * outer).toFloat()),
                                        isHour
                                    )
                                }

                                val measuredNumbers = (1..12).map { hNum ->
                                    val a = (hNum * 30.0 - 90.0) * PI / 180.0
                                    val numR = r * 0.69f
                                    val measured = textMeasurer.measure(hNum.toString(), numStyle)
                                    val tx = c.x + (cos(a) * numR).toFloat() - measured.size.width / 2f
                                    val ty = c.y + (sin(a) * numR).toFloat() - measured.size.height / 2f
                                    measured to Offset(tx, ty)
                                }

                                val hourHandW = 3.8.dp.toPx()
                                val minHandW = 2.6.dp.toPx()
                                val secHandW = 1.4.dp.toPx()
                                val secCenterR = 3.dp.toPx()

                                onDrawBehind {
                                    drawCircle(color = faceColor, radius = r, center = c)
                                    drawCircle(color = tokens.cardBorder, radius = r, center = c, style = borderStroke)

                                    ticks.forEach { (start, end, isHour) ->
                                        drawLine(
                                            color = if (isHour) tickColor else tickColor.copy(alpha = 0.38f),
                                            start = start,
                                            end = end,
                                            strokeWidth = if (isHour) hourTickW else minTickW,
                                            cap = StrokeCap.Round
                                        )
                                    }

                                    measuredNumbers.forEach { (layout, pos) ->
                                        drawText(layout, topLeft = pos)
                                    }

                                    val hourAngle = ((hour + minute / 60f) * 30f - 90f) * (PI / 180.0)
                                    drawLine(
                                        color = handColor,
                                        start = c,
                                        end = Offset(c.x + (cos(hourAngle) * r * 0.46f).toFloat(), c.y + (sin(hourAngle) * r * 0.46f).toFloat()),
                                        strokeWidth = hourHandW,
                                        cap = StrokeCap.Round
                                    )

                                    val minAngle = ((minute + second / 60f) * 6f - 90f) * (PI / 180.0)
                                    drawLine(
                                        color = handColor,
                                        start = c,
                                        end = Offset(c.x + (cos(minAngle) * r * 0.70f).toFloat(), c.y + (sin(minAngle) * r * 0.70f).toFloat()),
                                        strokeWidth = minHandW,
                                        cap = StrokeCap.Round
                                    )

                                    val secAngle = (second * 6f - 90f) * (PI / 180.0)
                                    drawLine(
                                        color = secondColor,
                                        start = Offset(c.x - (cos(secAngle) * r * 0.16f).toFloat(), c.y - (sin(secAngle) * r * 0.16f).toFloat()),
                                        end = Offset(c.x + (cos(secAngle) * r * 0.80f).toFloat(), c.y + (sin(secAngle) * r * 0.80f).toFloat()),
                                        strokeWidth = secHandW,
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(color = secondColor, radius = secCenterR, center = c)
                                }
                            }
                    )
                }

                CarPlayText(
                    text = cityCode,
                    style = type.micro,
                    color = tokens.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QuickDialContactsWidgetCard(
    contacts: List<QuickContact>,
    cardAlpha: Float,
    isEditMode: Boolean,
    onDialContact: (QuickContact) -> Unit,
    onEditContact: (QuickContact) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val displayed = contacts.take(4)

    CarPlayGlassCard(
        cornerRadius = AppleWidgetCornerRadius,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CarPlaySymbol(CarPlaySymbolType.PHONE, tokens.positiveGreen, Modifier.size(16.dp))
                    CarPlayText(stringResource(R.string.widget_speed_contacts), type.controlLabel, tokens.primaryText)
                }
                CarPlayText(
                    text = stringResource(R.string.contacts_tap_to_call),
                    style = type.micro,
                    color = tokens.secondaryText
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (rowIdx in 0..1) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (colIdx in 0..1) {
                            val contact = displayed.getOrNull(rowIdx * 2 + colIdx)
                            if (contact != null) {
                                val hasNumber = contact.phoneNumber.isNotBlank()
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (hasNumber) tokens.positiveGreen.copy(alpha = 0.16f)
                                            else tokens.pillBackground.copy(alpha = 0.72f)
                                        )
                                        .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (isEditMode || !hasNumber) {
                                                onEditContact(contact)
                                            } else {
                                                onDialContact(contact)
                                            }
                                        }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hasNumber) tokens.positiveGreen else tokens.secondaryText.copy(alpha = 0.35f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CarPlaySymbol(
                                            type = if (hasNumber) CarPlaySymbolType.PHONE else CarPlaySymbolType.PLUS,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        CarPlayText(
                                            text = contact.name,
                                            style = type.caption.copy(fontWeight = FontWeight.Bold),
                                            color = tokens.primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        CarPlayText(
                                            text = if (hasNumber) contact.phoneNumber else stringResource(R.string.contacts_set_number),
                                            style = type.micro.copy(fontFeatureSettings = "tnum"),
                                            color = tokens.secondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(tokens.pillBackground)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onEditContact(contact) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CarPlaySymbol(CarPlaySymbolType.PENCIL, tokens.secondaryText, Modifier.size(11.dp))
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleBatteryWidgetCard(
    telemetry: TelemetryState,
    systemStatus: SystemStatusState,
    hasParkedPin: Boolean,
    cardAlpha: Float,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit,
    onParkedPinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val batteryColor = when {
        telemetry.isCharging -> tokens.positiveGreen
        telemetry.batteryPercent <= 20 -> tokens.alertRed
        telemetry.batteryPercent <= 40 -> tokens.warningOrange
        else -> tokens.positiveGreen
    }

    CarPlayGlassCard(
        cornerRadius = AppleWidgetCornerRadius,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CarPlaySymbol(
                        type = if (telemetry.isCharging) CarPlaySymbolType.BOLT else CarPlaySymbolType.BATTERY,
                        tint = batteryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    CarPlayText(
                        text = stringResource(R.string.widget_vehicle_battery),
                        style = type.controlLabel,
                        color = tokens.primaryText
                    )
                }
                CarPlayText(
                    text = "${telemetry.batteryTempC.roundToInt()}°C",
                    style = type.micro.copy(fontFeatureSettings = "tnum"),
                    color = tokens.secondaryText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CarPlayText(
                    text = "${telemetry.batteryPercent}%",
                    style = type.displaySpeed.copy(fontSize = 36.sp, lineHeight = 40.sp),
                    color = batteryColor
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(tokens.secondaryText.copy(alpha = 0.22f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((telemetry.batteryPercent / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(batteryColor)
                        )
                    }
                    CarPlayText(
                        text = if (telemetry.isCharging) stringResource(R.string.vehicle_charging)
                        else stringResource(R.string.vehicle_on_battery),
                        style = type.caption,
                        color = tokens.secondaryText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPillChip(
                    symbol = CarPlaySymbolType.BLUETOOTH,
                    label = if (systemStatus.isBluetoothEnabled) "BT ON" else "BT OFF",
                    active = systemStatus.isBluetoothEnabled,
                    activeColor = tokens.accentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBluetooth
                )
                StatusPillChip(
                    symbol = CarPlaySymbolType.WIFI,
                    label = if (systemStatus.isWifiConnected) "Wi-Fi" else "Offline",
                    active = systemStatus.isWifiConnected,
                    activeColor = tokens.positiveGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWifi
                )
                StatusPillChip(
                    symbol = CarPlaySymbolType.LOCATION_PIN,
                    label = if (hasParkedPin) stringResource(R.string.parked_pin_go)
                    else stringResource(R.string.parked_pin_save),
                    active = hasParkedPin,
                    activeColor = tokens.accentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onParkedPinClick
                )
            }
        }
    }
}

@Composable
private fun StatusPillChip(
    symbol: CarPlaySymbolType,
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) activeColor.copy(alpha = 0.20f) else tokens.pillBackground.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CarPlaySymbol(symbol, if (active) activeColor else tokens.secondaryText, Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        CarPlayText(
            text = label,
            style = type.micro,
            color = tokens.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun QuickVehicleControlsWidgetCard(
    systemStatus: SystemStatusState,
    isHighwayFocusActive: Boolean,
    cardAlpha: Float,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenHotspot: () -> Unit,
    onOpenSplitApps: () -> Unit,
    onToggleHighwayFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    CarPlayGlassCard(
        cornerRadius = AppleWidgetCornerRadius,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarPlayText(stringResource(R.string.stack_controls_title), type.controlLabel, tokens.primaryText)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isHighwayFocusActive) tokens.warningOrange.copy(alpha = 0.24f) else tokens.pillBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleHighwayFocus
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CarPlaySymbol(CarPlaySymbolType.MOON, if (isHighwayFocusActive) tokens.warningOrange else tokens.secondaryText, Modifier.size(11.dp))
                    CarPlayText(stringResource(R.string.control_highway_focus), type.micro, tokens.primaryText)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetControlBtn(CarPlaySymbolType.BLUETOOTH, stringResource(R.string.control_bluetooth), systemStatus.isBluetoothEnabled, tokens.accentBlue, Modifier.weight(1f), onOpenBluetooth)
                    WidgetControlBtn(CarPlaySymbolType.WIFI, stringResource(R.string.control_wifi), systemStatus.isWifiConnected, tokens.positiveGreen, Modifier.weight(1f), onOpenWifi)
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetControlBtn(CarPlaySymbolType.HOTSPOT, stringResource(R.string.control_hotspot), false, tokens.warningOrange, Modifier.weight(1f), onOpenHotspot)
                    WidgetControlBtn(CarPlaySymbolType.SPLIT_SCREEN, stringResource(R.string.control_split_screen), true, tokens.accentBlue, Modifier.weight(1f), onOpenSplitApps)
                }
            }
        }
    }
}

@Composable
private fun WidgetControlBtn(
    symbol: CarPlaySymbolType,
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(13.dp))
            .background(if (active) activeColor.copy(alpha = 0.22f) else tokens.pillBackground.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CarPlaySymbol(symbol, if (active) activeColor else tokens.secondaryText, Modifier.size(16.dp))
        CarPlayText(label, type.caption.copy(fontWeight = FontWeight.SemiBold), tokens.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TripOdometerWidgetCard(
    telemetry: TelemetryState,
    speedUnit: SpeedUnit,
    cardAlpha: Float,
    onGrantLocation: () -> Unit,
    onResetTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    var elapsedSec by remember { mutableLongStateOf(0L) }
    LaunchedEffect(telemetry.tripStartTimeMs) {
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - telemetry.tripStartTimeMs) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }
    val factor = if (speedUnit == SpeedUnit.MPH) 0.621371f else 1f
    val unitStr = stringResource(if (speedUnit == SpeedUnit.MPH) R.string.stack_unit_mph else R.string.stack_unit_kmh)
    val curSpeed = (telemetry.speedKmh * factor).roundToInt()
    val peakSpeed = (telemetry.peakSpeedKmh * factor).roundToInt()
    val tripFmt = "%02d:%02d".format(elapsedSec / 60L, elapsedSec % 60L)
    val heading = remember(telemetry.bearingDegrees) {
        DriverAssistUtils.formatCardinalHeading(telemetry.bearingDegrees)
    }

    CarPlayGlassCard(
        cornerRadius = AppleWidgetCornerRadius,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CarPlaySymbol(CarPlaySymbolType.SPEEDOMETER, tokens.accentBlue, Modifier.size(18.dp))
                    CarPlayText(stringResource(R.string.widget_trip_odometer), type.controlLabel, tokens.primaryText)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (telemetry.hasGpsFix) {
                        CarPlayText(heading, type.micro, tokens.accentBlue)
                    }
                    if (!telemetry.hasLocationPermission) {
                        CarPlayButton(onClick = onGrantLocation) {
                            CarPlayText(stringResource(R.string.action_grant), type.micro, Color.White)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(tokens.pillBackground)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onResetTrip
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CarPlaySymbol(CarPlaySymbolType.REFRESH, tokens.secondaryText, Modifier.size(13.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    CarPlayText(stringResource(R.string.stack_trip_elapsed), type.micro, tokens.secondaryText)
                    CarPlayText(tripFmt, type.headline.copy(fontFeatureSettings = "tnum"), tokens.primaryText)
                }
                Column {
                    CarPlayText(stringResource(R.string.trip_current_speed), type.micro, tokens.secondaryText)
                    CarPlayText("$curSpeed $unitStr", type.headline.copy(fontFeatureSettings = "tnum"), tokens.accentBlue)
                }
                Column {
                    CarPlayText(stringResource(R.string.trip_peak_speed), type.micro, tokens.secondaryText)
                    CarPlayText("$peakSpeed $unitStr", type.headline.copy(fontFeatureSettings = "tnum"), tokens.warningOrange)
                }
            }
        }
    }
}

@Composable
private fun weatherCodeDescription(code: Int): String = when (code) {
    0 -> stringResource(R.string.weather_clear)
    in 1..3 -> stringResource(R.string.weather_partly_cloudy)
    45, 48 -> stringResource(R.string.weather_fog)
    in 51..67 -> stringResource(R.string.weather_rain)
    in 71..77 -> stringResource(R.string.weather_snow)
    in 80..99 -> stringResource(R.string.weather_storm)
    else -> stringResource(R.string.weather_cloudy)
}
