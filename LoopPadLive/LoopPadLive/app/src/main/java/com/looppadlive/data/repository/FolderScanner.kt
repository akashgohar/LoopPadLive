package com.looppadlive.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.looppadlive.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ScannedFolder(
    val folderPath: String,
    val folderName: String,
    val audioFiles: List<AudioFileInfo>,
    val jsonPackFile: File?
)

data class AudioFileInfo(
    val file: File,
    val uri: Uri,
    val name: String,
    val extension: String,
    val durationMs: Long = 0L
)

object FolderScanner {

    private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "ogg", "flac", "aac", "m4a")

    suspend fun scanFolder(folderPath: String): ScannedFolder = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        val audioFiles = mutableListOf<AudioFileInfo>()
        var jsonPackFile: File? = null

        if (!folder.exists() || !folder.isDirectory) {
            return@withContext ScannedFolder(folderPath, folder.name, emptyList(), null)
        }

        folder.listFiles()?.sortedBy { it.name }?.forEach { file ->
            when {
                file.extension.lowercase() in AUDIO_EXTENSIONS -> {
                    audioFiles.add(
                        AudioFileInfo(
                            file = file,
                            uri = Uri.fromFile(file),
                            name = file.nameWithoutExtension,
                            extension = file.extension.lowercase()
                        )
                    )
                }
                file.extension.lowercase() == "json" -> {
                    jsonPackFile = file
                }
            }
        }

        ScannedFolder(
            folderPath = folderPath,
            folderName = folder.name,
            audioFiles = audioFiles.sortedBy { it.name },
            jsonPackFile = jsonPackFile
        )
    }

    /**
     * Convert a scanned folder into a Project.
     * If a JSON pack file exists, use it for metadata (BPM, scene names etc).
     * Otherwise, auto-assign pads in row-major order.
     */
    suspend fun toProject(
        scanned: ScannedFolder,
        layout: GridLayout = GridLayout.GRID_4X4,
        customRows: Int = 4,
        customCols: Int = 4
    ): Project = withContext(Dispatchers.IO) {
        val effectiveCols = if (layout == GridLayout.CUSTOM) customCols else layout.cols
        val effectiveRows = if (layout == GridLayout.CUSTOM) customRows else layout.rows

        // If JSON pack exists, parse it
        val jsonPack = scanned.jsonPackFile?.let { JsonPackParser.parseFromFile(it) }

        if (jsonPack != null) {
            return@withContext JsonPackParser.toProject(jsonPack, scanned.folderPath, effectiveCols)
                .copy(gridLayout = layout, customRows = customRows, customCols = customCols)
        }

        // Auto-build pads from audio files
        val colors = PadColor.entries
        val pads = scanned.audioFiles.take(effectiveRows * effectiveCols).mapIndexed { index, audio ->
            val row = index / effectiveCols
            val col = index % effectiveCols
            Pad(
                id = UUID.randomUUID().toString(),
                label = audio.name.take(12),
                audioUri = audio.uri,
                audioFileName = audio.file.name,
                row = row,
                col = col,
                playbackType = PlaybackType.LOOP,
                padMode = PadMode.LOOP,
                color = colors[index % colors.size]
            )
        }

        val scenes = (0 until effectiveRows).map { row ->
            Scene(
                id = UUID.randomUUID().toString(),
                name = "Scene ${row + 1}",
                rowIndex = row
            )
        }

        Project(
            id = UUID.randomUUID().toString(),
            name = scanned.folderName,
            bpm = 120,
            gridLayout = layout,
            customRows = customRows,
            customCols = customCols,
            pads = pads,
            scenes = scenes,
            folderPath = scanned.folderPath
        )
    }

    /**
     * Resolve a SAF (Storage Access Framework) tree URI to an actual file path.
     * Returns null if path cannot be resolved.
     */
    fun resolveTreeUri(context: Context, treeUri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parts = docId.split(":")
            if (parts.size >= 2) {
                val type = parts[0]
                val path = parts[1]
                if (type == "primary") {
                    "${android.os.Environment.getExternalStorageDirectory()}/$path"
                } else {
                    // External SD card - try to find it
                    context.getExternalFilesDirs(null)
                        .mapNotNull { it?.parentFile?.parentFile?.parentFile?.parentFile }
                        .find { it?.name == type }
                        ?.absolutePath?.let { "$it/$path" }
                }
            } else null
        } catch (e: Exception) {
            Log.e("FolderScanner", "resolveTreeUri failed: ${e.message}")
            null
        }
    }
}
