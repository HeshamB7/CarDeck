package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppEntry
import com.example.model.DayNightMode
import com.example.model.DockSide
import com.example.model.IconPackInfo
import com.example.model.LauncherPreferences
import com.example.model.NavigationProvider
import com.example.model.SpeedUnit
import com.example.model.WallpaperPreset
import com.example.ui.theme.LocalCarTokens
import kotlin.math.roundToInt

private enum class SettingsCategory(
    val titleRes: Int,
    val icon: ImageVector,
    val badgeColor: Color
) {
    GEOMETRY(R.string.settings_cat_geometry, Icons.Rounded.Tune, Color(0xFF007AFF)),
    APPEARANCE(R.string.settings_cat_appearance, Icons.Rounded.DarkMode, Color(0xFF5856D6)),
    APPS(R.string.settings_cat_apps, Icons.Rounded.Apps, Color(0xFFFF9500)),
    NAVIGATION(R.string.settings_cat_navigation, Icons.Rounded.Navigation, Color(0xFF34C759)),
    SECURITY(R.string.settings_cat_security, Icons.Rounded.Lock, Color(0xFF8E8E93))
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
    onIconPackSelected: (String?) -> Unit,
    onUnhideApp: (String) -> Unit,
    onClearPinnedSlot: (Int) -> Unit,
    onNavProviderChange: (NavigationProvider) -> Unit,
    onHomeAddressChange: (String) -> Unit,
    onWorkAddressChange: (String) -> Unit,
    onSpeedGuardToggle: (Boolean) -> Unit,
    onSpeedLimitChange: (Int) -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    onValetModeToggle: (Boolean) -> Unit,
    onValetPinChange: (String) -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onImportBackupUri: (Uri) -> Unit,
    onOpenDefaultLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val tokens = LocalCarTokens.current
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

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Apple iOS Settings Sidebar with colorful rounded-square icon badges
        Column(
            modifier = Modifier
                .width(245.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(preferences.cornerRadiusDp.dp))
                .background(tokens.cardBackground.copy(alpha = (preferences.cardAlpha + 0.14f).coerceAtMost(0.96f)))
                .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(preferences.cornerRadiusDp.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = tokens.accentBlue
                    )
                }
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            SettingsCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) tokens.accentBlue else Color.Transparent)
                        .testTag("settings_cat_${cat.name.lowercase()}")
                        .clickable { selectedCategory = cat }
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
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = stringResource(cat.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right iOS Grouped Inset Settings Detail Pane
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(preferences.cornerRadiusDp.dp))
                .background(tokens.cardBackground.copy(alpha = (preferences.cardAlpha + 0.14f).coerceAtMost(0.96f)))
                .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(preferences.cornerRadiusDp.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedCategory) {
                SettingsCategory.GEOMETRY -> {
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
                            Text(
                                text = stringResource(R.string.setting_dock_side),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SegmentedChoiceChip(
                                    label = stringResource(R.string.dock_left),
                                    selected = preferences.dockSide == DockSide.LEFT,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDockSideChange(DockSide.LEFT) }
                                )
                                SegmentedChoiceChip(
                                    label = stringResource(R.string.dock_right),
                                    selected = preferences.dockSide == DockSide.RIGHT,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDockSideChange(DockSide.RIGHT) }
                                )
                            }
                        }
                    }
                }

                SettingsCategory.APPEARANCE -> {
                    item { SettingsSectionHeader(stringResource(R.string.setting_day_night)) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegmentedChoiceChip(
                                label = stringResource(R.string.mode_auto),
                                selected = preferences.dayNightMode == DayNightMode.AUTO_CANBUS,
                                modifier = Modifier.weight(1f),
                                onClick = { onDayNightModeChange(DayNightMode.AUTO_CANBUS) }
                            )
                            SegmentedChoiceChip(
                                label = stringResource(R.string.mode_day),
                                selected = preferences.dayNightMode == DayNightMode.FORCE_DAY,
                                modifier = Modifier.weight(1f),
                                onClick = { onDayNightModeChange(DayNightMode.FORCE_DAY) }
                            )
                            SegmentedChoiceChip(
                                label = stringResource(R.string.mode_night),
                                selected = preferences.dayNightMode == DayNightMode.FORCE_NIGHT,
                                modifier = Modifier.weight(1f),
                                onClick = { onDayNightModeChange(DayNightMode.FORCE_NIGHT) }
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
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_pick_wallpaper))
                        }
                    }
                }

                SettingsCategory.APPS -> {
                    item { SettingsSectionHeader(stringResource(R.string.setting_icon_packs)) }
                    item {
                        SegmentedChoiceChip(
                            label = stringResource(R.string.icon_pack_system),
                            selected = preferences.activeIconPackPackage == null,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onIconPackSelected(null) }
                        )
                    }
                    if (iconPacks.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.icon_pack_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.secondaryText
                            )
                        }
                    } else {
                        items(iconPacks, key = { it.packageName }) { pack ->
                            SegmentedChoiceChip(
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
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(tokens.pillBackground)
                                        .clickable(enabled = pkg != null) { onClearPinnedSlot(idx) }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}: $appLabel",
                                        style = MaterialTheme.typography.labelMedium,
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
                                    icons[hiddenApp.packageName]?.let { bmp ->
                                        Image(bmp, null, modifier = Modifier.size(34.dp))
                                    }
                                    Text(
                                        text = hiddenApp.displayName,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                OutlinedButton(onClick = { onUnhideApp(hiddenApp.packageName) }) {
                                    Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_unhide))
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
                                SegmentedChoiceChip(
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
                            OutlinedTextField(
                                value = homeText,
                                onValueChange = {
                                    homeText = it
                                    onHomeAddressChange(it)
                                },
                                label = { Text(stringResource(R.string.setting_home_address)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = workText,
                                onValueChange = {
                                    workText = it
                                    onWorkAddressChange(it)
                                },
                                label = { Text(stringResource(R.string.setting_work_address)) },
                                singleLine = true,
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
                                Icon(Icons.Rounded.Speed, null, tint = tokens.warningOrange)
                                Text(
                                    text = stringResource(R.string.setting_speed_guard),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Switch(
                                checked = preferences.speedGuardEnabled,
                                onCheckedChange = onSpeedGuardToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = tokens.positiveGreen
                                )
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
                            Text(
                                text = stringResource(R.string.setting_speed_unit),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SegmentedChoiceChip(
                                    label = stringResource(R.string.stack_unit_kmh),
                                    selected = preferences.speedUnit == SpeedUnit.KMH,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSpeedUnitChange(SpeedUnit.KMH) }
                                )
                                SegmentedChoiceChip(
                                    label = stringResource(R.string.stack_unit_mph),
                                    selected = preferences.speedUnit == SpeedUnit.MPH,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSpeedUnitChange(SpeedUnit.MPH) }
                                )
                            }
                        }
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
                            Text(
                                text = stringResource(R.string.setting_valet_mode),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = preferences.valetModeEnabled,
                                onCheckedChange = onValetModeToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = tokens.positiveGreen
                                )
                            )
                        }
                    }
                    item {
                        var pinDraft by remember(preferences.valetPin) {
                            mutableStateOf(preferences.valetPin)
                        }
                        OutlinedTextField(
                            value = pinDraft,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }.take(4)
                                pinDraft = digits
                                if (digits.length == 4) onValetPinChange(digits)
                            },
                            label = { Text(stringResource(R.string.setting_valet_pin)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item { SettingsSectionHeader(stringResource(R.string.setting_backup_title)) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { exportBackupLauncher.launch("cardeck_backup.json") },
                                colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Backup, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_export_backup))
                            }
                            OutlinedButton(
                                onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Restore, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_import_backup))
                            }
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = onOpenDefaultLauncher,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Home, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_default_launcher))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val tokens = LocalCarTokens.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = tokens.accentBlue)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = tokens.accentBlue
            )
        )
    }
}

@Composable
private fun SegmentedChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    Box(
        modifier = modifier
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) tokens.accentBlue else tokens.pillBackground)
            .border(
                width = 0.75.dp,
                color = if (selected) tokens.accentBlue else tokens.cardBorder,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.5.dp else 0.75.dp,
                color = if (selected) tokens.accentBlue else tokens.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
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
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) tokens.accentBlue else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
