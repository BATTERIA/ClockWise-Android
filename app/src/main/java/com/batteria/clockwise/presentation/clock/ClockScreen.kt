package com.batteria.clockwise.presentation.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.batteria.clockwise.presentation.theme.BlueyPalette
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
    )
}

@Composable
fun ClockScreenContent(
    state: ClockUiState,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onShowSecondsChange: (Boolean) -> Unit,
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
                    isTablet = isTablet,
                )
            } else {
                PortraitLayout(
                    state = state,
                    onTimeFormatChange = onTimeFormatChange,
                    onLanguageChange = onLanguageChange,
                    onShowSecondsChange = onShowSecondsChange,
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
    isTablet: Boolean,
) {
    // Golden ratio: clock CENTER lives at ~38.2% from top.
    // Achieved with weighted spacers above/below the clock region.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 48.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.10f))

        // Clock — its visual center should sit at ~38.2% of total height
        AnalogClock(
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
        ShowSecondsRow(
            value = state.showSeconds,
            onChange = onShowSecondsChange,
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
    isTablet: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 48.dp else 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 48.dp else 24.dp),
    ) {
        // Left: large analog clock
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            AnalogClock(
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(1f),
            )
        }

        // Right: digital + toggles stacked
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
            ShowSecondsRow(
                value = state.showSeconds,
                onChange = onShowSecondsChange,
                language = state.language,
                big = isTablet,
            )
        }
    }
}

/* -------------------- analog clock (Canvas) -------------------- */

@Composable
private fun AnalogClock(modifier: Modifier = Modifier) {
    // Use a ticking time source; recompose every frame.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nowMs = System.currentTimeMillis() }
        }
    }
    val cal = remember { Calendar.getInstance() }
    cal.timeInMillis = nowMs
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    val s = cal.get(Calendar.SECOND)
    val ms = cal.get(Calendar.MILLISECOND)

    val hourAngle = ((h % 12) + m / 60f) * 30f
    val minuteAngle = (m + s / 60f) * 6f
    val secondAngle = (s + ms / 1000f) * 6f

    Canvas(
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(percent = 50),
            ambientColor = BlueyPalette.Ink,
            spotColor = BlueyPalette.Ink,
        ).background(color = BlueyPalette.BgElevated, shape = RoundedCornerShape(percent = 50)),
    ) {
        drawClockFace()
        drawTicks()
        drawNumbers()
        drawHand(angleDeg = hourAngle,   lengthFrac = 0.55f, strokeWidth = 14f, color = BlueyPalette.Bandit)
        drawHand(angleDeg = minuteAngle, lengthFrac = 0.78f, strokeWidth = 10f, color = BlueyPalette.Bluey)
        drawHand(angleDeg = secondAngle, lengthFrac = 0.86f, strokeWidth = 4f,  color = BlueyPalette.Chilli)
        drawCenterPin()
    }
}

private fun DrawScope.drawClockFace() {
    val r = size.minDimension / 2f
    // Outline ring
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
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nowMs = System.currentTimeMillis() }
        }
    }
    val cal = remember { Calendar.getInstance() }
    cal.timeInMillis = nowMs
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    val s = cal.get(Calendar.SECOND)

    val timeText: String
    val periodText: String
    when (state.timeFormat) {
        TimeFormat.H24 -> {
            timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h, m, s)
                       else "%02d:%02d".format(h, m)
            periodText = ""
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
        }
    }

    OutlinedCard(
        modifier = Modifier
            .padding(horizontal = 4.dp),
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
            if (periodText.isNotEmpty()) {
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

/** M3 Switch row — binary toggle for show-seconds. */
@Composable
private fun ShowSecondsRow(
    value: Boolean,
    onChange: (Boolean) -> Unit,
    language: Language,
    big: Boolean,
) {
    Row(
        modifier = Modifier.width(if (big) 260.dp else 200.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (language == Language.ZH) "显示秒数" else "Show seconds",
            fontWeight = FontWeight.Bold,
            color = BlueyPalette.Ink,
            fontSize = if (big) 16.sp else 14.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BlueyPalette.Bluey,
                checkedBorderColor = BlueyPalette.BlueyDeep,
                uncheckedThumbColor = BlueyPalette.InkSoft,
                uncheckedTrackColor = BlueyPalette.BgElevated,
                uncheckedBorderColor = BlueyPalette.Outline,
            ),
        )
    }
}
