package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.BitmapBlurUtils
import com.example.model.MediaPlaybackState
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun UniversalMediaCard(
    mediaState: MediaPlaybackState,
    cornerRadiusDp: Float,
    cardAlpha: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenMediaApp: () -> Unit,
    onGrantMediaAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val artBitmap = remember(mediaState.albumArt) { mediaState.albumArt?.asImageBitmap() }
    val frostedArt by produceState<ImageBitmap?>(initialValue = null, mediaState.albumArt) {
        value = mediaState.albumArt?.let { raw ->
            withContext(Dispatchers.Default) {
                BitmapBlurUtils.createFrostedBitmap(raw, targetWidth = 72, radius = 6).asImageBitmap()
            }
        }
    }

    var swipeDragOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = swipeDragOffset.coerceIn(-42f, 42f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "media_swipe_offset"
    )

    CarPlayGlassCard(
        cornerRadius = cornerRadiusDp.dp,
        cardAlpha = cardAlpha,
        modifier = modifier
            .pointerInput(mediaState.hasNotificationPermission) {
                if (!mediaState.hasNotificationPermission) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeDragOffset <= -56f) onNext()
                        else if (swipeDragOffset >= 56f) onPrevious()
                        swipeDragOffset = 0f
                    },
                    onDragCancel = { swipeDragOffset = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    swipeDragOffset += dragAmount * 0.45f
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = mediaState.hasActiveSession,
                onClick = onOpenMediaApp
            )
    ) {
        if (frostedArt != null) {
            Image(
                bitmap = frostedArt!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (tokens.isNight) 0.42f else 0.24f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                tokens.cardBackground.copy(alpha = 0.36f),
                                tokens.cardBackground.copy(alpha = 0.84f)
                            )
                        )
                    )
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = animatedOffset }
                .padding(14.dp)
        ) {
            val isCompact = maxHeight < 148.dp
            val artSize = (maxHeight * 0.38f).coerceIn(46.dp, 60.dp)

            if (!mediaState.hasNotificationPermission) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        CarPlayText(
                            text = stringResource(R.string.media_no_active),
                            style = type.rowTitle,
                            color = tokens.primaryText
                        )
                        CarPlayText(
                            text = stringResource(R.string.media_permission_needed),
                            style = type.bodySecondary,
                            color = tokens.secondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    CarPlayButton(
                        onClick = onGrantMediaAccess,
                        modifier = Modifier.testTag("media_grant_permission_button")
                    ) {
                        CarPlayText(
                            text = stringResource(R.string.media_enable_access),
                            style = type.controlLabel,
                            color = Color.White
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header: Album Art + Track / Artist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(artSize)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFFA5C72), Color(0xFFFA233B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (artBitmap != null) {
                                Image(
                                    bitmap = artBitmap,
                                    contentDescription = mediaState.album,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CarPlaySymbol(
                                    type = CarPlaySymbolType.MUSIC_NOTE,
                                    tint = Color.White,
                                    modifier = Modifier.size(artSize * 0.52f)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            CarPlayText(
                                text = mediaState.title.ifBlank { stringResource(R.string.media_no_active) },
                                style = type.rowTitle.copy(fontWeight = FontWeight.Bold),
                                color = tokens.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            CarPlayText(
                                text = mediaState.artist.ifBlank { stringResource(R.string.media_tap_to_play) },
                                style = type.bodySecondary,
                                color = tokens.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isCompact && mediaState.sourceAppName.isNotBlank()) {
                                CarPlayText(
                                    text = mediaState.sourceAppName,
                                    style = type.micro,
                                    color = tokens.alertRed,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Thin Apple Music Scrubber Bar
                    val progress = if (mediaState.durationMs > 0L) {
                        (mediaState.positionMs.toFloat() / mediaState.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        CarPlayProgressBar(progress = progress)
                        if (!isCompact && mediaState.durationMs > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                CarPlayText(
                                    text = formatDuration(mediaState.positionMs),
                                    style = type.micro,
                                    color = tokens.secondaryText
                                )
                                CarPlayText(
                                    text = formatDuration(mediaState.durationMs),
                                    style = type.micro,
                                    color = tokens.secondaryText
                                )
                            }
                        }
                    }

                    // Spacious CarPlay Transport Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MediaTransportButton(
                            symbol = CarPlaySymbolType.PREV_TRACK,
                            testTag = "media_prev_button",
                            isPrimary = false,
                            onClick = onPrevious
                        )
                        MediaTransportButton(
                            symbol = if (mediaState.isPlaying) CarPlaySymbolType.PAUSE else CarPlaySymbolType.PLAY,
                            testTag = "media_play_pause_button",
                            isPrimary = true,
                            onClick = onPlayPause
                        )
                        MediaTransportButton(
                            symbol = CarPlaySymbolType.NEXT_TRACK,
                            testTag = "media_next_button",
                            isPrimary = false,
                            onClick = onNext
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTransportButton(
    symbol: CarPlaySymbolType,
    testTag: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "media_btn_scale"
    )
    Box(
        modifier = Modifier
            .size(if (isPrimary) 46.dp else 42.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPrimary) tokens.pillBackground.copy(alpha = 0.85f) else Color.Transparent)
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CarPlaySymbol(
            type = symbol,
            tint = tokens.primaryText,
            modifier = Modifier.size(if (isPrimary) 24.dp else 22.dp)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
