package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import com.example.R
import com.example.model.CarWidgetType
import com.example.model.DashboardPageConfig
import com.example.model.DashboardWidgetItem
import com.example.model.LauncherPreferences
import com.example.model.MediaPlaybackState
import com.example.model.QuickContact
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.ui.components.AppleWidgetCornerRadius
import com.example.ui.components.CarPlayButton
import com.example.ui.components.CarPlaySymbol
import com.example.ui.components.CarPlaySymbolType
import com.example.ui.components.CarPlayText
import com.example.ui.components.CarPlayWidgetRenderer
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    preferences: LauncherPreferences,
    telemetry: TelemetryState,
    systemStatus: SystemStatusState,
    mediaState: MediaPlaybackState,
    isEditMode: Boolean,
    isHighwayFocusActive: Boolean,
    isBreakReminderDue: Boolean,
    onToggleEditMode: () -> Unit,
    onOpenAddWidget: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: (Int) -> Unit,
    onDeleteWidget: (Int, String) -> Unit,
    onMoveWidget: (Int, String, Int) -> Unit,
    onSwapWidgets: (Int, String, String) -> Unit,
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
    onToggleHighwayFocus: () -> Unit,
    onParkedPinClick: () -> Unit,
    onClearParkedPin: () -> Unit,
    onResetTripTimer: () -> Unit,
    onRefreshWeather: () -> Unit,
    onDialContact: (QuickContact) -> Unit,
    onEditQuickContact: (QuickContact) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val pages = preferences.dashboardPages
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()
    var selectedWidgetIdForSwap by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isEditMode, pagerState.currentPage) {
        selectedWidgetIdForSwap = null
    }

    LaunchedEffect(pages.size) {
        if (pagerState.currentPage >= pages.size && pages.isNotEmpty()) {
            pagerState.animateScrollToPage(pages.lastIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = { if (!isEditMode && !isHighwayFocusActive) onToggleEditMode() }
            )
    ) {
        val outerPad = (maxWidth * 0.012f).coerceIn(8.dp, 14.dp)
        val gap = (maxWidth * 0.013f).coerceIn(8.dp, 14.dp)
        val isUltraWide = maxWidth / maxHeight.coerceAtLeast(1.dp) >= 2.2f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = outerPad, end = outerPad, top = outerPad, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("dashboard_horizontal_pager")
            ) { pageIndex ->
                val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                    .absoluteValue.coerceIn(0f, 1f)
                val pageScale = 1f - (pageOffset * 0.035f)
                val pageAlpha = 1f - (pageOffset * 0.18f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pageScale
                            scaleY = pageScale
                            alpha = pageAlpha
                        }
                ) {
                    DashboardWidgetPage(
                        page = page,
                        pageIndex = pageIndex,
                        gap = gap,
                        isUltraWide = isUltraWide,
                        preferences = preferences,
                        telemetry = telemetry,
                        systemStatus = systemStatus,
                        mediaState = mediaState,
                        isEditMode = isEditMode,
                        isHighwayFocusActive = isHighwayFocusActive,
                        isBreakReminderDue = isBreakReminderDue,
                        selectedWidgetIdForSwap = selectedWidgetIdForSwap,
                        onTapWidgetInEdit = { widgetId ->
                            val cur = selectedWidgetIdForSwap
                            if (cur == null) {
                                selectedWidgetIdForSwap = widgetId
                            } else if (cur == widgetId) {
                                selectedWidgetIdForSwap = null
                            } else {
                                onSwapWidgets(pageIndex, cur, widgetId)
                                selectedWidgetIdForSwap = null
                            }
                        },
                        onDeleteWidget = { widgetId ->
                            if (selectedWidgetIdForSwap == widgetId) selectedWidgetIdForSwap = null
                            onDeleteWidget(pageIndex, widgetId)
                        },
                        onMoveWidget = { widgetId, delta -> onMoveWidget(pageIndex, widgetId, delta) },
                        onOpenAddWidget = { onOpenAddWidget(pageIndex) },
                        onSearchNavigate = onSearchNavigate,
                        onOpenFullMap = onOpenFullMap,
                        onMediaPlayPause = onMediaPlayPause,
                        onMediaNext = onMediaNext,
                        onMediaPrevious = onMediaPrevious,
                        onOpenMediaApp = onOpenMediaApp,
                        onGrantMediaAccess = onGrantMediaAccess,
                        onGrantLocation = onGrantLocation,
                        onGrantCalendar = onGrantCalendar,
                        onOpenBluetooth = onOpenBluetooth,
                        onOpenWifi = onOpenWifi,
                        onOpenHotspot = onOpenHotspot,
                        onOpenSplitApps = onOpenSplitApps,
                        onToggleHighwayFocus = onToggleHighwayFocus,
                        onParkedPinClick = onParkedPinClick,
                        onClearParkedPin = onClearParkedPin,
                        onResetTripTimer = onResetTripTimer,
                        onRefreshWeather = onRefreshWeather,
                        onDialContact = onDialContact,
                        onEditQuickContact = onEditQuickContact
                    )
                }
            }

            // Bottom Apple CarPlay Page Dots + Edit / Page Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Edit Controls (visible in Edit Mode)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(visible = isEditMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DashboardBarPill(
                                symbol = CarPlaySymbolType.PLUS,
                                label = stringResource(R.string.widget_add_button),
                                tint = tokens.positiveGreen,
                                testTag = "dashboard_add_widget_button",
                                onClick = { onOpenAddWidget(pagerState.currentPage) }
                            )
                            DashboardBarPill(
                                symbol = CarPlaySymbolType.PLUS,
                                label = stringResource(R.string.widget_add_page),
                                tint = tokens.accentBlue,
                                testTag = "dashboard_add_page_button",
                                onClick = {
                                    val newIndex = pages.size
                                    onAddPage()
                                    scope.launch { pagerState.animateScrollToPage(newIndex) }
                                }
                            )
                            if (pages.size > 1) {
                                DashboardBarPill(
                                    symbol = CarPlaySymbolType.TRASH,
                                    label = stringResource(R.string.widget_delete_page),
                                    tint = tokens.alertRed,
                                    testTag = "dashboard_delete_page_button",
                                    onClick = { onDeletePage(pagerState.currentPage) }
                                )
                            }
                        }
                    }
                }

                // Center: Authentic Apple CarPlay Page Indicator Dots
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tokens.cardBackground.copy(alpha = 0.65f))
                        .border(0.75.dp, tokens.cardBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { idx, _ ->
                        val selected = pagerState.currentPage == idx
                        val dotWidth by animateDpAsState(
                            targetValue = if (selected) 20.dp else 7.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "dashboard_dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (selected) tokens.primaryText
                                    else tokens.secondaryText.copy(alpha = 0.42f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                }
                        )
                    }
                }

                // Right: Highway Focus Pill or Customize / Done Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isHighwayFocusActive && !isEditMode) {
                        DashboardBarPill(
                            symbol = CarPlaySymbolType.SPEEDOMETER,
                            label = stringResource(R.string.focus_mode_label),
                            tint = tokens.accentBlue,
                            testTag = "dashboard_focus_active_pill",
                            onClick = onToggleHighwayFocus
                        )
                    }
                    if (isEditMode) {
                        CarPlayButton(
                            onClick = onToggleEditMode,
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("dashboard_edit_done_button")
                        ) {
                            CarPlaySymbol(CarPlaySymbolType.CHECK, Color.White, Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            CarPlayText(
                                text = stringResource(R.string.widget_done),
                                style = type.caption.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    } else if (!isHighwayFocusActive) {
                        DashboardBarPill(
                            symbol = CarPlaySymbolType.PENCIL,
                            label = stringResource(R.string.widget_edit_button),
                            tint = tokens.primaryText,
                            testTag = "dashboard_edit_mode_button",
                            onClick = onToggleEditMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardBarPill(
    symbol: CarPlaySymbolType,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Row(
        modifier = Modifier
            .height(34.dp)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(tokens.cardBackground.copy(alpha = 0.78f))
            .border(0.75.dp, tokens.cardBorder, CircleShape)
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        CarPlaySymbol(symbol, tint, Modifier.size(13.dp))
        CarPlayText(
            text = label,
            style = type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.primaryText,
            maxLines = 1
        )
    }
}

@Composable
private fun DashboardWidgetPage(
    page: DashboardPageConfig,
    pageIndex: Int,
    gap: Dp,
    isUltraWide: Boolean,
    preferences: LauncherPreferences,
    telemetry: TelemetryState,
    systemStatus: SystemStatusState,
    mediaState: MediaPlaybackState,
    isEditMode: Boolean,
    isHighwayFocusActive: Boolean,
    isBreakReminderDue: Boolean,
    selectedWidgetIdForSwap: String?,
    onTapWidgetInEdit: (String) -> Unit,
    onDeleteWidget: (String) -> Unit,
    onMoveWidget: (String, Int) -> Unit,
    onOpenAddWidget: () -> Unit,
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
    onToggleHighwayFocus: () -> Unit,
    onParkedPinClick: () -> Unit,
    onClearParkedPin: () -> Unit,
    onResetTripTimer: () -> Unit,
    onRefreshWeather: () -> Unit,
    onDialContact: (QuickContact) -> Unit,
    onEditQuickContact: (QuickContact) -> Unit
) {
    val widgets = page.widgets

    if (widgets.isEmpty()) {
        EmptyWidgetPagePlaceholder(
            pageNumber = pageIndex + 1,
            onAddWidget = onOpenAddWidget
        )
        return
    }

    val heroMapIndex = widgets.indexOfFirst { it.type == CarWidgetType.NAVIGATION_MAP }
    val useHeroSplitLayout = heroMapIndex >= 0 && widgets.size <= 4

    @Composable
    fun renderWidget(item: DashboardWidgetItem, idx: Int, itemModifier: Modifier) {
        CarPlayWidgetRenderer(
            item = item,
            preferences = preferences,
            telemetry = telemetry,
            systemStatus = systemStatus,
            mediaState = mediaState,
            isEditMode = isEditMode,
            isSelectedForSwap = selectedWidgetIdForSwap == item.id,
            isHighwayFocusActive = isHighwayFocusActive,
            isBreakReminderDue = isBreakReminderDue,
            canMoveEarlier = idx > 0,
            canMoveLater = idx < widgets.lastIndex,
            onDelete = { onDeleteWidget(item.id) },
            onMoveEarlier = { onMoveWidget(item.id, -1) },
            onMoveLater = { onMoveWidget(item.id, 1) },
            onTapInEditMode = { onTapWidgetInEdit(item.id) },
            onSearchNavigate = onSearchNavigate,
            onOpenFullMap = onOpenFullMap,
            onMediaPlayPause = onMediaPlayPause,
            onMediaNext = onMediaNext,
            onMediaPrevious = onMediaPrevious,
            onOpenMediaApp = onOpenMediaApp,
            onGrantMediaAccess = onGrantMediaAccess,
            onGrantLocation = onGrantLocation,
            onGrantCalendar = onGrantCalendar,
            onOpenBluetooth = onOpenBluetooth,
            onOpenWifi = onOpenWifi,
            onOpenHotspot = onOpenHotspot,
            onOpenSplitApps = onOpenSplitApps,
            onToggleHighwayFocus = onToggleHighwayFocus,
            onParkedPinClick = onParkedPinClick,
            onClearParkedPin = onClearParkedPin,
            onResetTripTimer = onResetTripTimer,
            onRefreshWeather = onRefreshWeather,
            onDialContact = onDialContact,
            onEditQuickContact = onEditQuickContact,
            modifier = itemModifier
        )
    }

    if (useHeroSplitLayout) {
        val heroItem = widgets[heroMapIndex]
        val secondaryWidgets = widgets.filterIndexed { i, _ -> i != heroMapIndex }
        val mapWeight = if (isUltraWide) 0.58f else 0.55f
        val rightWeight = 1f - mapWeight

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            renderWidget(
                item = heroItem,
                idx = heroMapIndex,
                itemModifier = Modifier
                    .weight(mapWeight)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(rightWeight)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                if (secondaryWidgets.isEmpty()) {
                    AddWidgetSlotTile(
                        onClick = onOpenAddWidget,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    secondaryWidgets.forEach { item ->
                        val originalIdx = widgets.indexOfFirst { it.id == item.id }
                        val itemWeight = when (item.type) {
                            CarWidgetType.TURN_SPEED -> 0.27f
                            CarWidgetType.MEDIA_PLAYER -> 0.42f
                            CarWidgetType.CALENDAR_AGENDA -> 0.31f
                            else -> 1f / secondaryWidgets.size
                        }
                        renderWidget(
                            item = item,
                            idx = originalIdx,
                            itemModifier = Modifier
                                .weight(itemWeight)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    } else {
        // Responsive 24dp Apple CarPlay Multi-Widget Grid (2 cols for <=4, 3 cols for 5..6+)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val showAddTile = isEditMode && widgets.size < 6
            val totalTiles = widgets.size + if (showAddTile) 1 else 0
            val cols = when {
                totalTiles <= 1 -> 1
                totalTiles == 2 -> 2
                totalTiles == 3 -> if (maxWidth >= 720.dp) 3 else 2
                totalTiles == 4 -> 2
                else -> if (maxWidth >= 660.dp) 3 else 2
            }
            val rows = (totalTiles + cols - 1) / cols

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        for (c in 0 until cols) {
                            val tileIdx = r * cols + c
                            when {
                                tileIdx < widgets.size -> {
                                    renderWidget(
                                        item = widgets[tileIdx],
                                        idx = tileIdx,
                                        itemModifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                                showAddTile && tileIdx == widgets.size -> {
                                    AddWidgetSlotTile(
                                        onClick = onOpenAddWidget,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                                else -> {
                                    Spacer(Modifier.weight(1f))
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
private fun EmptyWidgetPagePlaceholder(
    pageNumber: Int,
    onAddWidget: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(AppleWidgetCornerRadius))
            .background(tokens.cardBackground.copy(alpha = 0.50f))
            .border(1.5.dp, tokens.cardBorder, RoundedCornerShape(AppleWidgetCornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAddWidget
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(tokens.positiveGreen),
                contentAlignment = Alignment.Center
            ) {
                CarPlaySymbol(CarPlaySymbolType.PLUS, Color.White, Modifier.size(26.dp))
            }
            CarPlayText(
                text = stringResource(R.string.widget_empty_page_title, pageNumber),
                style = type.headline,
                color = tokens.primaryText
            )
            CarPlayText(
                text = stringResource(R.string.widget_empty_page_desc),
                style = type.bodySecondary,
                color = tokens.secondaryText
            )
        }
    }
}

@Composable
private fun AddWidgetSlotTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppleWidgetCornerRadius))
            .background(tokens.cardBackground.copy(alpha = 0.45f))
            .border(1.5.dp, tokens.accentBlue.copy(alpha = 0.55f), RoundedCornerShape(AppleWidgetCornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tokens.positiveGreen),
                contentAlignment = Alignment.Center
            ) {
                CarPlaySymbol(CarPlaySymbolType.PLUS, Color.White, Modifier.size(20.dp))
            }
            CarPlayText(
                text = stringResource(R.string.widget_add_button),
                style = type.controlLabel,
                color = tokens.primaryText
            )
        }
    }
}
