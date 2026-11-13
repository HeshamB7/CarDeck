package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AppLauncherHelper
import com.example.model.AppEntry
import com.example.ui.theme.CarDisplayFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Pure Apple iOS / CarPlay continuous Squircle shape (22.5% corner curvature).
 * Built using a smooth superellipse curve |x/rx|^n + |y/ry|^n = 1 (n ≈ 4.65)
 * which eliminates the flat-to-arc tangent seam of standard rounded rectangles.
 */
object AppleSquircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(createSquirclePath(size.width, size.height))
}

fun computeSuperellipsePoint(
    theta: Double,
    width: Float,
    height: Float,
    exponent: Double = 4.65
): Pair<Float, Float> {
    val rx = width / 2f
    val ry = height / 2f
    val exp = 2.0 / exponent
    val cosT = cos(theta)
    val sinT = sin(theta)
    val x = rx + (rx * sign(cosT) * abs(cosT).pow(exp)).toFloat()
    val y = ry + (ry * sign(sinT) * abs(sinT).pow(exp)).toFloat()
    return x to y
}

fun createSquirclePath(width: Float, height: Float, exponent: Double = 4.65): Path {
    val path = Path()
    val steps = 120
    for (i in 0..steps) {
        val theta = (2.0 * PI * i) / steps
        val (x, y) = computeSuperellipsePoint(theta, width, height, exponent)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Custom Squircle Icon component that:
 * 1. Forces ANY installed Android app into an exact uniform 22.5% Apple Squircle frame
 *    with opaque background normalization (removing weird transparent margins).
 * 2. Renders crisp resolution-independent Vector / Canvas artwork for core Apple CarPlay apps
 *    (Phone, Maps, Music, Messages, Settings, Now Playing, Podcasts, Audiobooks, Calendar)
 *    matching the exact colors, gradients, and proportions of the reference screenshots.
 */
@Composable
fun CarPlaySquircleIcon(
    app: AppEntry,
    bitmap: ImageBitmap?,
    size: Dp,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    elevation: Dp = 6.dp
) {
    val coreKind = remember(app.packageName, app.virtualAction, app.label) {
        detectCoreAppleVectorKind(app)
    }
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = elevation,
                    shape = AppleSquircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.42f),
                    spotColor = Color.Black.copy(alpha = 0.42f)
                )
                .clip(AppleSquircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            if (coreKind != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawAppleCoreVectorIcon(coreKind, textMeasurer)
                }
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Clean fallback squircle when bitmap is still loading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CarPlaySymbol(
                        type = CarPlaySymbolType.APPS_GRID,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(size * 0.46f)
                    )
                }
            }

            // Subtle Apple top specular glass rim inside the 22.5% squircle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = createSquirclePath(this.size.width, this.size.height)
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.28f),
                        0.32f to Color.White.copy(alpha = 0.04f),
                        1f to Color.Transparent
                    ),
                    style = Stroke(width = (this.size.minDimension * 0.018f).coerceAtLeast(1.2f))
                )
            }
        }

        if (badgeCount > 0) {
            val badgeSize = (size * 0.31f).coerceAtLeast(18.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = badgeSize * 0.18f, y = -badgeSize * 0.18f)
                    .size(badgeSize)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30)),
                contentAlignment = Alignment.Center
            ) {
                CarPlayText(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    style = TextStyle(
                        fontFamily = CarDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (badgeSize.value * 0.56f).sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = Color.White
                )
            }
        }
    }
}

private enum class CoreAppleVectorKind {
    PHONE,
    MUSIC,
    MAPS,
    MESSAGES,
    NOW_PLAYING,
    PODCASTS,
    AUDIOBOOKS,
    CALENDAR,
    SETTINGS
}

