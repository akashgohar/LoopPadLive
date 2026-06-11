package com.looppadlive.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looppadlive.data.model.*
import com.looppadlive.ui.theme.*

@Composable
fun PadGrid(
    project: Project,
    activePadIds: Set<String>,
    queuedPadIds: Set<String>,
    activeSceneRow: Int,
    onPadTap: (String) -> Unit,
    onPadLongPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = project.effectiveRows
    val cols = project.effectiveCols
    val padsMap = project.pads.associateBy { "${it.row}_${it.col}" }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellWidth = maxWidth / cols
        val cellHeight = maxHeight / rows

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(rows) { rowIdx ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(cols) { colIdx ->
                        val pad = padsMap["${rowIdx}_$colIdx"]
                        if (pad != null) {
                            PadCell(
                                pad = pad,
                                isActive = activePadIds.contains(pad.id),
                                isQueued = queuedPadIds.contains(pad.id),
                                isSceneActive = activeSceneRow == rowIdx,
                                onTap = { onPadTap(pad.id) },
                                onLongPress = { onPadLongPress(pad.id) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Empty slot
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceCard.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PadCell(
    pad: Pad,
    isActive: Boolean,
    isQueued: Boolean,
    isSceneActive: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAudio = pad.audioUri != null

    // Determine pad base color from its assigned color
    val padBaseColor = Color(android.graphics.Color.parseColor(pad.color.hex))

    // Animate background color based on state
    val targetBgColor = when {
        pad.muted -> SurfaceCard
        isActive && pad.padMode == PadMode.BREAK -> RubyRed
        isActive && pad.padMode == PadMode.SCENE -> padBaseColor
        isActive -> padBaseColor
        isQueued -> GoldYellow.copy(alpha = 0.6f)
        isSceneActive && pad.padMode == PadMode.SCENE -> padBaseColor.copy(alpha = 0.3f)
        hasAudio -> SurfaceElevated
        else -> SurfaceCard
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 80),
        label = "padBg"
    )

    // Pulse animation when active
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = if (isActive && pad.playbackType == PlaybackType.LOOP) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Press animation
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else pulseScale,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "press"
    )

    // Border glow when active
    val borderColor = when {
        isActive && pad.padMode == PadMode.BREAK -> RubyRed
        isActive -> padBaseColor
        isQueued -> GoldYellow
        isSceneActive -> padBaseColor.copy(alpha = 0.5f)
        else -> SurfaceBorder
    }
    val borderWidth: Dp = if (isActive || isQueued) 2.dp else 1.dp

    Box(
        modifier = modifier
            .fillMaxHeight()
            .scale(pressScale)
            .clip(RoundedCornerShape(6.dp))
            .background(
                brush = if (isActive) {
                    Brush.verticalGradient(
                        listOf(animatedBg, animatedBg.copy(alpha = 0.7f))
                    )
                } else {
                    Brush.verticalGradient(listOf(animatedBg, animatedBg))
                }
            )
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .pointerInput(pad.id) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            // Mode indicator dot
            PadModeIndicator(pad.padMode, isActive)

            Spacer(modifier = Modifier.height(2.dp))

            // Label
            if (pad.label.isNotEmpty()) {
                Text(
                    text = pad.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isActive) Color.White else OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else if (!hasAudio) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceMuted
                )
            }

            // Queue indicator
            if (isQueued) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "▶",
                    fontSize = 8.sp,
                    color = GoldYellow
                )
            }
        }

        // Mute overlay
        if (pad.muted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.labelSmall,
                    color = RubyRed,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun PadModeIndicator(mode: PadMode, isActive: Boolean) {
    val color = when (mode) {
        PadMode.LOOP -> if (isActive) EmeraldGreen else OnSurfaceMuted
        PadMode.SCENE -> if (isActive) ElectricBlue else OnSurfaceMuted
        PadMode.BREAK -> if (isActive) RubyRed else OnSurfaceMuted
    }
    val symbol = when (mode) {
        PadMode.LOOP -> "↻"
        PadMode.SCENE -> "▶"
        PadMode.BREAK -> "■"
    }
    Text(
        text = symbol,
        fontSize = 8.sp,
        color = color
    )
}
