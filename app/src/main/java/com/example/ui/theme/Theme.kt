package com.example.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.example.model.DayNightMode

data class CarSurfaceTokens(
    val isNight: Boolean,
    val cardBackground: Color,
    val cardBorder: Color,
    val dockBackground: Color,
    val pillBackground: Color,
    val secondaryText: Color,
    val positiveGreen: Color,
    val warningOrange: Color,
    val alertRed: Color,
    val accentBlue: Color
)

val LocalCarTokens = staticCompositionLocalOf {
    CarSurfaceTokens(
        isNight = true,
        cardBackground = CarNightSurface,
        cardBorder = CarNightBorder,
        dockBackground = CarNightSurface,
        pillBackground = CarNightSurfaceElevated,
        secondaryText = CarNightOnSurfaceSecondary,
        positiveGreen = CarNightAccentGreen,
        warningOrange = CarNightAccentOrange,
        alertRed = CarNightAccentRed,
        accentBlue = CarNightAccentBlue
    )
}

private val CarDarkColorScheme = darkColorScheme(
    primary = CarNightAccentBlue,
    onPrimary = Color.White,
    secondary = CarNightAccentGreen,
    tertiary = CarNightAccentOrange,
    error = CarNightAccentRed,
    background = CarNightBackground,
    onBackground = CarNightOnSurface,
    surface = CarNightSurface,
    onSurface = CarNightOnSurface,
    surfaceVariant = CarNightSurfaceElevated,
    onSurfaceVariant = CarNightOnSurfaceSecondary,
    outline = CarNightBorder
)

private val CarLightColorScheme = lightColorScheme(
    primary = CarDayAccentBlue,
    onPrimary = Color.White,
    secondary = CarDayAccentGreen,
    tertiary = CarDayAccentOrange,
    error = CarDayAccentRed,
    background = CarDayBackground,
    onBackground = CarDayOnSurface,
    surface = CarDaySurface,
    onSurface = CarDayOnSurface,
    surfaceVariant = CarDaySurfaceElevated,
    onSurfaceVariant = CarDayOnSurfaceSecondary,
    outline = CarDayBorder
)

@Composable
fun CarDeckTheme(
    dayNightMode: DayNightMode,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val systemIsNight =
        (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val isNight = when (dayNightMode) {
        DayNightMode.AUTO_CANBUS -> systemIsNight
        DayNightMode.FORCE_DAY -> false
        DayNightMode.FORCE_NIGHT -> true
    }

    val colorScheme = if (isNight) CarDarkColorScheme else CarLightColorScheme
    val tokens = if (isNight) {
        CarSurfaceTokens(
            isNight = true,
            cardBackground = CarNightSurface,
            cardBorder = CarNightBorder,
            dockBackground = Color(0xCC161618),
            pillBackground = CarNightSurfaceElevated,
            secondaryText = CarNightOnSurfaceSecondary,
            positiveGreen = CarNightAccentGreen,
            warningOrange = CarNightAccentOrange,
            alertRed = CarNightAccentRed,
            accentBlue = CarNightAccentBlue
        )
    } else {
        CarSurfaceTokens(
            isNight = false,
            cardBackground = CarDaySurface,
            cardBorder = CarDayBorder,
            dockBackground = Color(0xD9F8F8FA),
            pillBackground = CarDaySurfaceElevated,
            secondaryText = CarDayOnSurfaceSecondary,
            positiveGreen = CarDayAccentGreen,
            warningOrange = CarDayAccentOrange,
            alertRed = CarDayAccentRed,
            accentBlue = CarDayAccentBlue
        )
    }

    CompositionLocalProvider(LocalCarTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildCarTypography(fontScale),
            content = content
        )
    }
}
