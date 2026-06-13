package com.batteria.clockwise.presentation.clock

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.batteria.clockwise.presentation.theme.BlueyPalette
import com.batteria.clockwise.util.TimeSpeech
import com.batteria.clockwise.util.TtsManager
import com.batteria.clockwise.util.toLocale
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val CYCLE_SECONDS = 43200f // 12h * 3600

/* -------------------- entry point -------------------- */

@Composable
fun ClockScreen(
    @Suppress("UNUSED_PARAMETER") navController: NavController? = null,
    viewModel: ClockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ClockScreenContent(
        state = state,
        onTimeFormatChange = viewModel::setTimeFormat,
        onLanguageChange = viewModel::setLanguage,
        onShowSecondsChange = viewModel::setShowSeconds,
        onModeChange = viewModel::setMode,
        onManualDelta = viewModel::addManualSeconds,
    )
}

@Composable
fun ClockScreenContent(
    state: ClockUiState,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onShowSecondsChange: (Boolean) -> Unit,
    onModeChange: (ClockMode) -> Unit,
    onManualDelta: (Float) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BlueyPalette.Bg,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            val isLandscape = maxWidth > maxHeight
            val isTablet = min(maxWidth.value, maxHeight.value) >= 600f

            if (isLandscape) {
                LandscapeLayout(
                    state = state,
                    onTimeFormatChange = onTimeFormatChange,
                    onLanguageChange = onLanguageChange,
                    onShowSecondsChange = onShowSecondsChange,
                    onModeChange = onModeChange,
                    onManualDelta = onManualDelta,
                    isTablet = isTablet,
                )
            } else {
                PortraitLayout(
                    state = state,
                    onTimeFormatChange = onTimeFormatChange,
                    onLanguageChange = onLanguageChange,
                    onShowSecondsChange = onShowSecondsChange,
                    onModeChange = onModeChange,
                    onManualDelta = onManualDelta,
                    isTablet = isTablet,
                )
            }
        }
    }
}

/* -------------------- portrait layout (golden ratio) -------------------- */

@Composable
private fun PortraitLayout(
    state: ClockUiState,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onShowSecondsChange: (Boolean) -> Unit,
    onModeChange: (ClockMode) -> Unit,
    onManualDelta: (Float) -> Unit,
    isTablet: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 48.dp else 24.dp)
            // v3.6: bouncy spring (medium-low stiffness, low bounce) when AM/PM
            // appears or disappears — toggles below glide with a tiny overshoot.
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.10f))

        AnalogClock(
            state = state,
            onManualDelta = onManualDelta,
            modifier = Modifier
                .fillMaxWidth(if (isTablet) 0.82f else 0.84f)
                .aspectRatio(1f),
        )

        Spacer(modifier = Modifier.weight(0.18f))

        DigitalCard(state = state, big = isTablet)

        Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 18.dp))

        FormatToggle(
            value = state.timeFormat,
            onChange = onTimeFormatChange,
            big = isTablet,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LanguageToggle(
            value = state.language,
            onChange = onLanguageChange,
            big = isTablet,
        )
        Spacer(modifier = Modifier.height(10.dp))
        ShowSecondsToggle(
            value = state.showSeconds,
            onChange = onShowSecondsChange,
            language = state.language,
            big = isTablet,
        )
        Spacer(modifier = Modifier.height(10.dp))
        ModeToggle(
            value = state.mode,
            onChange = onModeChange,
            language = state.language,
            big = isTablet,
        )

        Spacer(modifier = Modifier.weight(0.18f))
    }
}

/* -------------------- landscape / tablet-landscape layout -------------------- */

@Composable
private fun LandscapeLayout(
    state: ClockUiState,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onShowSecondsChange: (Boolean) -> Unit,
    onModeChange: (ClockMode) -> Unit,
    onManualDelta: (Float) -> Unit,
    isTablet: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 48.dp else 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 48.dp else 24.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            AnalogClock(
                state = state,
                onManualDelta = onManualDelta,
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(1f),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                // v3.6: same bouncy spring for landscape so toggles glide
                // with overshoot when AM/PM appears.
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DigitalCard(state = state, big = isTablet)
            Spacer(modifier = Modifier.height(if (isTablet) 28.dp else 18.dp))
            FormatToggle(
                value = state.timeFormat,
                onChange = onTimeFormatChange,
                big = isTablet,
            )
            Spacer(modifier = Modifier.height(10.dp))
            LanguageToggle(
                value = state.language,
                onChange = onLanguageChange,
                big = isTablet,
            )
            Spacer(modifier = Modifier.height(10.dp))
            ShowSecondsToggle(
                value = state.showSeconds,
                onChange = onShowSecondsChange,
                language = state.language,
                big = isTablet,
            )
            Spacer(modifier = Modifier.height(10.dp))
            ModeToggle(
                value = state.mode,
                onChange = onModeChange,
                language = state.language,
                big = isTablet,
            )
        }
    }
}

