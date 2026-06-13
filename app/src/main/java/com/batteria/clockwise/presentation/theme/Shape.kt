package com.batteria.clockwise.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * ClockWise v3.6 — Kid-friendly chunky corner radii.
 *
 * Larger-than-default Material 3 corners for a soft, candy-rounded look that
 * feels approachable to children. Used by Theme.kt to override the default
 * MaterialTheme shapes.
 */
val ClockWiseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(16.dp),
    medium     = RoundedCornerShape(24.dp),
    large      = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
)
