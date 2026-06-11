# LoopPad Live 🥁

**Offline Android music launcher for live Indian percussion — Tabla, Dholak, Keyboard Loops & Backing Tracks**

Inspired by Remixlive, built for stage-ready Indian percussion performance on Android.

---

## Project Structure

```
LoopPadLive/
├── app/
│   ├── src/main/
│   │   ├── java/com/looppadlive/
│   │   │   ├── MainActivity.kt                        # Entry point
│   │   │   ├── audio/
│   │   │   │   ├── AudioEngine.kt                     # Core playback engine (MediaPlayer pool)
│   │   │   │   └── AudioPlaybackService.kt            # Background foreground service
│   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Models.kt                      # All data classes, enums
│   │   │   │   │   └── JsonPackParser.kt              # JSON pack import/export
│   │   │   │   └── repository/
│   │   │   │       ├── ProjectRepository.kt           # Save/load projects
│   │   │   │       └── FolderScanner.kt               # Scan folders for audio files
│   │   │   ├── viewmodel/
│   │   │   │   └── MainViewModel.kt                   # Central state (MVVM)
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt                       # Dark palette (saffron/electric)
│   │   │   │   │   └── Theme.kt                       # Material 3 theme
│   │   │   │   ├── components/
│   │   │   │   │   ├── PadGrid.kt                     # Main performance grid
│   │   │   │   │   ├── SceneLane.kt                   # Left scene launch sidebar
│   │   │   │   │   └── TransportBar.kt                # BPM / quantization toolbar
│   │   │   │   └── screens/
│   │   │   │       ├── home/
│   │   │   │       │   ├── MainScreen.kt              # Performance view + overlay routing
│   │   │   │       │   └── ProjectsScreen.kt          # Project library
│   │   │   │       ├── mixer/
│   │   │   │       │   └── MixerDialog.kt             # Per-pad mixer (volume, pan, pitch)
│   │   │   │       └── settings/
│   │   │   │           └── SettingsDialogs.kt         # Grid picker, new project dialog
│   │   │   └── utils/
│   │   │       └── FolderPickerDialog.kt              # Storage browser + SAF picker
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   │       ├── values/
│   │       │   ├── colors.xml
│   │       │   ├── strings.xml
│   │       │   └── themes.xml
│   │       └── xml/
│   │           └── file_paths.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml                             # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Build Instructions

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 17+ |
| Gradle | 8.7 (auto-downloaded) |
| Android SDK | API 35 (Target) / API 26 (Min) |

### Steps

1. **Clone / Open in Android Studio**
   ```
   File → Open → select the `LoopPadLive/` folder
   ```

2. **Sync Gradle**
   Android Studio will auto-sync. If it doesn't:
   ```
   File → Sync Project with Gradle Files
   ```

3. **Add launcher icons** (optional for build to succeed)
   Place a `ic_launcher.png` (48×48px) in:
   ```
   app/src/main/res/mipmap-hdpi/ic_launcher.png
   app/src/main/res/mipmap-hdpi/ic_launcher_round.png
   ```
   Or use Android Studio's **Image Asset Studio** (right-click `res` → New → Image Asset).

4. **Run on device or emulator**
   - Connect Android 8.0+ device with USB debugging ON
   - Press ▶ Run in Android Studio
   - Or: `./gradlew assembleDebug`

5. **Install APK directly**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Feature Guide

### Folder Import
1. Tap **Folder** icon in transport bar
2. Choose **Browse with System Picker** (recommended, no permission needed)
   — OR — browse manually and grant storage permission
3. Navigate to your samples folder (WAV/MP3/OGG)
4. Tap **Select This Folder**

The app auto-scans for audio files and creates a project. If a `pack.json` exists in the folder, it reads scene names, BPM, and layout from it.

### JSON Pack Format
Create a `pack.json` in your samples folder:
```json
{
  "name": "Tabla Pack",
  "bpm": 108,
  "samples": ["Teen_Taal.wav", "Dadra.wav", "Keherwa.wav", "Thumri.wav", "Break1.wav"],
  "scenes": [
    { "name": "Teen Taal", "row": 0, "isBreak": false },
    { "name": "Dadra", "row": 1, "isBreak": false },
    { "name": "Break", "row": 2, "isBreak": true }
  ]
}
```

### Scene Mode
- Each **row** = one scene
- Tap the **scene button** (left sidebar) → stops the previous row, starts the new row
- Only one scene active at a time (quantized)
- Break rows stop all playback and play one-shot

### Pad Long Press → Mixer
- **Volume** — per-pad output level
- **Pan** — left/right stereo position
- **Pitch** — transpose ±12 semitones
- **Mute** — silence pad without stopping
- **Solo** — mutes all other active pads
- **Pad Mode**: Loop / Scene / Break
- **Playback Type**: Loop / One Shot / Gate
- **Trigger Group**: pads in the same group stop each other
- **Color** — choose from 10 colors for visual identification

### BPM Engine
- Range: 40 – 250 BPM
- Tap BPM number in transport bar to type a value
- Use ± buttons for ±1 step adjustment

### Quantization
Options: **None / 1 Beat / 2 Beats / 4 Beats / 1 Bar**

When not None, pad triggers queue until the next beat boundary for tight timing.

### Grid Layouts
| Layout | Pads | Use Case |
|--------|------|----------|
| 4×4 | 16 | Standard live set |
| 4×8 | 32 | Extended library |
| 8×6 | 48 | Full arrangement |
| 8×8 | 64 | Maximum coverage |
| Custom | Any | Your own rows×cols |

---

## Architecture Overview

```
UI Layer (Compose)
    ↕ StateFlow
