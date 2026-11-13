package com.example

import android.Manifest
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.engine.AppLauncherHelper
import com.example.engine.BitmapBlurUtils
import com.example.model.DockSide
import com.example.model.LauncherPreferences
import com.example.model.LauncherScreen
import com.example.model.WallpaperPreset
import com.example.service.CarMediaNotificationListenerService
import com.example.ui.components.AddWidgetsGalleryDialog
import com.example.ui.components.CarPlayDock
import com.example.ui.components.CarPlaySymbol
import com.example.ui.components.CarPlaySymbolType
import com.example.ui.components.CarPlayText
import com.example.ui.components.DockSlotPickerDialog
import com.example.ui.components.OledNightDimmerOverlay
import com.example.ui.components.OnboardingOverlay
import com.example.ui.components.QuickContactEditDialog
import com.example.ui.components.SplitAppPickerDialog
import com.example.ui.components.ValetPinDialog
import com.example.ui.screens.AppGridScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CarDeckTheme
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import com.example.viewmodel.BannerNotice
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min

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

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenScale = min(maxWidth.value / 960f, maxHeight.value / 540f)
                    .coerceIn(0.80f, 1.25f)

                CarDeckTheme(
                    dayNightMode = uiState.preferences.dayNightMode,
                    fontScale = uiState.preferences.fontScale,
                    screenScale = screenScale
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
    }

    override fun onResume() {
        super.onResume()
        viewModel.onPermissionsUpdated()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}

