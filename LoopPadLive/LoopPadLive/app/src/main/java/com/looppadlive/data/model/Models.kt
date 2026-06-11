package com.looppadlive.data.model

import android.net.Uri

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class PlaybackType { LOOP, ONE_SHOT, GATE }

enum class PadMode { LOOP, SCENE, BREAK }

enum class Quantization(val displayName: String, val beats: Float) {
    NONE("None", 0f),
    ONE_BEAT("1 Beat", 1f),
    TWO_BEATS("2 Beats", 2f),
    FOUR_BEATS("4 Beats", 4f),
    ONE_BAR("1 Bar", 4f)  // assumes 4/4
}

enum class GridLayout(val rows: Int, val cols: Int, val displayName: String) {
    GRID_4X4(4, 4, "4×4"),
    GRID_4X8(4, 8, "4×8"),
    GRID_8X6(8, 6, "8×6"),
    GRID_8X8(8, 8, "8×8"),
    CUSTOM(0, 0, "Custom")
}

enum class FxType { DELAY, REVERB, FILTER, ECHO }

enum class PadState { IDLE, PLAYING, QUEUED, MUTED }

// ─── Pad ─────────────────────────────────────────────────────────────────────

data class PadFx(
    val type: FxType = FxType.REVERB,
    val enabled: Boolean = false,
    val mix: Float = 0.3f,       // 0..1
    val param1: Float = 0.5f,    // delay time / reverb size / filter freq / echo decay
    val param2: Float = 0.5f     // feedback / wet / resonance / feedback
)

data class Pad(
    val id: String,                              // unique UUID
    val label: String = "",
    val audioUri: Uri? = null,
    val audioFileName: String = "",
    val row: Int = 0,
    val col: Int = 0,
    val playbackType: PlaybackType = PlaybackType.LOOP,
    val padMode: PadMode = PadMode.LOOP,
    val triggerGroup: Int = -1,                  // -1 = no group
    // Mixer
    val volume: Float = 1f,                      // 0..1
    val pan: Float = 0f,                         // -1 (L) .. +1 (R)
    val pitch: Float = 0f,                       // semitones -12..+12
    val muted: Boolean = false,
    val soloed: Boolean = false,
    // FX
    val fx: List<PadFx> = emptyList(),
    // State (transient, not saved)
    val state: PadState = PadState.IDLE,
    val color: PadColor = PadColor.SAFFRON
)

enum class PadColor(val hex: String) {
    SAFFRON("#FF9933"),
    GOLD("#FFD700"),
    EMERALD("#00C97A"),
    SAPPHIRE("#1A8FFF"),
    RUBY("#FF2D55"),
    VIOLET("#A855F7"),
    TEAL("#14B8A6"),
    AMBER("#F59E0B"),
    WHITE("#E5E7EB"),
    CRIMSON("#DC2626")
}

// ─── Scene ───────────────────────────────────────────────────────────────────

data class Scene(
    val id: String,
    val name: String,
    val rowIndex: Int,
    val isBreak: Boolean = false,
    val resumeAfterBreak: Boolean = false       // for break scenes
)

// ─── Project ─────────────────────────────────────────────────────────────────

data class Project(
    val id: String,
    val name: String,
    val bpm: Int = 120,
    val gridLayout: GridLayout = GridLayout.GRID_4X4,
    val customRows: Int = 4,
    val customCols: Int = 4,
    val quantization: Quantization = Quantization.ONE_BAR,
    val pads: List<Pad> = emptyList(),
    val scenes: List<Scene> = emptyList(),
    val folderPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val effectiveRows: Int get() = if (gridLayout == GridLayout.CUSTOM) customRows else gridLayout.rows
    val effectiveCols: Int get() = if (gridLayout == GridLayout.CUSTOM) customCols else gridLayout.cols
}

// ─── JSON Pack Format ────────────────────────────────────────────────────────

data class JsonPack(
    val name: String = "",
    val bpm: Int = 120,
    val samples: List<String> = emptyList(),
    val scenes: List<JsonScene>? = null
)

data class JsonScene(
    val name: String = "",
    val row: Int = 0,
    val isBreak: Boolean = false
)

// ─── Audio Engine State ───────────────────────────────────────────────────────

data class EngineState(
    val isPlaying: Boolean = false,
    val bpm: Int = 120,
    val currentBeat: Int = 0,
    val activeSceneRow: Int = -1,
    val activePadIds: Set<String> = emptySet(),
    val soloedPadId: String? = null
)
