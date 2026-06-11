package com.looppadlive.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.looppadlive.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .registerTypeHierarchyAdapter(Uri::class.java, UriAdapter())
        .create()

    private val projectsDir: File
        get() = File(context.filesDir, "projects").also { it.mkdirs() }

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    suspend fun loadAllProjects() = withContext(Dispatchers.IO) {
        val loaded = projectsDir.listFiles()
            ?.filter { it.extension == "json" && !it.name.startsWith("pack_") }
            ?.mapNotNull { file ->
                try {
                    gson.fromJson(file.readText(), ProjectJson::class.java)?.toDomain()
                } catch (e: Exception) {
                    Log.e("ProjectRepo", "Failed to load ${file.name}: ${e.message}")
                    null
                }
            } ?: emptyList()
        _projects.value = loaded.sortedByDescending { it.updatedAt }
    }

    suspend fun saveProject(project: Project): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(projectsDir, "${project.id}.json")
            file.writeText(gson.toJson(project.toJson()))
            val current = _projects.value.toMutableList()
            val idx = current.indexOfFirst { it.id == project.id }
            if (idx >= 0) current[idx] = project else current.add(0, project)
            _projects.value = current.sortedByDescending { it.updatedAt }
            true
        } catch (e: Exception) {
            Log.e("ProjectRepo", "Failed to save project: ${e.message}")
            false
        }
    }

    suspend fun deleteProject(projectId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(projectsDir, "$projectId.json").delete()
            _projects.value = _projects.value.filter { it.id != projectId }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportProjectJson(project: Project): String = withContext(Dispatchers.IO) {
        JsonPackParser.projectToJson(project)
    }

    suspend fun createNewProject(
        name: String = "New Project",
        bpm: Int = 120,
        layout: GridLayout = GridLayout.GRID_4X4
    ): Project {
        val project = Project(
            id = UUID.randomUUID().toString(),
            name = name,
            bpm = bpm,
            gridLayout = layout,
            pads = buildDefaultPads(layout.rows, layout.cols),
            scenes = buildDefaultScenes(layout.rows)
        )
        saveProject(project)
        return project
    }

    private fun buildDefaultPads(rows: Int, cols: Int): List<Pad> {
        val colors = PadColor.entries
        return (0 until rows).flatMap { row ->
            (0 until cols).map { col ->
                Pad(
                    id = UUID.randomUUID().toString(),
                    label = "",
                    row = row,
                    col = col,
                    color = colors[(row * cols + col) % colors.size]
                )
            }
        }
    }

    private fun buildDefaultScenes(rows: Int): List<Scene> {
        return (0 until rows).map { row ->
            Scene(
                id = UUID.randomUUID().toString(),
                name = "Scene ${row + 1}",
                rowIndex = row
            )
        }
    }
}

// ─── Gson-serializable shadow classes ─────────────────────────────────────────

private data class ProjectJson(
    val id: String,
    val name: String,
    val bpm: Int,
    val gridLayoutName: String,
    val customRows: Int,
    val customCols: Int,
    val quantizationName: String,
    val pads: List<PadJson>,
    val scenes: List<SceneJson>,
    val folderPath: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Project = Project(
        id = id,
        name = name,
        bpm = bpm,
        gridLayout = GridLayout.entries.find { it.name == gridLayoutName } ?: GridLayout.GRID_4X4,
        customRows = customRows,
        customCols = customCols,
        quantization = Quantization.entries.find { it.name == quantizationName } ?: Quantization.ONE_BAR,
        pads = pads.map { it.toDomain() },
        scenes = scenes.map { it.toDomain() },
        folderPath = folderPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private data class PadJson(
    val id: String,
    val label: String,
    val audioUriString: String?,
    val audioFileName: String,
    val row: Int,
    val col: Int,
    val playbackTypeName: String,
    val padModeName: String,
    val triggerGroup: Int,
    val volume: Float,
    val pan: Float,
    val pitch: Float,
    val muted: Boolean,
    val soloed: Boolean,
    val colorName: String
) {
    fun toDomain(): Pad = Pad(
        id = id,
        label = label,
        audioUri = audioUriString?.let { Uri.parse(it) },
        audioFileName = audioFileName,
        row = row,
        col = col,
        playbackType = PlaybackType.entries.find { it.name == playbackTypeName } ?: PlaybackType.LOOP,
        padMode = PadMode.entries.find { it.name == padModeName } ?: PadMode.LOOP,
        triggerGroup = triggerGroup,
        volume = volume,
        pan = pan,
        pitch = pitch,
        muted = muted,
        soloed = soloed,
        color = PadColor.entries.find { it.name == colorName } ?: PadColor.SAFFRON
    )
}

private data class SceneJson(
    val id: String,
    val name: String,
    val rowIndex: Int,
    val isBreak: Boolean,
    val resumeAfterBreak: Boolean
) {
    fun toDomain() = Scene(id, name, rowIndex, isBreak, resumeAfterBreak)
}

private fun Project.toJson() = ProjectJson(
    id = id, name = name, bpm = bpm,
    gridLayoutName = gridLayout.name,
    customRows = customRows, customCols = customCols,
    quantizationName = quantization.name,
    pads = pads.map {
        PadJson(
            id = it.id, label = it.label,
            audioUriString = it.audioUri?.toString(),
            audioFileName = it.audioFileName,
            row = it.row, col = it.col,
            playbackTypeName = it.playbackType.name,
            padModeName = it.padMode.name,
            triggerGroup = it.triggerGroup,
            volume = it.volume, pan = it.pan, pitch = it.pitch,
            muted = it.muted, soloed = it.soloed,
            colorName = it.color.name
        )
    },
    scenes = scenes.map { SceneJson(it.id, it.name, it.rowIndex, it.isBreak, it.resumeAfterBreak) },
    folderPath = folderPath,
    createdAt = createdAt, updatedAt = updatedAt
)

private class UriAdapter : com.google.gson.TypeAdapter<Uri>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: Uri?) {
        out.value(value?.toString())
    }
    override fun read(input: com.google.gson.stream.JsonReader): Uri? {
        val str = input.nextString()
        return if (str.isNullOrEmpty()) null else Uri.parse(str)
    }
}
