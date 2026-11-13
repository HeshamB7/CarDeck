package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.NavigationProvider
import com.example.model.TelemetryState
import com.example.ui.theme.LocalCarTokens

@Composable
fun NavigationMapCard(
    telemetry: TelemetryState,
    navProvider: NavigationProvider,
    homeAddress: String,
    workAddress: String,
    cornerRadiusDp: Float,
    cardAlpha: Float,
    onSearchNavigate: (String) -> Unit,
    onOpenFullMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.cardBackground.copy(alpha = cardAlpha))
            .border(0.75.dp, tokens.cardBorder, shape)
    ) {
        // Authentic Apple Maps Day/Night CarPlay Vector Map
        val isNight = tokens.isNight
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onOpenFullMap)
        ) {
            val w = size.width
            val h = size.height

            val bgTop = if (isNight) Color(0xFF1A1D24) else Color(0xFFF6F4ED)
            val bgBottom = if (isNight) Color(0xFF212630) else Color(0xFFEEECE2)
            drawRect(brush = Brush.verticalGradient(listOf(bgTop, bgBottom)))

            // Water river bend (Apple Maps style)
            val waterColor = if (isNight) Color(0xFF152840) else Color(0xFFA7D7F9)
            val riverPath = Path().apply {
                moveTo(w * 0.72f, 0f)
                cubicTo(w * 0.76f, h * 0.35f, w * 0.88f, h * 0.65f, w * 0.95f, h)
                lineTo(w, h)
                lineTo(w, 0f)
                close()
            }
            drawPath(riverPath, waterColor)

            // Green Parks (Apple Maps sage tint)
            val parkColor = if (isNight) Color(0xFF182C22) else Color(0xFFD2EAD0)
            drawRect(parkColor, Offset(w * 0.04f, h * 0.16f), androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.25f))
            drawRect(parkColor, Offset(w * 0.14f, h * 0.62f), androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.18f))

            // Minor Streets
            val minorRoadColor = if (isNight) Color(0xFF2D3340) else Color(0xFFFFFFFF)
            for (i in 1..5) {
                val y = h * (i / 6f)
                drawLine(minorRoadColor, Offset(0f, y), Offset(w, y), strokeWidth = 3.5.dp.toPx())
            }
            for (j in 1..4) {
                val x = w * (j / 5f)
                drawLine(minorRoadColor, Offset(x, 0f), Offset(x, h), strokeWidth = 3.5.dp.toPx())
            }

            // Major Freeway
            val highwayColor = if (isNight) Color(0xFF3E4758) else Color(0xFFF9D876)
            val highwayPath = Path().apply {
                moveTo(w * 0.08f, h)
                cubicTo(w * 0.34f, h * 0.64f, w * 0.45f, h * 0.46f, w * 0.78f, 0f)
            }
            drawPath(
                path = highwayPath,
                color = highwayColor,
                style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Apple Maps Active Blue Route with subtle darker casing
            val routePath = Path().apply {
                moveTo(w * 0.48f, h * 0.76f)
                lineTo(w * 0.48f, h * 0.48f)
                quadraticBezierTo(w * 0.48f, h * 0.33f, w * 0.64f, h * 0.27f)
                lineTo(w * 0.78f, h * 0.16f)
            }
            drawPath(
                path = routePath,
                color = Color(0xFF0051B3),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = routePath,
                color = tokens.accentBlue,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Apple Blue 3D Navigation Puck
            val puckCenter = Offset(w * 0.48f, h * 0.56f)
            drawCircle(
                color = tokens.accentBlue.copy(alpha = 0.22f),
                radius = 28.dp.toPx(),
                center = puckCenter
            )
            drawCircle(
                color = Color.White,
                radius = 15.dp.toPx(),
                center = puckCenter
            )
            drawCircle(
                color = tokens.accentBlue,
                radius = 12.dp.toPx(),
                center = puckCenter
            )
            rotate(degrees = telemetry.bearingDegrees, pivot = puckCenter) {
                val arrow = Path().apply {
                    moveTo(puckCenter.x, puckCenter.y - 7.dp.toPx())
                    lineTo(puckCenter.x + 5.dp.toPx(), puckCenter.y + 5.dp.toPx())
                    lineTo(puckCenter.x, puckCenter.y + 2.dp.toPx())
                    lineTo(puckCenter.x - 5.dp.toPx(), puckCenter.y + 5.dp.toPx())
                    close()
                }
                drawPath(arrow, Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Floating Apple Maps Search Pill + Provider Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.cardBackground.copy(alpha = 0.90f))
                        .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = tokens.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.nav_search_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(tokens.accentBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        onSearchNavigate(searchQuery.trim())
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nav_search_input")
                        )
                    }
                    if (searchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.accentBlue)
                                .clickable { onSearchNavigate(searchQuery.trim()) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.nav_go),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.cardBackground.copy(alpha = 0.90f))
                        .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
                        .testTag("nav_open_maps_button")
                        .clickable(onClick = onOpenFullMap)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription = stringResource(R.string.nav_open_maps),
                        tint = tokens.accentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = navProvider.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            // Bottom: GPS Pill + Apple Maps Quick Destination Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tokens.cardBackground.copy(alpha = 0.88f))
                        .border(0.75.dp, tokens.cardBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.hasGpsFix) tokens.positiveGreen else tokens.warningOrange)
                    )
                    Text(
                        text = if (telemetry.hasGpsFix) stringResource(R.string.nav_gps_active)
                        else stringResource(R.string.nav_gps_searching),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavShortcutChip(
                        icon = Icons.Rounded.Home,
                        label = stringResource(R.string.nav_home),
                        tint = tokens.accentBlue,
                        testTag = "nav_shortcut_home",
                        modifier = Modifier.weight(1f),
                        onClick = { onSearchNavigate(homeAddress.ifBlank { "Home" }) }
                    )
                    NavShortcutChip(
                        icon = Icons.Rounded.Work,
                        label = stringResource(R.string.nav_work),
                        tint = tokens.warningOrange,
                        testTag = "nav_shortcut_work",
                        modifier = Modifier.weight(1f),
                        onClick = { onSearchNavigate(workAddress.ifBlank { "Work" }) }
                    )
                    NavShortcutChip(
                        icon = Icons.Rounded.LocalGasStation,
                        label = stringResource(R.string.nav_gas),
                        tint = tokens.positiveGreen,
                        testTag = "nav_shortcut_gas",
                        modifier = Modifier.weight(1.25f),
                        onClick = { onSearchNavigate("gas station") }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavShortcutChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    Row(
        modifier = modifier
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.cardBackground.copy(alpha = 0.90f))
            .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(14.dp))
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
