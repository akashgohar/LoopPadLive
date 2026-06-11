package com.looppadlive.ui.screens.mixer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.looppadlive.data.model.*
import com.looppadlive.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun MixerDialog(
    pad: Pad,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (Pad) -> Unit,
    onSolo: () -> Unit
) {
    var volume by remember(pad.id) { mutableStateOf(pad.volume) }
    var pan by remember(pad.id) { mutableStateOf(pad.pan) }
    var pitch by remember(pad.id) { mutableStateOf(pad.pitch) }
    var muted by remember(pad.id) { mutableStateOf(pad.muted) }
    var soloed by remember(pad.id) { mutableStateOf(pad.soloed) }
    var playbackType by remember(pad.id) { mutableStateOf(pad.playbackType) }
    var padMode by remember(pad.id) { mutableStateOf(pad.padMode) }
    var triggerGroup by remember(pad.id) { mutableStateOf(pad.triggerGroup) }
    var selectedColor by remember(pad.id) { mutableStateOf(pad.color) }
    var label by remember(pad.id) { mutableStateOf(pad.label) }

    fun applyChanges() {
        onUpdate(
            pad.copy(
                label = label,
                volume = volume, pan = pan, pitch = pitch,
                muted = muted, soloed = soloed,
                playbackType = playbackType,
                padMode = padMode,
                triggerGroup = triggerGroup,
                color = selectedColor
            )
        )
    }

    Dialog(
        onDismissRequest = {
            applyChanges()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(BackgroundPanel)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "PAD MIXER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp, letterSpacing = 2.sp
                            ),
                            color = SaffronOrange
                        )
                        if (pad.audioFileName.isNotEmpty()) {
                            Text(
                                pad.audioFileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Active indicator
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
                            }
                        }
                        IconButton(onClick = {
                            applyChanges()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, "Close", tint = OnSurfaceVariant)
                        }
                    }
                }

                Divider(color = SurfaceBorder)

                // Label
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(20) },
                    label = { Text("Pad Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Volume slider
                MixerSlider(
                    label = "Volume",
                    value = volume,
                    onValueChange = {
                        volume = it
                        onUpdate(pad.copy(volume = it, pan = pan))
                    },
                    valueRange = 0f..1f,
                    displayValue = "${(volume * 100).roundToInt()}%",
                    accentColor = SaffronOrange
                )

                // Pan slider
                MixerSlider(
                    label = "Pan",
                    value = pan,
                    onValueChange = {
                        pan = it
                        onUpdate(pad.copy(volume = volume, pan = it))
                    },
                    valueRange = -1f..1f,
                    displayValue = when {
                        pan < -0.05f -> "L${(-pan * 100).roundToInt()}"
                        pan > 0.05f -> "R${(pan * 100).roundToInt()}"
                        else -> "C"
                    },
                    accentColor = ElectricBlue
                )

                // Pitch slider
                MixerSlider(
                    label = "Pitch",
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = -12f..12f,
                    displayValue = "${if (pitch >= 0) "+" else ""}${pitch.roundToInt()} st",
                    accentColor = EmeraldGreen
                )

                Divider(color = SurfaceBorder)

                // Mute / Solo row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MixerToggleButton(
                        label = "MUTE",
                        isActive = muted,
                        activeColor = RubyRed,
                        onClick = { muted = !muted },
                        modifier = Modifier.weight(1f)
                    )
                    MixerToggleButton(
                        label = "SOLO",
                        isActive = soloed,
                        activeColor = GoldYellow,
                        onClick = {
                            soloed = !soloed
                            onSolo()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Divider(color = SurfaceBorder)

                // Playback Type
                MixerSection(title = "PLAYBACK TYPE") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlaybackType.entries.forEach { type ->
                            FilterChip(
                                selected = playbackType == type,
                                onClick = { playbackType = type },
                                label = { Text(type.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaffronOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = SaffronOrange
                                )
                            )
                        }
                    }
                }

                // Pad Mode
                MixerSection(title = "PAD MODE") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PadMode.entries.forEach { mode ->
                            val modeColor = when (mode) {
                                PadMode.LOOP -> EmeraldGreen
                                PadMode.SCENE -> ElectricBlue
                                PadMode.BREAK -> RubyRed
                            }
                            FilterChip(
                                selected = padMode == mode,
                                onClick = { padMode = mode },
                                label = { Text(mode.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = modeColor.copy(alpha = 0.2f),
                                    selectedLabelColor = modeColor
                                )
                            )
                        }
                    }
                }

                // Trigger Group
                MixerSection(title = "TRIGGER GROUP") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "None",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (triggerGroup == -1) SaffronOrange else OnSurfaceVariant
                        )
                        Slider(
                            value = (triggerGroup + 1).toFloat(),
                            onValueChange = { triggerGroup = it.roundToInt() - 1 },
                            valueRange = 0f..8f,
                            steps = 7,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = SaffronOrange, activeTrackColor = SaffronOrange)
                        )
                        Text(
                            if (triggerGroup == -1) "-" else "G${triggerGroup + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SaffronOrange,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Pad Color
                MixerSection(title = "PAD COLOR") {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PadColor.entries.size) { idx ->
                            val color = PadColor.entries[idx]
                            val parsedColor = Color(android.graphics.Color.parseColor(color.hex))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(parsedColor)
                                    .then(
                                        if (selectedColor == color)
                                            Modifier.background(Color.White.copy(alpha = 0.3f))
                                        else Modifier
                                    )
                                    .clickable { selectedColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        Icons.Default.Check, null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Apply button
                Button(
                    onClick = { applyChanges(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MixerSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = OnSurfaceVariant
            )
            Text(
                displayValue,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = SurfaceBorder
            )
        )
    }
}

@Composable
fun MixerToggleButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) activeColor.copy(alpha = 0.2f) else SurfaceCard,
            contentColor = if (isActive) activeColor else OnSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
            fontWeight = if (isActive) FontWeight.Black else FontWeight.Normal
        )
    }
}

@Composable
fun MixerSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp, letterSpacing = 2.sp
            ),
            color = OnSurfaceMuted
        )
        content()
    }
}
