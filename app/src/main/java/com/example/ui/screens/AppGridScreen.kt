package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppEntry
import com.example.ui.theme.LocalCarTokens

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
    val pages = remember(apps) {
        if (apps.isEmpty()) listOf(emptyList()) else apps.chunked(8)
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.app_grid_empty),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val pageApps = pages[pageIndex]
                val topRow = pageApps.take(4)
                val bottomRow = pageApps.drop(4).take(4)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    AppGridRow(
                        rowApps = topRow,
                        icons = icons,
                        iconSizeDp = iconSizeDp,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        modifier = Modifier.weight(1f)
                    )
                    AppGridRow(
                        rowApps = bottomRow,
                        icons = icons,
                        iconSizeDp = iconSizeDp,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Authentic Apple CarPlay Page Dots (clean minimalist white dots)
        Row(
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
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
            onMoveLeft = {
                onMoveApp(appBeingEdited.packageName, -1)
            },
            onMoveRight = {
                onMoveApp(appBeingEdited.packageName, 1)
            },
            onOpenInSplit = {
                onDismissEdit()
                onOpenInSplit(appBeingEdited.packageName)
            }
        )
    }
}

@Composable
private fun AppGridRow(
    rowApps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    iconSizeDp: Float,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (col in 0 until 4) {
            val app = rowApps.getOrNull(col)
            if (app != null) {
                CarPlayAppIconTile(
                    app = app,
                    icon = icons[app.packageName],
                    iconSizeDp = iconSizeDp,
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
    iconSizeDp: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
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
            .minimumInteractiveComponentSize()
            .testTag("app_grid_item_${app.packageName}")
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSizeDp.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape((iconSizeDp * 0.225f).dp),
                    ambientColor = Color.Black.copy(alpha = 0.45f),
                    spotColor = Color.Black.copy(alpha = 0.45f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = app.displayName,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = app.displayName,
                    tint = tokens.secondaryText,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = app.displayName,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = (-0.15).sp,
                shadow = labelShadow
            ),
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
    var showRenameInput by remember { mutableStateOf(false) }
    var renameText by remember(app.packageName) { mutableStateOf(app.displayName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.cardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Text(
                    text = if (showRenameInput) stringResource(R.string.rename_dialog_title)
                    else app.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            if (showRenameInput) {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.rename_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContextChip(
                            icon = Icons.Rounded.Launch,
                            label = stringResource(R.string.app_action_open),
                            modifier = Modifier.weight(1f),
                            onClick = onOpen
                        )
                        if (app.virtualAction == null) {
                            ContextChip(
                                icon = Icons.Rounded.Splitscreen,
                                label = stringResource(R.string.app_action_split),
                                modifier = Modifier.weight(1f),
                                onClick = onOpenInSplit
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (slot in 0..2) {
                            ContextChip(
                                icon = Icons.Rounded.PushPin,
                                label = stringResource(R.string.app_action_pin_slot, slot + 1),
                                modifier = Modifier.weight(1f),
                                onClick = { onPinToSlot(slot) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContextChip(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            label = stringResource(R.string.app_action_move_left),
                            modifier = Modifier.weight(1f),
                            onClick = onMoveLeft
                        )
                        ContextChip(
                            icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            label = stringResource(R.string.app_action_move_right),
                            modifier = Modifier.weight(1f),
                            onClick = onMoveRight
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContextChip(
                            icon = Icons.Rounded.DriveFileRenameOutline,
                            label = stringResource(R.string.app_action_rename),
                            modifier = Modifier.weight(1f),
                            onClick = { showRenameInput = true }
                        )
                        ContextChip(
                            icon = Icons.Rounded.VisibilityOff,
                            label = stringResource(R.string.app_action_hide),
                            tint = tokens.alertRed,
                            modifier = Modifier.weight(1f),
                            onClick = onHide
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (showRenameInput) {
                Button(
                    onClick = { onRename(renameText) },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (showRenameInput) showRenameInput = false else onDismiss()
            }) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ContextChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val fg = tint ?: MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tokens.pillBackground)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