/* -------------------- analog clock (Canvas) -------------------- */

@Composable
private fun AnalogClock(
    state: ClockUiState,
    onManualDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Auto-mode time source.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val isAuto = state.mode == ClockMode.AUTO
    LaunchedEffect(isAuto) {
        if (isAuto) {
            while (true) {
                withFrameNanos { nowMs = System.currentTimeMillis() }
            }
        }
    }

    // Compute hand angles based on mode.
    val hourAngle: Float
    val minuteAngle: Float
    val secondAngle: Float

    if (isAuto) {
        val cal = remember { Calendar.getInstance() }
        cal.timeInMillis = nowMs
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)
        val ms = cal.get(Calendar.MILLISECOND)
        hourAngle = ((h % 12) + m / 60f) * 30f
        minuteAngle = (m + s / 60f) * 6f
        secondAngle = (s + ms / 1000f) * 6f
    } else {
        val total = state.manualTotalSeconds
        hourAngle   = (total / CYCLE_SECONDS) * 360f
        minuteAngle = ((total % 3600f) / 3600f) * 360f
        secondAngle = ((total % 60f) / 60f) * 360f
    }

    // Wrap angles into a State so the pointerInput closure (which captures these once)
    // always sees the latest values without re-keying the modifier.
    val hourAngleState = rememberUpdatedState(hourAngle)
    val minuteAngleState = rememberUpdatedState(minuteAngle)
    val secondAngleState = rememberUpdatedState(secondAngle)
    val onDeltaState = rememberUpdatedState(onManualDelta)
    val showSecondsState = rememberUpdatedState(state.showSeconds)

    // Track which hand is currently being dragged (purely visual: thicker stroke).
    var draggingHand by remember { mutableStateOf(HAND_NONE) }

    // v3.6.5: animate the stroke widths with a medium-bouncy spring so that the
    // press-down thickening pops smoothly instead of snapping. Defined here in
    // the composable scope so we can pass the live values into the Canvas draw.
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

    // v3.6.5: switch from detectDragGestures to awaitEachGesture so we can
    // mark draggingHand the instant a finger touches the hand — not only
    // after movement begins. The hand now thickens immediately on press-down.
    val dragModifier = if (state.mode == ClockMode.MANUAL) {
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
                // PRESS-DOWN visual: widen immediately, before any drag motion.
                draggingHand = grabbedHand
                try {
                    // Then handle the drag portion of the gesture in the same flow.
                    drag(down.id) { change ->
                        change.consume()
                        val pos = change.position
                        val newAngle = pointerAngleDeg(pos.x - cx, pos.y - cy)
                        val delta = shortestDelta(lastAngle, newAngle)
                        lastAngle = newAngle
                        val secondsDelta = when (grabbedHand) {
                            HAND_HOUR   -> delta * 120f   // 1° = 120s
                            HAND_MINUTE -> delta * 10f    // 1° = 10s
                            HAND_SECOND -> delta / 6f     // 1° = 1/6 s
                            else -> 0f
                        }
                        if (secondsDelta != 0f) onDeltaState.value(secondsDelta)
                    }
                } finally {
                    // Always reset, whether drag completed normally or was cancelled.
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
                color = if (state.mode == ClockMode.MANUAL)
                    BlueyPalette.BgElevated.copy(alpha = 0.95f)
                else BlueyPalette.BgElevated,
                shape = RoundedCornerShape(percent = 50),
            )
            .then(dragModifier),
    ) {
        drawClockFace()
        drawTicks()
        drawNumbers()
        // v3.6.5: stroke widths come from animated state (declared above) so the
        // press-down "pop" is a smooth bouncy spring rather than a hard step.
        drawHand(angleDeg = hourAngle,   lengthFrac = 0.55f, strokeWidth = hourSw,   color = BlueyPalette.Bandit)
        drawHand(angleDeg = minuteAngle, lengthFrac = 0.78f, strokeWidth = minuteSw, color = BlueyPalette.Bluey)
        if (state.showSeconds) {
            drawHand(angleDeg = secondAngle, lengthFrac = 0.86f, strokeWidth = secondSw, color = BlueyPalette.Chilli)
        }
        drawCenterPin()
        // v3.6.5: MANUAL badge removed — the Mode segmented toggle already tells
        // the user they're in manual mode; the badge looked ugly on the dial.
    }
}

