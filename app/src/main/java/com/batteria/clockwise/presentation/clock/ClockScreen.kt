package com.batteria.clockwise.presentation.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.batteria.clockwise.presentation.theme.BlueyPalette
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
            .padding(horizontal = if (isTablet) 48.dp else 24.dp),
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
                .fillMaxHeight(),
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

    // Drag state local to the pointerInput closure.
    val dragModifier = if (state.mode == ClockMode.MANUAL) {
        Modifier.pointerInput(Unit) {
            // Per-gesture variables captured in detectDragGestures.
            var grabbedHand: Int = HAND_NONE
            var lastAngle = 0f
            // Center of the drawing area in this pointerInput's local coords.
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val cx = w / 2f
            val cy = h / 2f
            detectDragGestures(
                onDragStart = { offset ->
                    val pointerAngle = pointerAngleDeg(offset.x - cx, offset.y - cy)
                    lastAngle = pointerAngle
                    grabbedHand = pickHand(
                        pointerAngle = pointerAngle,
                        hourAngle = hourAngleState.value,
                        minuteAngle = minuteAngleState.value,
                        secondAngle = secondAngleState.value,
                        showSeconds = showSecondsState.value,
                    )
                    draggingHand = grabbedHand
                },
                onDragEnd   = { grabbedHand = HAND_NONE; draggingHand = HAND_NONE },
                onDragCancel = { grabbedHand = HAND_NONE; draggingHand = HAND_NONE },
                onDrag = { change, _ ->
                    if (grabbedHand == HAND_NONE) return@detectDragGestures
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
                },
            )
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
        val hourSw   = if (draggingHand == HAND_HOUR)   14f * 1.6f else 14f
        val minuteSw = if (draggingHand == HAND_MINUTE) 10f * 1.6f else 10f
        val secondSw = if (draggingHand == HAND_SECOND) 4f  * 1.6f else 4f
        drawHand(angleDeg = hourAngle,   lengthFrac = 0.55f, strokeWidth = hourSw,   color = BlueyPalette.Bandit)
        drawHand(angleDeg = minuteAngle, lengthFrac = 0.78f, strokeWidth = minuteSw, color = BlueyPalette.Bluey)
        if (state.showSeconds) {
            drawHand(angleDeg = secondAngle, lengthFrac = 0.86f, strokeWidth = secondSw, color = BlueyPalette.Chilli)
        }
        drawCenterPin()
        if (state.mode == ClockMode.MANUAL) {
            drawManualBadge()
        }
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

private fun DrawScope.drawManualBadge() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val badgeW = r * 0.42f
    val badgeH = r * 0.12f
    val left = cx - badgeW / 2f
    val top = cy + r * 0.32f
    val bottom = top + badgeH
    drawIntoCanvasCompat { c ->
        val rectPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                235,
                (BlueyPalette.Bingo.red * 255).toInt(),
                (BlueyPalette.Bingo.green * 255).toInt(),
                (BlueyPalette.Bingo.blue * 255).toInt(),
            )
            isAntiAlias = true
        }
        val rectF = android.graphics.RectF(left, top, left + badgeW, bottom)
        c.drawRoundRect(rectF, badgeH / 2f, badgeH / 2f, rectPaint)
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = badgeH * 0.66f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            letterSpacing = 0.18f
        }
        val tx = cx
        val fm = textPaint.fontMetrics
        val ty = (top + bottom) / 2f - (fm.descent + fm.ascent) / 2f
        c.drawText("MANUAL", tx, ty, textPaint)
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
    } else {
        // Manual mode: derive from manualTotalSeconds; no AM/PM info.
        val total = ((state.manualTotalSeconds.toInt() % 43200) + 43200) % 43200
        val h12raw = total / 3600         // 0..11
        val m = (total % 3600) / 60
        val s = total % 60
        when (state.timeFormat) {
            TimeFormat.H24 -> {
                // 24h manual: show 0..11 as-is so the toggle is visibly different from 12h.
                timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h12raw, m, s)
                           else "%02d:%02d".format(h12raw, m)
            }
            TimeFormat.H12 -> {
                val h12 = if (h12raw == 0) 12 else h12raw
                timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h12, m, s)
                           else "%02d:%02d".format(h12, m)
            }
        }
        // Never lie about AM/PM in manual mode.
        periodText = when (state.language) { Language.EN -> "AM"; Language.ZH -> "上午" }
        showPeriod = false
    }

    OutlinedCard(
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (big) 40.dp else 28.dp,
                vertical = if (big) 22.dp else 16.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = timeText,
                fontSize = if (big) 80.sp else 56.sp,
                fontWeight = FontWeight.Bold,
                color = BlueyPalette.Ink,
                style = MaterialTheme.typography.displayLarge,
            )
            // AM/PM animates in/out instead of jumping.
            androidx.compose.animation.AnimatedVisibility(
                visible = showPeriod && periodText.isNotEmpty(),
                enter = androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.slideInVertically(initialOffsetY = { -it / 2 }),
                exit = androidx.compose.animation.fadeOut() +
                       androidx.compose.animation.slideOutVertically(targetOffsetY = { -it / 2 }),
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
    }
}

/* -------------------- segmented toggles -------------------- */

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
            SegmentedButton(
                selected = value == fmt,
                onClick = { onChange(fmt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = TimeFormat.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BlueyPalette.BlueySoft,
                    activeContentColor = BlueyPalette.BlueyDeep,
                    inactiveContainerColor = BlueyPalette.BgElevated,
                    inactiveContentColor = BlueyPalette.InkSoft,
                ),
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
            SegmentedButton(
                selected = value == lang,
                onClick = { onChange(lang) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = Language.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BlueyPalette.BlueySoft,
                    activeContentColor = BlueyPalette.BlueyDeep,
                    inactiveContainerColor = BlueyPalette.BgElevated,
                    inactiveContentColor = BlueyPalette.InkSoft,
                ),
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
            SegmentedButton(
                selected = value == opt,
                onClick = { onChange(opt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BlueyPalette.BlueySoft,
                    activeContentColor = BlueyPalette.BlueyDeep,
                    inactiveContainerColor = BlueyPalette.BgElevated,
                    inactiveContentColor = BlueyPalette.InkSoft,
                ),
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
            SegmentedButton(
                selected = value == opt,
                onClick = { onChange(opt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = BlueyPalette.BlueySoft,
                    activeContentColor = BlueyPalette.BlueyDeep,
                    inactiveContainerColor = BlueyPalette.BgElevated,
                    inactiveContentColor = BlueyPalette.InkSoft,
                ),
            ) {
                Text(text = labels[idx], fontWeight = FontWeight.Bold)
            }
        }
    }
}
