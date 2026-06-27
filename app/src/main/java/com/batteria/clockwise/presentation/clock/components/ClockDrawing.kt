package com.batteria.clockwise.presentation.clock.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.batteria.clockwise.presentation.theme.BlueyPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * v4.0 — Drawing primitives shared by every analog-clock rendering surface
 * (the full clock screen, the quiz prompt, future widgets, etc.).
 *
 * Refactored out of ClockScreen.kt during the v4.0 decoupling pass so the
 * quiz screen can render a non-interactive dial without dragging in the
 * whole interactive clock composable.
 */

internal fun DrawScope.drawClockFace() {
    val r = size.minDimension / 2f
    drawCircle(
        color = BlueyPalette.Outline,
        radius = r - 6f,
        style = Stroke(width = 6f),
    )
}

internal fun DrawScope.drawTicks() {
    val r = size.minDimension / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f
    for (i in 0 until 60) {
        val angle = Math.toRadians((i * 6 - 90).toDouble())
        val isMajor = i % 5 == 0
        val inner = if (isMajor) r * 0.84f else r * 0.88f
        val outer = r * 0.93f
        val x1 = cx + (cos(angle) * inner).toFloat()
        val y1 = cy + (sin(angle) * inner).toFloat()
        val x2 = cx + (cos(angle) * outer).toFloat()
        val y2 = cy + (sin(angle) * outer).toFloat()
        drawLine(
            color = if (isMajor) BlueyPalette.BlueyDeep.copy(alpha = 0.7f)
                    else BlueyPalette.Outline.copy(alpha = 0.55f),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = if (isMajor) 5f else 2f,
            cap = StrokeCap.Round,
        )
    }
}

internal fun DrawScope.drawNumbers() {
    val r = size.minDimension / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val numberR = r * 0.72f
    val textSizePx = r * 0.16f
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(
            255,
            (BlueyPalette.Ink.red * 255).toInt(),
            (BlueyPalette.Ink.green * 255).toInt(),
            (BlueyPalette.Ink.blue * 255).toInt(),
        )
        textSize = textSizePx
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD,
        )
    }
    val fm = paint.fontMetrics
    val baselineOffset = (fm.descent + fm.ascent) / 2f
    val nativeCanvas = this.drawContext.canvas.nativeCanvas
    for (n in 1..12) {
        val angle = Math.toRadians((n * 30 - 90).toDouble())
        val x = cx + (cos(angle) * numberR).toFloat()
        val y = cy + (sin(angle) * numberR).toFloat() - baselineOffset
        nativeCanvas.drawText(n.toString(), x, y, paint)
    }
}

internal fun DrawScope.drawHand(
    angleDeg: Float,
    lengthFrac: Float,
    strokeWidth: Float,
    color: Color,
) {
    val r = size.minDimension / 2f
    val cx = size.width / 2f
    val cy = size.height / 2f
    val angle = Math.toRadians((angleDeg - 90).toDouble())
    val length = r * lengthFrac
    val x = cx + (cos(angle) * length).toFloat()
    val y = cy + (sin(angle) * length).toFloat()
    drawLine(
        color = color,
        start = Offset(cx, cy),
        end = Offset(x, y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

internal fun DrawScope.drawCenterPin() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    drawCircle(color = BlueyPalette.Bandit, radius = r * 0.06f, center = Offset(cx, cy))
    drawCircle(color = BlueyPalette.Chilli, radius = r * 0.028f, center = Offset(cx, cy))
}
