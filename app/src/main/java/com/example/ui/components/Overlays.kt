package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import com.example.model.CarWidgetType
import com.example.model.QuickContact
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppEntry
import com.example.model.SpeedUnit
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import com.example.ui.theme.OledDimmerRed
import com.example.ui.theme.OledDimmerRedMuted
import kotlin.math.roundToInt

/**
 * Pure OLED Black Night Dimmer Overlay with eye-safe deep red digital clock and speed.
 */
@Composable
fun OledNightDimmerOverlay(
    systemStatus: SystemStatusState,
    telemetry: TelemetryState,
    speedUnit: SpeedUnit,
    onWake: () -> Unit
) {
    val type = LocalCarPlayTypography.current
    val speedValue = if (speedUnit == SpeedUnit.MPH) {
        (telemetry.speedKmh * 0.621371f).roundToInt()
    } else {
        telemetry.speedKmh.roundToInt()
    }
    val unitLabel = if (speedUnit == SpeedUnit.MPH) {
        stringResource(R.string.stack_unit_mph)
    } else {
        stringResource(R.string.stack_unit_kmh)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("oled_night_dimmer_overlay")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onWake
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CarPlayText(
                text = systemStatus.timeText,
                style = type.displaySpeed.copy(fontSize = 84.sp, lineHeight = 88.sp),
                color = OledDimmerRed
            )
            if (telemetry.hasLocationPermission) {
                CarPlayText(
                    text = "$speedValue $unitLabel",
                    style = type.headline.copy(fontFeatureSettings = "tnum"),
                    color = OledDimmerRed
                )
            }
            Spacer(Modifier.height(14.dp))
            CarPlayText(
                text = stringResource(R.string.oled_tap_to_wake),
                style = type.bodySecondary,
                color = OledDimmerRedMuted
            )
        }
    }
}

/**
 * Valet / Guest Mode 4-digit PIN Lock Modal (pure Apple CarPlay frosted modal, zero Material).
 */
@Composable
fun ValetPinDialog(
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    CarPlayModalOverlay(
        onDismiss = onDismiss,
        widthFraction = 0.44f
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarPlaySymbol(CarPlaySymbolType.LOCK, tokens.warningOrange, Modifier.size(18.dp))
                CarPlayText(stringResource(R.string.valet_enter_pin), type.headline, tokens.primaryText)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { idx ->
                    val filled = idx < pin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isError -> tokens.alertRed
                                    filled -> tokens.accentBlue
                                    else -> tokens.pillBackground
                                }
                            )
                    )
                }
            }

            if (isError) {
                CarPlayText(
                    text = stringResource(R.string.valet_wrong_pin),
                    style = type.caption,
                    color = tokens.alertRed
                )
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "<")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(width = 74.dp, height = 46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.pillBackground)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        isError = false
                                        when (key) {
                                            "C" -> pin = ""
                                            "<" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            else -> {
                                                if (pin.length < 4) {
                                                    val next = pin + key
                                                    pin = next
                                                    if (next.length == 4) {
                                                        if (!onVerify(next)) {
                                                            isError = true
                                                            pin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "<") {
                                    CarPlaySymbol(CarPlaySymbolType.BACKSPACE, tokens.primaryText, Modifier.size(18.dp))
                                } else {
                                    CarPlayText(
                                        text = key,
                                        style = type.rowTitle.copy(fontFeatureSettings = "tnum"),
                                        color = tokens.primaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CarPlaySecondaryButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                CarPlayText(stringResource(R.string.action_cancel), type.controlLabel, tokens.primaryText)
            }
        }
    }
}

/**
 * Dual-App Split-Screen Picker Modal (pure Apple CarPlay frosted modal, zero Material).
 */
@Composable
fun SplitAppPickerDialog(
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    preselectedPrimary: String?,
    onLaunchSplit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val launchableApps = remember(apps) { apps.filter { it.virtualAction == null } }
    var primaryPkg by remember(preselectedPrimary, launchableApps) {
        mutableStateOf(preselectedPrimary ?: launchableApps.firstOrNull()?.packageName)
    }
    var secondaryPkg by remember(launchableApps) {
        mutableStateOf(launchableApps.getOrNull(1)?.packageName ?: launchableApps.firstOrNull()?.packageName)
    }

    CarPlayModalOverlay(
        onDismiss = onDismiss,
        widthFraction = 0.64f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarPlaySymbol(CarPlaySymbolType.SPLIT_SCREEN, tokens.accentBlue, Modifier.size(18.dp))
                CarPlayText(stringResource(R.string.split_title), type.headline, tokens.primaryText)
            }

            CarPlayText(
                text = stringResource(R.string.split_subtitle),
                style = type.bodySecondary,
                color = tokens.secondaryText
            )

            CarPlayText(stringResource(R.string.split_primary), type.controlLabel, tokens.primaryText)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(launchableApps, key = { "p_${it.packageName}" }) { app ->
                    SplitAppMiniChip(
                        app = app,
                        icon = icons[app.packageName],
                        selected = primaryPkg == app.packageName,
                        onClick = { primaryPkg = app.packageName }
                    )
                }
            }

            CarPlayText(stringResource(R.string.split_secondary), type.controlLabel, tokens.primaryText)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(launchableApps, key = { "s_${it.packageName}" }) { app ->
                    SplitAppMiniChip(
                        app = app,
                        icon = icons[app.packageName],
                        selected = secondaryPkg == app.packageName,
                        onClick = { secondaryPkg = app.packageName }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CarPlaySecondaryButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    CarPlayText(stringResource(R.string.action_cancel), type.controlLabel, tokens.primaryText)
                }
                CarPlayButton(
                    onClick = {
                        val p = primaryPkg
                        val s = secondaryPkg
                        if (p != null && s != null) onLaunchSplit(p, s)
                    },
                    enabled = primaryPkg != null && secondaryPkg != null,
                    modifier = Modifier.weight(1f)
                ) {
                    CarPlayText(stringResource(R.string.split_launch), type.controlLabel, Color.White)
                }
            }
        }
    }
}