@Composable
private fun CarDeckRoot(
    viewModel: LauncherViewModel,
    uiState: com.example.viewmodel.LauncherUiState,
    icons: Map<String, ImageBitmap>,
    mediaState: com.example.model.MediaPlaybackState,
    systemStatus: com.example.model.SystemStatusState,
    telemetry: com.example.model.TelemetryState
) {
    val context = LocalContext.current
    val tokens = LocalCarTokens.current
    val prefs = uiState.preferences
    var dockBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var launchPulseTimestamp by remember { mutableLongStateOf(0L) }
    var launchPulseActive by remember { mutableStateOf(false) }

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

    LaunchedEffect(uiState.bannerNotice?.id) {
        if (uiState.bannerNotice != null) {
            delay(2600L)
            viewModel.consumeBanner()
        }
    }

    LaunchedEffect(launchPulseTimestamp) {
        if (launchPulseTimestamp > 0L) {
            launchPulseActive = true
            delay(220L)
            launchPulseActive = false
        }
    }

    val launchContentScale by animateFloatAsState(
        targetValue = if (launchPulseActive) 1.022f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "app_launch_spring_pulse"
    )

    BackHandler(enabled = uiState.currentScreen != LauncherScreen.DASHBOARD || uiState.isWidgetEditMode) {
        if (uiState.isWidgetEditMode) {
            viewModel.setWidgetEditMode(false)
        } else {
            viewModel.navigateBackToDashboard()
        }
    }

    val exportSuccessStr = stringResource(R.string.backup_exported)
    val importSuccessStr = stringResource(R.string.backup_imported)
    val backupFailStr = stringResource(R.string.backup_failed)
    val focusOnStr = stringResource(R.string.focus_mode_on)
    val focusOffStr = stringResource(R.string.focus_mode_off)
    val parkedSavedStr = stringResource(R.string.parked_pin_saved_msg)
    val parkedNoGpsStr = stringResource(R.string.parked_no_gps_msg)
    val parkedClearedStr = stringResource(R.string.parked_pin_cleared_msg)
    val tripResetStr = stringResource(R.string.trip_timer_reset_msg)
    val resetWidgetsStr = stringResource(R.string.setting_reset_widgets_msg)

    // Pre-computed cached blurred wallpaper slice for 60fps Dock Glassmorphism on all API levels
    val frostedDockBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        prefs.wallpaperPreset,
        tokens.isNight,
        prefs.customWallpaperUri
    ) {
        value = withContext(Dispatchers.Default) {
            if (prefs.wallpaperPreset == WallpaperPreset.MIDNIGHT_OLED) {
                null
            } else {
                val fallbackRes = when (prefs.wallpaperPreset) {
                    WallpaperPreset.IOS_17_DYNAMIC ->
                        if (tokens.isNight) R.drawable.img_wallpaper_ios17_night else R.drawable.img_wallpaper_ios17_day
                    else ->
                        if (tokens.isNight) R.drawable.img_wallpaper_ios18_night else R.drawable.img_wallpaper_ios18_day
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = 4 }
                val raw = if (prefs.wallpaperPreset == WallpaperPreset.CUSTOM_IMAGE && !prefs.customWallpaperUri.isNullOrBlank()) {
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(prefs.customWallpaperUri))?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, decodeOpts)
                        }
                    }.getOrNull() ?: BitmapFactory.decodeResource(context.resources, fallbackRes, decodeOpts)
                } else {
                    BitmapFactory.decodeResource(context.resources, fallbackRes, decodeOpts)
                }
                raw?.let { src ->
                    val frosted = BitmapBlurUtils.createFrostedBitmap(src, targetWidth = 128, radius = 8)
                    if (!src.isRecycled) src.recycle()
                    frosted.asImageBitmap()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Day/Night CarPlay Wallpaper Layer (full-bleed edge-to-edge)
        DynamicWallpaperBackground(preferences = prefs)

        // Cached Frosted Dock Slice behind the Dock (zero per-frame blur overhead)
        val cachedFrosted = frostedDockBitmap
        if (cachedFrosted != null &&
            dockBoundsInRoot.width > 0f &&
            dockBoundsInRoot.height > 0f
        ) {
            val dockBlurShape = remember(dockBoundsInRoot) {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density
                    ): Outline {
                        val rPx = with(density) { 24.dp.toPx() }
                        return Outline.Rounded(
                            RoundRect(
                                rect = dockBoundsInRoot,
                                cornerRadius = CornerRadius(rPx, rPx)
                            )
                        )
                    }
                }
            }
            Image(
                bitmap = cachedFrosted,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(dockBlurShape)
            )
        }

        // Responsive Automotive Landscape Frame
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .graphicsLayer {
                    scaleX = launchContentScale
                    scaleY = launchContentScale
                }
        ) {
            val dockWidth = (maxWidth * 0.088f).coerceIn(68.dp, 92.dp)

            Row(modifier = Modifier.fillMaxSize()) {
                val dockComposable: @Composable () -> Unit = {
                    CarPlayDock(
                        systemStatus = systemStatus,
                        dockSlotApps = uiState.dockSlotApps,
                        icons = icons,
                        pinnedPackages = prefs.pinnedDockPackages,
                        currentScreen = uiState.currentScreen,
                        cardAlpha = prefs.cardAlpha,
                        onAppClick = { app ->
                            launchPulseTimestamp = System.currentTimeMillis()
                            viewModel.launchApp(app)
                        },
                        onSlotLongClick = { slotIndex -> viewModel.requestCustomizeDockSlot(slotIndex) },
                        onSwitchViewClick = { viewModel.toggleDashboardAndGrid() },
                        onVoiceClick = { AppLauncherHelper.triggerVoiceAssistant(context) },
                        onOledDimmerClick = { viewModel.setOledDimmer(true) },
                        onSettingsClick = { viewModel.navigateTo(LauncherScreen.SETTINGS) },
                        onDockBoundsChanged = { dockBoundsInRoot = it },
                        modifier = Modifier.width(dockWidth)
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
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) +
                                slideInHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = 0.88f,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    initialOffsetX = { it / 14 }
                                )) togetherWith
                                (fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                    scaleOut(
                                        targetScale = 1.02f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                    ) +
                                    slideOutHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                        targetOffsetX = { -it / 14 }
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
                                isEditMode = uiState.isWidgetEditMode,
                                isHighwayFocusActive = uiState.isHighwayFocusActive,
                                isBreakReminderDue = uiState.isBreakReminderDue,
                                onToggleEditMode = { viewModel.toggleWidgetEditMode() },
                                onOpenAddWidget = { pageIdx -> viewModel.openAddWidgetSheet(pageIdx) },
                                onAddPage = { viewModel.addDashboardPage() },
                                onDeletePage = { pageIdx -> viewModel.deleteDashboardPage(pageIdx) },
                                onDeleteWidget = { pageIdx, wId -> viewModel.removeWidgetFromPage(pageIdx, wId) },
                                onMoveWidget = { pageIdx, wId, delta -> viewModel.moveWidgetInPage(pageIdx, wId, delta) },
                                onSwapWidgets = { pageIdx, id1, id2 -> viewModel.swapWidgetsInPage(pageIdx, id1, id2) },
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
                                onOpenSplitApps = { viewModel.openSplitAppDialog() },
                                onToggleHighwayFocus = {
                                    viewModel.toggleHighwayFocus(focusOnStr, focusOffStr)
                                },
                                onParkedPinClick = {
                                    viewModel.saveOrNavigateParkedSpot(parkedSavedStr, parkedNoGpsStr)
                                },
                                onClearParkedPin = {
                                    viewModel.clearParkedSpot(parkedClearedStr)
                                },
                                onResetTripTimer = {
                                    viewModel.resetTripTimer(tripResetStr)
                                },
                                onRefreshWeather = { viewModel.refreshWeather() },
                                onDialContact = { contact ->
                                    if (!AppLauncherHelper.dialPhoneNumber(context, contact.phoneNumber)) {
                                        viewModel.requestEditQuickContact(contact)
                                    }
                                },
                                onEditQuickContact = { contact -> viewModel.requestEditQuickContact(contact) }
                            )

                            LauncherScreen.APP_GRID -> AppGridScreen(
                                apps = uiState.visibleApps,
                                icons = icons,
                                iconSizeDp = prefs.iconSizeDp,
                                appBeingEdited = uiState.appBeingEdited,
                                onAppClick = { app ->
                                    launchPulseTimestamp = System.currentTimeMillis()
                                    viewModel.launchApp(app)
                                },
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
                                onNightWarmthToggle = viewModel::updateNightWarmth,
                                onResetDashboardPages = {
                                    viewModel.resetDashboardPagesToDefault()
                                    viewModel.showBanner(resetWidgetsStr)
                                },
                                onIconPackSelected = viewModel::setActiveIconPack,
                                onUnhideApp = { viewModel.setAppHidden(it, false) },
                                onClearPinnedSlot = { viewModel.pinAppToDockSlot(it, null) },
                                onNavProviderChange = viewModel::updateNavProvider,
                                onHomeAddressChange = viewModel::updateHomeAddress,
                                onWorkAddressChange = viewModel::updateWorkAddress,
                                onSpeedGuardToggle = viewModel::updateSpeedGuard,
                                onSpeedLimitChange = viewModel::updateSpeedLimit,
                                onSpeedUnitChange = viewModel::updateSpeedUnit,
                                onHighwayFocusAutoToggle = viewModel::updateHighwayFocusAuto,
                                onBreakReminderMinutesChange = viewModel::updateBreakReminderMinutes,
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
        }

        // Auto Night Eye-Comfort Warmth Filter (soft warm amber anti-glare tint)
        if (tokens.isNight && prefs.nightWarmthEnabled && !uiState.isOledDimmerActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x18FF9500))
            )
        }

        // In-App Frosted Apple Dynamic Banner Pill (replaces system Toast)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = uiState.bannerNotice != null,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { -it }
                ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                exit = slideOutVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    targetOffsetY = { -it }
                ) + fadeOut()
            ) {
                uiState.bannerNotice?.let { notice ->
                    CarPlayDynamicBannerPill(
                        notice = notice,
                        onDismiss = { viewModel.consumeBanner() }
                    )
                }
            }
        }

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

        uiState.dockSlotBeingCustomized?.let { slotIdx ->
            DockSlotPickerDialog(
                slotIndex = slotIdx,
                currentPinnedPkg = prefs.pinnedDockPackages.getOrNull(slotIdx),
                apps = uiState.visibleApps,
                icons = icons,
                onSelectApp = { pkg ->
                    viewModel.pinAppToDockSlot(slotIdx, pkg)
                    viewModel.dismissDockSlotCustomization()
                },
                onDismiss = { viewModel.dismissDockSlotCustomization() }
            )
        }

        uiState.addWidgetPageIndex?.let { targetPage ->
            AddWidgetsGalleryDialog(
                onAddWidget = { type -> viewModel.addWidgetToPage(targetPage, type) },
                onDismiss = { viewModel.dismissAddWidgetSheet() }
            )
        }

        uiState.quickContactBeingEdited?.let { contact ->
            QuickContactEditDialog(
                contact = contact,
                onSave = { updated -> viewModel.saveQuickContact(updated) },
                onDismiss = { viewModel.dismissEditQuickContact() }
            )
        }

        if (uiState.pendingValetAction != null) {
            ValetPinDialog(
                onVerify = { viewModel.verifyValetPin(it) },
                onDismiss = { viewModel.dismissValetPrompt() }
            )
        }

        if (uiState.showSplitAppDialog) {
            SplitAppPickerDialog(
                apps = uiState.visibleApps,
                icons = icons,
                preselectedPrimary = uiState.splitPreselectedPrimary,
                onLaunchSplit = { p, s -> viewModel.launchSplitScreen(p, s) },
                onDismiss = { viewModel.dismissSplitAppDialog() }
            )
        }

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
private fun CarPlayDynamicBannerPill(
    notice: BannerNotice,
    onDismiss: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val accent = if (notice.isWarning) tokens.warningOrange else tokens.accentBlue
    Row(
        modifier = Modifier
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(tokens.cardBackground.copy(alpha = 0.94f))
            .border(1.dp, accent.copy(alpha = 0.55f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CarPlaySymbol(
            type = if (notice.isWarning) CarPlaySymbolType.WARNING else CarPlaySymbolType.CHECK,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        CarPlayText(
            text = notice.message,
            style = type.controlLabel.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DynamicWallpaperBackground(
    preferences: LauncherPreferences,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val isNight = tokens.isNight

    when (preferences.wallpaperPreset) {
        WallpaperPreset.MIDNIGHT_OLED -> {
            Box(
                modifier = modifier
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
                    modifier = modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(
                        if (isNight) R.drawable.img_wallpaper_ios18_night
                        else R.drawable.img_wallpaper_ios18_day
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.fillMaxSize()
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
                modifier = modifier.fillMaxSize()
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
                modifier = modifier.fillMaxSize()
            )
        }
    }
}
