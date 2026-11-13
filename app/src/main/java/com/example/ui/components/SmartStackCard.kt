package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.DriverAssistUtils
import com.example.model.SpeedUnit
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Top-Right CarPlay Turn-by-Turn & Live Speed Guidance Card.
 */
@Composable
fun TurnGuidanceSpeedCard(
    telemetry: TelemetryState,
    speedUnit: SpeedUnit,
    speedLimitKmh: Int,
    speedGuardEnabled: Boolean,
    cornerRadiusDp: Float,
    cardAlpha: Float,
    onGrantLocation: () -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current

    val displaySpeed = if (speedUnit == SpeedUnit.MPH) {
        (telemetry.speedKmh * 0.621371f).roundToInt()
    } else {
        telemetry.speedKmh.roundToInt()
    }
    val displayLimit = if (speedUnit == SpeedUnit.MPH) {
        (speedLimitKmh * 0.621371f).roundToInt()
    } else {
        speedLimitKmh
    }
    val unitLabel = if (speedUnit == SpeedUnit.MPH) {
        stringResource(R.string.stack_unit_mph)
    } else {
        stringResource(R.string.stack_unit_kmh)
    }

    val isOverLimit = speedGuardEnabled && telemetry.speedKmh > speedLimitKmh
    val isApproaching = speedGuardEnabled && telemetry.speedKmh > speedLimitKmh * 0.85f && !isOverLimit

    val speedColor by animateColorAsState(
        targetValue = when {
            isOverLimit -> tokens.alertRed
            isApproaching -> tokens.warningOrange
            else -> tokens.primaryText
        },
        label = "turn_speed_color"
    )

    val haloTransition = rememberInfiniteTransition(label = "speed_halo")
    val haloAlpha by haloTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speed_halo_alpha"
    )

    val cardinal = remember(telemetry.bearingDegrees) {
        DriverAssistUtils.cardinalShort(telemetry.bearingDegrees)
    }

    val haloModifier = if (isOverLimit) {
        Modifier.border(2.dp, tokens.alertRed.copy(alpha = haloAlpha), RoundedCornerShape(cornerRadiusDp.dp))
    } else {
        Modifier
    }

    CarPlayGlassCard(
        cornerRadius = cornerRadiusDp.dp,
        cardAlpha = cardAlpha,
        modifier = modifier
            .then(haloModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenMap
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val isCompact = maxHeight < 105.dp

            if (!telemetry.hasLocationPermission) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CarPlaySymbol(
                            type = CarPlaySymbolType.TURN_STRAIGHT,
                            tint = tokens.accentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            CarPlayText(
                                text = stringResource(R.string.stack_speed_title),
                                style = type.rowTitle,
                                color = tokens.primaryText
                            )
                            CarPlayText(
                                text = stringResource(R.string.onboarding_location_desc),
                                style = type.caption,
                                color = tokens.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    CarPlayButton(onClick = onGrantLocation) {
                        CarPlayText(
                            text = stringResource(R.string.action_grant),
                            style = type.controlLabel,
                            color = Color.White
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CarPlaySymbol(
                        type = CarPlaySymbolType.TURN_STRAIGHT,
                        tint = if (isOverLimit) tokens.alertRed else tokens.primaryText,
                        modifier = Modifier.size(if (isCompact) 30.dp else 36.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CarPlayText(
                                text = displaySpeed.toString(),
                                style = type.turnDistance.copy(fontSize = if (isCompact) 26.sp else 34.sp),
                                color = speedColor
                            )
                            CarPlayText(
                                text = unitLabel,
                                style = type.controlLabel,
                                color = tokens.secondaryText,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        CarPlayText(
                            text = if (isOverLimit) {
                                stringResource(R.string.stack_speed_limit_alert, displayLimit, unitLabel)
                            } else {
                                stringResource(R.string.stack_speed_safe, displayLimit, unitLabel)
                            },
                            style = type.bodySecondary,
                            color = if (isOverLimit) tokens.alertRed else tokens.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Live Cardinal Compass + Day/Night badge
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.pillBackground.copy(alpha = 0.72f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CarPlaySymbol(
                                type = CarPlaySymbolType.COMPASS,
                                tint = tokens.accentBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            CarPlayText(
                                text = cardinal,
                                style = type.micro.copy(fontWeight = FontWeight.Bold),
                                color = tokens.primaryText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.pillBackground.copy(alpha = 0.72f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CarPlaySymbol(
                                type = if (tokens.isNight) CarPlaySymbolType.MOON else CarPlaySymbolType.SUN,
                                tint = if (tokens.isNight) tokens.accentBlue else tokens.warningOrange,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom-Right CarPlay Smart Stack Card:
 * Page 0: Calendar / Next Event & Drive Break Alert
 * Page 1: Quick Vehicle Controls (Bluetooth, Wi-Fi, Focus Mode, Split View)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmartStackCard(
    telemetry: TelemetryState,
    systemStatus: SystemStatusState,
    isHighwayFocusActive: Boolean,
    isBreakReminderDue: Boolean,
    cornerRadiusDp: Float,
    cardAlpha: Float,
    onGrantCalendar: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit,
    onToggleHighwayFocus: () -> Unit,
    onOpenSplitApps: () -> Unit,
    onFindRestStop: () -> Unit,
    onResetTripTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val springFling = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    )

    CarPlayGlassCard(
        cornerRadius = cornerRadiusDp.dp,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerticalPager(
                state = pagerState,
                flingBehavior = springFling,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { page ->
                when (page) {
                    0 -> CalendarAndTripStackPage(
                        telemetry = telemetry,
                        isBreakReminderDue = isBreakReminderDue,
                        onGrantCalendar = onGrantCalendar,
                        onFindRestStop = onFindRestStop,
                        onResetTripTimer = onResetTripTimer
                    )
                    else -> QuickVehicleControlsStackPage(
                        systemStatus = systemStatus,
                        isHighwayFocusActive = isHighwayFocusActive,
                        onOpenBluetooth = onOpenBluetooth,
                        onOpenWifi = onOpenWifi,
                        onToggleHighwayFocus = onToggleHighwayFocus,
                        onOpenSplitApps = onOpenSplitApps
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(2) { idx ->
                    val selected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(if (selected) 14.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) tokens.primaryText
                                else tokens.secondaryText.copy(alpha = 0.36f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarAndTripStackPage(
    telemetry: TelemetryState,
    isBreakReminderDue: Boolean,
    onGrantCalendar: () -> Unit,
    onFindRestStop: () -> Unit,
    onResetTripTimer: () -> Unit
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
    val tripMinutes = elapsedSec / 60L
    val tripSeconds = elapsedSec % 60L
    val tripFormatted = "%02d:%02d".format(tripMinutes, tripSeconds)

    val dayFmt = remember { SimpleDateFormat("EEE", Locale.US) }
    val dateFmt = remember { SimpleDateFormat("d", Locale.US) }
    val now = remember { Date() }
    val dayName = remember(now) { dayFmt.format(now).uppercase(Locale.US) }
    val dayNum = remember(now) { dateFmt.format(now) }

    if (isBreakReminderDue) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(tokens.warningOrange.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CarPlaySymbol(CarPlaySymbolType.WARNING, tokens.warningOrange, Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CarPlayText(
                    text = stringResource(R.string.break_alert_title),
                    style = type.caption.copy(fontWeight = FontWeight.Bold),
                    color = tokens.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CarPlayText(
                    text = "${stringResource(R.string.stack_trip_elapsed)}: $tripFormatted",
                    style = type.micro.copy(fontFeatureSettings = "tnum"),
                    color = tokens.secondaryText
                )
            }
            CarPlayButton(onClick = onFindRestStop) {
                CarPlayText(stringResource(R.string.break_alert_find_coffee), type.micro, Color.White)
            }
            CarPlaySecondaryButton(onClick = onResetTripTimer) {
                CarPlayText(stringResource(R.string.break_alert_reset), type.micro, tokens.primaryText)
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Authentic Mini Calendar Badge
        Column(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (tokens.isNight) Color(0xFF2C2C2E) else Color.White)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CarPlayText(
                text = dayName,
                style = type.micro.copy(fontSize = 10.sp),
                color = tokens.alertRed
            )
            CarPlayText(
                text = dayNum,
                style = type.headline.copy(fontSize = 21.sp, lineHeight = 23.sp, fontFeatureSettings = "tnum"),
                color = tokens.primaryText
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            if (!telemetry.hasCalendarPermission) {
                CarPlayText(
                    text = stringResource(R.string.stack_trip_elapsed),
                    style = type.caption,
                    color = tokens.secondaryText
                )
                CarPlayText(
                    text = tripFormatted,
                    style = type.headline.copy(fontFeatureSettings = "tnum"),
                    color = tokens.primaryText
                )
            } else {
                CarPlayText(
                    text = telemetry.nextEventTitle ?: stringResource(R.string.stack_no_events),
                    style = type.rowTitle.copy(fontWeight = FontWeight.Bold),
                    color = tokens.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CarPlayText(
                    text = telemetry.nextEventTime
                        ?: "${stringResource(R.string.stack_trip_elapsed)}: $tripFormatted",
                    style = type.bodySecondary,
                    color = tokens.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!telemetry.hasCalendarPermission) {
            CarPlayButton(onClick = onGrantCalendar) {
                CarPlayText(
                    text = stringResource(R.string.stack_connect_calendar),
                    style = type.controlLabel,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun QuickVehicleControlsStackPage(
    systemStatus: SystemStatusState,
    isHighwayFocusActive: Boolean,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit,
    onToggleHighwayFocus: () -> Unit,
    onOpenSplitApps: () -> Unit
) {
    val tokens = LocalCarTokens.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickControlTile(
                symbol = CarPlaySymbolType.BLUETOOTH,
                label = stringResource(R.string.control_bluetooth),
                isActive = systemStatus.isBluetoothEnabled,
                activeColor = tokens.accentBlue,
                testTag = "control_bluetooth_tile",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onOpenBluetooth
            )
            QuickControlTile(
                symbol = CarPlaySymbolType.WIFI,
                label = stringResource(R.string.control_wifi),
                isActive = systemStatus.isWifiConnected,
                activeColor = tokens.positiveGreen,
                testTag = "control_wifi_tile",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onOpenWifi
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickControlTile(
                symbol = CarPlaySymbolType.MOON,
                label = stringResource(R.string.control_highway_focus),
                isActive = isHighwayFocusActive,
                activeColor = tokens.warningOrange,
                testTag = "control_focus_tile",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onToggleHighwayFocus
            )
            QuickControlTile(
                symbol = CarPlaySymbolType.SPLIT_SCREEN,
                label = stringResource(R.string.control_split_screen),
                isActive = true,
                activeColor = tokens.accentBlue,
                testTag = "control_split_tile",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onOpenSplitApps
            )
        }
    }
}

@Composable
private fun QuickControlTile(
    symbol: CarPlaySymbolType,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) activeColor.copy(alpha = 0.22f)
                else tokens.pillBackground.copy(alpha = 0.72f)
            )
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CarPlaySymbol(
            type = symbol,
            tint = if (isActive) activeColor else tokens.secondaryText,
            modifier = Modifier.size(17.dp)
        )
        CarPlayText(
            text = label,
            style = type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
