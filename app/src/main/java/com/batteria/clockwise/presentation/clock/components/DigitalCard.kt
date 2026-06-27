package com.batteria.clockwise.presentation.clock.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batteria.clockwise.presentation.clock.ClockMode
import com.batteria.clockwise.presentation.clock.ClockUiState
import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import com.batteria.clockwise.presentation.theme.BlueyPalette
import com.batteria.clockwise.util.SmartTtsManager
import java.util.Calendar

/**
 * v4.0 — Decoupled digital readout card.
 *
 * Renders the chunky HH:MM(:SS) digits + optional AM/PM badge + speaker
 * button. Same visual treatment as the full-feature clock; extracted so
 * other surfaces (e.g. quiz "reveal answer" sheets) can reuse it.
 */
@Composable
fun DigitalCard(
    state: ClockUiState,
    big: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tts = remember { SmartTtsManager(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

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
    val showPeriod: Boolean
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
        val total = ((state.manualTotalSeconds.toInt() % 43200) + 43200) % 43200
        val h12raw = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        when (state.timeFormat) {
            TimeFormat.H24 -> {
                timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h12raw, m, s)
                           else "%02d:%02d".format(h12raw, m)
                periodText = when (state.language) { Language.EN -> "AM"; Language.ZH -> "上午" }
                showPeriod = false
            }
            TimeFormat.H12 -> {
                val h12 = if (h12raw == 0) 12 else h12raw
                timeText = if (state.showSeconds) "%02d:%02d:%02d".format(h12, m, s)
                           else "%02d:%02d".format(h12, m)
                periodText = ""
                showPeriod = false
            }
        }
        speakH = when (state.timeFormat) {
            TimeFormat.H24 -> h12raw
            TimeFormat.H12 -> if (h12raw == 0) 12 else h12raw
        }
        speakM = m
        speakS = s
        speakIsPm = false
    }

    OutlinedCard(
        modifier = modifier.padding(horizontal = 4.dp).width(
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (big) 16.dp else 12.dp,
                    vertical = if (big) 22.dp else 16.dp,
                )
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
                letterSpacing = 0.sp,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(if (big) 6.dp else 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = showPeriod && periodText.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(180)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = periodText,
                            fontSize = if (big) 18.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueyPalette.BlueyDeep,
                        )
                        Spacer(modifier = Modifier.width(if (big) 10.dp else 8.dp))
                    }
                }
                FilledIconButton(
                    onClick = {
                        tts.speakTime(
                            hour = speakH,
                            minute = speakM,
                            second = speakS,
                            includeSeconds = false,
                            format = state.timeFormat,
                            isPm = speakIsPm,
                            language = state.language,
                            gender = state.voiceGender,
                            mode = state.mode,
                        )
                    },
                    modifier = Modifier.size(if (big) 40.dp else 32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = BlueyPalette.Bluey.copy(alpha = 0.15f),
                        contentColor = BlueyPalette.Bluey,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (state.language == Language.ZH) "播报时间" else "Speak time",
                        modifier = Modifier.size(if (big) 22.dp else 18.dp),
                    )
                }
            }
        }
    }
}