ViewModel (MVVM)
    ↕ suspend functions
Repository Layer
    ↕ coroutines/IO
AudioEngine + ProjectRepository + FolderScanner
    ↕ MediaPlayer API / File I/O
Android Audio + Storage
```

- **ViewModel** = single source of truth via `UiState`
- **AudioEngine** = pool of `MediaPlayer` instances, one per active pad
- **SceneMode** = row-triggered; previous row stops before new row starts
- **Quantization** = beat-clock coroutine queues triggers at bar/beat boundaries
- **Projects** = saved as JSON to `context.filesDir/projects/`

---

## Extending the App

### Adding FX (Delay / Reverb / Filter / Echo)
The `PadFx` data class and `FxType` enum are already defined in `Models.kt`.
To wire them up:
1. Add AudioEffect integration in `AudioEngine.kt`'s `playPad()` method
2. Use `android.media.audiofx.Reverb`, `android.media.audiofx.EnvironmentalReverb`, `android.media.audiofx.Equalizer`
3. Add FX controls to `MixerDialog.kt`

### Time Stretch (Pitch-Preserve BPM Change)
Integrate [Sonic](https://github.com/waywardgeek/sonic) library for time-stretch without pitch change:
```gradle
implementation 'com.github.waywardgeek:sonic:1.0.0'
```
Process audio PCM through Sonic before feeding to AudioTrack.

### MIDI Clock Sync
Add `android.media.midi.MidiManager` support in `AudioEngine.kt` to receive external MIDI clock and sync BPM automatically.

---

## Permissions Reference

| Permission | Why |
|------------|-----|
| `READ_MEDIA_AUDIO` (API 33+) | Read audio files from storage |
| `READ_EXTERNAL_STORAGE` (API ≤32) | Read audio files on older Android |
| `FOREGROUND_SERVICE` | Background audio playback |
| `WAKE_LOCK` | Keep CPU active during live performance |

---

## Sample Folder Structure (Recommended)

```
/storage/emulated/0/LoopPad/TeenTaal/
├── pack.json
├── Teen_Taal_Theka.wav
├── Teen_Taal_Variation_1.wav
├── Teen_Taal_Variation_2.wav
├── Kaherava_Theka.wav
├── Dadra_Theka.wav
├── Break_Silence.wav
└── Break_Fill.wav
```

---

*Built with Kotlin · Jetpack Compose · Material 3 · MediaPlayer API*
*Designed for Indian classical and semi-classical live performance*