/* ---- drag helpers ---- */

private const val HAND_NONE = -1
private const val HAND_HOUR = 0
private const val HAND_MINUTE = 1
private const val HAND_SECOND = 2

/** Angle in degrees with 0 at 12 o'clock and clockwise positive. */
private fun pointerAngleDeg(dx: Float, dy: Float): Float {
    var deg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI + 90.0).toFloat()
    deg = ((deg % 360f) + 360f) % 360f
    return deg
}

/** Smallest signed delta from prev → curr, in (-180, 180]. */
private fun shortestDelta(prev: Float, curr: Float): Float {
    var d = curr - prev
    while (d > 180f) d -= 360f
    while (d <= -180f) d += 360f
    return d
}

/** Pick the hand whose angle is closest to the pointer angle (within ±20°). */
private fun pickHand(
    pointerAngle: Float,
    hourAngle: Float,
    minuteAngle: Float,
    secondAngle: Float,
    showSeconds: Boolean,
): Int {
    // Wider tolerance (was 15°) so taps land more easily on phones.
    val tolerance = 20f
    val dHour = abs(shortestDelta(hourAngle,   pointerAngle))
    val dMin  = abs(shortestDelta(minuteAngle, pointerAngle))
    val dSec  = if (showSeconds) abs(shortestDelta(secondAngle, pointerAngle)) else Float.MAX_VALUE
    // Prefer the closest. If second hand is visible and within tolerance, give it priority
    // because it sits on top visually — but only if it's actually the closest.
    val best = minOf(dHour, dMin, dSec)
    if (best > tolerance) return HAND_NONE
    return when (best) {
        dSec -> HAND_SECOND
        dMin -> HAND_MINUTE
        else -> HAND_HOUR
    }
}

private fun DrawScope.drawClockFace() {
    val r = size.minDimension / 2f
    drawCircle(
        color = BlueyPalette.Outline,
        radius = r - 6f,
        style = Stroke(width = 6f),
    )
}

