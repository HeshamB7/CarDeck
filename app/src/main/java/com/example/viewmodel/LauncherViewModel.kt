package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherPreferencesRepository
import com.example.engine.AppLauncherHelper
import com.example.engine.DriverAssistUtils
import com.example.engine.IconProcessor
import com.example.engine.TelemetryManager
import com.example.model.AppEntry
import com.example.model.CarWidgetType
import com.example.model.DashboardPageConfig
import com.example.model.DashboardWidgetItem
import com.example.model.DayNightMode
import com.example.model.DockSide
import com.example.model.IconPackInfo
import com.example.model.LauncherPreferences
import com.example.model.LauncherScreen
import com.example.model.MediaPlaybackState
import com.example.model.NavigationProvider
import com.example.model.QuickContact
import com.example.model.SpeedUnit
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.model.WallpaperPreset
import com.example.model.defaultDashboardPages
import com.example.service.CarMediaNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class BannerNotice(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val isWarning: Boolean = false
)

sealed interface ValetAction {
    data object OpenSettings : ValetAction
    data class EditApp(val app: AppEntry) : ValetAction
    data class CustomizeDockSlot(val slotIndex: Int) : ValetAction
    data object ToggleWidgetEdit : ValetAction
}

object DashboardPageMutator {
    fun addPage(pages: List<DashboardPageConfig>, newId: String = "page_${UUID.randomUUID().toString().take(8)}"): List<DashboardPageConfig> {
        val nextNum = pages.size + 1
        return pages + DashboardPageConfig(
            id = newId,
            title = "Page $nextNum",
            widgets = emptyList()
        )
    }

    fun deletePage(pages: List<DashboardPageConfig>, pageIndex: Int): List<DashboardPageConfig> {
        if (pages.size <= 1 || pageIndex !in pages.indices) return pages
        return pages.filterIndexed { index, _ -> index != pageIndex }
    }

    fun addWidget(
        pages: List<DashboardPageConfig>,
        pageIndex: Int,
        type: CarWidgetType,
        widgetId: String = "w_${UUID.randomUUID().toString().take(8)}"
    ): List<DashboardPageConfig> {
        if (pageIndex !in pages.indices) return pages
        return pages.mapIndexed { idx, page ->
            if (idx != pageIndex) page
            else page.copy(widgets = page.widgets + DashboardWidgetItem(id = widgetId, type = type))
        }
    }

    fun removeWidget(
        pages: List<DashboardPageConfig>,
        pageIndex: Int,
        widgetId: String
    ): List<DashboardPageConfig> {
        if (pageIndex !in pages.indices) return pages
        return pages.mapIndexed { idx, page ->
            if (idx != pageIndex) page
            else page.copy(widgets = page.widgets.filterNot { it.id == widgetId })
        }
    }

    fun moveWidget(
        pages: List<DashboardPageConfig>,
        pageIndex: Int,
        widgetId: String,
        delta: Int
    ): List<DashboardPageConfig> {
        if (pageIndex !in pages.indices) return pages
        return pages.mapIndexed { idx, page ->
            if (idx != pageIndex) page
            else {
                val list = page.widgets.toMutableList()
                val curIdx = list.indexOfFirst { it.id == widgetId }
                if (curIdx < 0) return@mapIndexed page
                val targetIdx = (curIdx + delta).coerceIn(0, list.lastIndex)
                if (curIdx == targetIdx) return@mapIndexed page
                val item = list.removeAt(curIdx)
                list.add(targetIdx, item)
                page.copy(widgets = list)
            }
        }
    }

