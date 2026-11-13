package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppEntry
import com.example.model.SpeedUnit
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.ui.theme.CarDisplayFontFamily
import com.example.ui.theme.LocalCarTokens
import com.example.ui.theme.OledDimmerRed
import com.example.ui.theme.OledDimmerRedMuted
import kotlin.math.roundToInt

/**
 * Pure OLED Black Night Dimmer Overlay with eye-safe deep red digital clock and speed.
 */
@Composable
fun OledNightDimmerOverlay(
    systemStatus: SystemStatusState,
    telemetry: TelemetryState,
    speedUnit: SpeedUnit,
    onWake: () -> Unit
) {
    val speedValue = if (speedUnit == SpeedUnit.MPH) {
        (telemetry.speedKmh * 0.621371f).roundToInt()
    } else {
        telemetry.speedKmh.roundToInt()
    }
    val unitLabel = if (speedUnit == SpeedUnit.MPH) {
        stringResource(R.string.stack_unit_mph)
    } else {
        stringResource(R.string.stack_unit_kmh)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("oled_night_dimmer_overlay")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onWake
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = systemStatus.timeText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = CarDisplayFontFamily,
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = OledDimmerRed
            )
            if (telemetry.hasLocationPermission) {
                Text(
                    text = "$speedValue $unitLabel",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = CarDisplayFontFamily,
                        fontFeatureSettings = "tnum"
                    ),
                    color = OledDimmerRed
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.oled_tap_to_wake),
                style = MaterialTheme.typography.bodyMedium,
                color = OledDimmerRedMuted
            )
        }
    }
}

/**
 * Valet / Guest Mode 4-digit PIN Lock Dialog.
 */
@Composable
fun ValetPinDialog(
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val tokens = LocalCarTokens.current
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Lock, null, tint = tokens.warningOrange)
                Text(stringResource(R.string.valet_enter_pin))
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < pin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> tokens.alertRed
                                        filled -> tokens.accentBlue
                                        else -> tokens.pillBackground
                                    }
                                )
                        )
                    }
                }
                if (isError) {
                    Text(
                        text = stringResource(R.string.valet_wrong_pin),
                        color = tokens.alertRed,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "<")
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tokens.pillBackground)
                                        .clickable {
                                            isError = false
                                            when (key) {
                                                "C" -> pin = ""
                                                "<" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                else -> {
                                                    if (pin.length < 4) {
                                                        val next = pin + key
                                                        pin = next
                                                        if (next.length == 4) {
                                                            if (!onVerify(next)) {
                                                                isError = true
                                                                pin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "<") {
                                        Icon(Icons.AutoMirrored.Rounded.Backspace, null, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = CarDisplayFontFamily,
                                                fontFeatureSettings = "tnum"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Dual-App Split-Screen Picker Dialog for installed Android apps.
 */
@Composable
fun SplitAppPickerDialog(
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    preselectedPrimary: String?,
    onLaunchSplit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val launchableApps = remember(apps) { apps.filter { it.virtualAction == null } }
    var primaryPkg by remember(preselectedPrimary, launchableApps) {
        mutableStateOf(preselectedPrimary ?: launchableApps.firstOrNull()?.packageName)
    }
    var secondaryPkg by remember(launchableApps) {
        mutableStateOf(launchableApps.getOrNull(1)?.packageName ?: launchableApps.firstOrNull()?.packageName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Splitscreen, null, tint = tokens.accentBlue)
                Text(stringResource(R.string.split_title))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.split_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.secondaryText
                )

                Text(stringResource(R.string.split_primary), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(launchableApps, key = { "p_${it.packageName}" }) { app ->
                        SplitAppMiniChip(
                            app = app,
                            icon = icons[app.packageName],
                            selected = primaryPkg == app.packageName,
                            onClick = { primaryPkg = app.packageName }
                        )
                    }
                }

                Text(stringResource(R.string.split_secondary), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(launchableApps, key = { "s_${it.packageName}" }) { app ->
                        SplitAppMiniChip(
                            app = app,
                            icon = icons[app.packageName],
                            selected = secondaryPkg == app.packageName,
                            onClick = { secondaryPkg = app.packageName }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = primaryPkg
                    val s = secondaryPkg
                    if (p != null && s != null) onLaunchSplit(p, s)
                },
                enabled = primaryPkg != null && secondaryPkg != null,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue)
            ) {
                Text(stringResource(R.string.split_launch))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun SplitAppMiniChip(
    app: AppEntry,
    icon: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) tokens.accentBlue.copy(alpha = 0.25f) else tokens.pillBackground)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) tokens.accentBlue else tokens.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Image(icon, app.displayName, modifier = Modifier.size(38.dp))
        }
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * First-Launch Welcome & Permissions Setup Overlay.
 */
@Composable
fun OnboardingOverlay(
    hasLocationPermission: Boolean,
    hasMediaPermission: Boolean,
    onGrantLocation: () -> Unit,
    onGrantMedia: () -> Unit,
    onSetDefaultHome: () -> Unit,
    onFinish: () -> Unit
) {
    val tokens = LocalCarTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .clip(RoundedCornerShape(24.dp))
                .background(tokens.cardBackground)
                .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.secondaryText
            )

            OnboardingPermissionRow(
                icon = Icons.Rounded.LocationOn,
                title = stringResource(R.string.onboarding_location_title),
                desc = stringResource(R.string.onboarding_location_desc),
                granted = hasLocationPermission,
                actionLabel = stringResource(if (hasLocationPermission) R.string.action_granted else R.string.action_grant),
                onAction = onGrantLocation
            )

            OnboardingPermissionRow(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(R.string.onboarding_media_title),
                desc = stringResource(R.string.onboarding_media_desc),
                granted = hasMediaPermission,
                actionLabel = stringResource(if (hasMediaPermission) R.string.action_granted else R.string.action_grant),
                onAction = onGrantMedia
            )

            OnboardingPermissionRow(
                icon = Icons.Rounded.Home,
                title = stringResource(R.string.onboarding_home_title),
                desc = stringResource(R.string.onboarding_home_desc),
                granted = false,
                actionLabel = stringResource(R.string.action_set_default),
                onAction = onSetDefaultHome
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue),
                    modifier = Modifier
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("onboarding_finish_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_start_driving),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPermissionRow(
    icon: ImageVector,
    title: String,
    desc: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    val tokens = LocalCarTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.pillBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (granted) tokens.positiveGreen else tokens.accentBlue,
            modifier = Modifier.size(26.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (granted) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = actionLabel,
                tint = tokens.positiveGreen,
                modifier = Modifier.size(26.dp)
            )
        } else {
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(actionLabel)
            }
        }
    }
}