private fun DrawScope.drawTicks() {
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
            color = if (isMajor) BlueyPalette.BlueyDeep.copy(alpha = 0.7f) else BlueyPalette.Outline.copy(alpha = 0.55f),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = if (isMajor) 5f else 2f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawNumbers() {
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
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val fm = paint.fontMetrics
    val baselineOffset = (fm.descent + fm.ascent) / 2f
    drawIntoCanvasCompat { c ->
        for (n in 1..12) {
            val angle = Math.toRadians((n * 30 - 90).toDouble())
            val x = cx + (cos(angle) * numberR).toFloat()
            val y = cy + (sin(angle) * numberR).toFloat() - baselineOffset
            c.drawText(n.toString(), x, y, paint)
        }
    }
}

private inline fun DrawScope.drawIntoCanvasCompat(block: (android.graphics.Canvas) -> Unit) {
    block(this.drawContext.canvas.nativeCanvas)
}

private fun DrawScope.drawHand(
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

private fun DrawScope.drawCenterPin() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    drawCircle(color = BlueyPalette.Bandit, radius = r * 0.06f, center = Offset(cx, cy))
    drawCircle(color = BlueyPalette.Chilli, radius = r * 0.028f, center = Offset(cx, cy))
}

/* -------------------- digital card -------------------- */

@Composable
private fun DigitalCard(state: ClockUiState, big: Boolean) {
    // v3.6.5: TTS engine, scoped to this composition. shutdown() runs on dispose
    // so we don't leak the TextToSpeech client between recompositions/screens.
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    // Auto-mode time source (only ticks when in AUTO).
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val isAuto = state.mode == ClockMode.AUTO
    LaunchedEffect(isAuto) {
        if (isAuto) {
            while (true) {
                withFrameNanos { nowMs = System.currentTimeMillis() }
            }
        }
    }

    val timeText: String
    val periodText: String
    // Track whether period should be visible (only true in AUTO + 12h).
    val showPeriod: Boolean
    // v3.6.5: parallel snapshot of h/m/s/isPm for TTS — same numbers the digits
    // show, but kept as ints so TimeSpeech can format the natural sentence.
    val speakH: Int
    val speakM: Int
    val speakS: Int
    val speakIsPm: Boolean
    if (isAuto) {
        val cal = remember { Calendar.getInstance() }
        cal.timeInMillis = nowMs
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)
        when (state.timeFormat) {
            TimeFormat.H24 -> {
                timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h, m, s)
                           else "%02d:%02d".format(h, m)
                periodText = ""
                showPeriod = false
            }
            TimeFormat.H12 -> {
                val isPm = h >= 12
                val h12 = if (h % 12 == 0) 12 else h % 12
                timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h12, m, s)
                           else "%02d:%02d".format(h12, m)
                periodText = when (state.language) {
                    Language.EN -> if (isPm) "PM" else "AM"
                    Language.ZH -> if (isPm) "下午" else "上午"
                }
                showPeriod = true
            }
        }
        speakH = when (state.timeFormat) {
            TimeFormat.H24 -> h
            TimeFormat.H12 -> if (h % 12 == 0) 12 else h % 12
        }
        speakM = m
        speakS = s
        speakIsPm = state.timeFormat == TimeFormat.H12 && h >= 12
    } else {
        // Manual mode: derive from manualTotalSeconds; no AM/PM info from the dial.
        val total = ((state.manualTotalSeconds.toInt() % 43200) + 43200) % 43200
        val h12raw = total / 3600         // 0..11
        val m = (total % 3600) / 60
        val s = total % 60
        when (state.timeFormat) {
            TimeFormat.H24 -> {
                // 24h manual: show 0..11 as-is so the toggle is visibly different from 12h.
                timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h12raw, m, s)
                           else "%02d:%02d".format(h12raw, m)
                periodText = when (state.language) { Language.EN -> "AM"; Language.ZH -> "上午" }
                showPeriod = false
            }
            TimeFormat.H12 -> {
                val h12 = if (h12raw == 0) 12 else h12raw
                timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h12, m, s)
                           else "%02d:%02d".format(h12, m)
                // Show AM as default in manual 12h so the format toggle is visibly different.
                periodText = when (state.language) { Language.EN -> "AM"; Language.ZH -> "上午" }
                showPeriod = true
            }
        }
        speakH = when (state.timeFormat) {
            TimeFormat.H24 -> h12raw
            TimeFormat.H12 -> if (h12raw == 0) 12 else h12raw
        }
        speakM = m
        speakS = s
        speakIsPm = false // manual has no PM info
    }

    OutlinedCard(
        // v3.6.4: back to fixed widths (Master prefers the stable visual), but
        // sized to actually contain Fredoka 700 tabular at 56sp/80sp.
        // Empirical measurements (see /tmp/measure.js):
        //   56sp "12:34:56 PM" ≈ 318dp
        //   80sp "12:34:56 PM" ≈ 454dp
        // Phone widths capped to stay within ~360dp device frames; tablet wider.
        // Two widths (with-seconds / without-seconds) animated by a bouncy
        // spring give the candy-soft Q-pop when Master toggles Show s.
        modifier = Modifier.padding(horizontal = 4.dp).width(
            animateDpAsState(
                targetValue = when {
                    big && state.showSeconds  -> 500.dp
                    big && !state.showSeconds -> 400.dp
                    !big && state.showSeconds -> 348.dp
                    else                      -> 268.dp
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "digitalCardWidth",
            ).value
        ),
    ) {
        // v3.6.5: Box overlays a speaker IconButton on top of the centered Column
        // so the digits stay dead-center inside the card while the mic floats
        // on the right edge. Tapping it speaks the currently-displayed time.
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        // v3.6.4: small horizontal padding — the fixed card width
                        // already includes breathing room around the text.
                        horizontal = if (big) 16.dp else 12.dp,
                        vertical = if (big) 22.dp else 16.dp,
                    )
                    // v3.6: bouncy spring on the card's interior height so the AM/PM
                    // collapse pops gently instead of snapping.
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = timeText,
                    fontSize = if (big) 80.sp else 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueyPalette.Ink,
                    // v3.6.1: drop letter-spacing — Fredoka is already chunky.
                    letterSpacing = 0.sp,
                    style = MaterialTheme.typography.displayLarge,
                    // v3.6.1: belt-and-suspenders — the readout must NEVER wrap.
                    maxLines = 1,
                    softWrap = false,
                    // v3.6.4: fixed-width card is back; explicit center alignment so
                    // the digits sit dead-center inside the card no matter what.
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                // AM/PM uses expandVertically/shrinkVertically so that BOTH the fade
                // AND the vertical layout collapse are animated. Without these the
                // height change snaps and the toggles below jump.
                // v3.6: spring-based enter/exit for a tiny overshoot.
                androidx.compose.animation.AnimatedVisibility(
                    visible = showPeriod && periodText.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(220)) +
                            expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                                expandFrom = Alignment.Top,
                            ),
                    exit = fadeOut(animationSpec = tween(180)) +
                           shrinkVertically(
                               animationSpec = spring(
                                   dampingRatio = Spring.DampingRatioNoBouncy,
                                   stiffness = Spring.StiffnessMedium,
                               ),
                               shrinkTowards = Alignment.Top,
                           ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = periodText,
                            fontSize = if (big) 16.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueyPalette.BlueyDeep,
                        )
                    }
                }
            }
            // v3.6.5: TTS speaker button. Floats on the right edge of the card,
            // small enough to not overlap the centered digits. Uses VolumeUp
            // (speaker) rather than Mic because we're playing audio, not recording.
            FilledIconButton(
                onClick = {
                    val sentence = TimeSpeech.build(
                        hour = speakH,
                        minute = speakM,
                        second = speakS,
                        includeSeconds = state.showSeconds,
                        format = state.timeFormat,
                        isPm = speakIsPm,
                        language = state.language,
                    )
                    tts.speak(sentence, state.language.toLocale())
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(if (big) 44.dp else 36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = BlueyPalette.Bluey.copy(alpha = 0.15f),
                    contentColor = BlueyPalette.Bluey,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (state.language == Language.ZH) "播报时间" else "Speak time",
                    modifier = Modifier.size(if (big) 24.dp else 20.dp),
                )
            }
        }
    }
}

