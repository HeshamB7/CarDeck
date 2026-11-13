package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppEntry
import com.example.ui.components.CarPlayButton
import com.example.ui.components.CarPlayModalOverlay
import com.example.ui.components.CarPlaySecondaryButton
import com.example.ui.components.CarPlaySquircleIcon
import com.example.ui.components.CarPlaySymbol
import com.example.ui.components.CarPlaySymbolType
import com.example.ui.components.CarPlayText
import com.example.ui.components.CarPlayTextField
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import kotlin.math.absoluteValue
import kotlin.math.min

@Composable
fun AppGridScreen(
    apps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    iconSizeDp: Float,
    appBeingEdited: AppEntry?,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onDismissEdit: () -> Unit,
    onPinToSlot: (Int, String) -> Unit,
    onRenameApp: (String, String) -> Unit,
    onHideApp: (String) -> Unit,
    onMoveApp: (String, Int) -> Unit,
    onOpenInSplit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Apple CarPlay uses 5x2 (10 apps/page) on standard landscape automotive displays,
        // gracefully adapting to 4x2 (8 apps/page) on compact screens.
        val columns = if (maxWidth >= 560.dp) 5 else 4
        val rows = 2
        val perPage = columns * rows

        val cellWidth = maxWidth / columns
        val cellHeight = (maxHeight - 28.dp) / rows
        val maxSafeIcon = min(cellWidth.value * 0.56f, cellHeight.value * 0.54f)
        val userRatio = (iconSizeDp / 68f).coerceIn(0.82f, 1.25f)
        val resolvedIconSize = (maxSafeIcon * userRatio).coerceIn(48f, 92f).dp

        val pages = remember(apps, perPage) {
            if (apps.isEmpty()) listOf(emptyList()) else apps.chunked(perPage)
        }
        val pagerState = rememberPagerState(pageCount = { pages.size })

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CarPlayText(
                        text = stringResource(R.string.app_grid_empty),
                        style = type.headline,
                        color = tokens.primaryText
                    )
                }
            } else {
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
                ) { pageIndex ->
                    val pageApps = pages[pageIndex]
                    val topRow = pageApps.take(columns)
                    val bottomRow = pageApps.drop(columns).take(columns)
                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                        .absoluteValue.coerceIn(0f, 1f)
                    val pageScale = 1f - (pageOffset * 0.045f)
                    val pageAlpha = 1f - (pageOffset * 0.20f)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pageScale
                                scaleY = pageScale
                                alpha = pageAlpha
                            }
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AppGridRow(
                            columns = columns,
                            rowApps = topRow,
                            icons = icons,
                            iconSize = resolvedIconSize,
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick,
                            modifier = Modifier.weight(1f)
                        )
                        AppGridRow(
                            columns = columns,
                            rowApps = bottomRow,
                            icons = icons,
                            iconSize = resolvedIconSize,
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Authentic Apple CarPlay Page Dots
            Row(
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val dotSize by animateFloatAsState(
                        targetValue = if (selected) 8f else 6.5f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "page_dot"
                    )
                    val dotColor = if (tokens.isNight) Color.White else Color(0xFF1C1C1E)
                    Box(
                        modifier = Modifier
                            .size(dotSize.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) dotColor
                                else dotColor.copy(alpha = 0.32f)
                            )
                    )
                }
            }
        }
    }

    if (appBeingEdited != null) {
        AppContextActionDialog(
            app = appBeingEdited,
            icon = icons[appBeingEdited.packageName],
            onDismiss = onDismissEdit,
            onOpen = {
                onDismissEdit()
                onAppClick(appBeingEdited)
            },
            onPinToSlot = { slot ->
                onPinToSlot(slot, appBeingEdited.packageName)
                onDismissEdit()
            },
            onRename = { newName ->
                onRenameApp(appBeingEdited.packageName, newName)
                onDismissEdit()
            },
            onHide = {
                onHideApp(appBeingEdited.packageName)
                onDismissEdit()
            },
            onMoveLeft = { onMoveApp(appBeingEdited.packageName, -1) },
            onMoveRight = { onMoveApp(appBeingEdited.packageName, 1) },
            onOpenInSplit = {
                onDismissEdit()
                onOpenInSplit(appBeingEdited.packageName)
            }
        )
    }
}

