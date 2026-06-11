package com.looppadlive.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looppadlive.data.model.GridLayout
import com.looppadlive.ui.components.*
import com.looppadlive.ui.screens.mixer.MixerDialog
import com.looppadlive.ui.screens.settings.*
import com.looppadlive.ui.theme.*
import com.looppadlive.utils.FolderPickerDialog
import com.looppadlive.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar messages
    LaunchedEffect(ui.snackbarMessage) {
        ui.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDeep
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Projects screen
                ui.showProjectsScreen -> {
                    ProjectsScreen(
                        projects = ui.projects,
                        currentProjectId = ui.currentProject?.id,
                        onProjectOpen = { viewModel.openProject(it) },
                        onProjectDelete = { viewModel.deleteProject(it) },
                        onNewProject = { viewModel.showNewProjectDialog() },
                        onImportFolder = { viewModel.showFolderPicker() },
                        onBack = { viewModel.hideProjects() }
                    )
                }

                // No project loaded → welcome state
                ui.currentProject == null && !ui.showProjectsScreen -> {
                    WelcomeScreen(
                        onNewProject = { viewModel.showNewProjectDialog() },
                        onImportFolder = { viewModel.showFolderPicker() },
                        onBrowseProjects = { viewModel.showProjects() }
                    )
                }

                // Main performance view
                else -> {
                    val project = ui.currentProject!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Transport bar
                        TransportBar(
                            projectName = project.name,
                            bpm = ui.bpm,
                            quantization = ui.quantization,
                            isPlaying = ui.isEngineRunning,
                            currentBeat = ui.currentBeat,
                            onBpmChange = { viewModel.setBpm(it) },
                            onQuantizationChange = { viewModel.setQuantization(it) },
                            onStopAll = { viewModel.stopAllPads() },
                            onMenuClick = { viewModel.showFolderPicker() },
                            onProjectsClick = { viewModel.showProjects() },
                            onGridClick = { viewModel.showGridDialog() },
                            onSettingsClick = { viewModel.showSettings() }
                        )

                        // Main grid area
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Scene lane (left sidebar)
                            SceneLane(
                                project = project,
                                activeSceneRow = ui.activeSceneRow,
                                onSceneTap = { row -> viewModel.onSceneTap(row) }
                            )

                            // Pad grid
                            PadGrid(
                                project = project,
                                activePadIds = ui.activePadIds,
                                queuedPadIds = ui.queuedPadIds,
                                activeSceneRow = ui.activeSceneRow,
                                onPadTap = { padId -> viewModel.onPadTap(padId) },
                                onPadLongPress = { padId -> viewModel.showMixer(padId) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ─── Overlays ─────────────────────────────────────────────────────

            // Folder picker
            if (ui.showFolderPicker) {
                FolderPickerDialog(
                    onFolderSelected = { path -> viewModel.importFolder(path) },
                    onDismiss = { viewModel.hideFolderPicker() }
                )
            }

            // New project dialog
            if (ui.showNewProjectDialog) {
                NewProjectDialog(
                    onConfirm = { name, bpm, layout ->
                        viewModel.createNewProject(name, bpm, layout)
                    },
                    onDismiss = { viewModel.hideNewProjectDialog() }
                )
            }

            // Grid layout dialog
            if (ui.showGridDialog) {
                val project = ui.currentProject
                GridLayoutDialog(
                    currentLayout = project?.gridLayout ?: GridLayout.GRID_4X4,
                    currentCustomRows = project?.customRows ?: 4,
                    currentCustomCols = project?.customCols ?: 4,
                    onConfirm = { layout, rows, cols ->
                        viewModel.updateGridLayout(layout, rows, cols)
                    },
                    onDismiss = { viewModel.hideGridDialog() }
                )
            }

            // Mixer dialog
            ui.showMixerForPadId?.let { padId ->
                val project = ui.currentProject
                val pad = project?.pads?.find { it.id == padId }
                if (pad != null) {
                    MixerDialog(
                        pad = pad,
                        isActive = ui.activePadIds.contains(padId),
                        onDismiss = { viewModel.hideMixer() },
                        onUpdate = { updated -> viewModel.updatePad(updated) },
                        onSolo = { viewModel.toggleSolo(padId) }
                    )
                }
            }

            // Loading overlay
            if (ui.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDeep.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SaffronOrange)
                        Spacer(Modifier.height(12.dp))
                        Text("Scanning folder…", color = OnSurfaceVariant)
                    }
                }
            }

            // Error snackbar
            ui.error?.let { error ->
                LaunchedEffect(error) {
                    snackbarHostState.showSnackbar(error)
                    viewModel.clearError()
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onNewProject: () -> Unit,
    onImportFolder: () -> Unit,
    onBrowseProjects: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo / Title area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "LOOP",
                    style = MaterialTheme.typography.displayMedium.copy(
                        letterSpacing = 8.dp.value.sp
                    ),
                    color = SaffronOrange
                )
                Text(
                    "PAD LIVE",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = 6.dp.value.sp
                    ),
                    color = OnSurface
                )
                Text(
                    "Indian Percussion · Tabla · Dholak · Live Performance",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted
                )
            }

            Divider(
                modifier = Modifier.width(200.dp),
                color = SurfaceBorder
            )

            // Action cards
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.width(340.dp)
            ) {
                WelcomeActionCard(
                    title = "Import Folder",
                    subtitle = "Load WAV, MP3, OGG samples from storage",
                    onClick = onImportFolder,
                    isPrimary = true
                )
                WelcomeActionCard(
                    title = "New Empty Project",
                    subtitle = "Start with a blank grid and assign pads",
                    onClick = onNewProject,
                    isPrimary = false
                )
                WelcomeActionCard(
                    title = "Open Projects",
                    subtitle = "Load a previously saved project",
                    onClick = onBrowseProjects,
                    isPrimary = false
                )
            }
        }
    }
}

@Composable
fun WelcomeActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isPrimary: Boolean
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = if (isPrimary) SaffronOrange.copy(alpha = 0.1f) else SurfaceCard,
        border = if (isPrimary)
            androidx.compose.foundation.BorderStroke(1.5.dp, SaffronOrange)
        else
            androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isPrimary) SaffronOrange else OnSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