    fun swapWidgets(
        pages: List<DashboardPageConfig>,
        pageIndex: Int,
        firstId: String,
        secondId: String
    ): List<DashboardPageConfig> {
        if (pageIndex !in pages.indices || firstId == secondId) return pages
        return pages.mapIndexed { idx, page ->
            if (idx != pageIndex) page
            else {
                val list = page.widgets.toMutableList()
                val i1 = list.indexOfFirst { it.id == firstId }
                val i2 = list.indexOfFirst { it.id == secondId }
                if (i1 < 0 || i2 < 0) return@mapIndexed page
                val temp = list[i1]
                list[i1] = list[i2]
                list[i2] = temp
                page.copy(widgets = list)
            }
        }
    }
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
    val dockSlotBeingCustomized: Int? = null,
    val isWidgetEditMode: Boolean = false,
    val addWidgetPageIndex: Int? = null,
    val quickContactBeingEdited: QuickContact? = null,
    val bannerNotice: BannerNotice? = null,
    val isHighwayFocusActive: Boolean = false,
    val isBreakReminderDue: Boolean = false
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
    private val _dockSlotBeingCustomized = MutableStateFlow<Int?>(null)
    private val _isWidgetEditMode = MutableStateFlow(false)
    private val _addWidgetPageIndex = MutableStateFlow<Int?>(null)
    private val _quickContactBeingEdited = MutableStateFlow<QuickContact?>(null)
    private val _bannerNotice = MutableStateFlow<BannerNotice?>(null)
    private val _manualHighwayFocus = MutableStateFlow<Boolean?>(null)
    private val _breakDismissedUntilMs = MutableStateFlow(0L)
    private var lastAutoFocusState = false

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

    private val editModalStateFlow = combine(
        _dockSlotBeingCustomized,
        _isWidgetEditMode,
        _addWidgetPageIndex,
        _quickContactBeingEdited
    ) { dockSlot, widgetEdit, addPageIdx, contact ->
        EditModalStates(dockSlot, widgetEdit, addPageIdx, contact)
    }

    private val assistStateFlow = combine(
        preferences,
        telemetryState,
        _manualHighwayFocus,
        _breakDismissedUntilMs
    ) { prefs, tel, manualFocus, dismissedUntil ->
        val focus = DriverAssistUtils.shouldHighwayFocusBeActive(
            manualOverride = manualFocus,
            autoEnabled = prefs.highwayFocusAutoEnabled,
            speedKmh = tel.speedKmh,
            currentlyActive = lastAutoFocusState
        )
        lastAutoFocusState = focus
        val breakDue = DriverAssistUtils.shouldTriggerBreakReminder(
            tripStartTimeMs = tel.tripStartTimeMs,
            nowMs = System.currentTimeMillis(),
            thresholdMinutes = prefs.breakReminderMinutes,
            dismissedUntilMs = dismissedUntil
        )
        AssistStates(focus, breakDue)
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        preferences,
        _allApps,
        _iconPacks,
        _currentScreen,
        modalStateFlow,
        editModalStateFlow,
        _bannerNotice,
        assistStateFlow
    ) { arr ->
        val prefs = arr[0] as LauncherPreferences
        @Suppress("UNCHECKED_CAST")
        val apps = arr[1] as List<AppEntry>
        @Suppress("UNCHECKED_CAST")
        val packs = arr[2] as List<IconPackInfo>
        val screen = arr[3] as LauncherScreen
        val modal = arr[4] as ModalStates
        val editModal = arr[5] as EditModalStates
        val banner = arr[6] as BannerNotice?
        val assist = arr[7] as AssistStates

        val visible = apps.filter { !it.isHidden }
        val appMap = apps.associateBy { it.packageName }

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
            dockSlotBeingCustomized = editModal.dockSlot,
            isWidgetEditMode = editModal.widgetEdit,
            addWidgetPageIndex = editModal.addPageIdx,
            quickContactBeingEdited = editModal.contact,
            bannerNotice = banner,
            isHighwayFocusActive = assist.highwayFocusActive,
            isBreakReminderDue = assist.breakReminderDue
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

        viewModelScope.launch {
            preferences
                .distinctUntilChangedBy { prefs ->
                    AppReloadKey(
                        hiddenPackages = prefs.hiddenPackages,
                        customLabels = prefs.customLabels,
                        appOrder = prefs.appOrder,
                        iconPack = prefs.activeIconPackPackage
                    )
                }
                .collect { prefs ->
                    refreshAppsList(prefs)
                }
        }

        viewModelScope.launch {
            iconProcessor.observePackageChanges().collect {
                _iconPacks.value = iconProcessor.discoverIconPacks()
                refreshAppsList(preferences.value)
            }
        }

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

        val iconMap = coroutineScope {
            loaded.map { app ->
                async(Dispatchers.Default) {
                    app.packageName to iconProcessor.getProcessedIcon(
                        app = app,
                        iconPackPackage = prefs.activeIconPackPackage,
                        sizePx = 192
                    )
                }
            }.awaitAll().toMap()
        }
        _processedIcons.value = iconMap
    }

