package com.batteria.clockwise.presentation.clock.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.batteria.clockwise.presentation.clock.ClockMode
import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import com.batteria.clockwise.presentation.clock.VoiceGender
import com.batteria.clockwise.presentation.theme.BlueyPalette

/**
 * v4.0 — Decoupled segmented-button toggles for the clock settings.
 *
 * All five toggles share the same Material 3 SingleChoiceSegmentedButtonRow
 * look + the Bluey palette + a small bouncy "selected" scale-up. They live
 * together because they all share that styling helper, but each composable
 * has a clean single-responsibility API (state in, callback out).
 */

@Composable
internal fun rememberSegmentScale(selected: Boolean): State<Float> =
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
fun FormatToggle(
    value: TimeFormat,
    onChange: (TimeFormat) -> Unit,
    big: Boolean,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.width(if (big) 260.dp else 200.dp)) {
        TimeFormat.entries.forEachIndexed { idx, fmt ->
            val selected = value == fmt
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(fmt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = TimeFormat.entries.size),
                colors = bandiColors(),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = if (fmt == TimeFormat.H12) "12h" else "24h", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageToggle(
    value: Language,
    onChange: (Language) -> Unit,
    big: Boolean,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.width(if (big) 260.dp else 200.dp)) {
        Language.entries.forEachIndexed { idx, lang ->
            val selected = value == lang
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(lang) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = Language.entries.size),
                colors = bandiColors(),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = if (lang == Language.ZH) "中" else "EN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowSecondsToggle(
    value: Boolean,
    onChange: (Boolean) -> Unit,
    language: Language,
    big: Boolean,
    modifier: Modifier = Modifier,
) {
    val options = listOf(true, false)
    val labels = if (language == Language.ZH) listOf("显示秒", "隐藏秒") else listOf("Show s", "Hide s")
    SingleChoiceSegmentedButtonRow(modifier = modifier.width(if (big) 260.dp else 200.dp)) {
        options.forEachIndexed { idx, opt ->
            val selected = value == opt
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(opt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                colors = bandiColors(),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = labels[idx], fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeToggle(
    value: ClockMode,
    onChange: (ClockMode) -> Unit,
    language: Language,
    big: Boolean,
    modifier: Modifier = Modifier,
) {
    val options = listOf(ClockMode.AUTO, ClockMode.MANUAL)
    val labels = if (language == Language.ZH) listOf("自动", "手动") else listOf("Auto", "Manual")
    SingleChoiceSegmentedButtonRow(modifier = modifier.width(if (big) 260.dp else 200.dp)) {
        options.forEachIndexed { idx, opt ->
            val selected = value == opt
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(opt) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                colors = bandiColors(),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = labels[idx], fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceGenderToggle(
    value: VoiceGender,
    onChange: (VoiceGender) -> Unit,
    language: Language,
    big: Boolean,
    modifier: Modifier = Modifier,
) {
    val options = VoiceGender.entries
    val labelsZh = mapOf(VoiceGender.GIRL to "👧 小姐姐", VoiceGender.BOY to "👦 小哥哥")
    val labelsEn = mapOf(VoiceGender.GIRL to "👧 Girl", VoiceGender.BOY to "👦 Boy")
    val labels = if (language == Language.ZH) labelsZh else labelsEn
    SingleChoiceSegmentedButtonRow(modifier = modifier.width(if (big) 260.dp else 200.dp)) {
        options.forEachIndexed { idx, g ->
            val selected = value == g
            val scale by rememberSegmentScale(selected)
            SegmentedButton(
                selected = selected,
                onClick = { onChange(g) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                colors = bandiColors(),
                modifier = Modifier.scale(scale),
            ) {
                Text(text = labels[g] ?: "", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun bandiColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = BlueyPalette.BlueySoft,
    activeContentColor = BlueyPalette.BlueyDeep,
    inactiveContainerColor = BlueyPalette.BgElevated,
    inactiveContentColor = BlueyPalette.InkSoft,
)
