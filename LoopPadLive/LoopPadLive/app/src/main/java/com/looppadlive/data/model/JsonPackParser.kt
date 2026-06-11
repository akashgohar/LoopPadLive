package com.looppadlive.data.model

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.File
import java.util.UUID

/**
 * Parses LoopPad JSON pack format:
 * {
 *   "name": "Pack",
 *   "bpm": 100,
 *   "samples": ["A.wav", "B.wav", ...],
 *   "scenes": [{ "name": "Verse", "row": 0, "isBreak": false }]
 * }
 */
object JsonPackParser {

    private val gson = Gson()

    fun parseFromString(json: String): JsonPack? {
        return try {
            gson.fromJson(json, JsonPack::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e("JsonPackParser", "Failed to parse JSON: ${e.message}")
            null
        }
    }

    fun parseFromFile(file: File): JsonPack? {
        return try {
            parseFromString(file.readText())
        } catch (e: Exception) {
            Log.e("JsonPackParser", "Failed to read file: ${e.message}")
            null
        }
    }

    /**
     * Convert a parsed JsonPack into a full Project, resolving audio URIs from the folder.
     */
    fun toProject(
        pack: JsonPack,
        folderPath: String,
        cols: Int = 4
    ): Project {
        val pads = mutableListOf<Pad>()
        val scenes = mutableListOf<Scene>()

        pack.samples.forEachIndexed { index, sampleName ->
            val file = File(folderPath, sampleName)
            val uri = if (file.exists()) Uri.fromFile(file) else null
            val row = index / cols
            val col = index % cols

            // Determine pad mode from scenes definition
            val jsonScene = pack.scenes?.find { it.row == row }
            val padMode = when {
                jsonScene?.isBreak == true -> PadMode.BREAK
                jsonScene != null -> PadMode.SCENE
                else -> PadMode.LOOP
            }

            pads.add(
                Pad(
                    id = UUID.randomUUID().toString(),
                    label = sampleName.substringBeforeLast("."),
                    audioUri = uri,
                    audioFileName = sampleName,
                    row = row,
                    col = col,
                    padMode = padMode,
                    playbackType = if (padMode == PadMode.BREAK) PlaybackType.ONE_SHOT else PlaybackType.LOOP,
                    color = PadColor.entries[index % PadColor.entries.size]
                )
            )
        }

        // Build scenes from rows
        val rowSet = pads.map { it.row }.distinct().sorted()
        rowSet.forEach { rowIdx ->
            val jsonScene = pack.scenes?.find { it.row == rowIdx }
            val isBreak = pads.any { it.row == rowIdx && it.padMode == PadMode.BREAK }
            scenes.add(
                Scene(
                    id = UUID.randomUUID().toString(),
                    name = jsonScene?.name ?: "Scene ${rowIdx + 1}",
                    rowIndex = rowIdx,
                    isBreak = isBreak
                )
            )
        }

        return Project(
            id = UUID.randomUUID().toString(),
            name = pack.name.ifEmpty { "Imported Pack" },
            bpm = pack.bpm,
            pads = pads,
            scenes = scenes,
            folderPath = folderPath
        )
    }

    fun projectToJson(project: Project): String {
        val samples = project.pads
            .sortedWith(compareBy({ it.row }, { it.col }))
            .map { it.audioFileName }

        val jsonScenes = project.scenes.map {
            JsonScene(name = it.name, row = it.rowIndex, isBreak = it.isBreak)
        }

        val pack = JsonPack(
            name = project.name,
            bpm = project.bpm,
            samples = samples,
            scenes = jsonScenes
        )
        return gson.toJson(pack)
    }
}