    fun onPermissionsUpdated() {
        telemetryManager.refreshPermissionsAndStatus()
        CarMediaNotificationListenerService.refreshPermissionStatus(getApplication())
    }

    fun refreshWeather() {
        telemetryManager.refreshWeatherNow()
    }

    fun toggleDashboardAndGrid() {
        _isWidgetEditMode.value = false
        _currentScreen.value = when (_currentScreen.value) {
            LauncherScreen.DASHBOARD -> LauncherScreen.APP_GRID
            LauncherScreen.APP_GRID -> LauncherScreen.DASHBOARD
            LauncherScreen.SETTINGS -> LauncherScreen.DASHBOARD
        }
    }

    fun navigateTo(screen: LauncherScreen) {
        _isWidgetEditMode.value = false
        if (screen == LauncherScreen.SETTINGS && preferences.value.valetModeEnabled) {
            _pendingValetAction.value = ValetAction.OpenSettings
        } else {
            _currentScreen.value = screen
        }
    }

    fun navigateBackToDashboard() {
        _isWidgetEditMode.value = false
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

    fun requestCustomizeDockSlot(slotIndex: Int) {
        if (preferences.value.valetModeEnabled) {
            _pendingValetAction.value = ValetAction.CustomizeDockSlot(slotIndex)
        } else {
            _dockSlotBeingCustomized.value = slotIndex.coerceIn(0, 2)
        }
    }

    fun dismissDockSlotCustomization() {
        _dockSlotBeingCustomized.value = null
    }

    fun toggleWidgetEditMode() {
        if (!_isWidgetEditMode.value && preferences.value.valetModeEnabled) {
            _pendingValetAction.value = ValetAction.ToggleWidgetEdit
        } else {
            _isWidgetEditMode.value = !_isWidgetEditMode.value
        }
    }

    fun setWidgetEditMode(enabled: Boolean) {
        _isWidgetEditMode.value = enabled
    }

    fun openAddWidgetSheet(pageIndex: Int) {
        _addWidgetPageIndex.value = pageIndex
    }

    fun dismissAddWidgetSheet() {
        _addWidgetPageIndex.value = null
    }

    fun addDashboardPage() {
        updatePref { prefs ->
            prefs.copy(dashboardPages = DashboardPageMutator.addPage(prefs.dashboardPages))
        }
    }

    fun deleteDashboardPage(pageIndex: Int) {
        updatePref { prefs ->
            prefs.copy(dashboardPages = DashboardPageMutator.deletePage(prefs.dashboardPages, pageIndex))
        }
    }

    fun addWidgetToPage(pageIndex: Int, type: CarWidgetType) {
        updatePref { prefs ->
            prefs.copy(dashboardPages = DashboardPageMutator.addWidget(prefs.dashboardPages, pageIndex, type))
        }
        _addWidgetPageIndex.value = null
    }

    fun removeWidgetFromPage(pageIndex: Int, widgetId: String) {
        updatePref { prefs ->
            prefs.copy(dashboardPages = DashboardPageMutator.removeWidget(prefs.dashboardPages, pageIndex, widgetId))
        }
    }

    fun moveWidgetInPage(pageIndex: Int, widgetId: String, delta: Int) {
        updatePref { prefs ->
            prefs.copy(dashboardPages = DashboardPageMutator.moveWidget(prefs.dashboardPages, pageIndex, widgetId, delta))
        }
    }

    fun swapWidgetsInPage(pageIndex: Int, firstId: String, secondId: String) {
        updatePref { prefs ->
            prefs.copy(dashboardPages = DashboardPageMutator.swapWidgets(prefs.dashboardPages, pageIndex, firstId, secondId))
        }
    }

    fun resetDashboardPagesToDefault() {
        updatePref { prefs -> prefs.copy(dashboardPages = defaultDashboardPages()) }
    }

    fun requestEditQuickContact(contact: QuickContact) {
        _quickContactBeingEdited.value = contact
    }

    fun dismissEditQuickContact() {
        _quickContactBeingEdited.value = null
    }

    fun saveQuickContact(updated: QuickContact) {
        _quickContactBeingEdited.value = null
        updatePref { prefs ->
            val list = prefs.quickContacts.map { if (it.id == updated.id) updated else it }
            prefs.copy(quickContacts = list)
        }
    }

    fun verifyValetPin(enteredPin: String): Boolean {
        val valid = enteredPin == preferences.value.valetPin
        if (valid) {
            val action = _pendingValetAction.value
            _pendingValetAction.value = null
            when (action) {
                ValetAction.OpenSettings -> _currentScreen.value = LauncherScreen.SETTINGS
                is ValetAction.EditApp -> _appBeingEdited.value = action.app
                is ValetAction.CustomizeDockSlot -> _dockSlotBeingCustomized.value = action.slotIndex.coerceIn(0, 2)
                ValetAction.ToggleWidgetEdit -> _isWidgetEditMode.value = true
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

    fun updateNightWarmth(enabled: Boolean) = updatePref { it.copy(nightWarmthEnabled = enabled) }
    fun updateHighwayFocusAuto(enabled: Boolean) = updatePref { it.copy(highwayFocusAutoEnabled = enabled) }
    fun updateBreakReminderMinutes(minutes: Int) = updatePref { it.copy(breakReminderMinutes = minutes.coerceIn(30, 240)) }

    fun toggleHighwayFocus(onMsg: String, offMsg: String) {
        val next = !uiState.value.isHighwayFocusActive
        _manualHighwayFocus.value = next
        if (next) _isWidgetEditMode.value = false
        showBanner(if (next) onMsg else offMsg)
    }

    fun dismissBreakReminder() {
        _breakDismissedUntilMs.value = System.currentTimeMillis() + 20 * 60_000L
    }

    fun resetTripTimer(resetMsg: String) {
        telemetryManager.resetTripTimer()
        _breakDismissedUntilMs.value = 0L
        showBanner(resetMsg)
    }

    fun saveOrNavigateParkedSpot(savedMsg: String, noGpsMsg: String) {
        val prefs = preferences.value
        val tel = telemetryState.value
        if (prefs.parkedLatitude != null && prefs.parkedLongitude != null) {
            AppLauncherHelper.launchCoordinatesNavigation(
                context = getApplication(),
                latitude = prefs.parkedLatitude,
                longitude = prefs.parkedLongitude,
                provider = prefs.navProvider
            )
        } else if (tel.hasGpsFix) {
            val now = System.currentTimeMillis()
            updatePref {
                it.copy(
                    parkedLatitude = tel.latitude,
                    parkedLongitude = tel.longitude,
                    parkedTimestampMs = now
                )
            }
            showBanner(savedMsg)
        } else {
            showBanner(noGpsMsg, isWarning = true)
        }
    }

    fun clearParkedSpot(clearedMsg: String) {
        updatePref {
            it.copy(
                parkedLatitude = null,
                parkedLongitude = null,
                parkedTimestampMs = null
            )
        }
        showBanner(clearedMsg)
    }

    fun completeOnboarding() = updatePref { it.copy(onboardingCompleted = true) }

    fun showBanner(message: String, isWarning: Boolean = false) {
        _bannerNotice.value = BannerNotice(message = message, isWarning = isWarning)
    }

    fun consumeBanner() {
        _bannerNotice.value = null
    }

    fun exportBackup(uri: Uri, successMsg: String, failMsg: String) {
        viewModelScope.launch {
            val ok = repo.exportToUri(uri)
            showBanner(if (ok) successMsg else failMsg, isWarning = !ok)
        }
    }

    fun importBackup(uri: Uri, successMsg: String, failMsg: String) {
        viewModelScope.launch {
            val ok = repo.importFromUri(uri)
            if (ok) iconProcessor.clearCache()
            showBanner(if (ok) successMsg else failMsg, isWarning = !ok)
        }
    }

    private fun updatePref(block: (LauncherPreferences) -> LauncherPreferences) {
        viewModelScope.launch { repo.updatePreferences(block) }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryManager.stop()
    }

    private data class AppReloadKey(
        val hiddenPackages: Set<String>,
        val customLabels: Map<String, String>,
        val appOrder: List<String>,
        val iconPack: String?
    )

    private data class ModalStates(
        val dimmer: Boolean,
        val splitDialog: Boolean,
        val splitPrimary: String?,
        val valetAction: ValetAction?,
        val editingApp: AppEntry?
    )

    private data class EditModalStates(
        val dockSlot: Int?,
        val widgetEdit: Boolean,
        val addPageIdx: Int?,
        val contact: QuickContact?
    )

    private data class AssistStates(
        val highwayFocusActive: Boolean,
        val breakReminderDue: Boolean
    )
}
