package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicText
import com.example.ui.theme.LocalCarPlayTypography
import com.example.ui.theme.LocalCarTokens
import kotlin.math.roundToInt

/**
 * Pure Apple CarPlay UI primitives (100% Material-Free).
 * Zero Android Material ripples, zero MaterialTheme, zero Material3 widgets.
 */
@Composable
fun CarPlayText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start
) {
    BasicText(
        text = text,
        style = style.copy(color = color, textAlign = textAlign),
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun CarPlayGlassCard(
    cornerRadius: Dp,
    cardAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalCarTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    val topSheen = if (tokens.isNight) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.28f)
    }
    Box(
        modifier = modifier
            .shadow(
                elevation = if (tokens.isNight) 10.dp else 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (tokens.isNight) 0.45f else 0.16f),
                spotColor = Color.Black.copy(alpha = if (tokens.isNight) 0.45f else 0.16f)
            )
            .clip(shape)
            .background(tokens.cardBackground.copy(alpha = cardAlpha))
            .background(
                Brush.verticalGradient(
                    listOf(topSheen, Color.Transparent, Color.Black.copy(alpha = if (tokens.isNight) 0.10f else 0.02f))
                )
            )
            .border(0.85.dp, tokens.cardBorder, shape),
        content = content
    )
}

@Composable
fun CarPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color = Color.White,
    cornerRadius: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit
) {
    val tokens = LocalCarTokens.current
    val bg = containerColor ?: tokens.accentBlue
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "carplay_btn_scale"
    )

    Row(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun CarPlaySecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit
) {
    val tokens = LocalCarTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "carplay_sec_btn_scale"
    )

    Row(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tokens.pillBackground.copy(alpha = if (enabled) 0.85f else 0.4f))
            .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun CarPlaySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) tokens.positiveGreen else tokens.pillBackground,
        label = "ios_switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "ios_switch_thumb"
    )

    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(0.5.dp, tokens.cardBorder, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun CarPlaySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    var trackWidthPx by remember { mutableIntStateOf(1) }
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .onSizeChanged { trackWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val frac = (offset.x / trackWidthPx.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + frac * span)
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val frac = (change.position.x / trackWidthPx.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + frac * span)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(tokens.secondaryText.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(tokens.accentBlue)
            )
        }
        val thumbSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { 24.dp.toPx() }
        val offsetX = ((trackWidthPx - thumbSizePx) * fraction).roundToInt().coerceAtLeast(0)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX, 0) }
                .size(24.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(0.5.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
        )
    }
}

@Composable
fun CarPlayProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(tokens.secondaryText.copy(alpha = 0.26f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(tokens.primaryText)
        )
    }
}

@Composable
fun CarPlayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingSymbol: CarPlaySymbolType? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current

    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tokens.pillBackground.copy(alpha = 0.82f))
            .border(0.75.dp, tokens.cardBorder, RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingSymbol != null) {
            CarPlaySymbol(
                type = leadingSymbol,
                tint = tokens.secondaryText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                CarPlayText(
                    text = placeholder,
                    style = type.body,
                    color = tokens.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = type.body.copy(color = tokens.primaryText),
                cursorBrush = SolidColor(tokens.accentBlue),
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onDone = { onImeAction() },
                    onSearch = { onImeAction() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CarPlayModalOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.62f,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalCarTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.scrimColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(24.dp)
        Box(
            modifier = modifier
                .fillMaxWidth(widthFraction)
                .shadow(22.dp, shape)
                .clip(shape)
                .background(tokens.cardBackground.copy(alpha = 0.96f))
                .border(0.75.dp, tokens.cardBorder, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
fun CarPlaySegmentedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCarTokens.current
    val type = LocalCarPlayTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "seg_scale"
    )
    Box(
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) tokens.accentBlue else tokens.pillBackground.copy(alpha = 0.8f))
            .border(
                width = 0.75.dp,
                color = if (selected) tokens.accentBlue else tokens.cardBorder,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        CarPlayText(
            text = label,
            style = type.controlLabel.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else tokens.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
