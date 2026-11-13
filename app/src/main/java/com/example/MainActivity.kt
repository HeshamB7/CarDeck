package com.example

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.engine.AppLauncherHelper
import com.example.model.DockSide
import com.example.model.LauncherPreferences
import com.example.model.LauncherScreen
import com.example.model.WallpaperPreset
import com.example.service.CarMediaNotificationListenerService
import com.example.ui.components.CarPlayDock
import com.example.ui.components.OledNightDimmerOverlay
import com.example.ui.components.OnboardingOverlay
import com.example.ui.components.SplitAppPickerDialog
import com.example.ui.components.ValetPinDialog
import com.example.ui.screens.AppGridScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CarDeckTheme
import com.example.ui.theme.LocalCarTokens
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val icons by viewModel.processedIcons.collectAsStateWithLifecycle()
            val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
            val systemStatus by viewModel.systemStatus.collectAsStateWithLifecycle()
            val telemetry by viewModel.telemetryState.collectAsStateWithLifecycle()

            CarDeckTheme(
                dayNightMode = uiState.preferences.dayNightMode,
                fontScale = uiState.preferences.fontScale
            ) {
                CarDeckRoot(
                    viewModel = viewModel,
                    uiState = uiState,
                    icons = icons,
                    mediaState = mediaState,
                    systemStatus = systemStatus,
                    telemetry = telemetry
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onPermissionsUpdated()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Compose LocalConfiguration updates automatically for CANBUS Day/Night uiMode
    }
}