private fun detectCoreAppleVectorKind(app: AppEntry): CoreAppleVectorKind? {
    return when (app.virtualAction) {
        AppLauncherHelper.VIRTUAL_PHONE -> CoreAppleVectorKind.PHONE
        AppLauncherHelper.VIRTUAL_MUSIC -> CoreAppleVectorKind.MUSIC
        AppLauncherHelper.VIRTUAL_MAPS -> CoreAppleVectorKind.MAPS
        AppLauncherHelper.VIRTUAL_MESSAGES -> CoreAppleVectorKind.MESSAGES
        AppLauncherHelper.VIRTUAL_NOW_PLAYING -> CoreAppleVectorKind.NOW_PLAYING
        AppLauncherHelper.VIRTUAL_PODCASTS -> CoreAppleVectorKind.PODCASTS
        AppLauncherHelper.VIRTUAL_AUDIOBOOKS -> CoreAppleVectorKind.AUDIOBOOKS
        AppLauncherHelper.VIRTUAL_CALENDAR -> CoreAppleVectorKind.CALENDAR
        AppLauncherHelper.VIRTUAL_SETTINGS -> CoreAppleVectorKind.SETTINGS
        else -> {
            val pkg = app.packageName.lowercase(Locale.US)
            val lbl = app.label.lowercase(Locale.US)
            when {
                pkg.contains("dialer") || pkg.contains("telecom") || lbl == "phone" -> CoreAppleVectorKind.PHONE
                pkg.contains("messaging") || pkg.contains("mms") || lbl == "messages" -> CoreAppleVectorKind.MESSAGES
                pkg == "com.google.android.apps.maps" -> CoreAppleVectorKind.MAPS
                pkg.contains("calendar") -> CoreAppleVectorKind.CALENDAR
                pkg == "com.android.settings" -> CoreAppleVectorKind.SETTINGS
                pkg.contains("podcasts") -> CoreAppleVectorKind.PODCASTS
                pkg.contains("audible") -> CoreAppleVectorKind.AUDIOBOOKS
                else -> null
            }
        }
    }
}

