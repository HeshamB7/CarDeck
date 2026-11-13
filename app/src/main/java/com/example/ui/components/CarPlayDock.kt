package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.example.ui.theme.LocalCarPlayTypography
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
    onSlotLongClick: (Int) -> Unit,
    onSwitchViewClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onOledDimmerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDockBoundsChanged: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val dockGlassAlpha = (cardAlpha * 0.58f).coerceIn(0.32f, 0.68f)
    val dockBg = tokens.dockBackground.copy(alpha = dockGlassAlpha)
    val glassSheenTop = if (tokens.isNight) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.42f)
    val glassSheenMid = if (tokens.isNight) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.14f)
    val glassSheenBot = if (tokens.isNight) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.24f)
    val glassRimColor = if (tokens.isNight) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.72f)
    val dockShape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp, horizontal = 6.dp)
            .onGloballyPositioned { coords ->
                onDockBoundsChanged(coords.boundsInRoot())
            }
            .shadow(
                elevation = if (tokens.isNight) 12.dp else 8.dp,
                shape = dockShape,
                ambientColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(dockShape)
            .background(dockBg)
            .background(
                Brush.verticalGradient(
                    listOf(glassSheenTop, glassSheenMid, glassSheenBot)
                )
            )
            .border(1.dp, glassRimColor, dockShape)
    ) {
        val slotSize = (maxWidth * 0.72f).coerceIn(40.dp, 54.dp)
        val slotGap = (maxHeight * 0.022f).coerceIn(6.dp, 14.dp)
        val utilityBtnSize = (maxWidth * 0.40f).coerceIn(26.dp, 32.dp)
        val compactHeight = maxHeight < 440.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = if (compactHeight) 8.dp else 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Authentic Apple CarPlay Status Cluster (SF Pro Clock + Signal + 5G/Wi-Fi)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                CarPlayText(
                    text = systemStatus.timeText,
                    style = type.dockClock,
                    color = tokens.primaryText,
                    maxLines = 1
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleSignalBars(
                        connected = systemStatus.isCellularConnected || systemStatus.isWifiConnected,
                        activeColor = tokens.primaryText,
                        inactiveColor = tokens.secondaryText.copy(alpha = 0.35f),
                        contentDesc = stringResource(R.string.cd_status_cellular)
                    )
                    if (systemStatus.isWifiConnected) {
                        CarPlaySymbol(
                            type = CarPlaySymbolType.WIFI,
                            tint = tokens.primaryText,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        CarPlayText(
                            text = if (systemStatus.isCellularConnected) "5G" else "LTE",
                            style = type.micro.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                            color = if (systemStatus.isCellularConnected) tokens.primaryText
                            else tokens.secondaryText.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Middle: 3 Authentic CarPlay Squircle Recent/Pinned Slots (Long press any slot to replace app)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(slotGap)
            ) {
                for (idx in 0..2) {
                    val app = dockSlotApps.getOrNull(idx)
                    val isPinned = pinnedPackages.getOrNull(idx) != null
                    DockAppSlotItem(
                        app = app,
                        icon = app?.let { icons[it.packageName] },
                        slotSize = slotSize,
                        slotIndex = idx,
                        isPinned = isPinned,
                        onClick = {
                            if (app != null) onAppClick(app) else onSlotLongClick(idx)
                        },
                        onLongClick = { onSlotLongClick(idx) }
                    )
                }
            }

            // Bottom: OLED Dimmer + Settings Pills, Siri Orb, and App Grid Switcher at bottom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compactHeight) 5.dp else 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dimmerDesc = stringResource(R.string.cd_dock_oled_dimmer)
                    Box(
                        modifier = Modifier
                            .size(utilityBtnSize)
                            .clip(CircleShape)
                            .background(tokens.pillBackground.copy(alpha = 0.56f))
                            .testTag("dock_oled_dimmer_button")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOledDimmerClick
                            )
                            .semantics { contentDescription = dimmerDesc },
                        contentAlignment = Alignment.Center
                    ) {
                        CarPlaySymbol(
                            type = CarPlaySymbolType.MOON,
                            tint = tokens.secondaryText,
                            modifier = Modifier.size(utilityBtnSize * 0.52f)
                        )
                    }

                    val settingsDesc = stringResource(R.string.cd_dock_settings)
                    Box(
                        modifier = Modifier
                            .size(utilityBtnSize)
                            .clip(CircleShape)
                            .background(
                                if (currentScreen == LauncherScreen.SETTINGS) tokens.accentBlue.copy(alpha = 0.25f)
                                else tokens.pillBackground.copy(alpha = 0.56f)
                            )
                            .testTag("dock_settings_button")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onSettingsClick
                            )
                            .semantics { contentDescription = settingsDesc },
                        contentAlignment = Alignment.Center
                    ) {
                        CarPlaySymbol(
                            type = CarPlaySymbolType.SETTINGS_GEAR,
                            tint = if (currentScreen == LauncherScreen.SETTINGS) tokens.accentBlue
                            else tokens.secondaryText,
                            modifier = Modifier.size(utilityBtnSize * 0.54f)
                        )
                    }
                }

                SiriVoiceOrbButton(onClick = onVoiceClick)

                CarPlayViewSwitcherButton(
                    isDashboard = currentScreen == LauncherScreen.DASHBOARD,
                    onClick = onSwitchViewClick
                )
            }
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
            .size(width = 16.dp, height = 10.dp)
            .semantics { contentDescription = contentDesc }
    ) {
        val barWidth = size.width / 6.2f
        val gap = barWidth * 0.6f
        val heights = listOf(0.35f, 0.55f, 0.78f, 1.0f)
        heights.forEachIndexed { idx, frac ->
            val h = size.height * frac
            val x = idx * (barWidth + gap)
            val color = if (connected) activeColor else if (idx < 2) activeColor.copy(alpha = 0.45f) else inactiveColor
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "siri_scale"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .testTag("dock_voice_button")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = voiceDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
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
                style = Stroke(width = 3.dp.toPx())
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
    slotSize: androidx.compose.ui.unit.Dp,
    slotIndex: Int,
    isPinned: Boolean,
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
            .size(slotSize)
            .scale(scale)
            .testTag("dock_slot_$slotIndex")
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            CarPlaySquircleIcon(
                app = app,
                bitmap = icon,
                size = slotSize,
                elevation = 4.dp
            )
            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 1.dp)
                        .size(width = 10.dp, height = 2.5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.72f))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(AppleSquircleShape)
                    .background(tokens.pillBackground.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CarPlaySymbol(
                    type = CarPlaySymbolType.PLUS,
                    tint = tokens.secondaryText.copy(alpha = 0.65f),
                    modifier = Modifier.size(slotSize * 0.42f)
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
    val tokens = LocalCarTokens.current
    val fgColor = tokens.primaryText
    val switchDesc = stringResource(R.string.cd_dock_switch_view)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switcher_scale"
    )

    Box(
        modifier = Modifier
            .size(46.dp)
            .scale(scale)
            .clip(CircleShape)
            .testTag("dock_view_switcher")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = switchDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            if (isDashboard) {
                val cols = 3
                val rows = 3
                val cell = size.width * 0.25f
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
