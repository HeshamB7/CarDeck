package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Pure Apple SF-Symbol-inspired custom vector glyphs drawn with Compose Canvas.
 * Replaces all Android Material icons across the CarPlay UI.
 */
enum class CarPlaySymbolType {
    BACK_CHEVRON,
    SEARCH,
    HOME,
    WORK,
    FUEL,
    NAVIGATION_ARROW,
    TURN_STRAIGHT,
    PLAY,
    PAUSE,
    NEXT_TRACK,
    PREV_TRACK,
    MUSIC_NOTE,
    MOON,
    SETTINGS_GEAR,
    WIFI,
    BLUETOOTH,
    HOTSPOT,
    SPLIT_SCREEN,
    SPEEDOMETER,
    CALENDAR,
    LOCATION_PIN,
    LOCK,
    BACKSPACE,
    CHECK_CIRCLE,
    WARNING,
    SUN,
    SLIDERS,
    APPS_GRID,
    PHOTO,
    BACKUP,
    RESTORE,
    EYE_SHOW,
    EYE_HIDE,
    RENAME,
    PIN,
    OPEN_APP,
    ARROW_LEFT,
    ARROW_RIGHT,
    PHONE,
    BATTERY,
    BOLT,
    WEATHER_CLOUD,
    WEATHER_RAIN,
    CLOCK,
    PLUS,
    MINUS,
    PENCIL,
    TRASH,
    CHECK,
    REFRESH,
    COMPASS
}

