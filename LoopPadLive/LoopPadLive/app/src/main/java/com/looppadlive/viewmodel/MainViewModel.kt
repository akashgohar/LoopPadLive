package com.looppadlive.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.looppadlive.audio.AudioEngine
import com.looppadlive.audio.AudioPlaybackService
import com.looppadlive.data.model.*
import com.looppadlive.data.repository.FolderScanner
import com.looppadlive.data.repository.ProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class UiState(
    val currentProject: Project? = null,
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showFolderPicker: Boolean = false,
    val showNewProjectDialog: Boolean = false,
    val showGridDialog: Boolean = false,
    val showMixerForPadId: String? = null,
    val showSettingsScreen: Boolean = false,
    val showProjectsScreen: Boolean = false,
    val selectedPadForEdit: String? = null,
    val bpm: Int = 120,
    val isEngineRunning: Boolean = false,
    val activePadIds: Set<String> = emptySet(),
    val queuedPadIds: Set<String> = emptySet(),
    val activeSceneRow: Int = -1,
    val currentBeat: Int = 0,
    val quantization: Quantization = Quantization.ONE_BAR,
    val snackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ProjectRepository(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Audio service binding
    private var audioService: AudioPlaybackService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? AudioPlaybackService.LocalBinder ?: return
            audioService = localBinder.getService()
            serviceBound = true

            // Start engine with current BPM
            audioService?.audioEngine?.start(_uiState.value.bpm)

            // Observe engine state
            viewModelScope.launch {
                audioService?.audioEngine?.engineState?.collect { engineState ->
                    _uiState.update { ui ->
                        ui.copy(
                            isEngineRunning = engineState.isPlaying,
                            activePadIds = engineState.activePadIds,
                            activeSceneRow = engineState.activeSceneRow,
                            currentBeat = engineState.currentBeat,
                            bpm = engineState.bpm
                        )
                    }
                }
            }
            Log.d("MainViewModel", "AudioService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            serviceBound = false
        }
    }

    init {
        bindAudioService()
        loadProjects()
    }

    private fun bindAudioService() {
        val app = getApplication<Application>()
        val intent = Intent(app, AudioPlaybackService::class.java)
        app.startService(intent)
        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun loadProjects() {
        viewModelScope.launch {
            repo.loadAllProjects()
            repo.projects.collect { projects ->
                _uiState.update { it.copy(projects = projects) }
            }
        }
    }

    // ─── Project Management ───────────────────────────────────────────────────

    fun openProject(project: Project) {
        audioService?.audioEngine?.stopAllPads()
        _uiState.update {
            it.copy(
                currentProject = project,
                bpm = project.bpm,
                quantization = project.quantization,
                showProjectsScreen = false
            )
        }
        audioService?.audioEngine?.setBpm(project.bpm)
        audioService?.audioEngine?.setQuantization(project.quantization)
    }

    fun saveCurrentProject() {
        val project = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            val updated = project.copy(updatedAt = System.currentTimeMillis())
            repo.saveProject(updated)
            _uiState.update { it.copy(snackbarMessage = "Project saved") }
        }
    }

    fun createNewProject(name: String, bpm: Int, layout: GridLayout) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val project = repo.createNewProject(name, bpm, layout)
            _uiState.update {
                it.copy(
                    currentProject = project,
                    bpm = bpm,
                    isLoading = false,
                    showNewProjectDialog = false
                )
            }
            audioService?.audioEngine?.setBpm(bpm)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repo.deleteProject(projectId)
            if (_uiState.value.currentProject?.id == projectId) {
                _uiState.update { it.copy(currentProject = null) }
            }
        }
    }

    fun exportProject() {
        val project = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            val json = repo.exportProjectJson(project)
            _uiState.update { it.copy(snackbarMessage = "Exported ${project.name}.json") }
            Log.d("Export", json)
        }
    }

    // ─── Folder Import ────────────────────────────────────────────────────────

    fun importFolder(folderPath: String, layout: GridLayout = GridLayout.GRID_4X4) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showFolderPicker = false) }
            try {
                val scanned = FolderScanner.scanFolder(folderPath)
                if (scanned.audioFiles.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No audio files found in folder"
                        )
                    }
                    return@launch
                }
                val project = FolderScanner.toProject(scanned, layout)
                repo.saveProject(project)
                openProject(project)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = "Loaded ${scanned.audioFiles.size} samples"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Import failed: ${e.message}")
                }
            }
        }
    }

    // ─── Pad Triggers ─────────────────────────────────────────────────────────

    fun onPadTap(padId: String) {
        val project = _uiState.value.currentProject ?: return
        val pad = project.pads.find { it.id == padId } ?: return
        val engine = audioService?.audioEngine ?: return

        when (pad.padMode) {
            PadMode.SCENE -> engine.triggerScene(pad.row, project.pads)
            PadMode.BREAK -> engine.triggerPad(pad, project.pads)
            PadMode.LOOP -> engine.triggerPad(pad, project.pads)
        }
    }

    fun onSceneTap(rowIndex: Int) {
        val project = _uiState.value.currentProject ?: return
        val engine = audioService?.audioEngine ?: return
        engine.triggerScene(rowIndex, project.pads)
    }

    fun stopAllPads() {
        audioService?.audioEngine?.stopAllPads()
    }

    // ─── BPM ──────────────────────────────────────────────────────────────────

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(40, 250)
        _uiState.update { it.copy(bpm = clamped) }
        audioService?.audioEngine?.setBpm(clamped)
        _uiState.value.currentProject?.let { project ->
            val updated = project.copy(bpm = clamped)
            _uiState.update { it.copy(currentProject = updated) }
        }
    }

    fun setQuantization(q: Quantization) {
        _uiState.update { it.copy(quantization = q) }
        audioService?.audioEngine?.setQuantization(q)
        _uiState.value.currentProject?.let { project ->
            val updated = project.copy(quantization = q)
            _uiState.update { it.copy(currentProject = updated) }
        }
    }

    // ─── Pad Editing ──────────────────────────────────────────────────────────

    fun updatePad(updatedPad: Pad) {
        val project = _uiState.value.currentProject ?: return
        val newPads = project.pads.map { if (it.id == updatedPad.id) updatedPad else it }
        val updated = project.copy(pads = newPads, updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(currentProject = updated) }
        // Apply volume/pan changes live
        audioService?.audioEngine?.updatePadVolume(updatedPad.id, updatedPad.volume, updatedPad.pan)
    }

    fun updatePadMixer(padId: String, volume: Float, pan: Float, pitch: Float, muted: Boolean) {
        val project = _uiState.value.currentProject ?: return
        val pad = project.pads.find { it.id == padId } ?: return
        updatePad(pad.copy(volume = volume, pan = pan, pitch = pitch, muted = muted))
    }

    fun toggleSolo(padId: String) {
        val project = _uiState.value.currentProject ?: return
        val pad = project.pads.find { it.id == padId } ?: return
        val newSoloed = !pad.soloed
        // Clear all solos first, then set this one
        val newPads = project.pads.map {
            it.copy(soloed = if (it.id == padId) newSoloed else false)
        }
        val updated = project.copy(pads = newPads)
        _uiState.update { it.copy(currentProject = updated) }
        audioService?.audioEngine?.setSolo(if (newSoloed) padId else null)
    }

    fun updateScene(updatedScene: Scene) {
        val project = _uiState.value.currentProject ?: return
        val newScenes = project.scenes.map { if (it.id == updatedScene.id) updatedScene else it }
        _uiState.update { it.copy(currentProject = project.copy(scenes = newScenes)) }
    }

    fun setPadColor(padId: String, color: PadColor) {
        val project = _uiState.value.currentProject ?: return
        val pad = project.pads.find { it.id == padId } ?: return
        updatePad(pad.copy(color = color))
    }

    fun setPadMode(padId: String, mode: PadMode) {
        val project = _uiState.value.currentProject ?: return
        val pad = project.pads.find { it.id == padId } ?: return
        updatePad(pad.copy(padMode = mode))
    }

    fun setPadPlaybackType(padId: String, type: PlaybackType) {
        val project = _uiState.value.currentProject ?: return
        val pad = project.pads.find { it.id == padId } ?: return
        updatePad(pad.copy(playbackType = type))
    }

    fun setPadTriggerGroup(padId: String, group: Int) {
        val project = _uiState.value.currentProject ?: return
        val pad = project.pads.find { it.id == padId } ?: return
        updatePad(pad.copy(triggerGroup = group))
    }

    fun updateGridLayout(layout: GridLayout, customRows: Int = 4, customCols: Int = 4) {
        val project = _uiState.value.currentProject ?: return
        val updated = project.copy(gridLayout = layout, customRows = customRows, customCols = customCols)
        _uiState.update { it.copy(currentProject = updated, showGridDialog = false) }
    }

    // ─── UI Event Handlers ────────────────────────────────────────────────────

    fun showFolderPicker() = _uiState.update { it.copy(showFolderPicker = true) }
    fun hideFolderPicker() = _uiState.update { it.copy(showFolderPicker = false) }
    fun showNewProjectDialog() = _uiState.update { it.copy(showNewProjectDialog = true) }
    fun hideNewProjectDialog() = _uiState.update { it.copy(showNewProjectDialog = false) }
    fun showGridDialog() = _uiState.update { it.copy(showGridDialog = true) }
    fun hideGridDialog() = _uiState.update { it.copy(showGridDialog = false) }
    fun showMixer(padId: String) = _uiState.update { it.copy(showMixerForPadId = padId) }
    fun hideMixer() = _uiState.update { it.copy(showMixerForPadId = null) }
    fun showSettings() = _uiState.update { it.copy(showSettingsScreen = true) }
    fun hideSettings() = _uiState.update { it.copy(showSettingsScreen = false) }
    fun showProjects() = _uiState.update { it.copy(showProjectsScreen = true) }
    fun hideProjects() = _uiState.update { it.copy(showProjectsScreen = false) }
    fun selectPadForEdit(padId: String?) = _uiState.update { it.copy(selectedPadForEdit = padId) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    override fun onCleared() {
        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceBound = false
        }
        super.onCleared()
    }
}
