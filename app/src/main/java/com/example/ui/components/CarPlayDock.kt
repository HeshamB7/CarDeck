package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppEntry
import com.example.model.LauncherScreen
import com.example.model.SystemStatusState
import com.example.ui.theme.CarDisplayFontFamily
import com.example.ui.theme.LocalCarTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarPlayDock(
    systemStatus: SystemStatusState,
    dockSlotApps: List<AppEntry?>,
    icons: Map<String, ImageBitmap>,
    pinnedPackages: List<String?>,
    currentScreen: LauncherScreen,
    cardAlpha: Float,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onSwitchViewClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onOledDimmerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val dockBg = tokens.dockBackground.copy(alpha = (cardAlpha + 0.08f).coerceIn(0.65f, 0.92f))

    Column(
        modifier = modifier
            .width(82.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp, horizontal = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(dockBg)
            .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(24.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Exact Apple CarPlay Status Bar (Clock + 4 Signal Bars + 5G/Wi-Fi)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = systemStatus.timeText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = CarDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    fontFeatureSettings = "tnum",
                    letterSpacing = (-0.4).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppleSignalBars(
                    connected = systemStatus.isCellularConnected || systemStatus.isWifiConnected,
                    activeColor = MaterialTheme.colorScheme.onSurface,
                    inactiveColor = tokens.secondaryText.copy(alpha = 0.35f),
                    contentDesc = stringResource(R.string.cd_status_cellular)
                )
                if (systemStatus.isWifiConnected) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = stringResource(R.string.cd_status_wifi),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(13.dp)
                    )
                } else {
                    Text(
                        text = if (systemStatus.isCellularConnected) "5G" else "SOS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (systemStatus.isCellularConnected) MaterialTheme.colorScheme.onSurface
                        else tokens.secondaryText.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Middle: 3 Authentic CarPlay Squircle Recent/Pinned Slots
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            dockSlotApps.forEachIndexed { idx, app ->
                DockAppSlotItem(
                    app = app,
                    icon = app?.let { icons[it.packageName] },
                    slotIndex = idx,
                    onClick = { app?.let(onAppClick) },
                    onLongClick = { app?.let(onAppLongClick) }
                )
            }
        }

        // Bottom: Siri Orb + Subtle Dimmer/Settings + Iconic CarPlay Home/Grid Switcher
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(tokens.pillBackground.copy(alpha = 0.55f))
                        .testTag("dock_oled_dimmer_button")
                        .clickable(onClick = onOledDimmerClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = stringResource(R.string.cd_dock_oled_dimmer),
                        tint = tokens.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(
                            if (currentScreen == LauncherScreen.SETTINGS) tokens.accentBlue.copy(alpha = 0.25f)
                            else tokens.pillBackground.copy(alpha = 0.55f)
                        )
                        .testTag("dock_settings_button")
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.cd_dock_settings),
                        tint = if (currentScreen == LauncherScreen.SETTINGS) tokens.accentBlue
                        else tokens.secondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Siri-style Glowing Voice Orb
            SiriVoiceOrbButton(onClick = onVoiceClick)

            // Authentic Apple CarPlay Home / App Grid Button
            CarPlayViewSwitcherButton(
                isDashboard = currentScreen == LauncherScreen.DASHBOARD,
                onClick = onSwitchViewClick
            )
        }
    }
}

@Composable
private fun AppleSignalBars(
    connected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    contentDesc: String
) {
    Canvas(
        modifier = Modifier
            .size(width = 16.dp, height = 11.dp)
            .semantics { contentDescription = contentDesc }
    ) {
        val barWidth = size.width / 6.2f
        val gap = barWidth * 0.6f
        val heights = listOf(0.35f, 0.55f, 0.78f, 1.0f)
        heights.forEachIndexed { idx, frac ->
            val h = size.height * frac
            val x = idx * (barWidth + gap)
            val color = if (connected && idx < 3) activeColor else if (connected) activeColor else inactiveColor
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun SiriVoiceOrbButton(onClick: () -> Unit) {
    val voiceDesc = stringResource(R.string.cd_dock_voice)
    Box(
        modifier = Modifier
            .size(40.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .testTag("dock_voice_button")
            .clickable(onClick = onClick)
            .semantics { contentDescription = voiceDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF00C2FF),
                        Color(0xFF7B61FF),
                        Color(0xFFFF375F),
                        Color(0xFF30D158),
                        Color(0xFF00C2FF)
                    )
                ),
                style = Stroke(width = 3.2.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0x665E5CE6),
                        Color(0x220A84FF),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension * 0.42f
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppSlotItem(
    app: AppEntry?,
    icon: ImageBitmap?,
    slotIndex: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dock_slot_scale"
    )

    Box(
        modifier = Modifier
            .size(50.dp)
            .scale(scale)
            .testTag("dock_slot_$slotIndex")
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = app != null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null && app != null) {
            Image(
                bitmap = icon,
                contentDescription = app.displayName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(13.dp))
                    .background(tokens.pillBackground.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = tokens.secondaryText.copy(alpha = 0.45f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CarPlayViewSwitcherButton(
    isDashboard: Boolean,
    onClick: () -> Unit
) {
    val fgColor = MaterialTheme.colorScheme.onSurface
    val switchDesc = stringResource(R.string.cd_dock_switch_view)

    Box(
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .testTag("dock_view_switcher")
            .clickable(onClick = onClick)
            .semantics { contentDescription = switchDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(23.dp)) {
            if (isDashboard) {
                // Iconic Apple CarPlay 3x3 / 2x4 rounded squares glyph
                val cols = 3
                val rows = 3
                val cell = size.width * 0.24f
                val gap = (size.width - cell * cols) / (cols - 1)
                val r = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        drawRoundRect(
                            color = fgColor,
                            topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                            size = Size(cell, cell),
                            cornerRadius = r
                        )
                    }
                }
            } else {
                // Iconic Apple CarPlay Dashboard Split-View glyph
                val r = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                val leftW = size.width * 0.52f
                val rightW = size.width * 0.36f
                val gap = size.width * 0.12f
                val halfH = (size.height - gap) / 2f
                drawRoundRect(fgColor, Offset(0f, 0f), Size(leftW, size.height), r)
                drawRoundRect(fgColor, Offset(leftW + gap, 0f), Size(rightW, halfH), r)
                drawRoundRect(fgColor, Offset(leftW + gap, halfH + gap), Size(rightW, halfH), r)
            }
        }
    }
}
