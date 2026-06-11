package com.looppadlive.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.looppadlive.data.model.Quantization
import com.looppadlive.ui.theme.*

@Composable
fun TransportBar(
    projectName: String,
    bpm: Int,
    quantization: Quantization,
    isPlaying: Boolean,
    currentBeat: Int,
    onBpmChange: (Int) -> Unit,
    onQuantizationChange: (Quantization) -> Unit,
    onStopAll: () -> Unit,
    onMenuClick: () -> Unit,
    onProjectsClick: () -> Unit,
    onGridClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBpmDialog by remember { mutableStateOf(false) }
    var showQuantMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(BackgroundPanel)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ─── App Name / Project ───────────────────────────────────────────────
        Column(modifier = Modifier.width(110.dp)) {
            Text(
                text = "LOOPPAD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    letterSpacing = 2.sp
                ),
                color = SaffronOrange
            )
            Text(
                text = projectName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                maxLines = 1,
                modifier = Modifier.clickable { onProjectsClick() }
            )
        }

        Divider(
            modifier = Modifier.height(32.dp).width(1.dp),
            color = SurfaceBorder
        )

        // ─── Beat Clock ───────────────────────────────────────────────────────
        BeatClock(currentBeat = currentBeat, isPlaying = isPlaying)

        Divider(
            modifier = Modifier.height(32.dp).width(1.dp),
            color = SurfaceBorder
        )

        // ─── BPM Control ──────────────────────────────────────────────────────
        BpmControl(
            bpm = bpm,
            onMinus = { onBpmChange(bpm - 1) },
            onPlus = { onBpmChange(bpm + 1) },
            onLongTap = { showBpmDialog = true }
        )

        Divider(
            modifier = Modifier.height(32.dp).width(1.dp),
            color = SurfaceBorder
        )

        // ─── Quantization ─────────────────────────────────────────────────────
        Box {
            QuantizationChip(
                quantization = quantization,
                onClick = { showQuantMenu = true }
            )
            DropdownMenu(
                expanded = showQuantMenu,
                onDismissRequest = { showQuantMenu = false }
            ) {
                Quantization.entries.forEach { q ->
                    DropdownMenuItem(
                        text = { Text(q.displayName) },
                        onClick = {
                            onQuantizationChange(q)
                            showQuantMenu = false
                        },
                        leadingIcon = {
                            if (q == quantization) {
                                Icon(Icons.Default.Check, null, tint = SaffronOrange)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ─── Action Buttons ───────────────────────────────────────────────────
        TransportButton(
            icon = Icons.Default.Stop,
            label = "Stop",
            tint = RubyRed,
            onClick = onStopAll
        )
        TransportButton(
            icon = Icons.Default.GridView,
            label = "Grid",
            onClick = onGridClick
        )
        TransportButton(
            icon = Icons.Default.FolderOpen,
            label = "Import",
            onClick = onMenuClick
        )
        TransportButton(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = onSettingsClick
        )
    }

    // BPM dialog
    if (showBpmDialog) {
        BpmInputDialog(
            currentBpm = bpm,
            onConfirm = {
                onBpmChange(it)
                showBpmDialog = false
            },
            onDismiss = { showBpmDialog = false }
        )
    }
}

@Composable
fun BeatClock(currentBeat: Int, isPlaying: Boolean) {
    val beatsPerBar = 4
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(beatsPerBar) { beatIdx ->
            val isCurrentBeat = isPlaying && (currentBeat % beatsPerBar) == beatIdx
            val dotColor by animateColorAsState(
                targetValue = when {
                    isCurrentBeat && beatIdx == 0 -> SaffronOrange
                    isCurrentBeat -> EmeraldGreen
                    else -> SurfaceBorder
                },
                animationSpec = tween(80),
                label = "beat$beatIdx"
            )
            Box(
                modifier = Modifier
                    .size(if (isCurrentBeat) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun BpmControl(
    bpm: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onLongTap: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onMinus,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "BPM -1",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onLongTap() }
        ) {
            Text(
                text = "$bpm",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                ),
                color = SaffronOrange,
                textAlign = TextAlign.Center
            )
            Text(
                text = "BPM",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = OnSurfaceMuted,
                letterSpacing = 1.sp
            )
        }

        IconButton(
            onClick = onPlus,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "BPM +1",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun QuantizationChip(quantization: Quantization, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "QUANT",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                color = OnSurfaceMuted,
                letterSpacing = 1.sp
            )
            Text(
                text = quantization.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ElectricBlue
            )
        }
    }
}

@Composable
fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = OnSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BpmInputDialog(
    currentBpm: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var bpmText by remember { mutableStateOf(currentBpm.toString()) }
    val bpmValue = bpmText.toIntOrNull()?.coerceIn(40, 250)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set BPM") },
        text = {
            OutlinedTextField(
                value = bpmText,
                onValueChange = { if (it.length <= 3) bpmText = it.filter { c -> c.isDigit() } },
                label = { Text("BPM (40–250)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { bpmValue?.let { onConfirm(it) } },
                enabled = bpmValue != null
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
