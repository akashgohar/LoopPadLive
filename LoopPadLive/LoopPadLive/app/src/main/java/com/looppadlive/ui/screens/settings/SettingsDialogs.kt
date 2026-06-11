package com.looppadlive.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looppadlive.data.model.GridLayout
import com.looppadlive.ui.theme.*

@Composable
fun GridLayoutDialog(
    currentLayout: GridLayout,
    currentCustomRows: Int,
    currentCustomCols: Int,
    onConfirm: (GridLayout, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLayout by remember { mutableStateOf(currentLayout) }
    var customRows by remember { mutableStateOf(currentCustomRows.toString()) }
    var customCols by remember { mutableStateOf(currentCustomCols.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Grid Layout",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Preset layouts
                GridLayout.entries.forEach { layout ->
                    if (layout == GridLayout.CUSTOM) return@forEach
                    GridLayoutOption(
                        layout = layout,
                        isSelected = selectedLayout == layout,
                        onClick = { selectedLayout = layout }
                    )
                }

                // Custom option
                GridLayoutOption(
                    layout = GridLayout.CUSTOM,
                    isSelected = selectedLayout == GridLayout.CUSTOM,
                    onClick = { selectedLayout = GridLayout.CUSTOM }
                )

                // Custom rows/cols inputs
                if (selectedLayout == GridLayout.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customRows,
                            onValueChange = { if (it.length <= 2) customRows = it.filter { c -> c.isDigit() } },
                            label = { Text("Rows") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customCols,
                            onValueChange = { if (it.length <= 2) customCols = it.filter { c -> c.isDigit() } },
                            label = { Text("Cols") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rows = customRows.toIntOrNull()?.coerceIn(1, 16) ?: 4
                    val cols = customCols.toIntOrNull()?.coerceIn(1, 16) ?: 4
                    onConfirm(selectedLayout, rows, cols)
                }
            ) { Text("Apply", color = SaffronOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GridLayoutOption(
    layout: GridLayout,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) SaffronOrange.copy(alpha = 0.1f) else SurfaceCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) SaffronOrange else SurfaceBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Mini grid preview
            if (layout != GridLayout.CUSTOM) {
                MiniGridPreview(rows = layout.rows, cols = layout.cols)
            } else {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GridOn, null, tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
            Column {
                Text(
                    layout.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) SaffronOrange else OnSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (layout != GridLayout.CUSTOM) {
                    Text(
                        "${layout.rows * layout.cols} pads",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                } else {
                    Text(
                        "Custom rows & columns",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null, tint = SaffronOrange)
        }
    }
}

@Composable
fun MiniGridPreview(rows: Int, cols: Int) {
    val previewRows = minOf(rows, 4)
    val previewCols = minOf(cols, 6)
    val dotSize = 5.dp
    val gap = 2.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(gap),
        modifier = Modifier.size(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val totalRows = if (previewRows > 0) previewRows else 1
        repeat(totalRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(previewCols) {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(RoundedCornerShape(1.dp))
                            .background(SaffronOrange.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

@Composable
fun NewProjectDialog(
    onConfirm: (String, Int, GridLayout) -> Unit,
    onDismiss: () -> Unit
) {
    var projectName by remember { mutableStateOf("New Project") }
    var bpmText by remember { mutableStateOf("120") }
    var selectedLayout by remember { mutableStateOf(GridLayout.GRID_4X4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { if (it.length <= 3) bpmText = it.filter { c -> c.isDigit() } },
                    label = { Text("BPM (40–250)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Grid Layout",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GridLayout.entries
                        .filter { it != GridLayout.CUSTOM }
                        .forEach { layout ->
                            FilterChip(
                                selected = selectedLayout == layout,
                                onClick = { selectedLayout = layout },
                                label = { Text(layout.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SaffronOrange.copy(alpha = 0.2f),
                                    selectedLabelColor = SaffronOrange
                                )
                            )
                        }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bpm = bpmText.toIntOrNull()?.coerceIn(40, 250) ?: 120
                    onConfirm(projectName, bpm, selectedLayout)
                }
            ) { Text("Create", color = SaffronOrange, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
