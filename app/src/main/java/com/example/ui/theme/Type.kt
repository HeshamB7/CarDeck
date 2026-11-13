package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// SF Pro Display / SF Pro Text optical equivalents using bundled typefaces
val CarDisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val CarTextFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val CarMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

@Immutable
data class CarPlayTypography(
    val displaySpeed: TextStyle,
    val turnDistance: TextStyle,
    val headline: TextStyle,
    val dockClock: TextStyle,
    val cardTitle: TextStyle,
    val rowTitle: TextStyle,
    val body: TextStyle,
    val bodySecondary: TextStyle,
    val appLabel: TextStyle,
    val controlLabel: TextStyle,
    val caption: TextStyle,
    val micro: TextStyle
)

val LocalCarPlayTypography = staticCompositionLocalOf {
    buildCarPlayTypography(fontScale = 1f, screenScale = 1f)
}

fun buildCarPlayTypography(
    fontScale: Float = 1f,
    screenScale: Float = 1f
): CarPlayTypography {
    val s = (fontScale.coerceIn(0.8f, 1.35f) * screenScale.coerceIn(0.82f, 1.25f))
    return CarPlayTypography(
        displaySpeed = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (46f * s).sp,
            lineHeight = (50f * s).sp,
            letterSpacing = (-1.3).sp,
            fontFeatureSettings = "tnum"
        ),
        turnDistance = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * s).sp,
            lineHeight = (32f * s).sp,
            letterSpacing = (-0.65).sp,
            fontFeatureSettings = "tnum"
        ),
        headline = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22f * s).sp,
            lineHeight = (27f * s).sp,
            letterSpacing = (-0.4).sp
        ),
        dockClock = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18.5f * s).sp,
            lineHeight = (21f * s).sp,
            letterSpacing = (-0.45).sp,
            fontFeatureSettings = "tnum"
        ),
        cardTitle = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (17.5f * s).sp,
            lineHeight = (22f * s).sp,
            letterSpacing = (-0.25).sp
        ),
        rowTitle = TextStyle(
            fontFamily = CarTextFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15.5f * s).sp,
            lineHeight = (20f * s).sp,
            letterSpacing = (-0.2).sp
        ),
        body = TextStyle(
            fontFamily = CarTextFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * s).sp,
            lineHeight = (20f * s).sp,
            letterSpacing = (-0.15).sp
        ),
        bodySecondary = TextStyle(
            fontFamily = CarTextFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * s).sp,
            lineHeight = (18f * s).sp,
            letterSpacing = (-0.1).sp
        ),
        appLabel = TextStyle(
            fontFamily = CarTextFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13f * s).sp,
            lineHeight = (16f * s).sp,
            letterSpacing = (-0.12).sp
        ),
        controlLabel = TextStyle(
            fontFamily = CarTextFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * s).sp,
            lineHeight = (17f * s).sp,
            letterSpacing = (-0.15).sp
        ),
        caption = TextStyle(
            fontFamily = CarTextFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * s).sp,
            lineHeight = (15f * s).sp,
            letterSpacing = (-0.05).sp
        ),
        micro = TextStyle(
            fontFamily = CarTextFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (10.5f * s).sp,
            lineHeight = (13f * s).sp,
            letterSpacing = 0.05.sp,
            fontFeatureSettings = "tnum"
        )
    )
}
