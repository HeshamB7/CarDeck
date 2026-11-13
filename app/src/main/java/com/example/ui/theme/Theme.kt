package com.example.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.example.model.DayNightMode

@Immutable
data class CarSurfaceTokens(
    val isNight: Boolean,
    val background: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val dockBackground: Color,
    val pillBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val positiveGreen: Color,
    val warningOrange: Color,
    val alertRed: Color,
    val accentBlue: Color,
    val dividerColor: Color,
    val scrimColor: Color
)

val LocalCarTokens = staticCompositionLocalOf {
    CarSurfaceTokens(
        isNight = true,
        background = CarNightBackground,
        cardBackground = CarNightSurface,
        cardBorder = CarNightBorder,
        dockBackground = CarNightDockSurface,
        pillBackground = CarNightSurfaceElevated,
        primaryText = CarNightOnSurface,
        secondaryText = CarNightOnSurfaceSecondary,
        positiveGreen = CarNightAccentGreen,
        warningOrange = CarNightAccentOrange,
        alertRed = CarNightAccentRed,
        accentBlue = CarNightAccentBlue,
        dividerColor = Color(0x26FFFFFF),
        scrimColor = Color(0xA6000000)
    )
}

@Composable
fun CarDeckTheme(
    dayNightMode: DayNightMode,
    fontScale: Float = 1f,
    screenScale: Float = 1f,
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

    val tokens = if (isNight) {
        CarSurfaceTokens(
            isNight = true,
            background = CarNightBackground,
            cardBackground = CarNightSurface,
            cardBorder = CarNightBorder,
            dockBackground = CarNightDockSurface,
            pillBackground = CarNightSurfaceElevated,
            primaryText = CarNightOnSurface,
            secondaryText = CarNightOnSurfaceSecondary,
            positiveGreen = CarNightAccentGreen,
            warningOrange = CarNightAccentOrange,
            alertRed = CarNightAccentRed,
            accentBlue = CarNightAccentBlue,
            dividerColor = Color(0x29FFFFFF),
            scrimColor = Color(0xB3000000)
        )
    } else {
        CarSurfaceTokens(
            isNight = false,
            background = CarDayBackground,
            cardBackground = CarDaySurface,
            cardBorder = CarDayBorder,
            dockBackground = CarDayDockSurface,
            pillBackground = CarDaySurfaceElevated,
            primaryText = CarDayOnSurface,
            secondaryText = CarDayOnSurfaceSecondary,
            positiveGreen = CarDayAccentGreen,
            warningOrange = CarDayAccentOrange,
            alertRed = CarDayAccentRed,
            accentBlue = CarDayAccentBlue,
            dividerColor = Color(0x18000000),
            scrimColor = Color(0x66000000)
        )
    }

    val typography = buildCarPlayTypography(fontScale = fontScale, screenScale = screenScale)

    CompositionLocalProvider(
        LocalCarTokens provides tokens,
        LocalCarPlayTypography provides typography,
        content = content
    )
}
