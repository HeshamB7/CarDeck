package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MediaPlaybackState
import com.example.ui.theme.CarDisplayFontFamily
import com.example.ui.theme.LocalCarTokens

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
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    val artBitmap = mediaState.albumArt?.asImageBitmap()

    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.cardBackground.copy(alpha = cardAlpha))
            .border(0.75.dp, tokens.cardBorder, shape)
            .clickable(enabled = mediaState.hasActiveSession, onClick = onOpenMediaApp)
    ) {
        if (artBitmap != null) {
            Image(
                bitmap = artBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) it.blur(28.dp) else it }
                    .alpha(if (tokens.isNight) 0.36f else 0.22f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                tokens.cardBackground.copy(alpha = 0.42f),
                                tokens.cardBackground.copy(alpha = 0.86f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (!mediaState.hasNotificationPermission) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.media_no_active),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.media_permission_needed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = onGrantMediaAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.accentBlue),
                        modifier = Modifier.testTag("media_grant_permission_button")
                    ) {
                        Text(stringResource(R.string.media_enable_access))
                    }
                }
            } else {
                // Apple CarPlay Now Playing Header: Square Album Art + Track/Artist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFC5C76), Color(0xFFFA233B))
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
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = mediaState.title.ifBlank { stringResource(R.string.media_no_active) },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mediaState.artist.ifBlank { stringResource(R.string.media_tap_to_play) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (mediaState.sourceAppName.isNotBlank()) {
                            Text(
                                text = mediaState.sourceAppName,
                                style = MaterialTheme.typography.labelSmall,
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

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = tokens.secondaryText.copy(alpha = 0.25f)
                    )
                    if (mediaState.durationMs > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(mediaState.positionMs),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = CarDisplayFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = tokens.secondaryText
                            )
                            Text(
                                text = formatDuration(mediaState.durationMs),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = CarDisplayFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = tokens.secondaryText
                            )
                        }
                    }
                }

                // Clean Apple CarPlay Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaTransportButton(
                        icon = Icons.Rounded.FastRewind,
                        contentDescription = stringResource(R.string.cd_media_prev),
                        testTag = "media_prev_button",
                        isPrimary = false,
                        onClick = onPrevious
                    )
                    MediaTransportButton(
                        icon = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_media_play_pause),
                        testTag = "media_play_pause_button",
                        isPrimary = true,
                        onClick = onPlayPause
                    )
                    MediaTransportButton(
                        icon = Icons.Rounded.FastForward,
                        contentDescription = stringResource(R.string.cd_media_next),
                        testTag = "media_next_button",
                        isPrimary = false,
                        onClick = onNext
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaTransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalCarTokens.current
    Box(
        modifier = Modifier
            .size(if (isPrimary) 52.dp else 48.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(if (isPrimary) tokens.pillBackground.copy(alpha = 0.85f) else Color.Transparent)
            .testTag(testTag)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(if (isPrimary) 30.dp else 26.dp)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
