package com.batteria.clockwise.presentation.clock.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.batteria.clockwise.presentation.theme.BlueyPalette
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan2

/**
 * v4.0 — Decoupled, reusable analog clock face.
 *
 * Two display modes are supported via the [time] parameter:
 *  - [ClockTime.Live]   — ticks in real time (uses withFrameNanos)
 *  - [ClockTime.Static] — frozen at a specific HH:MM:SS (no animation)
 *
 * Interaction is opt-in via [onManualDelta]. When non-null and the time
 * source is [ClockTime.Live] / [ClockTime.Static], dragging a hand calls
 * the callback with a delta in seconds. Callers that want a fully
 * non-interactive face (the quiz prompt) just pass `null`.
 *
 * The quiz screen relies on this composable to render its question dial
 * with no toggles, no drag handlers, and no second hand — just numerals,
 * hour, and minute hands at the question's target time.
 */
@Composable
fun AnalogClockFace(
    time: ClockTime,
    showSeconds: Boolean,
    modifier: Modifier = Modifier,
    onManualDelta: ((Float) -> Unit)? = null,
) {
    // Live tick source (only running when in Live mode).
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val isLive = time is ClockTime.Live
    LaunchedEffect(isLive) {
        if (isLive) {
            while (true) {
                withFrameNanos { nowMs = System.currentTimeMillis() }
            }
        }
    }

    // Resolve hand angles from the current time source.
    val (hourAngle, minuteAngle, secondAngle) = when (time) {
        is ClockTime.Live -> {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val m = cal.get(Calendar.MINUTE)
            val s = cal.get(Calendar.SECOND)
            val ms = cal.get(Calendar.MILLISECOND)
            Triple(
                ((h % 12) + m / 60f) * 30f,
                (m + s / 60f) * 6f,
                (s + ms / 1000f) * 6f,
            )
        }
        is ClockTime.Static -> {
            val total = time.totalSecondsInCycle()
            Triple(
                (total / CYCLE_SECONDS) * 360f,
                ((total % 3600f) / 3600f) * 360f,
                ((total % 60f) / 60f) * 360f,
            )
        }
        is ClockTime.Manual -> {
            val total = time.totalSecondsInCycle()
            Triple(
                (total / CYCLE_SECONDS) * 360f,
                ((total % 3600f) / 3600f) * 360f,
                ((total % 60f) / 60f) * 360f,
            )
        }
    }

    // Wrap angles so the (single-bound) pointerInput closure always sees fresh values.
    val hourAngleState = rememberUpdatedState(hourAngle)
    val minuteAngleState = rememberUpdatedState(minuteAngle)
    val secondAngleState = rememberUpdatedState(secondAngle)
    val showSecondsState = rememberUpdatedState(showSeconds)
    val onDeltaState = rememberUpdatedState(onManualDelta)

    var draggingHand by remember { mutableStateOf(HAND_NONE) }

    // Bouncy press-down stroke widening (same feel as v3.6.5).
    val hourSw by animateFloatAsState(
        targetValue = if (draggingHand == HAND_HOUR) 14f * 1.6f else 14f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "hourSw",
    )
    val minuteSw by animateFloatAsState(
        targetValue = if (draggingHand == HAND_MINUTE) 10f * 1.6f else 10f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "minuteSw",
    )
    val secondSw by animateFloatAsState(
        targetValue = if (draggingHand == HAND_SECOND) 4f * 1.6f else 4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "secondSw",
    )

    val interactive = onManualDelta != null
    val dragModifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downAngle = pointerAngleDeg(down.position.x - cx, down.position.y - cy)
                var lastAngle = downAngle
                val grabbedHand = pickHand(
                    pointerAngle = downAngle,
                    hourAngle = hourAngleState.value,
                    minuteAngle = minuteAngleState.value,
                    secondAngle = secondAngleState.value,
                    showSeconds = showSecondsState.value,
                )
                if (grabbedHand == HAND_NONE) return@awaitEachGesture
                draggingHand = grabbedHand
                try {
                    drag(down.id) { change ->
                        change.consume()
                        val pos = change.position
                        val newAngle = pointerAngleDeg(pos.x - cx, pos.y - cy)
                        val delta = shortestDelta(lastAngle, newAngle)
                        lastAngle = newAngle
                        val secondsDelta = when (grabbedHand) {
                            HAND_HOUR -> delta * 120f
                            HAND_MINUTE -> delta * 10f
                            HAND_SECOND -> delta / 6f
                            else -> 0f
                        }
                        if (secondsDelta != 0f) onDeltaState.value?.invoke(secondsDelta)
                    }
                } finally {
                    draggingHand = HAND_NONE
                }
            }
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(percent = 50),
                ambientColor = BlueyPalette.Ink,
                spotColor = BlueyPalette.Ink,
            )
            .background(
                color = if (interactive) BlueyPalette.BgElevated.copy(alpha = 0.95f)
                        else BlueyPalette.BgElevated,
                shape = RoundedCornerShape(percent = 50),
            )
            .then(dragModifier),
    ) {
        drawClockFace()
        drawTicks()
        drawNumbers()
        drawHand(angleDeg = hourAngle, lengthFrac = 0.55f, strokeWidth = hourSw, color = BlueyPalette.Bandit)
        drawHand(angleDeg = minuteAngle, lengthFrac = 0.78f, strokeWidth = minuteSw, color = BlueyPalette.Bluey)
        if (showSeconds) {
            drawHand(angleDeg = secondAngle, lengthFrac = 0.86f, strokeWidth = secondSw, color = BlueyPalette.Chilli)
        }
        drawCenterPin()
    }
}

