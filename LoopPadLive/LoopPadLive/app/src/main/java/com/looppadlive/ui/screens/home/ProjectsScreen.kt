package com.looppadlive.ui.screens.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.looppadlive.data.model.Project
import com.looppadlive.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projects: List<Project>,
    currentProjectId: String?,
    onProjectOpen: (Project) -> Unit,
    onProjectDelete: (String) -> Unit,
    onNewProject: () -> Unit,
    onImportFolder: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "PROJECTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp, fontSize = 9.sp
                            ),
                            color = SaffronOrange
                        )
                        Text(
                            "${projects.size} project${if (projects.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = OnSurfaceVariant)
                    }
                },
                actions = {
                    TextButton(
                        onClick = onImportFolder,
                        colors = ButtonDefaults.textButtonColors(contentColor = ElectricBlue)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import Folder")
                    }
                    TextButton(
                        onClick = onNewProject,
                        colors = ButtonDefaults.textButtonColors(contentColor = SaffronOrange)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPanel
                )
            )
        },
        containerColor = BackgroundDeep
    ) { padding ->
        if (projects.isEmpty()) {
            EmptyProjectsState(
                onNewProject = onNewProject,
                onImportFolder = onImportFolder,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        isActive = project.id == currentProjectId,
                        onOpen = { onProjectOpen(project) },
                        onDelete = { onProjectDelete(project.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isActive: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) SaffronOrange.copy(alpha = 0.08f) else SurfaceCard)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) SaffronOrange else SurfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onOpen() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Grid icon with layout info
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GridView,
                        null,
                        tint = if (isActive) SaffronOrange else OnSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isActive) SaffronOrange else OnSurface,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProjectInfoChip(
                            text = "${project.bpm} BPM",
                            icon = Icons.Default.Speed
                        )
                        ProjectInfoChip(
                            text = project.gridLayout.displayName,
                            icon = Icons.Default.GridView
                        )
                        ProjectInfoChip(
                            text = "${project.pads.count { it.audioUri != null }} samples",
                            icon = Icons.Default.MusicNote
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(project.updatedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceMuted
                    )
                }
            }

            Row {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("OPEN", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen)
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        "Delete",
                        tint = OnSurfaceMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Project?") },
            text = { Text("\"${project.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = RubyRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProjectInfoChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = OnSurfaceMuted, modifier = Modifier.size(11.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
    }
}

@Composable
fun EmptyProjectsState(
    onNewProject: () -> Unit,
    onImportFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            null,
            tint = SaffronOrange.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No projects yet",
            style = MaterialTheme.typography.titleMedium,
            color = OnSurfaceVariant
        )
        Text(
            "Create a new project or import a folder of samples",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceMuted
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onImportFolder,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Import Folder")
            }
            Button(
                onClick = onNewProject,
                colors = ButtonDefaults.buttonColors(containerColor = SaffronOrange)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Project")
            }
        }
    }
}
