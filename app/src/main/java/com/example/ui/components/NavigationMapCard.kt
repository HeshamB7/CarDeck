package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.DriverAssistUtils
import com.example.model.NavigationProvider
import com.example.model.SpeedUnit
import com.example.model.TelemetryState
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavigationMapCard(
    telemetry: TelemetryState,
    navProvider: NavigationProvider,
    homeAddress: String,
    workAddress: String,
    speedLimitKmh: Int,
    speedGuardEnabled: Boolean,
    speedUnit: SpeedUnit,
    cornerRadiusDp: Float,
    cardAlpha: Float,
    hasParkedPin: Boolean,
    parkedAgeMinutes: Int?,
    onSearchNavigate: (String) -> Unit,
    onOpenFullMap: () -> Unit,
    onParkedPinClick: () -> Unit,
    onClearParkedPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var pitStopsExpanded by remember { mutableStateOf(false) }

    val displayLimit = if (speedUnit == SpeedUnit.MPH) {
        (speedLimitKmh * 0.621371f).roundToInt()
    } else {
        speedLimitKmh
    }
    val isOverLimit = speedGuardEnabled && telemetry.speedKmh > speedLimitKmh

    val pulseTransition = rememberInfiniteTransition(label = "overspeed_sign_pulse")
    val signPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overspeed_halo"
    )

    val headingLabel = remember(telemetry.bearingDegrees) {
        DriverAssistUtils.formatCardinalHeading(telemetry.bearingDegrees)
    }

    CarPlayGlassCard(
        cornerRadius = cornerRadiusDp.dp,
        cardAlpha = cardAlpha,
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxHeight < 340.dp || maxWidth < 380.dp
            val isNight = tokens.isNight

            // 60fps Vector Map: static road/water geometry cached in drawWithCache, only puck bearing updates
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (searchExpanded) searchExpanded = false else onOpenFullMap()
                            },
                            onDoubleTap = { onOpenFullMap() }
                        )
                    }
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val bgBrush = Brush.verticalGradient(
                            if (isNight) listOf(Color(0xFF181C24), Color(0xFF202530))
                            else listOf(Color(0xFFF6F4EC), Color(0xFFEEECE2))
                        )
                        val waterColor = if (isNight) Color(0xFF14273E) else Color(0xFFA7D7F9)
                        val riverPath = Path().apply {
                            moveTo(w * 0.72f, 0f)
                            cubicTo(w * 0.76f, h * 0.35f, w * 0.88f, h * 0.65f, w * 0.96f, h)
                            lineTo(w, h)
                            lineTo(w, 0f)
                            close()
                        }
                        val parkColor = if (isNight) Color(0xFF182C22) else Color(0xFFD2EAD0)
                        val minorRoadColor = if (isNight) Color(0xFF2D3340) else Color.White
                        val minorStrokePx = 3.5.dp.toPx()

                        val highwayColor = if (isNight) Color(0xFF3E4758) else Color(0xFFF9D876)
                        val highwayPath = Path().apply {
                            moveTo(w * 0.08f, h)
                            cubicTo(w * 0.34f, h * 0.64f, w * 0.45f, h * 0.46f, w * 0.78f, 0f)
                        }
                        val highwayStroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

                        val routePath = Path().apply {
                            moveTo(w * 0.50f, h * 0.84f)
                            lineTo(w * 0.50f, h * 0.46f)
                            quadraticBezierTo(w * 0.50f, h * 0.29f, w * 0.66f, h * 0.23f)
                            lineTo(w * 0.80f, h * 0.10f)
                        }
                        val routeCasingStroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val routeInnerStroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

                        val puckCenter = Offset(w * 0.50f, h * 0.58f)
                        val arrowPath = Path().apply {
                            moveTo(puckCenter.x, puckCenter.y - 6.5.dp.toPx())
                            lineTo(puckCenter.x + 4.8.dp.toPx(), puckCenter.y + 4.8.dp.toPx())
                            lineTo(puckCenter.x, puckCenter.y + 1.8.dp.toPx())
                            lineTo(puckCenter.x - 4.8.dp.toPx(), puckCenter.y + 4.8.dp.toPx())
                            close()
                        }
                        val haloRadius = 26.dp.toPx()
                        val ringRadius = 14.dp.toPx()
                        val innerRadius = 11.dp.toPx()

                        onDrawBehind {
                            drawRect(brush = bgBrush)
                            drawPath(riverPath, waterColor)
                            drawRect(parkColor, Offset(w * 0.04f, h * 0.14f), Size(w * 0.25f, h * 0.24f))
                            drawRect(parkColor, Offset(w * 0.12f, h * 0.60f), Size(w * 0.22f, h * 0.18f))

                            for (i in 1..5) {
                                val y = h * (i / 6f)
                                drawLine(minorRoadColor, Offset(0f, y), Offset(w, y), strokeWidth = minorStrokePx)
                            }
                            for (j in 1..4) {
                                val x = w * (j / 5f)
                                drawLine(minorRoadColor, Offset(x, 0f), Offset(x, h), strokeWidth = minorStrokePx)
                            }

                            drawPath(highwayPath, highwayColor, style = highwayStroke)
                            drawPath(routePath, Color(0xFF0051B3), style = routeCasingStroke)
                            drawPath(routePath, tokens.accentBlue, style = routeInnerStroke)

                            drawCircle(tokens.accentBlue.copy(alpha = 0.22f), radius = haloRadius, center = puckCenter)
                            drawCircle(Color.White, radius = ringRadius, center = puckCenter)
                            drawCircle(tokens.accentBlue, radius = innerRadius, center = puckCenter)
                            rotate(degrees = telemetry.bearingDegrees, pivot = puckCenter) {
                                drawPath(arrowPath, Color.White)
                            }
                        }
                    }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isCompact) 10.dp else 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Overlay: Floating Search Pill / Road Badge + Authentic CarPlay SPEED LIMIT Sign
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (searchExpanded) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CarPlayTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = stringResource(R.string.nav_search_placeholder),
                                leadingSymbol = CarPlaySymbolType.SEARCH,
                                imeAction = ImeAction.Search,
                                onImeAction = {
                                    if (searchQuery.isNotBlank()) {
                                        onSearchNavigate(searchQuery.trim())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nav_search_input")
                            )
                            CarPlayButton(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        onSearchNavigate(searchQuery.trim())
                                    } else {
                                        searchExpanded = false
                                    }
                                }
                            ) {
                                CarPlayText(
                                    text = if (searchQuery.isNotBlank()) stringResource(R.string.nav_go)
                                    else stringResource(R.string.action_cancel),
                                    style = type.controlLabel,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search Pill Button
                            Row(
                                modifier = Modifier
                                    .height(40.dp)
                                    .shadow(4.dp, RoundedCornerShape(13.dp))
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(tokens.cardBackground.copy(alpha = 0.92f))
                                    .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(13.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { searchExpanded = true }
                                    )
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CarPlaySymbol(
                                    type = CarPlaySymbolType.SEARCH,
                                    tint = tokens.accentBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                CarPlayText(
                                    text = stringResource(R.string.nav_search_placeholder),
                                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = tokens.primaryText,
                                    maxLines = 1
                                )
                            }

                            // Provider Badge
                            Row(
                                modifier = Modifier
                                    .height(40.dp)
                                    .shadow(4.dp, RoundedCornerShape(13.dp))
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(tokens.cardBackground.copy(alpha = 0.92f))
                                    .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(13.dp))
                                    .testTag("nav_open_maps_button")
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onOpenFullMap
                                    )
                                    .padding(horizontal = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CarPlaySymbol(
                                    type = CarPlaySymbolType.NAVIGATION_ARROW,
                                    tint = tokens.accentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                CarPlayText(
                                    text = navProvider.label,
                                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = tokens.primaryText,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Authentic White Rectangular SPEED LIMIT Road Sign with Overspeed Halo
                    Column(
                        modifier = Modifier
                            .shadow(5.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(
                                width = if (isOverLimit) 2.5.dp else 1.5.dp,
                                color = if (isOverLimit) tokens.alertRed.copy(alpha = signPulseAlpha) else Color(0xFF1C1C1E),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CarPlayText(
                            text = "SPEED",
                            style = type.micro.copy(fontSize = 8.sp, lineHeight = 9.sp, letterSpacing = 0.3.sp),
                            color = if (isOverLimit) tokens.alertRed else Color(0xFF1C1C1E)
                        )
                        CarPlayText(
                            text = "LIMIT",
                            style = type.micro.copy(fontSize = 8.sp, lineHeight = 9.sp, letterSpacing = 0.3.sp),
                            color = if (isOverLimit) tokens.alertRed else Color(0xFF1C1C1E)
                        )
                        CarPlayText(
                            text = displayLimit.toString(),
                            style = type.headline.copy(fontSize = 20.sp, lineHeight = 22.sp, fontFeatureSettings = "tnum"),
                            color = if (isOverLimit) tokens.alertRed else Color.Black
                        )
                    }
                }

                // Bottom: GPS + Compass Heading + Parked Car Pin + Expandable Pit-Stop Strip + Quick Destinations
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // GPS & Cardinal Compass Pill
                            Row(
                                modifier = Modifier
                                    .shadow(3.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(tokens.cardBackground.copy(alpha = 0.92f))
                                    .border(0.75.dp, tokens.cardBorder, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (telemetry.hasGpsFix) tokens.positiveGreen else tokens.warningOrange)
                                )
                                CarPlayText(
                                    text = if (telemetry.hasGpsFix) headingLabel else stringResource(R.string.nav_gps_searching),
                                    style = type.caption.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                                    color = tokens.primaryText
                                )
                            }

                            // 1-Tap Parked Car Saver / Car Finder Pin
                            Row(
                                modifier = Modifier
                                    .shadow(3.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasParkedPin) tokens.accentBlue.copy(alpha = 0.92f)
                                        else tokens.cardBackground.copy(alpha = 0.92f)
                                    )
                                    .border(0.75.dp, tokens.cardBorder, CircleShape)
                                    .testTag("nav_parked_pin_button")
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onParkedPinClick,
                                        onLongClick = { if (hasParkedPin) onClearParkedPin() }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CarPlaySymbol(
                                    type = CarPlaySymbolType.LOCATION_PIN,
                                    tint = if (hasParkedPin) Color.White else tokens.accentBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                val parkLabel = if (hasParkedPin) {
                                    val ageSuffix = parkedAgeMinutes?.let {
                                        " · ${stringResource(R.string.parked_age_mins, it)}"
                                    }.orEmpty()
                                    stringResource(R.string.parked_pin_go) + ageSuffix
                                } else {
                                    stringResource(R.string.parked_pin_save)
                                }
                                CarPlayText(
                                    text = parkLabel,
                                    style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (hasParkedPin) Color.White else tokens.primaryText,
                                    maxLines = 1
                                )
                            }
                        }

                        // Expandable Pit-Stop Strip Toggle
                        Row(
                            modifier = Modifier
                                .shadow(3.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (pitStopsExpanded) tokens.positiveGreen.copy(alpha = 0.92f)
                                    else tokens.cardBackground.copy(alpha = 0.92f)
                                )
                                .border(0.75.dp, tokens.cardBorder, CircleShape)
                                .testTag("nav_pitstops_toggle")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { pitStopsExpanded = !pitStopsExpanded }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CarPlaySymbol(
                                type = CarPlaySymbolType.FUEL,
                                tint = if (pitStopsExpanded) Color.White else tokens.positiveGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            CarPlayText(
                                text = stringResource(R.string.pitstop_toggle),
                                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = if (pitStopsExpanded) Color.White else tokens.primaryText
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = pitStopsExpanded,
                        enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PitStopMiniChip(stringResource(R.string.pitstop_fuel), tokens.positiveGreen, Modifier.weight(1f)) {
                                onSearchNavigate("gas station")
                            }
                            PitStopMiniChip(stringResource(R.string.pitstop_ev), tokens.accentBlue, Modifier.weight(1f)) {
                                onSearchNavigate("EV charging station")
                            }
                            PitStopMiniChip(stringResource(R.string.pitstop_coffee), tokens.warningOrange, Modifier.weight(1f)) {
                                onSearchNavigate("coffee shop")
                            }
                            PitStopMiniChip(stringResource(R.string.pitstop_parking), tokens.accentBlue, Modifier.weight(1f)) {
                                onSearchNavigate("parking garage")
                            }
                            PitStopMiniChip(stringResource(R.string.pitstop_hospital), tokens.alertRed, Modifier.weight(1f)) {
                                onSearchNavigate("hospital emergency")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NavShortcutChip(
                            symbol = CarPlaySymbolType.HOME,
                            label = stringResource(R.string.nav_home),
                            tint = tokens.accentBlue,
                            testTag = "nav_shortcut_home",
                            modifier = Modifier.weight(1f),
                            onClick = { onSearchNavigate(homeAddress.ifBlank { "Home" }) }
                        )
                        NavShortcutChip(
                            symbol = CarPlaySymbolType.WORK,
                            label = stringResource(R.string.nav_work),
                            tint = tokens.warningOrange,
                            testTag = "nav_shortcut_work",
                            modifier = Modifier.weight(1f),
                            onClick = { onSearchNavigate(workAddress.ifBlank { "Work" }) }
                        )
                        NavShortcutChip(
                            symbol = CarPlaySymbolType.FUEL,
                            label = stringResource(R.string.nav_gas),
                            tint = tokens.positiveGreen,
                            testTag = "nav_shortcut_gas",
                            modifier = Modifier.weight(1.2f),
                            onClick = { onSearchNavigate("gas station") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PitStopMiniChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Box(
        modifier = modifier
            .height(32.dp)
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(tokens.cardBackground.copy(alpha = 0.94f))
            .border(0.75.dp, accent.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        CarPlayText(
            text = label,
            style = type.micro.copy(fontWeight = FontWeight.Bold),
            color = tokens.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NavShortcutChip(
    symbol: CarPlaySymbolType,
    label: String,
    tint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Row(
        modifier = modifier
            .height(42.dp)
            .shadow(3.dp, RoundedCornerShape(13.dp))
            .clip(RoundedCornerShape(13.dp))
            .background(tokens.cardBackground.copy(alpha = 0.92f))
            .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(13.dp))
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CarPlaySymbol(
            type = symbol,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        CarPlayText(
            text = label,
            style = type.controlLabel,
            color = tokens.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
