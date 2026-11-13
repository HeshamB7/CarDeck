package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AppEntry
import com.example.model.DayNightMode
import com.example.model.DockSide
import com.example.model.IconPackInfo
import com.example.model.LauncherPreferences
import com.example.model.NavigationProvider
import com.example.model.SpeedUnit
import com.example.model.WallpaperPreset
import com.example.ui.components.CarPlayButton
import com.example.ui.components.CarPlayGlassCard
import com.example.ui.components.CarPlaySecondaryButton
import com.example.ui.components.CarPlaySegmentedChip
import com.example.ui.components.CarPlaySlider
import com.example.ui.components.CarPlaySquircleIcon
import com.example.ui.components.CarPlaySwitch
import com.example.ui.components.CarPlaySymbol
import com.example.ui.components.CarPlaySymbolType
import com.example.ui.components.CarPlayText
import com.example.ui.components.CarPlayTextField
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import kotlin.math.roundToInt

private enum class SettingsCategory(
    val titleRes: Int,
    val symbol: CarPlaySymbolType,
    val badgeColor: Color
) {
    GEOMETRY(R.string.settings_cat_geometry, CarPlaySymbolType.SLIDERS, Color(0xFF007AFF)),
    APPEARANCE(R.string.settings_cat_appearance, CarPlaySymbolType.MOON, Color(0xFF5856D6)),
    APPS(R.string.settings_cat_apps, CarPlaySymbolType.APPS_GRID, Color(0xFFFF9500)),
    NAVIGATION(R.string.settings_cat_navigation, CarPlaySymbolType.NAVIGATION_ARROW, Color(0xFF34C759)),
    SECURITY(R.string.settings_cat_security, CarPlaySymbolType.LOCK, Color(0xFF8E8E93))
}

