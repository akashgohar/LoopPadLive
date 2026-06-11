package com.looppadlive.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// ─── LoopPad Dark Palette ─────────────────────────────────────────────────────
// Inspired by professional performance hardware (Akai MPC / Pioneer DJ)
// Deep blacks, electric accents, warm Indian saffron as primary

val SaffronOrange     = Color(0xFFFF9933)  // Primary — saffron
val SaffronLight      = Color(0xFFFFB96A)  // Primary container light
val SaffronDark       = Color(0xFFB86A1A)  // Primary dark

val ElectricBlue      = Color(0xFF1A8FFF)  // Secondary — electric
val ElectricBlueLight = Color(0xFF82C4FF)
val ElectricBlueDark  = Color(0xFF004999)

val EmeraldGreen      = Color(0xFF00C97A)  // Tertiary — go/active
val RubyRed           = Color(0xFFFF2D55)  // Error / break mode
val GoldYellow        = Color(0xFFFFD700)  // Beat pulse

// Backgrounds — near-black with warm tint
val BackgroundDeep    = Color(0xFF0A0A0C)
val BackgroundPanel   = Color(0xFF131318)
val SurfaceCard       = Color(0xFF1C1C24)
val SurfaceElevated   = Color(0xFF232330)
val SurfaceBorder     = Color(0xFF2E2E3E)

// Text
val OnSurface         = Color(0xFFE8E8F0)
val OnSurfaceVariant  = Color(0xFF9090A8)
val OnSurfaceMuted    = Color(0xFF555570)

// Pad state colors
val PadActive         = SaffronOrange
val PadQueued         = GoldYellow
val PadIdle           = SurfaceCard
val PadBreak          = RubyRed
val PadScene          = ElectricBlue

val LoopPadDarkColorScheme = darkColorScheme(
    primary                = SaffronOrange,
    onPrimary              = Color(0xFF1A0A00),
    primaryContainer       = Color(0xFF6B3000),
    onPrimaryContainer     = SaffronLight,

    secondary              = ElectricBlue,
    onSecondary            = Color(0xFF001B40),
    secondaryContainer     = Color(0xFF003166),
    onSecondaryContainer   = ElectricBlueLight,

    tertiary               = EmeraldGreen,
    onTertiary             = Color(0xFF003820),
    tertiaryContainer      = Color(0xFF005232),
    onTertiaryContainer    = Color(0xFF70FFC0),

    error                  = RubyRed,
    onError                = Color(0xFF3B0012),
    errorContainer         = Color(0xFF7A0025),
    onErrorContainer       = Color(0xFFFFB3C1),

    background             = BackgroundDeep,
    onBackground           = OnSurface,

    surface                = BackgroundPanel,
    onSurface              = OnSurface,
    surfaceVariant         = SurfaceCard,
    onSurfaceVariant       = OnSurfaceVariant,

    outline                = SurfaceBorder,
    outlineVariant         = Color(0xFF1E1E2E),

    scrim                  = Color(0xCC000000),
    inverseSurface         = OnSurface,
    inverseOnSurface       = BackgroundDeep,
    inversePrimary         = SaffronDark
)