@Composable
private fun CarDeckRoot(
    viewModel: LauncherViewModel,
    uiState: com.example.viewmodel.LauncherUiState,
    icons: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    mediaState: com.example.model.MediaPlaybackState,
    systemStatus: com.example.model.SystemStatusState,
    telemetry: com.example.model.TelemetryState
) {
    val context = LocalContext.current
    val prefs = uiState.preferences

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onPermissionsUpdated()
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onPermissionsUpdated()
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    // Intercept Back press on sub-screens; on Dashboard, stay on Launcher Home
    BackHandler(enabled = uiState.currentScreen != LauncherScreen.DASHBOARD) {
        viewModel.navigateBackToDashboard()
    }

    val exportSuccessStr = stringResource(R.string.backup_exported)
    val importSuccessStr = stringResource(R.string.backup_imported)
    val backupFailStr = stringResource(R.string.backup_failed)

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Day/Night CarPlay Wallpaper Layer (full-bleed edge-to-edge)
        DynamicWallpaperBackground(preferences = prefs)

        // Main Automotive Landscape Frame
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val dockComposable: @Composable () -> Unit = {
                CarPlayDock(
                    systemStatus = systemStatus,
                    dockSlotApps = uiState.dockSlotApps,
                    icons = icons,
                    pinnedPackages = prefs.pinnedDockPackages,
                    currentScreen = uiState.currentScreen,
                    cardAlpha = prefs.cardAlpha,
                    onAppClick = { viewModel.launchApp(it) },
                    onAppLongClick = { viewModel.requestAppEdit(it) },
                    onSwitchViewClick = { viewModel.toggleDashboardAndGrid() },
                    onVoiceClick = { AppLauncherHelper.triggerVoiceAssistant(context) },
                    onOledDimmerClick = { viewModel.setOledDimmer(true) },
                    onSettingsClick = { viewModel.navigateTo(LauncherScreen.SETTINGS) }
                )
            }

            if (prefs.dockSide == DockSide.LEFT) {
                dockComposable()
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = uiState.currentScreen,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            slideInHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { it / 12 }
                            )) togetherWith
                            (fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideOutHorizontally(
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    targetOffsetX = { -it / 12 }
                                ))
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        LauncherScreen.DASHBOARD -> DashboardScreen(
                            preferences = prefs,
                            telemetry = telemetry,
                            systemStatus = systemStatus,
                            mediaState = mediaState,
                            onSearchNavigate = { query ->
                                AppLauncherHelper.launchNavigation(context, query, prefs.navProvider)
                            },
                            onOpenFullMap = {
                                AppLauncherHelper.openMapApp(context, prefs.navProvider)
                            },
                            onMediaPlayPause = { CarMediaNotificationListenerService.togglePlayPause() },
                            onMediaNext = { CarMediaNotificationListenerService.skipNext() },
                            onMediaPrevious = { CarMediaNotificationListenerService.skipPrevious() },
                            onOpenMediaApp = { CarMediaNotificationListenerService.openActiveMediaApp(context) },
                            onGrantMediaAccess = { AppLauncherHelper.openNotificationListenerSettings(context) },
                            onGrantLocation = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                            onGrantCalendar = {
                                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            },
                            onOpenBluetooth = { AppLauncherHelper.openBluetoothSettings(context) },
                            onOpenWifi = { AppLauncherHelper.openWifiPanel(context) },
                            onOpenHotspot = { AppLauncherHelper.openHotspotSettings(context) },
                            onOpenSplitApps = { viewModel.openSplitAppDialog() }
                        )

                        LauncherScreen.APP_GRID -> AppGridScreen(
                            apps = uiState.visibleApps,
                            icons = icons,
                            iconSizeDp = prefs.iconSizeDp,
                            appBeingEdited = uiState.appBeingEdited,
                            onAppClick = { viewModel.launchApp(it) },
                            onAppLongClick = { viewModel.requestAppEdit(it) },
                            onDismissEdit = { viewModel.dismissAppEdit() },
                            onPinToSlot = { slot, pkg -> viewModel.pinAppToDockSlot(slot, pkg) },
                            onRenameApp = { pkg, name -> viewModel.renameApp(pkg, name) },
                            onHideApp = { pkg -> viewModel.setAppHidden(pkg, true) },
                            onMoveApp = { pkg, delta -> viewModel.moveAppInOrder(pkg, delta) },
                            onOpenInSplit = { pkg -> viewModel.openSplitAppDialog(pkg) }
                        )

                        LauncherScreen.SETTINGS -> SettingsScreen(
                            preferences = prefs,
                            allApps = uiState.allApps,
                            icons = icons,
                            iconPacks = uiState.iconPacks,
                            onBack = { viewModel.navigateBackToDashboard() },
                            onIconSizeChange = viewModel::updateIconSize,
                            onCornerRadiusChange = viewModel::updateCornerRadius,
                            onCardAlphaChange = viewModel::updateCardAlpha,
                            onFontScaleChange = viewModel::updateFontScale,
                            onDockSideChange = viewModel::updateDockSide,
                            onDayNightModeChange = viewModel::updateDayNightMode,
                            onWallpaperPresetChange = viewModel::updateWallpaperPreset,
                            onCustomWallpaperPicked = viewModel::setCustomWallpaperUri,
                            onIconPackSelected = viewModel::setActiveIconPack,
                            onUnhideApp = { viewModel.setAppHidden(it, false) },
                            onClearPinnedSlot = { viewModel.pinAppToDockSlot(it, null) },
                            onNavProviderChange = viewModel::updateNavProvider,
                            onHomeAddressChange = viewModel::updateHomeAddress,
                            onWorkAddressChange = viewModel::updateWorkAddress,
                            onSpeedGuardToggle = viewModel::updateSpeedGuard,
                            onSpeedLimitChange = viewModel::updateSpeedLimit,
                            onSpeedUnitChange = viewModel::updateSpeedUnit,
                            onValetModeToggle = viewModel::updateValetMode,
                            onValetPinChange = viewModel::updateValetPin,
                            onExportBackupUri = { uri ->
                                viewModel.exportBackup(uri, exportSuccessStr, backupFailStr)
                            },
                            onImportBackupUri = { uri ->
                                viewModel.importBackup(uri, importSuccessStr, backupFailStr)
                            },
                            onOpenDefaultLauncher = { AppLauncherHelper.openDefaultHomeSettings(context) }
                        )
                    }
                }
            }

            if (prefs.dockSide == DockSide.RIGHT) {
                dockComposable()
            }
        }

        // First-Run Onboarding Setup Overlay
        AnimatedVisibility(
            visible = !prefs.onboardingCompleted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OnboardingOverlay(
                hasLocationPermission = telemetry.hasLocationPermission,
                hasMediaPermission = mediaState.hasNotificationPermission,
                onGrantLocation = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onGrantMedia = {
                    AppLauncherHelper.openNotificationListenerSettings(context)
                },
                onSetDefaultHome = {
                    AppLauncherHelper.openDefaultHomeSettings(context)
                },
                onFinish = { viewModel.completeOnboarding() }
            )
        }

        // Valet Mode PIN Pad
        if (uiState.pendingValetAction != null) {
            ValetPinDialog(
                onVerify = { viewModel.verifyValetPin(it) },
                onDismiss = { viewModel.dismissValetPrompt() }
            )
        }

        // Dual-App Split-Screen Picker
        if (uiState.showSplitAppDialog) {
            SplitAppPickerDialog(
                apps = uiState.visibleApps,
                icons = icons,
                preselectedPrimary = uiState.splitPreselectedPrimary,
                onLaunchSplit = { p, s -> viewModel.launchSplitScreen(p, s) },
                onDismiss = { viewModel.dismissSplitAppDialog() }
            )
        }

        // OLED Night Dimmer Overlay (pure black + dim deep-red clock)
        AnimatedVisibility(
            visible = uiState.isOledDimmerActive,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
        ) {
            OledNightDimmerOverlay(
                systemStatus = systemStatus,
                telemetry = telemetry,
                speedUnit = prefs.speedUnit,
                onWake = { viewModel.setOledDimmer(false) }
            )
        }
    }
}

@Composable
private fun DynamicWallpaperBackground(preferences: LauncherPreferences) {
    val tokens = LocalCarTokens.current
    val isNight = tokens.isNight

    when (preferences.wallpaperPreset) {
        WallpaperPreset.MIDNIGHT_OLED -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isNight) Color.Black else Color(0xFFF2F2F7))
            )
        }
        WallpaperPreset.CUSTOM_IMAGE -> {
            val uri = preferences.customWallpaperUri
            if (!uri.isNullOrBlank()) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(
                        if (isNight) R.drawable.img_wallpaper_ios18_night
                        else R.drawable.img_wallpaper_ios18_day
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        WallpaperPreset.IOS_17_DYNAMIC -> {
            Image(
                painter = painterResource(
                    if (isNight) R.drawable.img_wallpaper_ios17_night
                    else R.drawable.img_wallpaper_ios17_day
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        WallpaperPreset.IOS_18_DYNAMIC -> {
            Image(
                painter = painterResource(
                    if (isNight) R.drawable.img_wallpaper_ios18_night
                    else R.drawable.img_wallpaper_ios18_day
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