private fun DrawScope.drawAppleCoreVectorIcon(
    kind: CoreAppleVectorKind,
    textMeasurer: TextMeasurer
) {
    val s = size.minDimension
    when (kind) {
        CoreAppleVectorKind.PHONE -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF67ED75), Color(0xFF1AC936))))
            translate(left = s * 0.50f, top = s * 0.50f) {
                rotate(degrees = -42f, pivot = Offset.Zero) {
                    val handset = Path().apply {
                        moveTo(-s * 0.25f, -s * 0.08f)
                        cubicTo(-s * 0.27f, -s * 0.20f, -s * 0.14f, -s * 0.22f, -s * 0.10f, -s * 0.14f)
                        lineTo(-s * 0.08f, -s * 0.06f)
                        cubicTo(-s * 0.07f, -s * 0.02f, -s * 0.12f, 0.00f, -s * 0.10f, s * 0.03f)
                        cubicTo(-s * 0.04f, s * 0.08f, s * 0.04f, s * 0.08f, s * 0.10f, s * 0.03f)
                        cubicTo(s * 0.12f, 0.00f, s * 0.07f, -s * 0.02f, s * 0.08f, -s * 0.06f)
                        lineTo(s * 0.10f, -s * 0.14f)
                        cubicTo(s * 0.14f, -s * 0.22f, s * 0.27f, -s * 0.20f, s * 0.25f, -s * 0.08f)
                        cubicTo(s * 0.22f, s * 0.14f, -s * 0.22f, s * 0.14f, -s * 0.25f, -s * 0.08f)
                        close()
                    }
                    drawPath(handset, Color.White)
                }
            }
        }

        CoreAppleVectorKind.MESSAGES -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF67ED75), Color(0xFF1AC936))))
            drawOval(
                color = Color.White,
                topLeft = Offset(s * 0.18f, s * 0.22f),
                size = Size(s * 0.64f, s * 0.50f)
            )
            val tail = Path().apply {
                moveTo(s * 0.28f, s * 0.63f)
                lineTo(s * 0.21f, s * 0.79f)
                quadraticBezierTo(s * 0.34f, s * 0.74f, s * 0.42f, s * 0.68f)
                close()
            }
            drawPath(tail, Color.White)
        }

        CoreAppleVectorKind.MUSIC -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFA5C72), Color(0xFFFA233B))))
            rotate(degrees = -16f, pivot = Offset(s * 0.31f, s * 0.68f)) {
                drawOval(Color.White, Offset(s * 0.21f, s * 0.61f), Size(s * 0.19f, s * 0.14f))
            }
            rotate(degrees = -16f, pivot = Offset(s * 0.65f, s * 0.62f)) {
                drawOval(Color.White, Offset(s * 0.55f, s * 0.55f), Size(s * 0.19f, s * 0.14f))
            }
            val stemsAndBeam = Path().apply {
                moveTo(s * 0.34f, s * 0.66f)
                lineTo(s * 0.34f, s * 0.28f)
                lineTo(s * 0.73f, s * 0.20f)
                lineTo(s * 0.73f, s * 0.60f)
                lineTo(s * 0.67f, s * 0.60f)
                lineTo(s * 0.67f, s * 0.33f)
                lineTo(s * 0.40f, s * 0.39f)
                lineTo(s * 0.40f, s * 0.66f)
                close()
            }
            drawPath(stemsAndBeam, Color.White)
        }

        CoreAppleVectorKind.NOW_PLAYING -> {
            // Exact Apple CarPlay iOS 17/18 Now Playing icon: White squircle with 5 red/pink vertical waveform bars
            drawRect(Color.White)
            val barBrush = Brush.verticalGradient(listOf(Color(0xFFFF375F), Color(0xFFFA233B)))
            val barWidths = s * 0.085f
            val heights = listOf(0.24f, 0.42f, 0.60f, 0.38f, 0.22f)
            val xCenters = listOf(0.24f, 0.37f, 0.50f, 0.63f, 0.76f)
            heights.forEachIndexed { idx, hFrac ->
                val bh = s * hFrac
                val cx = s * xCenters[idx]
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(cx - barWidths / 2f, (s - bh) / 2f),
                    size = Size(barWidths, bh),
                    cornerRadius = CornerRadius(barWidths / 2f, barWidths / 2f)
                )
            }
        }

        CoreAppleVectorKind.MAPS -> {
            // Cream Apple Maps base
            drawRect(Color(0xFFF4F1E8))
            // Top-left green park
            drawRect(Color(0xFF72D76D), Offset.Zero, Size(s * 0.44f, s * 0.52f))
            // Bottom-right blush urban block
            drawRect(Color(0xFFFCCED6), Offset(s * 0.56f, s * 0.44f), Size(s * 0.44f, s * 0.56f))
            // Yellow cross arterial street
            drawLine(Color(0xFFFFCC00), Offset(0f, s * 0.52f), Offset(s, s * 0.39f), strokeWidth = s * 0.11f)
            // White-cased blue Interstate highway
            drawLine(Color.White, Offset(s * 0.47f, s), Offset(s * 0.52f, 0f), strokeWidth = s * 0.22f, cap = StrokeCap.Round)
            drawLine(Color(0xFF0A84FF), Offset(s * 0.47f, s), Offset(s * 0.52f, 0f), strokeWidth = s * 0.14f, cap = StrokeCap.Round)
            // Classic red/blue highway shield (top-right)
            drawRoundRect(Color.White, Offset(s * 0.64f, s * 0.10f), Size(s * 0.26f, s * 0.24f), CornerRadius(s * 0.06f, s * 0.06f))
            drawRoundRect(Color(0xFFFF3B30), Offset(s * 0.66f, s * 0.12f), Size(s * 0.22f, s * 0.07f), CornerRadius(s * 0.03f, s * 0.03f))
            drawRoundRect(Color(0xFF007AFF), Offset(s * 0.66f, s * 0.18f), Size(s * 0.22f, s * 0.14f), CornerRadius(s * 0.03f, s * 0.03f))
            // Blue circular navigation puck
            val cx = s * 0.48f
            val cy = s * 0.62f
            drawCircle(Color.White, radius = s * 0.24f, center = Offset(cx, cy))
            drawCircle(Color(0xFF007AFF), radius = s * 0.20f, center = Offset(cx, cy))
            val arrow = Path().apply {
                moveTo(cx, cy - s * 0.11f)
                lineTo(cx + s * 0.09f, cy + s * 0.09f)
                lineTo(cx, cy + s * 0.04f)
                lineTo(cx - s * 0.09f, cy + s * 0.09f)
                close()
            }
            drawPath(arrow, Color.White)
        }

        CoreAppleVectorKind.PODCASTS -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFC865FA), Color(0xFF7B22E2))))
            val arcStroke = Stroke(width = s * 0.052f, cap = StrokeCap.Round)
            drawArc(Color.White, 140f, 260f, false, Offset(s * 0.20f, s * 0.18f), Size(s * 0.60f, s * 0.60f), style = arcStroke)
            drawArc(Color.White, 145f, 250f, false, Offset(s * 0.31f, s * 0.29f), Size(s * 0.38f, s * 0.38f), style = arcStroke)
            drawCircle(Color.White, radius = s * 0.078f, center = Offset(s * 0.50f, s * 0.47f))
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(s * 0.435f, s * 0.57f),
                size = Size(s * 0.13f, s * 0.22f),
                cornerRadius = CornerRadius(s * 0.06f, s * 0.06f)
            )
        }

        CoreAppleVectorKind.AUDIOBOOKS -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFB638), Color(0xFFFF6A00))))
            val leftPage = Path().apply {
                moveTo(s * 0.485f, s * 0.31f)
                quadraticBezierTo(s * 0.34f, s * 0.26f, s * 0.20f, s * 0.31f)
                lineTo(s * 0.20f, s * 0.71f)
                quadraticBezierTo(s * 0.34f, s * 0.66f, s * 0.485f, s * 0.72f)
                close()
            }
            val rightPage = Path().apply {
                moveTo(s * 0.515f, s * 0.31f)
                quadraticBezierTo(s * 0.66f, s * 0.26f, s * 0.80f, s * 0.31f)
                lineTo(s * 0.80f, s * 0.71f)
                quadraticBezierTo(s * 0.66f, s * 0.66f, s * 0.515f, s * 0.72f)
                close()
            }
            drawPath(leftPage, Color.White)
            drawPath(rightPage, Color.White)
        }

        CoreAppleVectorKind.CALENDAR -> {
            // Exact iOS 18 CarPlay Calendar: crisp white card, red weekday, bold black date number
            drawRect(Color.White)
            val now = Date()
            val dayName = SimpleDateFormat("EEE", Locale.US).format(now).uppercase(Locale.US)
            val dayNum = SimpleDateFormat("d", Locale.US).format(now)

            val dayLayout = textMeasurer.measure(
                text = dayName,
                style = TextStyle(
                    fontFamily = CarDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (s * 0.16f / density).sp,
                    color = Color(0xFFFF3B30),
                    letterSpacing = 0.6.sp
                )
            )
            drawText(
                textLayoutResult = dayLayout,
                topLeft = Offset((s - dayLayout.size.width) / 2f, s * 0.13f)
            )

            val numLayout = textMeasurer.measure(
                text = dayNum,
                style = TextStyle(
                    fontFamily = CarDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (s * 0.52f / density).sp,
                    color = Color(0xFF1C1C1E),
                    fontFeatureSettings = "tnum"
                )
            )
            drawText(
                textLayoutResult = numLayout,
                topLeft = Offset((s - numLayout.size.width) / 2f, s * 0.31f)
            )
        }

        CoreAppleVectorKind.SETTINGS -> {
            // Authentic Apple brushed metallic Settings gear with 24 fine outer notches and 3 inner spokes
            drawRect(Brush.verticalGradient(listOf(Color(0xFFA2A4AC), Color(0xFF60626A))))
            val cx = s * 0.5f
            val cy = s * 0.5f
            val center = Offset(cx, cy)

            // Outer metallic bezel
            drawCircle(Color(0xFFDADCE2), radius = s * 0.40f, center = center)
            drawCircle(Color(0xFF2B2B2E), radius = s * 0.34f, center = center)

            // 24 fine gear teeth along outer ring
            for (i in 0 until 24) {
                rotate(degrees = i * 15f, pivot = center) {
                    drawRoundRect(
                        color = Color(0xFFE5E6EC),
                        topLeft = Offset(cx - s * 0.025f, cy - s * 0.43f),
                        size = Size(s * 0.05f, s * 0.10f),
                        cornerRadius = CornerRadius(s * 0.015f, s * 0.015f)
                    )
                }
            }

            // Inner metallic ring + 3 precision spokes
            drawCircle(Color(0xFFC5C7CE), radius = s * 0.25f, center = center)
            drawCircle(Color(0xFF2B2B2E), radius = s * 0.18f, center = center)
            for (i in 0 until 3) {
                rotate(degrees = i * 120f, pivot = center) {
                    drawRoundRect(
                        color = Color(0xFFD0D2D8),
                        topLeft = Offset(cx - s * 0.032f, cy - s * 0.26f),
                        size = Size(s * 0.064f, s * 0.26f),
                        cornerRadius = CornerRadius(s * 0.02f, s * 0.02f)
                    )
                }
            }
            // Center metallic axle hub
            drawCircle(Color(0xFFE2E4EA), radius = s * 0.11f, center = center)
            drawCircle(Color(0xFF3A3A3D), radius = s * 0.06f, center = center)
        }
    }
}
