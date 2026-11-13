package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.LauncherPreferences
import com.example.model.MediaPlaybackState
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.ui.components.NavigationMapCard
import com.example.ui.components.SmartStackCard
import com.example.ui.components.UniversalMediaCard

@Composable
fun DashboardScreen(
    preferences: LauncherPreferences,
    telemetry: TelemetryState,
    systemStatus: SystemStatusState,
    mediaState: MediaPlaybackState,
    onSearchNavigate: (String) -> Unit,
    onOpenFullMap: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaPrevious: () -> Unit,
    onOpenMediaApp: () -> Unit,
    onGrantMediaAccess: () -> Unit,
    onGrantLocation: () -> Unit,
    onGrantCalendar: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenHotspot: () -> Unit,
    onOpenSplitApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary Section (55% width): Live Navigation Card
        NavigationMapCard(
            telemetry = telemetry,
            navProvider = preferences.navProvider,
            homeAddress = preferences.homeAddress,
            workAddress = preferences.workAddress,
            cornerRadiusDp = preferences.cornerRadiusDp,
            cardAlpha = preferences.cardAlpha,
            onSearchNavigate = onSearchNavigate,
            onOpenFullMap = onOpenFullMap,
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
        )

        // Secondary Section (45% width): Universal Media + Smart Stack Widget
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UniversalMediaCard(
                mediaState = mediaState,
                cornerRadiusDp = preferences.cornerRadiusDp,
                cardAlpha = preferences.cardAlpha,
                onPlayPause = onMediaPlayPause,
                onNext = onMediaNext,
                onPrevious = onMediaPrevious,
                onOpenMediaApp = onOpenMediaApp,
                onGrantMediaAccess = onGrantMediaAccess,
                modifier = Modifier.weight(0.54f)
            )

            SmartStackCard(
                telemetry = telemetry,
                systemStatus = systemStatus,
                speedUnit = preferences.speedUnit,
                speedLimitKmh = preferences.speedLimitKmh,
                speedGuardEnabled = preferences.speedGuardEnabled,
                cornerRadiusDp = preferences.cornerRadiusDp,
                cardAlpha = preferences.cardAlpha,
                onGrantLocation = onGrantLocation,
                onGrantCalendar = onGrantCalendar,
                onOpenBluetooth = onOpenBluetooth,
                onOpenWifi = onOpenWifi,
                onOpenHotspot = onOpenHotspot,
                onOpenSplitApps = onOpenSplitApps,
                modifier = Modifier.weight(0.46f)
            )
        }
    }
}