@Composable
private fun AppGridRow(
    columns: Int,
    rowApps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    iconSize: Dp,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (col in 0 until columns) {
            val app = rowApps.getOrNull(col)
            if (app != null) {
                CarPlayAppIconTile(
                    app = app,
                    icon = icons[app.packageName],
                    iconSize = iconSize,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarPlayAppIconTile(
    app: AppEntry,
    icon: ImageBitmap?,
    iconSize: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.89f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "app_icon_press"
    )

    val labelColor = if (tokens.isNight) Color.White else Color(0xFF111111)
    val labelShadow = if (tokens.isNight) {
        Shadow(Color.Black.copy(alpha = 0.65f), Offset(0f, 1.5f), 4f)
    } else {
        Shadow(Color.White.copy(alpha = 0.65f), Offset(0f, 1f), 3f)
    }

    Column(
        modifier = modifier
            .scale(scale)
            .testTag("app_grid_item_${app.packageName}")
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CarPlaySquircleIcon(
            app = app,
            bitmap = icon,
            size = iconSize,
            elevation = 8.dp
        )
        Spacer(Modifier.height(7.dp))
        CarPlayText(
            text = app.displayName,
            style = type.appLabel.copy(shadow = labelShadow),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppContextActionDialog(
    app: AppEntry,
    icon: ImageBitmap?,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onPinToSlot: (Int) -> Unit,
    onRename: (String) -> Unit,
    onHide: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onOpenInSplit: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    var showRenameInput by remember { mutableStateOf(false) }
    var renameText by remember(app.packageName) { mutableStateOf(app.displayName) }

    CarPlayModalOverlay(
        onDismiss = onDismiss,
        widthFraction = 0.56f
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CarPlaySquircleIcon(
                    app = app,
                    bitmap = icon,
                    size = 44.dp,
                    elevation = 4.dp
                )
                CarPlayText(
                    text = if (showRenameInput) stringResource(R.string.rename_dialog_title)
                    else app.displayName,
                    style = type.headline,
                    color = tokens.primaryText
                )
            }

            if (showRenameInput) {
                CarPlayTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = stringResource(R.string.rename_hint),
                    onImeAction = { onRename(renameText) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CarPlaySecondaryButton(
                        onClick = { showRenameInput = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        CarPlayText(stringResource(R.string.action_cancel), type.controlLabel, tokens.primaryText)
                    }
                    CarPlayButton(
                        onClick = { onRename(renameText) },
                        modifier = Modifier.weight(1f)
                    ) {
                        CarPlayText(stringResource(R.string.action_save), type.controlLabel, Color.White)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContextActionChip(
                            symbol = CarPlaySymbolType.OPEN_APP,
                            label = stringResource(R.string.app_action_open),
                            modifier = Modifier.weight(1f),
                            onClick = onOpen
                        )
                        if (app.virtualAction == null) {
                            ContextActionChip(
                                symbol = CarPlaySymbolType.SPLIT_SCREEN,
                                label = stringResource(R.string.app_action_split),
                                modifier = Modifier.weight(1f),
                                onClick = onOpenInSplit
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (slot in 0..2) {
                            ContextActionChip(
                                symbol = CarPlaySymbolType.PIN,
                                label = stringResource(R.string.app_action_pin_slot, slot + 1),
                                modifier = Modifier.weight(1f),
                                onClick = { onPinToSlot(slot) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContextActionChip(
                            symbol = CarPlaySymbolType.ARROW_LEFT,
                            label = stringResource(R.string.app_action_move_left),
                            modifier = Modifier.weight(1f),
                            onClick = onMoveLeft
                        )
                        ContextActionChip(
                            symbol = CarPlaySymbolType.ARROW_RIGHT,
                            label = stringResource(R.string.app_action_move_right),
                            modifier = Modifier.weight(1f),
                            onClick = onMoveRight
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContextActionChip(
                            symbol = CarPlaySymbolType.RENAME,
                            label = stringResource(R.string.app_action_rename),
                            modifier = Modifier.weight(1f),
                            onClick = { showRenameInput = true }
                        )
                        ContextActionChip(
                            symbol = CarPlaySymbolType.EYE_HIDE,
                            label = stringResource(R.string.app_action_hide),
                            tint = tokens.alertRed,
                            modifier = Modifier.weight(1f),
                            onClick = onHide
                        )
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
    }
}

@Composable
private fun ContextActionChip(
    symbol: CarPlaySymbolType,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val fg = tint ?: tokens.primaryText
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tokens.pillBackground.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CarPlaySymbol(symbol, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        CarPlayText(
            text = label,
            style = type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
