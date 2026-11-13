package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.SpeedUnit
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.ui.theme.CarDisplayFontFamily
import com.example.ui.theme.LocalCarTokens
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SmartStackCard(
    telemetry: TelemetryState,
    systemStatus: SystemStatusState,
    speedUnit: SpeedUnit,
    speedLimitKmh: Int,
    speedGuardEnabled: Boolean,
    cornerRadiusDp: Float,
    cardAlpha: Float,
    onGrantLocation: () -> Unit,
    onGrantCalendar: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenHotspot: () -> Unit,
    onOpenSplitApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    val pagerState = rememberPagerState(pageCount = { 3 })

    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.cardBackground.copy(alpha = cardAlpha))
            .border(0.75.dp, tokens.cardBorder, shape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) { page ->
                when (page) {
                    0 -> SpeedometerStackPage(
                        telemetry = telemetry,
                        speedUnit = speedUnit,
                        speedLimitKmh = speedLimitKmh,
                        speedGuardEnabled = speedGuardEnabled,
                        onGrantLocation = onGrantLocation
                    )
                    1 -> CalendarAndTripStackPage(
                        telemetry = telemetry,
                        systemStatus = systemStatus,
                        onGrantCalendar = onGrantCalendar
                    )
                    else -> QuickVehicleControlsStackPage(
                        systemStatus = systemStatus,
                        onOpenBluetooth = onOpenBluetooth,
                        onOpenWifi = onOpenWifi,
                        onOpenHotspot = onOpenHotspot,
                        onOpenSplitApps = onOpenSplitApps
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) { idx ->
                    val selected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(if (selected) 14.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.onSurface
                                else tokens.secondaryText.copy(alpha = 0.38f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedometerStackPage(
    telemetry: TelemetryState,
    speedUnit: SpeedUnit,
    speedLimitKmh: Int,
    speedGuardEnabled: Boolean,
    onGrantLocation: () -> Unit
) {
    val tokens = LocalCarTokens.current
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
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "speed_color"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
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
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = tokens.accentBlue,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.stack_speed_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tokens.pillBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (tokens.isNight) Icons.Rounded.NightlightRound else Icons.Rounded.WbSunny,
                    contentDescription = null,
                    tint = if (tokens.isNight) tokens.accentBlue else tokens.warningOrange,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (tokens.isNight) "Night" else "Day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (!telemetry.hasLocationPermission) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.onboarding_location_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.secondaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onGrantLocation,
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue)
                ) {
                    Text(stringResource(R.string.action_grant))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = displaySpeed.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = CarDisplayFontFamily,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = speedColor
                    )
                    Text(
                        text = unitLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.secondaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Authentic iOS Speed Limit / Alert Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isOverLimit) tokens.alertRed.copy(alpha = 0.22f)
                            else tokens.pillBackground
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isOverLimit) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = tokens.alertRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = if (isOverLimit) {
                            stringResource(R.string.stack_speed_limit_alert, displayLimit, unitLabel)
                        } else {
                            stringResource(R.string.stack_speed_safe, displayLimit, unitLabel)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOverLimit) tokens.alertRed else tokens.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarAndTripStackPage(
    telemetry: TelemetryState,
    systemStatus: SystemStatusState,
    onGrantCalendar: () -> Unit
) {
    val tokens = LocalCarTokens.current
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

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
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
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = tokens.alertRed,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.stack_calendar_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = systemStatus.dateText,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.alertRed
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!telemetry.hasCalendarPermission) {
                    Text(
                        text = stringResource(R.string.stack_trip_elapsed),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.secondaryText
                    )
                    Text(
                        text = tripFormatted,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = CarDisplayFontFamily,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = telemetry.nextEventTitle ?: stringResource(R.string.stack_no_events),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = telemetry.nextEventTime
                            ?: "${stringResource(R.string.stack_trip_elapsed)}: $tripFormatted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.secondaryText
                    )
                }
            }

            if (!telemetry.hasCalendarPermission) {
                Button(
                    onClick = onGrantCalendar,
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue)
                ) {
                    Text(stringResource(R.string.stack_connect_calendar))
                }
            }
        }
    }
}

@Composable
private fun QuickVehicleControlsStackPage(
    systemStatus: SystemStatusState,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenHotspot: () -> Unit,
    onOpenSplitApps: () -> Unit
) {
    val tokens = LocalCarTokens.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.stack_controls_title),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickControlTile(
                    icon = Icons.Rounded.Bluetooth,
                    label = stringResource(R.string.control_bluetooth),
                    isActive = systemStatus.isBluetoothEnabled,
                    activeColor = tokens.accentBlue,
                    testTag = "control_bluetooth_tile",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBluetooth
                )
                QuickControlTile(
                    icon = Icons.Rounded.Wifi,
                    label = stringResource(R.string.control_wifi),
                    isActive = systemStatus.isWifiConnected,
                    activeColor = tokens.positiveGreen,
                    testTag = "control_wifi_tile",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWifi
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickControlTile(
                    icon = Icons.Rounded.WifiTethering,
                    label = stringResource(R.string.control_hotspot),
                    isActive = false,
                    activeColor = tokens.warningOrange,
                    testTag = "control_hotspot_tile",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenHotspot
                )
                QuickControlTile(
                    icon = Icons.Rounded.Splitscreen,
                    label = stringResource(R.string.control_split_screen),
                    isActive = true,
                    activeColor = tokens.accentBlue,
                    testTag = "control_split_tile",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSplitApps
                )
            }
        }
    }
}

@Composable
private fun QuickControlTile(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    Row(
        modifier = modifier
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (isActive) activeColor.copy(alpha = 0.22f)
                else tokens.pillBackground.copy(alpha = 0.72f)
            )
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else tokens.secondaryText,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
