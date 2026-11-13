package com.example.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

enum class LauncherScreen {
    DASHBOARD,
    APP_GRID,
    SETTINGS
}

enum class DockSide {
    LEFT,
    RIGHT
}

enum class DayNightMode {
    AUTO_CANBUS,
    FORCE_DAY,
    FORCE_NIGHT
}

enum class WallpaperPreset {
    IOS_18_DYNAMIC,
    IOS_17_DYNAMIC,
    MIDNIGHT_OLED,
    CUSTOM_IMAGE
}

enum class NavigationProvider(val label: String, val packageName: String) {
    GOOGLE_MAPS("Google Maps", "com.google.android.apps.maps"),
    WAZE("Waze", "com.waze")
}

enum class SpeedUnit {
    KMH,
    MPH
}

@Immutable
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
    val sortOrder: Int = 0,
    val virtualAction: String? = null
) {
    val displayName: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}

@Immutable
data class IconPackInfo(
    val packageName: String,
    val label: String
)

@Immutable
data class LauncherPreferences(
    val iconSizeDp: Float = 68f,
    val cornerRadiusDp: Float = 22f,
    val cardAlpha: Float = 0.78f,
    val fontScale: Float = 1.0f,
    val dockSide: DockSide = DockSide.LEFT,
    val dayNightMode: DayNightMode = DayNightMode.AUTO_CANBUS,
    val wallpaperPreset: WallpaperPreset = WallpaperPreset.IOS_18_DYNAMIC,
    val customWallpaperUri: String? = null,
    val activeIconPackPackage: String? = null,
    val pinnedDockPackages: List<String?> = listOf(null, null, null),
    val recentPackages: List<String> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
    val customLabels: Map<String, String> = emptyMap(),
    val appOrder: List<String> = emptyList(),
    val navProvider: NavigationProvider = NavigationProvider.GOOGLE_MAPS,
    val homeAddress: String = "Home",
    val workAddress: String = "Work",
    val speedLimitKmh: Int = 120,
    val speedGuardEnabled: Boolean = true,
    val speedUnit: SpeedUnit = SpeedUnit.MPH,
    val valetModeEnabled: Boolean = false,
    val valetPin: String = "1234",
    val onboardingCompleted: Boolean = false
)

@Immutable
data class MediaPlaybackState(
    val hasNotificationPermission: Boolean = false,
    val hasActiveSession: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val sourcePackage: String? = null,
    val sourceAppName: String = "",
    val albumArt: Bitmap? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

@Immutable
data class SystemStatusState(
    val timeText: String = "9:41",
    val dateText: String = "",
    val isWifiConnected: Boolean = false,
    val isCellularConnected: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isGpsActive: Boolean = false
)

@Immutable
data class TelemetryState(
    val speedKmh: Float = 0f,
    val bearingDegrees: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val hasLocationPermission: Boolean = false,
    val hasGpsFix: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val nextEventTitle: String? = null,
    val nextEventTime: String? = null,
    val tripStartTimeMs: Long = System.currentTimeMillis()
)
