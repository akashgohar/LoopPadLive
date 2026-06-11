package com.looppadlive.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.looppadlive.ui.theme.*
import java.io.File

/**
 * Folder picker that handles both SAF (Storage Access Framework) for Android 11+
 * and direct file browsing for older Android versions.
 */
@Composable
fun FolderPickerDialog(
    onFolderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf(getDefaultStartPath()) }
    var folders by remember(currentPath) { mutableStateOf(listFolders(currentPath)) }
    var hasPermission by remember { mutableStateOf(checkStoragePermission(context)) }

    // SAF launcher (for Android 13+)
    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolved = resolveSafUri(context, uri)
            if (resolved != null) {
                onFolderSelected(resolved)
            } else {
                // Fallback: use content URI string
                onFolderSelected(uri.toString())
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms.values.any { it }
        if (hasPermission) {
            folders = listFolders(currentPath)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .background(BackgroundPanel)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "SELECT FOLDER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp, fontSize = 9.sp
                            ),
                            color = SaffronOrange
                        )
                        Text(
                            currentPath.take(40) + if (currentPath.length > 40) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = OnSurfaceVariant)
                    }
                }

                // Use SAF button (recommended)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElectricBlue.copy(alpha = 0.08f))
                        .clickable { safLauncher.launch(null) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Browse with System Picker", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
                        Text("Recommended – access any folder", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = ElectricBlue)
                }

                Divider(color = SurfaceBorder)

                // Permission check
                if (!hasPermission) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Storage permission needed to browse files", color = OnSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionLauncher.launch(perms)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                        ) { Text("Grant Permission") }
                    }
                } else {
                    // Navigation row
                    if (currentPath != getDefaultStartPath()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parent = File(currentPath).parentFile?.absolutePath
                                    if (parent != null) {
                                        currentPath = parent
                                        folders = listFolders(parent)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                            Text(".. (up)", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                        }
                    }

                    // File/folder list
                    if (folders.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No folders found", color = OnSurfaceMuted)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(folders) { entry ->
                                FolderEntry(
                                    entry = entry,
                                    onNavigate = {
                                        currentPath = entry.path
                                        folders = listFolders(entry.path)
                                    },
                                    onSelect = { onFolderSelected(entry.path) }
                                )
                            }
                        }
                    }

                    // Select current folder button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard)
                            .padding(12.dp)
                    ) {
                        Button(
                            onClick = { onFolderSelected(currentPath) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Select This Folder", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class FolderEntry(val name: String, val path: String, val hasAudio: Boolean)

@Composable
private fun FolderEntry(
    entry: FolderEntry,
    onNavigate: () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                if (entry.hasAudio) Icons.Default.MusicNote else Icons.Default.Folder,
                null,
                tint = if (entry.hasAudio) SaffronOrange else OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.hasAudio) OnSurface else OnSurfaceVariant
            )
        }
        if (entry.hasAudio) {
            TextButton(
                onClick = onSelect,
                colors = ButtonDefaults.textButtonColors(contentColor = SaffronOrange)
            ) { Text("Select", style = MaterialTheme.typography.labelMedium) }
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = OnSurfaceMuted)
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "ogg", "flac", "aac", "m4a")

private fun getDefaultStartPath(): String =
    Environment.getExternalStorageDirectory().absolutePath

private fun listFolders(path: String): List<FolderEntry> {
    return try {
        File(path).listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") }
            ?.sortedBy { it.name.lowercase() }
            ?.map { dir ->
                val hasAudio = dir.listFiles()?.any { f ->
                    f.isFile && f.extension.lowercase() in AUDIO_EXTENSIONS
                } == true
                FolderEntry(name = dir.name, path = dir.absolutePath, hasAudio = hasAudio)
            } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
    }
}

private fun resolveSafUri(context: Context, treeUri: Uri): String? {
    return try {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
        val parts = docId.split(":")
        if (parts.size >= 2 && parts[0] == "primary") {
            "${Environment.getExternalStorageDirectory()}/${parts[1]}"
        } else null
    } catch (e: Exception) { null }
}
