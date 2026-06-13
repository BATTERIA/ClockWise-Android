package com.batteria.clockwise.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * ClockWise v3 — Bluey-themed light-only theme.
 * Dark mode intentionally NOT supported yet; cream bg is the eye-friendly default.
 */
private val BlueyLightColors = lightColorScheme(
    primary            = BlueyPalette.Bluey,
    onPrimary          = BlueyPalette.BgElevated,
    primaryContainer   = BlueyPalette.BlueySoft,
    onPrimaryContainer = BlueyPalette.BlueyDeep,

    secondary            = BlueyPalette.Bandit,
    onSecondary          = BlueyPalette.BgElevated,
    secondaryContainer   = BlueyPalette.BingoSoft,
    onSecondaryContainer = BlueyPalette.Ink,

    tertiary            = BlueyPalette.Chilli,
    onTertiary          = BlueyPalette.BgElevated,

    background    = BlueyPalette.Bg,
    onBackground  = BlueyPalette.Ink,
    surface       = BlueyPalette.Bg,
    onSurface     = BlueyPalette.Ink,

    surfaceVariant     = BlueyPalette.BgDeep,
    onSurfaceVariant   = BlueyPalette.InkSoft,
    surfaceContainer   = BlueyPalette.BgElevated,

    outline        = BlueyPalette.Outline,
    outlineVariant = BlueyPalette.BgDeep,

    error          = BlueyPalette.Chilli,
    onError        = BlueyPalette.BgElevated,
)

@Composable
fun ClockWiseTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BlueyLightColors,
        typography = Typography,
        shapes = ClockWiseShapes,
        content = content,
    )
}