/* -------------------- public time source -------------------- */

/** Source of truth for what the clock should display. */
sealed class ClockTime {
    /** Wall-clock time, ticked every frame. */
    data object Live : ClockTime()

    /** Frozen integer-precision time (quiz prompts, screenshots, widgets). */
    data class Static(val hour: Int, val minute: Int, val second: Int = 0) : ClockTime() {
        fun totalSecondsInCycle(): Float {
            val h = ((hour % 12) + 12) % 12
            val m = ((minute % 60) + 60) % 60
            val s = ((second % 60) + 60) % 60
            return (h * 3600 + m * 60 + s).toFloat()
        }
    }

    /** Float-precision time, used by manual-drag mode so the hour hand glides smoothly. */
    data class Manual(val totalSeconds: Float) : ClockTime() {
        fun totalSecondsInCycle(): Float {
            var x = totalSeconds % CYCLE_SECONDS
            if (x < 0f) x += CYCLE_SECONDS
            return x
        }
    }
}

/** Public constant for callers wanting to reason about a 12h cycle. */
const val CYCLE_SECONDS: Float = 43200f

/* -------------------- internal drag helpers -------------------- */

private const val HAND_NONE = -1
private const val HAND_HOUR = 0
private const val HAND_MINUTE = 1
private const val HAND_SECOND = 2

private fun pointerAngleDeg(dx: Float, dy: Float): Float {
    var deg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI + 90.0).toFloat()
    deg = ((deg % 360f) + 360f) % 360f
    return deg
}

private fun shortestDelta(prev: Float, curr: Float): Float {
    var d = curr - prev
    while (d > 180f) d -= 360f
    while (d <= -180f) d += 360f
    return d
}

private fun pickHand(
    pointerAngle: Float,
    hourAngle: Float,
    minuteAngle: Float,
    secondAngle: Float,
    showSeconds: Boolean,
): Int {
    val tolerance = 20f
    val dHour = abs(shortestDelta(hourAngle, pointerAngle))
    val dMin = abs(shortestDelta(minuteAngle, pointerAngle))
    val dSec = if (showSeconds) abs(shortestDelta(secondAngle, pointerAngle)) else Float.MAX_VALUE
    val best = minOf(dHour, dMin, dSec)
    if (best > tolerance) return HAND_NONE
    return when (best) {
        dSec -> HAND_SECOND
        dMin -> HAND_MINUTE
        else -> HAND_HOUR
    }
}
