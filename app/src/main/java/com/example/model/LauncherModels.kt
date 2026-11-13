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

enum class CarWidgetType {
    NAVIGATION_MAP,
    MINI_MAP,
    TURN_SPEED,
    MEDIA_PLAYER,
    WEATHER,
    DIGITAL_CLOCK,
    ANALOG_CLOCK,
    CALENDAR_AGENDA,
    SPEED_CONTACTS,
    VEHICLE_BATTERY,
    QUICK_CONTROLS,
    TRIP_ODOMETER
}

@Immutable
data class DashboardWidgetItem(
    val id: String,
    val type: CarWidgetType
)

@Immutable
data class DashboardPageConfig(
    val id: String,
    val title: String,
    val widgets: List<DashboardWidgetItem>
)

@Immutable
data class QuickContact(
    val id: String,
    val name: String,
    val phoneNumber: String
)

fun defaultDashboardPages(): List<DashboardPageConfig> = listOf(
    DashboardPageConfig(
        id = "page_split_home",
        title = "CarPlay",
        widgets = listOf(
            DashboardWidgetItem(id = "w_hero_map", type = CarWidgetType.NAVIGATION_MAP),
            DashboardWidgetItem(id = "w_turn_speed", type = CarWidgetType.TURN_SPEED),
            DashboardWidgetItem(id = "w_media", type = CarWidgetType.MEDIA_PLAYER),
            DashboardWidgetItem(id = "w_calendar", type = CarWidgetType.CALENDAR_AGENDA)
        )
    ),
    DashboardPageConfig(
        id = "page_widgets_deck",
        title = "Widgets",
        widgets = listOf(
            DashboardWidgetItem(id = "w_analog_clock", type = CarWidgetType.ANALOG_CLOCK),
            DashboardWidgetItem(id = "w_weather", type = CarWidgetType.WEATHER),
            DashboardWidgetItem(id = "w_contacts", type = CarWidgetType.SPEED_CONTACTS),
            DashboardWidgetItem(id = "w_vehicle", type = CarWidgetType.VEHICLE_BATTERY)
        )
    )
)

fun defaultQuickContacts(): List<QuickContact> = listOf(
    QuickContact(id = "qc_1", name = "Home", phoneNumber = ""),
    QuickContact(id = "qc_2", name = "Work", phoneNumber = ""),
    QuickContact(id = "qc_3", name = "Roadside", phoneNumber = "112"),
    QuickContact(id = "qc_4", name = "Emergency", phoneNumber = "911")
)

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
    val cornerRadiusDp: Float = 24f,
    val cardAlpha: Float = 0.76f,
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
    val onboardingCompleted: Boolean = false,
    val dashboardPages: List<DashboardPageConfig> = defaultDashboardPages(),
    val quickContacts: List<QuickContact> = defaultQuickContacts(),
    val nightWarmthEnabled: Boolean = true,
    val highwayFocusAutoEnabled: Boolean = false,
    val breakReminderMinutes: Int = 90,
    val parkedLatitude: Double? = null,
    val parkedLongitude: Double? = null,
    val parkedTimestampMs: Long? = null
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
data class WeatherState(
    val hasData: Boolean = false,
    val isLoading: Boolean = false,
    val temperatureC: Float = 0f,
    val apparentTempC: Float = 0f,
    val highTempC: Float = 0f,
    val lowTempC: Float = 0f,
    val windSpeedKmh: Float = 0f,
    val humidityPercent: Int = 0,
    val precipChancePercent: Int = 0,
    val weatherCode: Int = 0,
    val isDaylight: Boolean = true,
    val lastUpdatedMs: Long = 0L
)

@Immutable
data class TelemetryState(
    val speedKmh: Float = 0f,
    val peakSpeedKmh: Float = 0f,
    val bearingDegrees: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val hasLocationPermission: Boolean = false,
    val hasGpsFix: Boolean = false,
    val hasCalendarPermission: Boolean = false,
    val nextEventTitle: String? = null,
    val nextEventTime: String? = null,
    val tripStartTimeMs: Long = System.currentTimeMillis(),
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 28f,
    val weather: WeatherState = WeatherState()
)