/* -------------------- segmented toggles -------------------- */

/**
 * v3.6: shared bouncy scale for SegmentedButton selected state.
 * The active segment pops to ~1.04x with a medium-bouncy spring so kids
 * get a satisfying confirmation when they tap a toggle.
 */
@Composable
private fun rememberSegmentScale(selected: Boolean): androidx.compose.runtime.State<Float> =
    animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "segmentScale",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatToggle(
    value: TimeFormat,
    onChange: (TimeFormat) -> Unit,
    big: Boolean,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.width(if (big) 260.dp else 200.dp),
    ) {
        TimeFormat.entries.forEachIndexed { idx, fmt ->
            val selected = value == fmt
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(fmt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = TimeFormat.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BlueyPalette.BlueySoft,
                    activeContentColor = BlueyPalette.BlueyDeep,
                    inactiveContainerColor = BlueyPalette.BgElevated,
                    inactiveContentColor = BlueyPalette.InkSoft,
                ),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = if (fmt == TimeFormat.H12) "12h" else "24h", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageToggle(
    value: Language,
    onChange: (Language) -> Unit,
    big: Boolean,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.width(if (big) 260.dp else 200.dp),
    ) {
        Language.entries.forEachIndexed { idx, lang ->
            val selected = value == lang
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(lang) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = Language.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BlueyPalette.BlueySoft,
                    activeContentColor = BlueyPalette.BlueyDeep,
                    inactiveContainerColor = BlueyPalette.BgElevated,
                    inactiveContentColor = BlueyPalette.InkSoft,
                ),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = if (lang == Language.ZH) "中" else "EN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowSecondsToggle(
    value: Boolean,
    onChange: (Boolean) -> Unit,
    language: Language,
    big: Boolean,
) {
    // Two options: ON / OFF, modeled as a SingleChoiceSegmentedButtonRow so the visual style
    // matches the other rows exactly.
    val options = listOf(true, false)
    val labels = if (language == Language.ZH) listOf("显示秒", "隐藏秒") else listOf("Show s", "Hide s")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.width(if (big) 260.dp else 200.dp),
    ) {
        options.forEachIndexed { idx, opt ->
            val selected = value == opt
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(opt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BlueyPalette.BlueySoft,
                    activeContentColor = BlueyPalette.BlueyDeep,
                    inactiveContainerColor = BlueyPalette.BgElevated,
                    inactiveContentColor = BlueyPalette.InkSoft,
                ),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = labels[idx], fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeToggle(
    value: ClockMode,
    onChange: (ClockMode) -> Unit,
    language: Language,
    big: Boolean,
) {
    val options = listOf(ClockMode.AUTO, ClockMode.MANUAL)
    val labels = if (language == Language.ZH) listOf("自动", "手动") else listOf("Auto", "Manual")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.width(if (big) 260.dp else 200.dp),
    ) {
        options.forEachIndexed { idx, opt ->
            val selected = value == opt
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(opt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BlueyPalette.BlueySoft,
                    activeContentColor = BlueyPalette.BlueyDeep,
                    inactiveContainerColor = BlueyPalette.BgElevated,
                    inactiveContentColor = BlueyPalette.InkSoft,
                ),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = labels[idx], fontWeight = FontWeight.Bold)
            }
        }
    }
}