@Composable
fun SettingsScreen(
    preferences: LauncherPreferences,
    allApps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    iconPacks: List<IconPackInfo>,
    onBack: () -> Unit,
    onIconSizeChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onCardAlphaChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onDockSideChange: (DockSide) -> Unit,
    onDayNightModeChange: (DayNightMode) -> Unit,
    onWallpaperPresetChange: (WallpaperPreset) -> Unit,
    onCustomWallpaperPicked: (Uri) -> Unit,
    onNightWarmthToggle: (Boolean) -> Unit,
    onResetDashboardPages: () -> Unit,
    onIconPackSelected: (String?) -> Unit,
    onUnhideApp: (String) -> Unit,
    onClearPinnedSlot: (Int) -> Unit,
    onNavProviderChange: (NavigationProvider) -> Unit,
    onHomeAddressChange: (String) -> Unit,
    onWorkAddressChange: (String) -> Unit,
    onSpeedGuardToggle: (Boolean) -> Unit,
    onSpeedLimitChange: (Int) -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    onHighwayFocusAutoToggle: (Boolean) -> Unit,
    onBreakReminderMinutesChange: (Int) -> Unit,
    onValetModeToggle: (Boolean) -> Unit,
    onValetPinChange: (String) -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onImportBackupUri: (Uri) -> Unit,
    onOpenDefaultLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GEOMETRY) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onCustomWallpaperPicked(uri)
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupUri(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackupUri(uri)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sidebarWidth = (maxWidth * 0.28f).coerceIn(200.dp, 265.dp)
        val pad = (maxWidth * 0.012f).coerceIn(8.dp, 14.dp)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            horizontalArrangement = Arrangement.spacedBy(pad)
        ) {
            // Left Apple iOS CarPlay Settings Sidebar
            CarPlayGlassCard(
                cornerRadius = preferences.cornerRadiusDp.dp,
                cardAlpha = (preferences.cardAlpha + 0.14f).coerceAtMost(0.96f),
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(tokens.pillBackground.copy(alpha = 0.72f))
                                .testTag("settings_back_button")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onBack
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CarPlaySymbol(
                                type = CarPlaySymbolType.BACK_CHEVRON,
                                tint = tokens.accentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        CarPlayText(
                            text = stringResource(R.string.settings_title),
                            style = type.headline,
                            color = tokens.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    SettingsCategory.entries.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) tokens.accentBlue else Color.Transparent)
                                .testTag("settings_cat_${cat.name.lowercase()}")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedCategory = cat }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.25f) else cat.badgeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                CarPlaySymbol(
                                    type = cat.symbol,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            CarPlayText(
                                text = stringResource(cat.titleRes),
                                style = type.controlLabel,
                                color = if (isSelected) Color.White else tokens.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Right iOS Grouped Inset Detail Pane
            CarPlayGlassCard(
                cornerRadius = preferences.cornerRadiusDp.dp,
                cardAlpha = (preferences.cardAlpha + 0.14f).coerceAtMost(0.96f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            slideInVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                initialOffsetY = { it / 16 }
                            )) togetherWith
                            (fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                slideOutVertically(
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                    targetOffsetY = { -it / 16 }
                                ))
                    },
                    label = "settings_category_transition"
                ) { activeCategory ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (activeCategory) {
                            SettingsCategory.GEOMETRY -> {
                                item { SettingsSectionHeader(stringResource(R.string.setting_live_preview)) }
                                item { LiveCarPlayGlassPreviewCard(preferences = preferences) }
                                item { SettingsSectionHeader(stringResource(R.string.settings_cat_geometry)) }
                                item {
                                    SettingsSliderRow(
                                        label = stringResource(R.string.setting_icon_size),
                                        valueText = "${preferences.iconSizeDp.roundToInt()} dp",
                                        value = preferences.iconSizeDp,
                                        range = 56f..92f,
                                        onValueChange = onIconSizeChange
                                    )
                                }
                                item {
                                    SettingsSliderRow(
                                        label = stringResource(R.string.setting_corner_radius),
                                        valueText = "${preferences.cornerRadiusDp.roundToInt()} dp",
                                        value = preferences.cornerRadiusDp,
                                        range = 12f..32f,
                                        onValueChange = onCornerRadiusChange
                                    )
                                }
                                item {
                                    SettingsSliderRow(
                                        label = stringResource(R.string.setting_card_alpha),
                                        valueText = "${(preferences.cardAlpha * 100).roundToInt()}%",
                                        value = preferences.cardAlpha,
                                        range = 0.35f..0.95f,
                                        onValueChange = onCardAlphaChange
                                    )
                                }
                                item {
                                    SettingsSliderRow(
                                        label = stringResource(R.string.setting_font_scale),
                                        valueText = "${(preferences.fontScale * 100).roundToInt()}%",
                                        value = preferences.fontScale,
                                        range = 0.85f..1.30f,
                                        onValueChange = onFontScaleChange
                                    )
                                }
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CarPlayText(
                                            text = stringResource(R.string.setting_dock_side),
                                            style = type.rowTitle,
                                            color = tokens.primaryText
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            CarPlaySegmentedChip(
                                                label = stringResource(R.string.dock_left),
                                                selected = preferences.dockSide == DockSide.LEFT,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onDockSideChange(DockSide.LEFT) }
                                            )
                                            CarPlaySegmentedChip(
                                                label = stringResource(R.string.dock_right),
                                                selected = preferences.dockSide == DockSide.RIGHT,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onDockSideChange(DockSide.RIGHT) }
                                            )
                                        }
                                    }
                                }
                                item {
                                    CarPlaySecondaryButton(
                                        onClick = onResetDashboardPages,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CarPlaySymbol(CarPlaySymbolType.RESTORE, tokens.accentBlue, Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        CarPlayText(stringResource(R.string.setting_reset_widgets), type.controlLabel, tokens.accentBlue)
                                    }
                                }
                            }

                            SettingsCategory.APPEARANCE -> {
                                item { SettingsSectionHeader(stringResource(R.string.setting_live_preview)) }
                                item { LiveCarPlayGlassPreviewCard(preferences = preferences) }
                                item { SettingsSectionHeader(stringResource(R.string.setting_day_night)) }
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CarPlaySegmentedChip(
                                            label = stringResource(R.string.mode_auto),
                                            selected = preferences.dayNightMode == DayNightMode.AUTO_CANBUS,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onDayNightModeChange(DayNightMode.AUTO_CANBUS) }
                                        )
                                        CarPlaySegmentedChip(
                                            label = stringResource(R.string.mode_day),
                                            selected = preferences.dayNightMode == DayNightMode.FORCE_DAY,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onDayNightModeChange(DayNightMode.FORCE_DAY) }
                                        )
                                        CarPlaySegmentedChip(
                                            label = stringResource(R.string.mode_night),
                                            selected = preferences.dayNightMode == DayNightMode.FORCE_NIGHT,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onDayNightModeChange(DayNightMode.FORCE_NIGHT) }
                                        )
                                    }
                                }
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(tokens.pillBackground.copy(alpha = 0.65f))
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            CarPlaySymbol(CarPlaySymbolType.MOON, tokens.warningOrange, Modifier.size(18.dp))
                                            CarPlayText(
                                                text = stringResource(R.string.setting_night_warmth),
                                                style = type.rowTitle,
                                                color = tokens.primaryText
                                            )
                                        }
                                        CarPlaySwitch(
                                            checked = preferences.nightWarmthEnabled,
                                            onCheckedChange = onNightWarmthToggle
                                        )
                                    }
                                }

                                item { SettingsSectionHeader(stringResource(R.string.setting_wallpapers)) }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        WallpaperPreviewTile(
                                            title = stringResource(R.string.wallpaper_ios18),
                                            previewRes = if (tokens.isNight) R.drawable.img_wallpaper_ios18_night else R.drawable.img_wallpaper_ios18_day,
                                            selected = preferences.wallpaperPreset == WallpaperPreset.IOS_18_DYNAMIC,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onWallpaperPresetChange(WallpaperPreset.IOS_18_DYNAMIC) }
                                        )
                                        WallpaperPreviewTile(
                                            title = stringResource(R.string.wallpaper_ios17),
                                            previewRes = if (tokens.isNight) R.drawable.img_wallpaper_ios17_night else R.drawable.img_wallpaper_ios17_day,
                                            selected = preferences.wallpaperPreset == WallpaperPreset.IOS_17_DYNAMIC,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onWallpaperPresetChange(WallpaperPreset.IOS_17_DYNAMIC) }
                                        )
                                        WallpaperPreviewTile(
                                            title = stringResource(R.string.wallpaper_obsidian),
                                            previewRes = null,
                                            selected = preferences.wallpaperPreset == WallpaperPreset.MIDNIGHT_OLED,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onWallpaperPresetChange(WallpaperPreset.MIDNIGHT_OLED) }
                                        )
                                    }
                                }
                                item {
                                    CarPlayButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CarPlaySymbol(CarPlaySymbolType.PHOTO, Color.White, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        CarPlayText(stringResource(R.string.action_pick_wallpaper), type.controlLabel, Color.White)
                                    }
                                }
                            }

                        SettingsCategory.APPS -> {
                            item { SettingsSectionHeader(stringResource(R.string.setting_icon_packs)) }
                            item {
                                CarPlaySegmentedChip(
                                    label = stringResource(R.string.icon_pack_system),
                                    selected = preferences.activeIconPackPackage == null,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onIconPackSelected(null) }
                                )
                            }
                            if (iconPacks.isEmpty()) {
                                item {
                                    CarPlayText(
                                        text = stringResource(R.string.icon_pack_none),
                                        style = type.bodySecondary,
                                        color = tokens.secondaryText
                                    )
                                }
                            } else {
                                items(iconPacks, key = { it.packageName }) { pack ->
                                    CarPlaySegmentedChip(
                                        label = pack.label,
                                        selected = preferences.activeIconPackPackage == pack.packageName,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { onIconPackSelected(pack.packageName) }
                                    )
                                }
                            }

                            item { SettingsSectionHeader(stringResource(R.string.setting_pinned_slots)) }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    preferences.pinnedDockPackages.forEachIndexed { idx, pkg ->
                                        val appLabel = allApps.firstOrNull { it.packageName == pkg }?.displayName
                                            ?: stringResource(R.string.action_clear_slot)
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(tokens.pillBackground)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    enabled = pkg != null
                                                ) { onClearPinnedSlot(idx) }
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            CarPlayText(
                                                text = "${idx + 1}: $appLabel",
                                                style = type.caption.copy(fontWeight = FontWeight.SemiBold),
                                                color = tokens.primaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            val hiddenApps = allApps.filter { it.isHidden }
                            if (hiddenApps.isNotEmpty()) {
                                item { SettingsSectionHeader(stringResource(R.string.setting_hidden_apps)) }
                                items(hiddenApps, key = { it.packageName }) { hiddenApp ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(tokens.pillBackground)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CarPlaySquircleIcon(
                                                app = hiddenApp,
                                                bitmap = icons[hiddenApp.packageName],
                                                size = 34.dp,
                                                elevation = 2.dp
                                            )
                                            CarPlayText(
                                                text = hiddenApp.displayName,
                                                style = type.body,
                                                color = tokens.primaryText
                                            )
                                        }
                                        CarPlaySecondaryButton(onClick = { onUnhideApp(hiddenApp.packageName) }) {
                                            CarPlaySymbol(CarPlaySymbolType.EYE_SHOW, tokens.accentBlue, Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            CarPlayText(stringResource(R.string.action_unhide), type.controlLabel, tokens.accentBlue)
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.NAVIGATION -> {
                            item { SettingsSectionHeader(stringResource(R.string.setting_nav_app)) }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NavigationProvider.entries.forEach { provider ->
                                        CarPlaySegmentedChip(
                                            label = provider.label,
                                            selected = preferences.navProvider == provider,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onNavProviderChange(provider) }
                                        )
                                    }
                                }
                            }
                            item {
                                var homeText by remember(preferences.homeAddress) {
                                    mutableStateOf(preferences.homeAddress)
                                }
                                var workText by remember(preferences.workAddress) {
                                    mutableStateOf(preferences.workAddress)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CarPlayText(stringResource(R.string.setting_home_address), type.caption, tokens.secondaryText)
                                    CarPlayTextField(
                                        value = homeText,
                                        onValueChange = {
                                            homeText = it
                                            onHomeAddressChange(it)
                                        },
                                        placeholder = stringResource(R.string.setting_home_address),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    CarPlayText(stringResource(R.string.setting_work_address), type.caption, tokens.secondaryText)
                                    CarPlayTextField(
                                        value = workText,
                                        onValueChange = {
                                            workText = it
                                            onWorkAddressChange(it)
                                        },
                                        placeholder = stringResource(R.string.setting_work_address),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            item { SettingsSectionHeader(stringResource(R.string.setting_speed_guard)) }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CarPlaySymbol(CarPlaySymbolType.SPEEDOMETER, tokens.warningOrange, Modifier.size(18.dp))
                                        CarPlayText(
                                            text = stringResource(R.string.setting_speed_guard),
                                            style = type.rowTitle,
                                            color = tokens.primaryText
                                        )
                                    }
                                    CarPlaySwitch(
                                        checked = preferences.speedGuardEnabled,
                                        onCheckedChange = onSpeedGuardToggle
                                    )
                                }
                            }
                            item {
                                SettingsSliderRow(
                                    label = stringResource(R.string.setting_speed_limit),
                                    valueText = "${preferences.speedLimitKmh} km/h",
                                    value = preferences.speedLimitKmh.toFloat(),
                                    range = 50f..180f,
                                    onValueChange = { onSpeedLimitChange(it.roundToInt()) }
                                )
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CarPlayText(
                                        text = stringResource(R.string.setting_speed_unit),
                                        style = type.rowTitle,
                                        color = tokens.primaryText
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CarPlaySegmentedChip(
                                            label = stringResource(R.string.stack_unit_kmh),
                                            selected = preferences.speedUnit == SpeedUnit.KMH,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onSpeedUnitChange(SpeedUnit.KMH) }
                                        )
                                        CarPlaySegmentedChip(
                                            label = stringResource(R.string.stack_unit_mph),
                                            selected = preferences.speedUnit == SpeedUnit.MPH,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onSpeedUnitChange(SpeedUnit.MPH) }
                                        )
                                    }
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(tokens.pillBackground.copy(alpha = 0.65f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CarPlaySymbol(CarPlaySymbolType.SPEEDOMETER, tokens.accentBlue, Modifier.size(18.dp))
                                        CarPlayText(
                                            text = stringResource(R.string.setting_highway_focus_auto),
                                            style = type.rowTitle,
                                            color = tokens.primaryText
                                        )
                                    }
                                    CarPlaySwitch(
                                        checked = preferences.highwayFocusAutoEnabled,
                                        onCheckedChange = onHighwayFocusAutoToggle
                                    )
                                }
                            }
                            item {
                                SettingsSliderRow(
                                    label = stringResource(R.string.setting_break_reminder),
                                    valueText = stringResource(R.string.setting_break_minutes, preferences.breakReminderMinutes),
                                    value = preferences.breakReminderMinutes.toFloat(),
                                    range = 30f..180f,
                                    onValueChange = { onBreakReminderMinutesChange(it.roundToInt()) }
                                )
                            }
                        }

                        SettingsCategory.SECURITY -> {
                            item { SettingsSectionHeader(stringResource(R.string.settings_cat_security)) }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CarPlayText(
                                        text = stringResource(R.string.setting_valet_mode),
                                        style = type.rowTitle,
                                        color = tokens.primaryText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CarPlaySwitch(
                                        checked = preferences.valetModeEnabled,
                                        onCheckedChange = onValetModeToggle
                                    )
                                }
                            }
                            item {
                                var pinDraft by remember(preferences.valetPin) {
                                    mutableStateOf(preferences.valetPin)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CarPlayText(stringResource(R.string.setting_valet_pin), type.caption, tokens.secondaryText)
                                    CarPlayTextField(
                                        value = pinDraft,
                                        onValueChange = { input ->
                                            val digits = input.filter { it.isDigit() }.take(4)
                                            pinDraft = digits
                                            if (digits.length == 4) onValetPinChange(digits)
                                        },
                                        placeholder = "1234",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            item { SettingsSectionHeader(stringResource(R.string.setting_backup_title)) }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CarPlayButton(
                                        onClick = { exportBackupLauncher.launch("cardeck_backup.json") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CarPlaySymbol(CarPlaySymbolType.BACKUP, Color.White, Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        CarPlayText(stringResource(R.string.action_export_backup), type.controlLabel, Color.White)
                                    }
                                    CarPlaySecondaryButton(
                                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CarPlaySymbol(CarPlaySymbolType.RESTORE, tokens.primaryText, Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        CarPlayText(stringResource(R.string.action_import_backup), type.controlLabel, tokens.primaryText)
                                    }
                                }
                            }
                            item {
                                CarPlaySecondaryButton(
                                    onClick = onOpenDefaultLauncher,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CarPlaySymbol(CarPlaySymbolType.HOME, tokens.accentBlue, Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    CarPlayText(stringResource(R.string.action_default_launcher), type.controlLabel, tokens.accentBlue)
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun LiveCarPlayGlassPreviewCard(preferences: LauncherPreferences) {
    val tokens = LocalCarTokens.current
    val previewRadius = (preferences.cornerRadiusDp * 0.55f).dp
    val previewCardAlpha = preferences.cardAlpha

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(16.dp))
    ) {
        when (preferences.wallpaperPreset) {
            WallpaperPreset.MIDNIGHT_OLED -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (tokens.isNight) Color.Black else Color(0xFFF2F2F7))
                )
            }
            WallpaperPreset.CUSTOM_IMAGE -> {
                if (!preferences.customWallpaperUri.isNullOrBlank()) {
                    AsyncImage(
                        model = preferences.customWallpaperUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(
                            if (tokens.isNight) R.drawable.img_wallpaper_ios18_night
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
                        if (tokens.isNight) R.drawable.img_wallpaper_ios17_night
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
                        if (tokens.isNight) R.drawable.img_wallpaper_ios18_night
                        else R.drawable.img_wallpaper_ios18_day
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (tokens.isNight && preferences.nightWarmthEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x18FF9500))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val dockPreview: @Composable () -> Unit = {
                Column(
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(tokens.dockBackground.copy(alpha = (previewCardAlpha * 0.9f).coerceIn(0.30f, 0.88f)))
                        .border(0.6.dp, tokens.cardBorder, RoundedCornerShape(9.dp))
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.5.dp)).background(Color(0xFF34C759)))
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.5.dp)).background(Color(0xFF007AFF)))
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.5.dp)).background(Color(0xFFFF2D55)))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(tokens.primaryText.copy(alpha = 0.7f)))
                }
            }

            if (preferences.dockSide == DockSide.LEFT) dockPreview()

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(previewRadius))
                        .background(tokens.cardBackground.copy(alpha = previewCardAlpha))
                        .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(previewRadius))
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(previewRadius))
                            .background(tokens.cardBackground.copy(alpha = previewCardAlpha))
                            .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(previewRadius))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(previewRadius))
                            .background(tokens.cardBackground.copy(alpha = previewCardAlpha))
                            .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(previewRadius))
                    )
                }
            }

            if (preferences.dockSide == DockSide.RIGHT) dockPreview()
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    CarPlayText(
        text = title,
        style = type.cardTitle,
        color = tokens.accentBlue
    )
}

@Composable
private fun SettingsSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.pillBackground.copy(alpha = 0.65f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CarPlayText(label, type.rowTitle, tokens.primaryText)
            CarPlayText(valueText, type.controlLabel, tokens.accentBlue)
        }
        Spacer(Modifier.height(4.dp))
        CarPlaySlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun WallpaperPreviewTile(
    title: String,
    previewRes: Int?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.5.dp else 0.75.dp,
                color = if (selected) tokens.accentBlue else tokens.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
        ) {
            if (previewRes != null) {
                Image(
                    painter = painterResource(previewRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        CarPlayText(
            text = title,
            style = type.caption,
            color = if (selected) tokens.accentBlue else tokens.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
