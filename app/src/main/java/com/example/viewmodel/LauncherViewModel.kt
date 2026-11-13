package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherPreferencesRepository
import com.example.engine.AppLauncherHelper
import com.example.engine.IconProcessor
import com.example.engine.TelemetryManager
import com.example.model.AppEntry
import com.example.model.DayNightMode
import com.example.model.DockSide
import com.example.model.IconPackInfo
import com.example.model.LauncherPreferences
import com.example.model.LauncherScreen
import com.example.model.MediaPlaybackState
import com.example.model.NavigationProvider
import com.example.model.SpeedUnit
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.model.WallpaperPreset
import com.example.service.CarMediaNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ValetAction {
    data object OpenSettings : ValetAction
    data class EditApp(val app: AppEntry) : ValetAction
}

data class LauncherUiState(
    val preferences: LauncherPreferences = LauncherPreferences(),
    val allApps: List<AppEntry> = emptyList(),
    val visibleApps: List<AppEntry> = emptyList(),
    val dockSlotApps: List<AppEntry?> = listOf(null, null, null),
    val iconPacks: List<IconPackInfo> = emptyList(),
    val currentScreen: LauncherScreen = LauncherScreen.DASHBOARD,
    val isOledDimmerActive: Boolean = false,
    val showSplitAppDialog: Boolean = false,
    val splitPreselectedPrimary: String? = null,
    val pendingValetAction: ValetAction? = null,
    val appBeingEdited: AppEntry? = null,
    val toastMessage: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LauncherPreferencesRepository(application)
    val iconProcessor = IconProcessor(application)
    val telemetryManager = TelemetryManager(application)

    private val _allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private val _iconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val _currentScreen = MutableStateFlow(LauncherScreen.DASHBOARD)
    private val _isOledDimmerActive = MutableStateFlow(false)
    private val _showSplitAppDialog = MutableStateFlow(false)
    private val _splitPreselectedPrimary = MutableStateFlow<String?>(null)
    private val _pendingValetAction = MutableStateFlow<ValetAction?>(null)
    private val _appBeingEdited = MutableStateFlow<AppEntry?>(null)
    private val _toastMessage = MutableStateFlow<String?>(null)

    private val _processedIcons = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val processedIcons: StateFlow<Map<String, ImageBitmap>> = _processedIcons.asStateFlow()

    val mediaState: StateFlow<MediaPlaybackState> = CarMediaNotificationListenerService.mediaState
    val systemStatus: StateFlow<SystemStatusState> = telemetryManager.systemStatus
    val telemetryState: StateFlow<TelemetryState> = telemetryManager.telemetry

    val preferences: StateFlow<LauncherPreferences> = repo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LauncherPreferences()
    )

    private val modalStateFlow = combine(
        _isOledDimmerActive,
        _showSplitAppDialog,
        _splitPreselectedPrimary,
        _pendingValetAction,
        _appBeingEdited
    ) { dimmer, splitDialog, splitPrimary, valetAction, editingApp ->
        ModalStates(dimmer, splitDialog, splitPrimary, valetAction, editingApp)
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        preferences,
        _allApps,
        _iconPacks,
        _currentScreen,
        modalStateFlow,
        _toastMessage
    ) { arr ->
        val prefs = arr[0] as LauncherPreferences
        @Suppress("UNCHECKED_CAST")
        val apps = arr[1] as List<AppEntry>
        @Suppress("UNCHECKED_CAST")
        val packs = arr[2] as List<IconPackInfo>
        val screen = arr[3] as LauncherScreen
        val modal = arr[4] as ModalStates
        val toast = arr[5] as String?

        val visible = apps.filter { !it.isHidden }
        val appMap = apps.associateBy { it.packageName }

        // Compute 3 dynamic dock slots: pinned overrides recent; remaining filled by recent then popular visible apps
        val used = mutableSetOf<String>()
        val resolvedSlots = MutableList<AppEntry?>(3) { idx ->
            val pinnedPkg = prefs.pinnedDockPackages.getOrNull(idx)
            val pinnedApp = pinnedPkg?.let { appMap[it] }
            if (pinnedApp != null) {
                used.add(pinnedApp.packageName)
                pinnedApp
            } else {
                null
            }
        }
        val candidates = (prefs.recentPackages.mapNotNull { appMap[it] } + visible)
            .distinctBy { it.packageName }
            .filter { it.packageName !in used }

        var candIdx = 0
        for (i in 0..2) {
            if (resolvedSlots[i] == null && candIdx < candidates.size) {
                resolvedSlots[i] = candidates[candIdx++]
            }
        }

        LauncherUiState(
            preferences = prefs,
            allApps = apps,
            visibleApps = visible,
            dockSlotApps = resolvedSlots,
            iconPacks = packs,
            currentScreen = screen,
            isOledDimmerActive = modal.dimmer,
            showSplitAppDialog = modal.splitDialog,
            splitPreselectedPrimary = modal.splitPrimary,
            pendingValetAction = modal.valetAction,
            appBeingEdited = modal.editingApp,
            toastMessage = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LauncherUiState()
    )

    init {
        telemetryManager.start()
        CarMediaNotificationListenerService.refreshPermissionStatus(getApplication())

        viewModelScope.launch {
            _iconPacks.value = iconProcessor.discoverIconPacks()
        }

        // Reload installed apps whenever preferences change (hidden/labels/order/iconPack)
        viewModelScope.launch {
            preferences.collect { prefs ->
                refreshAppsList(prefs)
            }
        }

        // Watch for package install / uninstall / update events
        viewModelScope.launch {
            iconProcessor.observePackageChanges().collect {
                _iconPacks.value = iconProcessor.discoverIconPacks()
                refreshAppsList(preferences.value)
            }
        }

        // Trigger speed guard alert chime when speeding
        viewModelScope.launch {
            combine(telemetryState, preferences) { tel, prefs ->
                Triple(prefs.speedGuardEnabled, tel.speedKmh, prefs.speedLimitKmh)
            }.collect { (enabled, speed, limit) ->
                telemetryManager.playSpeedWarningChimeIfNeeded(enabled, speed, limit)
            }
        }
    }

    private suspend fun refreshAppsList(prefs: LauncherPreferences) {
        val loaded = iconProcessor.loadInstalledApps(
            hiddenPackages = prefs.hiddenPackages,
            customLabels = prefs.customLabels,
            savedOrder = prefs.appOrder
        )
        _allApps.value = loaded

        val iconMap = mutableMapOf<String, ImageBitmap>()
        for (app in loaded) {
            val bmp = iconProcessor.getProcessedIcon(
                app = app,
                iconPackPackage = prefs.activeIconPackPackage,
                sizePx = 192
            )
            iconMap[app.packageName] = bmp
        }
        _processedIcons.value = iconMap
    }

    fun onPermissionsUpdated() {
        telemetryManager.refreshPermissionsAndStatus()
        CarMediaNotificationListenerService.refreshPermissionStatus(getApplication())
    }

    fun toggleDashboardAndGrid() {
        _currentScreen.value = when (_currentScreen.value) {
            LauncherScreen.DASHBOARD -> LauncherScreen.APP_GRID
            LauncherScreen.APP_GRID -> LauncherScreen.DASHBOARD
            LauncherScreen.SETTINGS -> LauncherScreen.DASHBOARD
        }
    }

    fun navigateTo(screen: LauncherScreen) {
        if (screen == LauncherScreen.SETTINGS && preferences.value.valetModeEnabled) {
            _pendingValetAction.value = ValetAction.OpenSettings
        } else {
            _currentScreen.value = screen
        }
    }

    fun navigateBackToDashboard() {
        _currentScreen.value = LauncherScreen.DASHBOARD
    }

    fun requestAppEdit(app: AppEntry) {
        if (preferences.value.valetModeEnabled) {
            _pendingValetAction.value = ValetAction.EditApp(app)
        } else {
            _appBeingEdited.value = app
        }
    }

    fun dismissAppEdit() {
        _appBeingEdited.value = null
    }

    fun verifyValetPin(enteredPin: String): Boolean {
        val valid = enteredPin == preferences.value.valetPin
        if (valid) {
            val action = _pendingValetAction.value
            _pendingValetAction.value = null
            when (action) {
                ValetAction.OpenSettings -> _currentScreen.value = LauncherScreen.SETTINGS
                is ValetAction.EditApp -> _appBeingEdited.value = action.app
                null -> Unit
            }
        }
        return valid
    }

    fun dismissValetPrompt() {
        _pendingValetAction.value = null
    }

    fun setOledDimmer(active: Boolean) {
        _isOledDimmerActive.value = active
    }

    fun openSplitAppDialog(preselectedPrimaryPkg: String? = null) {
        _splitPreselectedPrimary.value = preselectedPrimaryPkg
        _showSplitAppDialog.value = true
    }

    fun dismissSplitAppDialog() {
        _showSplitAppDialog.value = false
        _splitPreselectedPrimary.value = null
    }

    fun launchSplitScreen(primaryPkg: String, secondaryPkg: String) {
        dismissSplitAppDialog()
        AppLauncherHelper.launchSplitScreenPair(getApplication(), primaryPkg, secondaryPkg)
        recordRecentApp(primaryPkg)
        recordRecentApp(secondaryPkg)
    }

    fun launchApp(app: AppEntry) {
        val launched = AppLauncherHelper.launchEntry(
            context = getApplication(),
            app = app,
            navProvider = preferences.value.navProvider,
            onOpenSettings = { navigateTo(LauncherScreen.SETTINGS) },
            onOpenDashboard = { navigateBackToDashboard() }
        )
        if (launched && app.virtualAction != AppLauncherHelper.VIRTUAL_SETTINGS) {
            recordRecentApp(app.packageName)
        }
    }

    private fun recordRecentApp(packageName: String) {
        viewModelScope.launch {
            repo.updatePreferences { prefs ->
                val updated = listOf(packageName) + prefs.recentPackages.filter { it != packageName }
                prefs.copy(recentPackages = updated.take(6))
            }
        }
    }

    fun pinAppToDockSlot(slotIndex: Int, packageName: String?) {
        viewModelScope.launch {
            repo.updatePreferences { prefs ->
                val mutable = prefs.pinnedDockPackages.toMutableList()
                while (mutable.size < 3) mutable.add(null)
                // Remove package from other slots if already pinned
                if (packageName != null) {
                    for (i in mutable.indices) {
                        if (mutable[i] == packageName) mutable[i] = null
                    }
                }
                mutable[slotIndex.coerceIn(0, 2)] = packageName
                prefs.copy(pinnedDockPackages = mutable)
            }
        }
    }

    fun renameApp(packageName: String, newName: String) {
        viewModelScope.launch {
            repo.updatePreferences { prefs ->
                val updated = prefs.customLabels.toMutableMap()
                if (newName.isBlank()) updated.remove(packageName) else updated[packageName] = newName.trim()
                prefs.copy(customLabels = updated)
            }
        }
    }

    fun setAppHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch {
            repo.updatePreferences { prefs ->
                val updated = prefs.hiddenPackages.toMutableSet()
                if (hidden) updated.add(packageName) else updated.remove(packageName)
                prefs.copy(hiddenPackages = updated)
            }
        }
    }

    fun moveAppInOrder(packageName: String, delta: Int) {
        viewModelScope.launch {
            val currentVisible = uiState.value.visibleApps.map { it.packageName }.toMutableList()
            val idx = currentVisible.indexOf(packageName)
            if (idx < 0) return@launch
            val targetIdx = (idx + delta).coerceIn(0, currentVisible.lastIndex)
            if (idx == targetIdx) return@launch
            currentVisible.removeAt(idx)
            currentVisible.add(targetIdx, packageName)

            val hidden = uiState.value.allApps.filter { it.isHidden }.map { it.packageName }
            repo.updatePreferences { prefs ->
                prefs.copy(appOrder = currentVisible + hidden)
            }
        }
    }

    fun updateIconSize(dp: Float) = updatePref { it.copy(iconSizeDp = dp) }
    fun updateCornerRadius(dp: Float) = updatePref { it.copy(cornerRadiusDp = dp) }
    fun updateCardAlpha(alpha: Float) = updatePref { it.copy(cardAlpha = alpha) }
    fun updateFontScale(scale: Float) = updatePref { it.copy(fontScale = scale) }
    fun updateDockSide(side: DockSide) = updatePref { it.copy(dockSide = side) }
    fun updateDayNightMode(mode: DayNightMode) = updatePref { it.copy(dayNightMode = mode) }
    fun updateWallpaperPreset(preset: WallpaperPreset) = updatePref { it.copy(wallpaperPreset = preset) }

    fun setCustomWallpaperUri(uri: Uri) {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        updatePref {
            it.copy(
                wallpaperPreset = WallpaperPreset.CUSTOM_IMAGE,
                customWallpaperUri = uri.toString()
            )
        }
    }

    fun setActiveIconPack(packageName: String?) {
        iconProcessor.clearCache()
        updatePref { it.copy(activeIconPackPackage = packageName) }
    }

    fun updateNavProvider(provider: NavigationProvider) = updatePref { it.copy(navProvider = provider) }
    fun updateHomeAddress(addr: String) = updatePref { it.copy(homeAddress = addr) }
    fun updateWorkAddress(addr: String) = updatePref { it.copy(workAddress = addr) }
    fun updateSpeedGuard(enabled: Boolean) = updatePref { it.copy(speedGuardEnabled = enabled) }
    fun updateSpeedLimit(limitKmh: Int) = updatePref { it.copy(speedLimitKmh = limitKmh) }
    fun updateSpeedUnit(unit: SpeedUnit) = updatePref { it.copy(speedUnit = unit) }
    fun updateValetMode(enabled: Boolean) = updatePref { it.copy(valetModeEnabled = enabled) }
    fun updateValetPin(pin: String) {
        val clean = pin.filter { it.isDigit() }.take(4)
        if (clean.length == 4) updatePref { it.copy(valetPin = clean) }
    }

    fun completeOnboarding() = updatePref { it.copy(onboardingCompleted = true) }

    fun exportBackup(uri: Uri, successMsg: String, failMsg: String) {
        viewModelScope.launch {
            val ok = repo.exportToUri(uri)
            _toastMessage.value = if (ok) successMsg else failMsg
        }
    }

    fun importBackup(uri: Uri, successMsg: String, failMsg: String) {
        viewModelScope.launch {
            val ok = repo.importFromUri(uri)
            if (ok) iconProcessor.clearCache()
            _toastMessage.value = if (ok) successMsg else failMsg
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    private fun updatePref(block: (LauncherPreferences) -> LauncherPreferences) {
        viewModelScope.launch { repo.updatePreferences(block) }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryManager.stop()
    }

    private data class ModalStates(
        val dimmer: Boolean,
        val splitDialog: Boolean,
        val splitPrimary: String?,
        val valetAction: ValetAction?,
        val editingApp: AppEntry?
    )
}
