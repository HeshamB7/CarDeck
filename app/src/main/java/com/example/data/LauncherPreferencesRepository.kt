package com.example.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CarWidgetType
import com.example.model.DashboardPageConfig
import com.example.model.DashboardWidgetItem
import com.example.model.DayNightMode
import com.example.model.DockSide
import com.example.model.LauncherPreferences
import com.example.model.NavigationProvider
import com.example.model.QuickContact
import com.example.model.SpeedUnit
import com.example.model.WallpaperPreset
import com.example.model.defaultDashboardPages
import com.example.model.defaultQuickContacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.carDeckDataStore: DataStore<Preferences> by preferencesDataStore(name = "cardeck_prefs")

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val ICON_SIZE_DP = floatPreferencesKey("icon_size_dp")
        val CORNER_RADIUS_DP = floatPreferencesKey("corner_radius_dp")
        val CARD_ALPHA = floatPreferencesKey("card_alpha")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val DOCK_SIDE = stringPreferencesKey("dock_side")
        val DAY_NIGHT_MODE = stringPreferencesKey("day_night_mode")
        val WALLPAPER_PRESET = stringPreferencesKey("wallpaper_preset")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val ACTIVE_ICON_PACK = stringPreferencesKey("active_icon_pack")
        val PINNED_DOCK_SLOTS = stringPreferencesKey("pinned_dock_slots")
        val RECENT_PACKAGES = stringPreferencesKey("recent_packages")
        val HIDDEN_PACKAGES = stringPreferencesKey("hidden_packages")
        val CUSTOM_LABELS = stringPreferencesKey("custom_labels")
        val APP_ORDER = stringPreferencesKey("app_order")
        val NAV_PROVIDER = stringPreferencesKey("nav_provider")
        val HOME_ADDRESS = stringPreferencesKey("home_address")
        val WORK_ADDRESS = stringPreferencesKey("work_address")
        val SPEED_LIMIT_KMH = intPreferencesKey("speed_limit_kmh")
        val SPEED_GUARD_ENABLED = booleanPreferencesKey("speed_guard_enabled")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val VALET_MODE_ENABLED = booleanPreferencesKey("valet_mode_enabled")
        val VALET_PIN = stringPreferencesKey("valet_pin")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DASHBOARD_PAGES = stringPreferencesKey("dashboard_pages")
        val QUICK_CONTACTS = stringPreferencesKey("quick_contacts")
        val NIGHT_WARMTH_ENABLED = booleanPreferencesKey("night_warmth_enabled")
        val HIGHWAY_FOCUS_AUTO = booleanPreferencesKey("highway_focus_auto")
        val BREAK_REMINDER_MINUTES = intPreferencesKey("break_reminder_minutes")
        val PARKED_LAT = doublePreferencesKey("parked_lat")
        val PARKED_LON = doublePreferencesKey("parked_lon")
        val PARKED_TIME_MS = longPreferencesKey("parked_time_ms")
    }

    val preferencesFlow: Flow<LauncherPreferences> = context.carDeckDataStore.data.map { prefs ->
        decodePreferences(prefs)
    }

    suspend fun updatePreferences(transform: (LauncherPreferences) -> LauncherPreferences) {
        context.carDeckDataStore.edit { prefs ->
            val current = decodePreferences(prefs)
            val updated = transform(current)
            writeAll(prefs, updated)
        }
    }

    private fun decodePreferences(prefs: Preferences): LauncherPreferences {
        return LauncherPreferences(
            iconSizeDp = prefs[Keys.ICON_SIZE_DP] ?: 68f,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS_DP] ?: 24f,
            cardAlpha = prefs[Keys.CARD_ALPHA] ?: 0.76f,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            dockSide = parseEnum(prefs[Keys.DOCK_SIDE], DockSide.LEFT),
            dayNightMode = parseEnum(prefs[Keys.DAY_NIGHT_MODE], DayNightMode.AUTO_CANBUS),
            wallpaperPreset = parseEnum(prefs[Keys.WALLPAPER_PRESET], WallpaperPreset.IOS_18_DYNAMIC),
            customWallpaperUri = prefs[Keys.CUSTOM_WALLPAPER_URI]?.takeIf { it.isNotBlank() },
            activeIconPackPackage = prefs[Keys.ACTIVE_ICON_PACK]?.takeIf { it.isNotBlank() },
            pinnedDockPackages = decodeNullableStringList(prefs[Keys.PINNED_DOCK_SLOTS], 3),
            recentPackages = decodeStringList(prefs[Keys.RECENT_PACKAGES]),
            hiddenPackages = decodeStringList(prefs[Keys.HIDDEN_PACKAGES]).toSet(),
            customLabels = decodeStringMap(prefs[Keys.CUSTOM_LABELS]),
            appOrder = decodeStringList(prefs[Keys.APP_ORDER]),
            navProvider = parseEnum(prefs[Keys.NAV_PROVIDER], NavigationProvider.GOOGLE_MAPS),
            homeAddress = prefs[Keys.HOME_ADDRESS] ?: "Home",
            workAddress = prefs[Keys.WORK_ADDRESS] ?: "Work",
            speedLimitKmh = prefs[Keys.SPEED_LIMIT_KMH] ?: 120,
            speedGuardEnabled = prefs[Keys.SPEED_GUARD_ENABLED] ?: true,
            speedUnit = parseEnum(prefs[Keys.SPEED_UNIT], SpeedUnit.MPH),
            valetModeEnabled = prefs[Keys.VALET_MODE_ENABLED] ?: false,
            valetPin = prefs[Keys.VALET_PIN] ?: "1234",
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            dashboardPages = decodeDashboardPages(prefs[Keys.DASHBOARD_PAGES]),
            quickContacts = decodeQuickContacts(prefs[Keys.QUICK_CONTACTS]),
            nightWarmthEnabled = prefs[Keys.NIGHT_WARMTH_ENABLED] ?: true,
            highwayFocusAutoEnabled = prefs[Keys.HIGHWAY_FOCUS_AUTO] ?: false,
            breakReminderMinutes = prefs[Keys.BREAK_REMINDER_MINUTES] ?: 90,
            parkedLatitude = prefs[Keys.PARKED_LAT]?.takeIf { !it.isNaN() },
            parkedLongitude = prefs[Keys.PARKED_LON]?.takeIf { !it.isNaN() },
            parkedTimestampMs = prefs[Keys.PARKED_TIME_MS]?.takeIf { it > 0L }
        )
    }

    private fun writeAll(prefs: androidx.datastore.preferences.core.MutablePreferences, u: LauncherPreferences) {
        prefs[Keys.ICON_SIZE_DP] = u.iconSizeDp
        prefs[Keys.CORNER_RADIUS_DP] = u.cornerRadiusDp
        prefs[Keys.CARD_ALPHA] = u.cardAlpha
        prefs[Keys.FONT_SCALE] = u.fontScale
        prefs[Keys.DOCK_SIDE] = u.dockSide.name
        prefs[Keys.DAY_NIGHT_MODE] = u.dayNightMode.name
        prefs[Keys.WALLPAPER_PRESET] = u.wallpaperPreset.name
        prefs[Keys.CUSTOM_WALLPAPER_URI] = u.customWallpaperUri ?: ""
        prefs[Keys.ACTIVE_ICON_PACK] = u.activeIconPackPackage ?: ""
        prefs[Keys.PINNED_DOCK_SLOTS] = encodeNullableStringList(u.pinnedDockPackages)
        prefs[Keys.RECENT_PACKAGES] = encodeStringList(u.recentPackages)
        prefs[Keys.HIDDEN_PACKAGES] = encodeStringList(u.hiddenPackages.toList())
        prefs[Keys.CUSTOM_LABELS] = encodeStringMap(u.customLabels)
        prefs[Keys.APP_ORDER] = encodeStringList(u.appOrder)
        prefs[Keys.NAV_PROVIDER] = u.navProvider.name
        prefs[Keys.HOME_ADDRESS] = u.homeAddress
        prefs[Keys.WORK_ADDRESS] = u.workAddress
        prefs[Keys.SPEED_LIMIT_KMH] = u.speedLimitKmh
        prefs[Keys.SPEED_GUARD_ENABLED] = u.speedGuardEnabled
        prefs[Keys.SPEED_UNIT] = u.speedUnit.name
        prefs[Keys.VALET_MODE_ENABLED] = u.valetModeEnabled
        prefs[Keys.VALET_PIN] = u.valetPin
        prefs[Keys.ONBOARDING_COMPLETED] = u.onboardingCompleted
        prefs[Keys.DASHBOARD_PAGES] = encodeDashboardPages(u.dashboardPages)
        prefs[Keys.QUICK_CONTACTS] = encodeQuickContacts(u.quickContacts)
        prefs[Keys.NIGHT_WARMTH_ENABLED] = u.nightWarmthEnabled
        prefs[Keys.HIGHWAY_FOCUS_AUTO] = u.highwayFocusAutoEnabled
        prefs[Keys.BREAK_REMINDER_MINUTES] = u.breakReminderMinutes
        if (u.parkedLatitude != null && u.parkedLongitude != null && u.parkedTimestampMs != null) {
            prefs[Keys.PARKED_LAT] = u.parkedLatitude
            prefs[Keys.PARKED_LON] = u.parkedLongitude
            prefs[Keys.PARKED_TIME_MS] = u.parkedTimestampMs
        } else {
            prefs.remove(Keys.PARKED_LAT)
            prefs.remove(Keys.PARKED_LON)
            prefs.remove(Keys.PARKED_TIME_MS)
        }
    }

    suspend fun exportToUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val current = preferencesFlow.first()
            val json = JSONObject().apply {
                put("iconSizeDp", current.iconSizeDp.toDouble())
                put("cornerRadiusDp", current.cornerRadiusDp.toDouble())
                put("cardAlpha", current.cardAlpha.toDouble())
                put("fontScale", current.fontScale.toDouble())
                put("dockSide", current.dockSide.name)
                put("dayNightMode", current.dayNightMode.name)
                put("wallpaperPreset", current.wallpaperPreset.name)
                put("activeIconPackPackage", current.activeIconPackPackage ?: "")
                put("pinnedDockPackages", JSONArray(current.pinnedDockPackages.map { it ?: "" }))
                put("hiddenPackages", JSONArray(current.hiddenPackages.toList()))
                put("customLabels", JSONObject(current.customLabels))
                put("appOrder", JSONArray(current.appOrder))
                put("navProvider", current.navProvider.name)
                put("homeAddress", current.homeAddress)
                put("workAddress", current.workAddress)
                put("speedLimitKmh", current.speedLimitKmh)
                put("speedGuardEnabled", current.speedGuardEnabled)
                put("speedUnit", current.speedUnit.name)
                put("valetModeEnabled", current.valetModeEnabled)
                put("valetPin", current.valetPin)
                put("dashboardPages", JSONArray(encodeDashboardPages(current.dashboardPages)))
                put("quickContacts", JSONArray(encodeQuickContacts(current.quickContacts)))
                put("nightWarmthEnabled", current.nightWarmthEnabled)
                put("highwayFocusAutoEnabled", current.highwayFocusAutoEnabled)
                put("breakReminderMinutes", current.breakReminderMinutes)
            }
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    suspend fun importFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: return@runCatching false
            val json = JSONObject(raw)
            updatePreferences { current ->
                val pinnedJson = json.optJSONArray("pinnedDockPackages")
                val pinnedList = List(3) { idx ->
                    pinnedJson?.optString(idx)?.takeIf { it.isNotBlank() }
                }
                val hiddenSet = buildSet {
                    val arr = json.optJSONArray("hiddenPackages")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            arr.optString(i).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
                val labelsMap = buildMap {
                    val obj = json.optJSONObject("customLabels")
                    obj?.keys()?.forEach { key ->
                        put(key, obj.optString(key))
                    }
                }
                val orderList = buildList {
                    val arr = json.optJSONArray("appOrder")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            arr.optString(i).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
                val importedPagesRaw = json.optJSONArray("dashboardPages")?.toString()
                val importedContactsRaw = json.optJSONArray("quickContacts")?.toString()

                current.copy(
                    iconSizeDp = json.optDouble("iconSizeDp", current.iconSizeDp.toDouble()).toFloat().coerceIn(56f, 92f),
                    cornerRadiusDp = json.optDouble("cornerRadiusDp", current.cornerRadiusDp.toDouble()).toFloat().coerceIn(12f, 32f),
                    cardAlpha = json.optDouble("cardAlpha", current.cardAlpha.toDouble()).toFloat().coerceIn(0.35f, 0.96f),
                    fontScale = json.optDouble("fontScale", current.fontScale.toDouble()).toFloat().coerceIn(0.85f, 1.3f),
                    dockSide = parseEnum(json.optString("dockSide"), current.dockSide),
                    dayNightMode = parseEnum(json.optString("dayNightMode"), current.dayNightMode),
                    wallpaperPreset = parseEnum(json.optString("wallpaperPreset"), current.wallpaperPreset),
                    activeIconPackPackage = json.optString("activeIconPackPackage").takeIf { it.isNotBlank() },
                    pinnedDockPackages = pinnedList,
                    hiddenPackages = hiddenSet,
                    customLabels = labelsMap,
                    appOrder = orderList,
                    navProvider = parseEnum(json.optString("navProvider"), current.navProvider),
                    homeAddress = json.optString("homeAddress", current.homeAddress),
                    workAddress = json.optString("workAddress", current.workAddress),
                    speedLimitKmh = json.optInt("speedLimitKmh", current.speedLimitKmh).coerceIn(40, 220),
                    speedGuardEnabled = json.optBoolean("speedGuardEnabled", current.speedGuardEnabled),
                    speedUnit = parseEnum(json.optString("speedUnit"), current.speedUnit),
                    valetModeEnabled = json.optBoolean("valetModeEnabled", current.valetModeEnabled),
                    valetPin = json.optString("valetPin", current.valetPin).take(4).ifBlank { "1234" },
                    dashboardPages = importedPagesRaw?.let { decodeDashboardPages(it) } ?: current.dashboardPages,
                    quickContacts = importedContactsRaw?.let { decodeQuickContacts(it) } ?: current.quickContacts,
                    nightWarmthEnabled = json.optBoolean("nightWarmthEnabled", current.nightWarmthEnabled),
                    highwayFocusAutoEnabled = json.optBoolean("highwayFocusAutoEnabled", current.highwayFocusAutoEnabled),
                    breakReminderMinutes = json.optInt("breakReminderMinutes", current.breakReminderMinutes).coerceIn(30, 240)
                )
            }
            true
        }.getOrDefault(false)
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?, fallback: T): T {
        if (value.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
    }

    private fun encodeStringList(list: List<String>): String = JSONArray(list).toString()

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.optString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun encodeNullableStringList(list: List<String?>): String =
        JSONArray(list.map { it ?: "" }).toString()

    private fun decodeNullableStringList(raw: String?, size: Int): List<String?> {
        if (raw.isNullOrBlank()) return List(size) { null }
        return runCatching {
            val arr = JSONArray(raw)
            List(size) { idx -> arr.optString(idx).takeIf { it.isNotBlank() } }
        }.getOrDefault(List(size) { null })
    }

    private fun encodeStringMap(map: Map<String, String>): String = JSONObject(map).toString()

    private fun decodeStringMap(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key -> put(key, obj.optString(key)) }
            }
        }.getOrDefault(emptyMap())
    }

    companion object {
        fun encodeDashboardPages(pages: List<DashboardPageConfig>): String {
            val arr = JSONArray()
            pages.forEach { page ->
                val pageObj = JSONObject().apply {
                    put("id", page.id)
                    put("title", page.title)
                    val wArr = JSONArray()
                    page.widgets.forEach { w ->
                        wArr.put(
                            JSONObject().apply {
                                put("id", w.id)
                                put("type", w.type.name)
                            }
                        )
                    }
                    put("widgets", wArr)
                }
                arr.put(pageObj)
            }
            return arr.toString()
        }

        fun decodeDashboardPages(raw: String?): List<DashboardPageConfig> {
            if (raw.isNullOrBlank()) return defaultDashboardPages()
            return runCatching {
                val arr = JSONArray(raw)
                val pages = buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id").ifBlank { "page_$i" }
                        val title = obj.optString("title").ifBlank { "Page ${i + 1}" }
                        val wArr = obj.optJSONArray("widgets") ?: JSONArray()
                        val widgets = buildList {
                            for (j in 0 until wArr.length()) {
                                val wObj = wArr.optJSONObject(j) ?: continue
                                val wId = wObj.optString("id").ifBlank { "w_${i}_$j" }
                                val typeName = wObj.optString("type")
                                val type = runCatching { CarWidgetType.valueOf(typeName) }.getOrNull() ?: continue
                                add(DashboardWidgetItem(id = wId, type = type))
                            }
                        }
                        add(DashboardPageConfig(id = id, title = title, widgets = widgets))
                    }
                }
                pages.ifEmpty { defaultDashboardPages() }
            }.getOrDefault(defaultDashboardPages())
        }

        fun encodeQuickContacts(contacts: List<QuickContact>): String {
            val arr = JSONArray()
            contacts.forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phoneNumber", c.phoneNumber)
                    }
                )
            }
            return arr.toString()
        }

        fun decodeQuickContacts(raw: String?): List<QuickContact> {
            if (raw.isNullOrBlank()) return defaultQuickContacts()
            return runCatching {
                val arr = JSONArray(raw)
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        add(
                            QuickContact(
                                id = obj.optString("id").ifBlank { "qc_${i + 1}" },
                                name = obj.optString("name").ifBlank { "Contact ${i + 1}" },
                                phoneNumber = obj.optString("phoneNumber", "")
                            )
                        )
                    }
                }
                list.ifEmpty { defaultQuickContacts() }
            }.getOrDefault(defaultQuickContacts())
        }
    }
}
