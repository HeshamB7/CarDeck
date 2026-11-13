package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// SF Pro Display / Rounded style typography using local Inter typeface
val CarDisplayFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val CarMonoFontFamily = CarDisplayFontFamily

fun buildCarTypography(fontScale: Float = 1f): Typography {
    val s = fontScale.coerceIn(0.8f, 1.35f)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (46 * s).sp,
            lineHeight = (50 * s).sp,
            letterSpacing = (-1.2).sp,
            fontFeatureSettings = "tnum"
        ),
        headlineLarge = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28 * s).sp,
            lineHeight = (33 * s).sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22 * s).sp,
            lineHeight = (27 * s).sp,
            letterSpacing = (-0.35).sp
        ),
        titleLarge = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (19 * s).sp,
            lineHeight = (23 * s).sp,
            letterSpacing = (-0.25).sp
        ),
        titleMedium = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * s).sp,
            lineHeight = (20 * s).sp,
            letterSpacing = (-0.2).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15 * s).sp,
            lineHeight = (20 * s).sp,
            letterSpacing = (-0.15).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13 * s).sp,
            lineHeight = (17 * s).sp,
            letterSpacing = (-0.1).sp
        ),
        labelLarge = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13.5f * s).sp,
            lineHeight = (17 * s).sp,
            letterSpacing = (-0.15).sp
        ),
        labelMedium = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * s).sp,
            lineHeight = (15 * s).sp,
            letterSpacing = (-0.1).sp
        ),
        labelSmall = TextStyle(
            fontFamily = CarDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (11 * s).sp,
            lineHeight = (14 * s).sp,
            letterSpacing = 0.sp
        )
    )
}