@Composable
private fun SplitAppMiniChip(
    app: AppEntry,
    icon: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) tokens.accentBlue.copy(alpha = 0.25f) else tokens.pillBackground)
            .border(
                width = if (selected) 2.dp else 0.75.dp,
                color = if (selected) tokens.accentBlue else tokens.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        CarPlaySquircleIcon(
            app = app,
            bitmap = icon,
            size = 38.dp,
            elevation = 2.dp
        )
        CarPlayText(
            text = app.displayName,
            style = type.micro,
            color = tokens.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * First-Launch Welcome & Permissions Setup Overlay (pure Apple CarPlay frosted modal, zero Material).
 */
@Composable
fun OnboardingOverlay(
    hasLocationPermission: Boolean,
    hasMediaPermission: Boolean,
    onGrantLocation: () -> Unit,
    onGrantMedia: () -> Unit,
    onSetDefaultHome: () -> Unit,
    onFinish: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current

    CarPlayModalOverlay(
        onDismiss = {},
        widthFraction = 0.74f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CarPlayText(
                text = stringResource(R.string.onboarding_title),
                style = type.headline,
                color = tokens.primaryText
            )
            CarPlayText(
                text = stringResource(R.string.onboarding_subtitle),
                style = type.body,
                color = tokens.secondaryText
            )

            OnboardingPermissionRow(
                symbol = CarPlaySymbolType.LOCATION_PIN,
                title = stringResource(R.string.onboarding_location_title),
                desc = stringResource(R.string.onboarding_location_desc),
                granted = hasLocationPermission,
                actionLabel = stringResource(if (hasLocationPermission) R.string.action_granted else R.string.action_grant),
                onAction = onGrantLocation
            )

            OnboardingPermissionRow(
                symbol = CarPlaySymbolType.MUSIC_NOTE,
                title = stringResource(R.string.onboarding_media_title),
                desc = stringResource(R.string.onboarding_media_desc),
                granted = hasMediaPermission,
                actionLabel = stringResource(if (hasMediaPermission) R.string.action_granted else R.string.action_grant),
                onAction = onGrantMedia
            )

            OnboardingPermissionRow(
                symbol = CarPlaySymbolType.HOME,
                title = stringResource(R.string.onboarding_home_title),
                desc = stringResource(R.string.onboarding_home_desc),
                granted = false,
                actionLabel = stringResource(R.string.action_set_default),
                onAction = onSetDefaultHome
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CarPlayButton(
                    onClick = onFinish,
                    modifier = Modifier.testTag("onboarding_finish_button")
                ) {
                    CarPlayText(
                        text = stringResource(R.string.action_start_driving),
                        style = type.controlLabel,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPermissionRow(
    symbol: CarPlaySymbolType,
    title: String,
    desc: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.pillBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CarPlaySymbol(
            type = symbol,
            tint = if (granted) tokens.positiveGreen else tokens.accentBlue,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            CarPlayText(title, type.rowTitle, tokens.primaryText)
            CarPlayText(
                text = desc,
                style = type.bodySecondary,
                color = tokens.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (granted) {
            CarPlaySymbol(
                type = CarPlaySymbolType.CHECK_CIRCLE,
                tint = tokens.positiveGreen,
                modifier = Modifier.size(24.dp)
            )
        } else {
            CarPlaySecondaryButton(onClick = onAction) {
                CarPlayText(actionLabel, type.controlLabel, tokens.accentBlue)
            }
        }
    }
}

/**
 * Long-press Dock Slot Customization Modal: pick ANY installed app on the device to replace the slot.
 */
@Composable
fun DockSlotPickerDialog(
    slotIndex: Int,
    currentPinnedPkg: String?,
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onSelectApp: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps
        else apps.filter { it.displayName.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
    }

    CarPlayModalOverlay(
        onDismiss = onDismiss,
        widthFraction = 0.72f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CarPlayText(
                        text = stringResource(R.string.dock_picker_title, slotIndex + 1),
                        style = type.headline,
                        color = tokens.primaryText
                    )
                    CarPlayText(
                        text = stringResource(R.string.dock_picker_subtitle),
                        style = type.bodySecondary,
                        color = tokens.secondaryText
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentPinnedPkg != null) {
                        CarPlaySecondaryButton(onClick = { onSelectApp(null) }) {
                            CarPlayText(stringResource(R.string.action_clear_slot), type.caption.copy(fontWeight = FontWeight.SemiBold), tokens.accentBlue)
                        }
                    }
                    CarPlaySecondaryButton(onClick = onDismiss) {
                        CarPlayText(stringResource(R.string.action_cancel), type.caption.copy(fontWeight = FontWeight.SemiBold), tokens.primaryText)
                    }
                }
            }

            CarPlayTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.dock_picker_search),
                leadingSymbol = CarPlaySymbolType.SEARCH,
                modifier = Modifier.fillMaxWidth()
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 84.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 280.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isCurrent = currentPinnedPkg == app.packageName
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCurrent) tokens.accentBlue.copy(alpha = 0.22f) else tokens.pillBackground.copy(alpha = 0.55f))
                            .border(
                                width = if (isCurrent) 1.75.dp else 0.75.dp,
                                color = if (isCurrent) tokens.accentBlue else tokens.cardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectApp(app.packageName) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CarPlaySquircleIcon(
                            app = app,
                            bitmap = icons[app.packageName],
                            size = 50.dp,
                            elevation = 4.dp
                        )
                        CarPlayText(
                            text = app.displayName,
                            style = type.micro,
                            color = tokens.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apple CarPlay "Add Widgets" Gallery Modal matching the reference screenshot:
 * Top bar with Close pill + "Add Widgets", horizontal gallery of 24dp Apple widget cards with green + badges.
 */
@Composable
fun AddWidgetsGalleryDialog(
    onAddWidget: (CarWidgetType) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val catalog = remember {
        listOf(
            WidgetCatalogSpec(CarWidgetType.NAVIGATION_MAP, R.string.widget_nav_map, R.string.widget_nav_map_desc, CarPlaySymbolType.NAVIGATION_ARROW, Color(0xFF0A84FF)),
            WidgetCatalogSpec(CarWidgetType.MINI_MAP, R.string.widget_mini_map, R.string.widget_mini_map_desc, CarPlaySymbolType.LOCATION_PIN, Color(0xFF34C759)),
            WidgetCatalogSpec(CarWidgetType.MEDIA_PLAYER, R.string.widget_media, R.string.widget_media_desc, CarPlaySymbolType.MUSIC_NOTE, Color(0xFFFF2D55)),
            WidgetCatalogSpec(CarWidgetType.WEATHER, R.string.widget_weather, R.string.widget_weather_desc, CarPlaySymbolType.SUN, Color(0xFF388BFD)),
            WidgetCatalogSpec(CarWidgetType.ANALOG_CLOCK, R.string.widget_analog_clock, R.string.widget_analog_clock_desc, CarPlaySymbolType.CLOCK, Color(0xFF1C1C1E)),
            WidgetCatalogSpec(CarWidgetType.DIGITAL_CLOCK, R.string.widget_digital_clock, R.string.widget_digital_clock_desc, CarPlaySymbolType.CLOCK, Color(0xFF5E5CE6)),
            WidgetCatalogSpec(CarWidgetType.SPEED_CONTACTS, R.string.widget_speed_contacts, R.string.widget_speed_contacts_desc, CarPlaySymbolType.PHONE, Color(0xFF30D158)),
            WidgetCatalogSpec(CarWidgetType.VEHICLE_BATTERY, R.string.widget_vehicle_battery, R.string.widget_vehicle_battery_desc, CarPlaySymbolType.BATTERY, Color(0xFF34C759)),
            WidgetCatalogSpec(CarWidgetType.TURN_SPEED, R.string.widget_turn_speed, R.string.widget_turn_speed_desc, CarPlaySymbolType.SPEEDOMETER, Color(0xFFFF9500)),
            WidgetCatalogSpec(CarWidgetType.CALENDAR_AGENDA, R.string.widget_calendar, R.string.widget_calendar_desc, CarPlaySymbolType.CALENDAR, Color(0xFFFF3B30)),
            WidgetCatalogSpec(CarWidgetType.QUICK_CONTROLS, R.string.widget_quick_controls, R.string.widget_quick_controls_desc, CarPlaySymbolType.SLIDERS, Color(0xFF007AFF)),
            WidgetCatalogSpec(CarWidgetType.TRIP_ODOMETER, R.string.widget_trip_odometer, R.string.widget_trip_odometer_desc, CarPlaySymbolType.COMPASS, Color(0xFFFF9F0A))
        )
    }

    CarPlayModalOverlay(
        onDismiss = onDismiss,
        widthFraction = 0.84f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tokens.pillBackground)
                        .border(0.75.dp, tokens.cardBorder, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CarPlaySymbol(CarPlaySymbolType.ARROW_LEFT, tokens.primaryText, Modifier.size(13.dp))
                    CarPlayText(stringResource(R.string.action_close), type.controlLabel, tokens.primaryText)
                }

                CarPlayText(
                    text = stringResource(R.string.widget_gallery_title),
                    style = type.headline,
                    color = tokens.primaryText
                )

                Spacer(Modifier.width(72.dp))
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(catalog, key = { it.type.name }) { spec ->
                    Box(
                        modifier = Modifier
                            .width(178.dp)
                            .padding(top = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(182.dp)
                                .shadow(6.dp, RoundedCornerShape(AppleWidgetCornerRadius))
                                .clip(RoundedCornerShape(AppleWidgetCornerRadius))
                                .background(tokens.pillBackground.copy(alpha = 0.88f))
                                .border(0.85.dp, tokens.cardBorder, RoundedCornerShape(AppleWidgetCornerRadius))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onAddWidget(spec.type) }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(spec.accentColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CarPlaySymbol(spec.symbol, spec.accentColor, Modifier.size(24.dp))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CarPlayText(
                                    text = stringResource(spec.titleRes),
                                    style = type.rowTitle,
                                    color = tokens.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CarPlayText(
                                    text = stringResource(spec.descRes),
                                    style = type.caption,
                                    color = tokens.secondaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Green Apple CarPlay (+) circular badge at top-left
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(28.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(tokens.positiveGreen)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onAddWidget(spec.type) },
                            contentAlignment = Alignment.Center
                        ) {
                            CarPlaySymbol(CarPlaySymbolType.PLUS, Color.White, Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

private data class WidgetCatalogSpec(
    val type: CarWidgetType,
    val titleRes: Int,
    val descRes: Int,
    val symbol: CarPlaySymbolType,
    val accentColor: Color
)

@Composable
fun QuickContactEditDialog(
    contact: QuickContact,
    onSave: (QuickContact) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    var name by remember(contact.id) { mutableStateOf(contact.name) }
    var phone by remember(contact.id) { mutableStateOf(contact.phoneNumber) }

    CarPlayModalOverlay(
        onDismiss = onDismiss,
        widthFraction = 0.52f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarPlaySymbol(CarPlaySymbolType.PHONE, tokens.positiveGreen, Modifier.size(18.dp))
                CarPlayText(stringResource(R.string.contacts_edit_title), type.headline, tokens.primaryText)
            }

            CarPlayTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.contacts_name_hint),
                leadingSymbol = CarPlaySymbolType.PENCIL,
                modifier = Modifier.fillMaxWidth()
            )

            CarPlayTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = stringResource(R.string.contacts_phone_hint),
                leadingSymbol = CarPlaySymbolType.PHONE,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CarPlaySecondaryButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    CarPlayText(stringResource(R.string.action_cancel), type.controlLabel, tokens.primaryText)
                }
                CarPlayButton(
                    onClick = {
                        onSave(
                            contact.copy(
                                name = name.trim().ifBlank { contact.name },
                                phoneNumber = phone.trim()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    CarPlayText(stringResource(R.string.action_save), type.controlLabel, Color.White)
                }
            }
        }
    }
}