@Composable
fun CarPlaySymbol(
    type: CarPlaySymbolType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thinStroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (type) {
            CarPlaySymbolType.BACK_CHEVRON -> {
                val p = Path().apply {
                    moveTo(w * 0.64f, h * 0.18f)
                    lineTo(w * 0.30f, h * 0.50f)
                    lineTo(w * 0.64f, h * 0.82f)
                }
                drawPath(p, tint, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            CarPlaySymbolType.ARROW_LEFT -> {
                drawLine(tint, Offset(w * 0.78f, h * 0.50f), Offset(w * 0.24f, h * 0.50f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
                val p = Path().apply {
                    moveTo(w * 0.46f, h * 0.26f)
                    lineTo(w * 0.22f, h * 0.50f)
                    lineTo(w * 0.46f, h * 0.74f)
                }
                drawPath(p, tint, style = stroke)
            }

            CarPlaySymbolType.ARROW_RIGHT -> {
                drawLine(tint, Offset(w * 0.22f, h * 0.50f), Offset(w * 0.76f, h * 0.50f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
                val p = Path().apply {
                    moveTo(w * 0.54f, h * 0.26f)
                    lineTo(w * 0.78f, h * 0.50f)
                    lineTo(w * 0.54f, h * 0.74f)
                }
                drawPath(p, tint, style = stroke)
            }

            CarPlaySymbolType.SEARCH -> {
                drawCircle(tint, radius = w * 0.27f, center = Offset(w * 0.43f, h * 0.43f), style = stroke)
                drawLine(tint, Offset(w * 0.63f, h * 0.63f), Offset(w * 0.84f, h * 0.84f), strokeWidth = w * 0.115f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.HOME -> {
                val roof = Path().apply {
                    moveTo(w * 0.14f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.48f)
                }
                drawPath(roof, tint, style = stroke)
                val body = Path().apply {
                    moveTo(w * 0.24f, h * 0.44f)
                    lineTo(w * 0.24f, h * 0.84f)
                    lineTo(w * 0.76f, h * 0.84f)
                    lineTo(w * 0.76f, h * 0.44f)
                }
                drawPath(body, tint, style = stroke)
            }

            CarPlaySymbolType.WORK -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.30f),
                    size = Size(w * 0.72f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                    style = stroke
                )
                val handle = Path().apply {
                    moveTo(w * 0.36f, h * 0.30f)
                    lineTo(w * 0.36f, h * 0.20f)
                    lineTo(w * 0.64f, h * 0.20f)
                    lineTo(w * 0.64f, h * 0.30f)
                }
                drawPath(handle, tint, style = stroke)
            }

            CarPlaySymbolType.FUEL -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = Size(w * 0.42f, h * 0.66f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = thinStroke
                )
                drawRect(tint, Offset(w * 0.25f, h * 0.26f), Size(w * 0.28f, h * 0.18f))
                val hose = Path().apply {
                    moveTo(w * 0.60f, h * 0.38f)
                    lineTo(w * 0.76f, h * 0.38f)
                    lineTo(w * 0.76f, h * 0.74f)
                    quadraticBezierTo(w * 0.76f, h * 0.82f, w * 0.68f, h * 0.82f)
                    lineTo(w * 0.60f, h * 0.82f)
                }
                drawPath(hose, tint, style = thinStroke)
            }

            CarPlaySymbolType.NAVIGATION_ARROW -> {
                val p = Path().apply {
                    moveTo(w * 0.84f, h * 0.16f)
                    lineTo(w * 0.14f, h * 0.46f)
                    lineTo(w * 0.46f, h * 0.54f)
                    lineTo(w * 0.54f, h * 0.86f)
                    close()
                }
                drawPath(p, tint)
            }

            CarPlaySymbolType.TURN_STRAIGHT -> {
                drawLine(tint, Offset(w * 0.50f, h * 0.86f), Offset(w * 0.50f, h * 0.20f), strokeWidth = w * 0.14f, cap = StrokeCap.Round)
                val head = Path().apply {
                    moveTo(w * 0.24f, h * 0.42f)
                    lineTo(w * 0.50f, h * 0.15f)
                    lineTo(w * 0.76f, h * 0.42f)
                }
                drawPath(head, tint, style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            CarPlaySymbolType.PLAY -> {
                val p = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.80f, h * 0.50f)
                    lineTo(w * 0.28f, h * 0.82f)
                    close()
                }
                drawPath(p, tint)
            }

            CarPlaySymbolType.PAUSE -> {
                val barW = w * 0.22f
                val barH = h * 0.62f
                val top = h * 0.19f
                val r = CornerRadius(w * 0.06f, w * 0.06f)
                drawRoundRect(tint, Offset(w * 0.22f, top), Size(barW, barH), r)
                drawRoundRect(tint, Offset(w * 0.56f, top), Size(barW, barH), r)
            }

            CarPlaySymbolType.NEXT_TRACK -> {
                val p1 = Path().apply {
                    moveTo(w * 0.14f, h * 0.24f)
                    lineTo(w * 0.52f, h * 0.50f)
                    lineTo(w * 0.14f, h * 0.76f)
                    close()
                }
                val p2 = Path().apply {
                    moveTo(w * 0.48f, h * 0.24f)
                    lineTo(w * 0.86f, h * 0.50f)
                    lineTo(w * 0.48f, h * 0.76f)
                    close()
                }
                drawPath(p1, tint)
                drawPath(p2, tint)
            }

            CarPlaySymbolType.PREV_TRACK -> {
                val p1 = Path().apply {
                    moveTo(w * 0.86f, h * 0.24f)
                    lineTo(w * 0.48f, h * 0.50f)
                    lineTo(w * 0.86f, h * 0.76f)
                    close()
                }
                val p2 = Path().apply {
                    moveTo(w * 0.52f, h * 0.24f)
                    lineTo(w * 0.14f, h * 0.50f)
                    lineTo(w * 0.52f, h * 0.76f)
                    close()
                }
                drawPath(p1, tint)
                drawPath(p2, tint)
            }

            CarPlaySymbolType.MUSIC_NOTE -> {
                drawOval(tint, Offset(w * 0.20f, h * 0.64f), Size(w * 0.22f, h * 0.16f))
                drawOval(tint, Offset(w * 0.58f, h * 0.56f), Size(w * 0.22f, h * 0.16f))
                val stems = Path().apply {
                    moveTo(w * 0.36f, h * 0.70f)
                    lineTo(w * 0.36f, h * 0.26f)
                    lineTo(w * 0.76f, h * 0.18f)
                    lineTo(w * 0.76f, h * 0.62f)
                    lineTo(w * 0.68f, h * 0.62f)
                    lineTo(w * 0.68f, h * 0.31f)
                    lineTo(w * 0.44f, h * 0.37f)
                    lineTo(w * 0.44f, h * 0.70f)
                    close()
                }
                drawPath(stems, tint)
            }

            CarPlaySymbolType.MOON -> {
                val moon = Path().apply {
                    moveTo(w * 0.60f, h * 0.16f)
                    cubicTo(w * 0.32f, h * 0.18f, w * 0.16f, h * 0.40f, w * 0.22f, h * 0.64f)
                    cubicTo(w * 0.28f, h * 0.84f, w * 0.56f, h * 0.90f, w * 0.78f, h * 0.74f)
                    cubicTo(w * 0.52f, h * 0.76f, w * 0.40f, h * 0.52f, w * 0.50f, h * 0.30f)
                    cubicTo(w * 0.53f, h * 0.24f, w * 0.56f, h * 0.20f, w * 0.60f, h * 0.16f)
                    close()
                }
                drawPath(moon, tint)
            }

            CarPlaySymbolType.SETTINGS_GEAR -> {
                val cx = w * 0.5f
                val cy = h * 0.5f
                drawCircle(tint, radius = w * 0.26f, center = Offset(cx, cy), style = Stroke(width = w * 0.13f))
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    val dx = kotlin.math.cos(angle).toFloat()
                    val dy = kotlin.math.sin(angle).toFloat()
                    drawLine(
                        tint,
                        Offset(cx + dx * w * 0.28f, cy + dy * h * 0.28f),
                        Offset(cx + dx * w * 0.41f, cy + dy * h * 0.41f),
                        strokeWidth = w * 0.13f,
                        cap = StrokeCap.Round
                    )
                }
            }

            CarPlaySymbolType.WIFI -> {
                drawArc(tint, 225f, 90f, false, Offset(w * 0.10f, h * 0.22f), Size(w * 0.80f, h * 0.80f), style = thinStroke)
                drawArc(tint, 225f, 90f, false, Offset(w * 0.24f, h * 0.38f), Size(w * 0.52f, h * 0.52f), style = thinStroke)
                drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.74f))
            }

            CarPlaySymbolType.BLUETOOTH -> {
                val bt = Path().apply {
                    moveTo(w * 0.32f, h * 0.34f)
                    lineTo(w * 0.68f, h * 0.66f)
                    lineTo(w * 0.50f, h * 0.84f)
                    lineTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.68f, h * 0.34f)
                    lineTo(w * 0.32f, h * 0.66f)
                }
                drawPath(bt, tint, style = thinStroke)
            }

            CarPlaySymbolType.HOTSPOT -> {
                drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.56f))
                drawArc(tint, 145f, 250f, false, Offset(w * 0.26f, h * 0.32f), Size(w * 0.48f, h * 0.48f), style = thinStroke)
                drawArc(tint, 140f, 260f, false, Offset(w * 0.12f, h * 0.18f), Size(w * 0.76f, h * 0.76f), style = thinStroke)
            }

            CarPlaySymbolType.SPLIT_SCREEN -> {
                val r = CornerRadius(w * 0.10f, w * 0.10f)
                drawRoundRect(tint, Offset(w * 0.12f, h * 0.18f), Size(w * 0.34f, h * 0.64f), r, style = thinStroke)
                drawRoundRect(tint, Offset(w * 0.54f, h * 0.18f), Size(w * 0.34f, h * 0.64f), r, style = thinStroke)
            }

            CarPlaySymbolType.SPEEDOMETER -> {
                drawArc(tint, 145f, 250f, false, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.72f), style = stroke)
                drawLine(tint, Offset(w * 0.50f, h * 0.56f), Offset(w * 0.69f, h * 0.36f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.56f))
            }

            CarPlaySymbolType.CALENDAR -> {
                val r = CornerRadius(w * 0.12f, w * 0.12f)
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.20f), Size(w * 0.72f, h * 0.66f), r, style = thinStroke)
                drawLine(tint, Offset(w * 0.14f, h * 0.38f), Offset(w * 0.86f, h * 0.38f), strokeWidth = w * 0.085f)
                drawLine(tint, Offset(w * 0.34f, h * 0.12f), Offset(w * 0.34f, h * 0.26f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.66f, h * 0.12f), Offset(w * 0.66f, h * 0.26f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.LOCATION_PIN -> {
                drawCircle(tint, radius = w * 0.26f, center = Offset(w * 0.50f, h * 0.38f), style = thinStroke)
                val tip = Path().apply {
                    moveTo(w * 0.26f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.86f)
                    lineTo(w * 0.74f, h * 0.48f)
                }
                drawPath(tip, tint, style = thinStroke)
            }

            CarPlaySymbolType.LOCK -> {
                drawRoundRect(
                    tint,
                    Offset(w * 0.20f, h * 0.44f),
                    Size(w * 0.60f, h * 0.42f),
                    CornerRadius(w * 0.10f, w * 0.10f),
                    style = thinStroke
                )
                drawArc(tint, 180f, 180f, false, Offset(w * 0.30f, h * 0.16f), Size(w * 0.40f, h * 0.54f), style = thinStroke)
            }

            CarPlaySymbolType.BACKSPACE -> {
                val box = Path().apply {
                    moveTo(w * 0.34f, h * 0.24f)
                    lineTo(w * 0.86f, h * 0.24f)
                    lineTo(w * 0.86f, h * 0.76f)
                    lineTo(w * 0.34f, h * 0.76f)
                    lineTo(w * 0.14f, h * 0.50f)
                    close()
                }
                drawPath(box, tint, style = thinStroke)
                drawLine(tint, Offset(w * 0.48f, h * 0.40f), Offset(w * 0.70f, h * 0.60f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.70f, h * 0.40f), Offset(w * 0.48f, h * 0.60f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.CHECK_CIRCLE -> {
                drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f))
                val check = Path().apply {
                    moveTo(w * 0.32f, h * 0.50f)
                    lineTo(w * 0.45f, h * 0.64f)
                    lineTo(w * 0.69f, h * 0.36f)
                }
                drawPath(check, Color.White, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            CarPlaySymbolType.WARNING -> {
                val tri = Path().apply {
                    moveTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.82f)
                    lineTo(w * 0.14f, h * 0.82f)
                    close()
                }
                drawPath(tri, tint, style = thinStroke)
                drawLine(tint, Offset(w * 0.50f, h * 0.42f), Offset(w * 0.50f, h * 0.58f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.70f))
            }

            CarPlaySymbolType.SUN -> {
                drawCircle(tint, radius = w * 0.21f, center = Offset(w * 0.50f, h * 0.50f))
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val dx = kotlin.math.cos(a).toFloat()
                    val dy = kotlin.math.sin(a).toFloat()
                    drawLine(
                        tint,
                        Offset(w * 0.5f + dx * w * 0.30f, h * 0.5f + dy * h * 0.30f),
                        Offset(w * 0.5f + dx * w * 0.42f, h * 0.5f + dy * h * 0.42f),
                        strokeWidth = w * 0.085f,
                        cap = StrokeCap.Round
                    )
                }
            }

            CarPlaySymbolType.SLIDERS -> {
                val ys = listOf(0.28f, 0.50f, 0.72f)
                val xs = listOf(0.36f, 0.68f, 0.44f)
                ys.forEachIndexed { idx, y ->
                    drawLine(tint, Offset(w * 0.14f, h * y), Offset(w * 0.86f, h * y), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                    drawCircle(tint, radius = w * 0.09f, center = Offset(w * xs[idx], h * y))
                }
            }

            CarPlaySymbolType.APPS_GRID -> {
                val cell = w * 0.22f
                val gap = w * 0.09f
                val r = CornerRadius(w * 0.06f, w * 0.06f)
                for (row in 0 until 3) {
                    for (col in 0 until 3) {
                        drawRoundRect(
                            tint,
                            Offset(w * 0.08f + col * (cell + gap), h * 0.08f + row * (cell + gap)),
                            Size(cell, cell),
                            r
                        )
                    }
                }
            }

            CarPlaySymbolType.PHOTO -> {
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.22f), Size(w * 0.72f, h * 0.56f), CornerRadius(w * 0.10f, w * 0.10f), style = thinStroke)
                drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.34f, h * 0.40f))
                val mtn = Path().apply {
                    moveTo(w * 0.22f, h * 0.70f)
                    lineTo(w * 0.44f, h * 0.50f)
                    lineTo(w * 0.58f, h * 0.62f)
                    lineTo(w * 0.70f, h * 0.48f)
                    lineTo(w * 0.80f, h * 0.70f)
                }
                drawPath(mtn, tint, style = thinStroke)
            }

            CarPlaySymbolType.BACKUP -> {
                drawArc(tint, 180f, 180f, false, Offset(w * 0.18f, h * 0.30f), Size(w * 0.64f, h * 0.48f), style = thinStroke)
                drawLine(tint, Offset(w * 0.50f, h * 0.80f), Offset(w * 0.50f, h * 0.42f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                val up = Path().apply {
                    moveTo(w * 0.36f, h * 0.54f)
                    lineTo(w * 0.50f, h * 0.38f)
                    lineTo(w * 0.64f, h * 0.54f)
                }
                drawPath(up, tint, style = thinStroke)
            }

            CarPlaySymbolType.RESTORE -> {
                drawArc(tint, 40f, 280f, false, Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.64f), style = thinStroke)
                val arrow = Path().apply {
                    moveTo(w * 0.14f, h * 0.32f)
                    lineTo(w * 0.24f, h * 0.48f)
                    lineTo(w * 0.38f, h * 0.36f)
                }
                drawPath(arrow, tint, style = thinStroke)
            }

            CarPlaySymbolType.EYE_SHOW -> {
                val eye = Path().apply {
                    moveTo(w * 0.10f, h * 0.50f)
                    quadraticBezierTo(w * 0.50f, h * 0.18f, w * 0.90f, h * 0.50f)
                    quadraticBezierTo(w * 0.50f, h * 0.82f, w * 0.10f, h * 0.50f)
                }
                drawPath(eye, tint, style = thinStroke)
                drawCircle(tint, radius = w * 0.13f, center = Offset(w * 0.50f, h * 0.50f))
            }

            CarPlaySymbolType.EYE_HIDE -> {
                val eye = Path().apply {
                    moveTo(w * 0.12f, h * 0.50f)
                    quadraticBezierTo(w * 0.50f, h * 0.22f, w * 0.88f, h * 0.50f)
                }
                drawPath(eye, tint, style = thinStroke)
                drawLine(tint, Offset(w * 0.20f, h * 0.80f), Offset(w * 0.80f, h * 0.20f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.RENAME -> {
                val pen = Path().apply {
                    moveTo(w * 0.20f, h * 0.80f)
                    lineTo(w * 0.26f, h * 0.60f)
                    lineTo(w * 0.68f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.32f)
                    lineTo(w * 0.40f, h * 0.74f)
                    close()
                }
                drawPath(pen, tint, style = thinStroke)
            }

            CarPlaySymbolType.PIN -> {
                drawLine(tint, Offset(w * 0.32f, h * 0.22f), Offset(w * 0.68f, h * 0.22f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.38f, h * 0.22f), Offset(w * 0.34f, h * 0.52f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.62f, h * 0.22f), Offset(w * 0.66f, h * 0.52f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.24f, h * 0.52f), Offset(w * 0.76f, h * 0.52f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.50f, h * 0.52f), Offset(w * 0.50f, h * 0.84f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.OPEN_APP -> {
                drawRoundRect(tint, Offset(w * 0.16f, h * 0.24f), Size(w * 0.60f, h * 0.60f), CornerRadius(w * 0.10f, w * 0.10f), style = thinStroke)
                drawLine(tint, Offset(w * 0.48f, h * 0.52f), Offset(w * 0.82f, h * 0.18f), strokeWidth = w * 0.095f, cap = StrokeCap.Round)
                val corner = Path().apply {
                    moveTo(w * 0.58f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.42f)
                }
                drawPath(corner, tint, style = thinStroke)
            }

            CarPlaySymbolType.PHONE -> {
                val handset = Path().apply {
                    moveTo(w * 0.28f, h * 0.16f)
                    cubicTo(w * 0.14f, h * 0.24f, w * 0.16f, h * 0.48f, w * 0.34f, h * 0.66f)
                    cubicTo(w * 0.52f, h * 0.84f, w * 0.76f, h * 0.86f, w * 0.84f, h * 0.72f)
                    lineTo(w * 0.68f, h * 0.56f)
                    lineTo(w * 0.56f, h * 0.62f)
                    quadraticBezierTo(w * 0.46f, h * 0.56f, w * 0.38f, h * 0.44f)
                    lineTo(w * 0.44f, h * 0.32f)
                    close()
                }
                drawPath(handset, tint)
            }

            CarPlaySymbolType.BATTERY -> {
                drawRoundRect(
                    tint,
                    Offset(w * 0.10f, h * 0.28f),
                    Size(w * 0.72f, h * 0.44f),
                    CornerRadius(w * 0.08f, w * 0.08f),
                    style = thinStroke
                )
                drawRoundRect(
                    tint,
                    Offset(w * 0.85f, h * 0.40f),
                    Size(w * 0.07f, h * 0.20f),
                    CornerRadius(w * 0.03f, w * 0.03f)
                )
                drawRoundRect(
                    tint,
                    Offset(w * 0.17f, h * 0.35f),
                    Size(w * 0.48f, h * 0.30f),
                    CornerRadius(w * 0.04f, w * 0.04f)
                )
            }

            CarPlaySymbolType.BOLT -> {
                val bolt = Path().apply {
                    moveTo(w * 0.56f, h * 0.10f)
                    lineTo(w * 0.26f, h * 0.54f)
                    lineTo(w * 0.50f, h * 0.54f)
                    lineTo(w * 0.42f, h * 0.90f)
                    lineTo(w * 0.74f, h * 0.46f)
                    lineTo(w * 0.50f, h * 0.46f)
                    close()
                }
                drawPath(bolt, tint)
            }

            CarPlaySymbolType.WEATHER_CLOUD -> {
                drawCircle(tint, radius = w * 0.20f, center = Offset(w * 0.44f, h * 0.44f))
                drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.64f, h * 0.50f))
                drawRoundRect(
                    tint,
                    Offset(w * 0.18f, h * 0.44f),
                    Size(w * 0.64f, h * 0.28f),
                    CornerRadius(w * 0.14f, w * 0.14f)
                )
            }

            CarPlaySymbolType.WEATHER_RAIN -> {
                drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.42f, h * 0.36f))
                drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.62f, h * 0.41f))
                drawRoundRect(
                    tint,
                    Offset(w * 0.18f, h * 0.36f),
                    Size(w * 0.62f, h * 0.24f),
                    CornerRadius(w * 0.12f, w * 0.12f)
                )
                drawLine(tint, Offset(w * 0.34f, h * 0.68f), Offset(w * 0.28f, h * 0.84f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.52f, h * 0.68f), Offset(w * 0.46f, h * 0.84f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.70f, h * 0.68f), Offset(w * 0.64f, h * 0.84f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.CLOCK -> {
                drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f), style = thinStroke)
                drawLine(tint, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.50f, h * 0.27f), strokeWidth = w * 0.085f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.68f, h * 0.58f), strokeWidth = w * 0.085f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.PLUS -> {
                drawLine(tint, Offset(w * 0.50f, h * 0.20f), Offset(w * 0.50f, h * 0.80f), strokeWidth = w * 0.13f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.80f, h * 0.50f), strokeWidth = w * 0.13f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.MINUS -> {
                drawLine(tint, Offset(w * 0.22f, h * 0.50f), Offset(w * 0.78f, h * 0.50f), strokeWidth = w * 0.14f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.PENCIL -> {
                val body = Path().apply {
                    moveTo(w * 0.18f, h * 0.82f)
                    lineTo(w * 0.24f, h * 0.62f)
                    lineTo(w * 0.68f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.32f)
                    lineTo(w * 0.38f, h * 0.76f)
                    close()
                }
                drawPath(body, tint, style = thinStroke)
            }

            CarPlaySymbolType.TRASH -> {
                drawLine(tint, Offset(w * 0.20f, h * 0.28f), Offset(w * 0.80f, h * 0.28f), strokeWidth = w * 0.085f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.38f, h * 0.18f), Offset(w * 0.62f, h * 0.18f), strokeWidth = w * 0.085f, cap = StrokeCap.Round)
                drawRoundRect(tint, Offset(w * 0.26f, h * 0.28f), Size(w * 0.48f, h * 0.54f), CornerRadius(w * 0.08f, w * 0.08f), style = thinStroke)
                drawLine(tint, Offset(w * 0.42f, h * 0.42f), Offset(w * 0.42f, h * 0.70f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.58f, h * 0.42f), Offset(w * 0.58f, h * 0.70f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
            }

            CarPlaySymbolType.CHECK -> {
                val p = Path().apply {
                    moveTo(w * 0.20f, h * 0.52f)
                    lineTo(w * 0.42f, h * 0.74f)
                    lineTo(w * 0.80f, h * 0.28f)
                }
                drawPath(p, tint, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            CarPlaySymbolType.REFRESH -> {
                drawArc(tint, 35f, 290f, false, Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.64f), style = thinStroke)
                val arrow = Path().apply {
                    moveTo(w * 0.64f, h * 0.20f)
                    lineTo(w * 0.82f, h * 0.22f)
                    lineTo(w * 0.78f, h * 0.40f)
                }
                drawPath(arrow, tint, style = thinStroke)
            }

            CarPlaySymbolType.COMPASS -> {
                drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f), style = thinStroke)
                val needle = Path().apply {
                    moveTo(w * 0.64f, h * 0.30f)
                    lineTo(w * 0.44f, h * 0.46f)
                    lineTo(w * 0.36f, h * 0.70f)
                    lineTo(w * 0.56f, h * 0.54f)
                    close()
                }
                drawPath(needle, tint)
            }
        }
    }
}
