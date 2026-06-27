package com.batteria.clockwise.presentation.clock

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.batteria.clockwise.presentation.clock.components.AnalogClockFace
import com.batteria.clockwise.presentation.clock.components.ClockTime
import com.batteria.clockwise.presentation.clock.components.DigitalCard
import com.batteria.clockwise.presentation.clock.components.FormatToggle
import com.batteria.clockwise.presentation.clock.components.LanguageToggle
import com.batteria.clockwise.presentation.clock.components.ModeToggle
import com.batteria.clockwise.presentation.clock.components.ShowSecondsToggle
import com.batteria.clockwise.presentation.clock.components.VoiceGenderToggle
import com.batteria.clockwise.presentation.theme.BlueyPalette
import kotlin.math.min

/**
 * v4.0 — Slimmed-down clock screen.
 *
 * Behavior is identical to v3.7.x: the full analog dial, the chunky digital
 * card, and the 5 toggles. What's new:
 *  - Every visual piece now lives in its own file under
 *    `presentation/clock/components/` so the quiz screen (and future
 *    surfaces) can reuse them without copy-pasting.
 *  - A back IconButton appears in the top-start corner so kids can return
 *    to the game; the system back gesture also navigates back via NavController.
 *  - Behavior preserved: AUTO/MANUAL, drag-to-set, TTS speaker, 12/24h, ZH/EN,
 *    show/hide seconds, voice persona — all wired through ClockViewModel.
 */

@Composable
fun ClockScreen(
    navController: NavController? = null,
    viewModel: ClockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ClockScreenContent(
        state = state,
        onTimeFormatChange = viewModel::setTimeFormat,
        onLanguageChange = viewModel::setLanguage,
        onShowSecondsChange = viewModel::setShowSeconds,
        onVoiceGenderChange = viewModel::setVoiceGender,
        onModeChange = viewModel::setMode,
        onManualDelta = viewModel::addManualSeconds,
        onBack = { navController?.popBackStack() },
    )
}

@Composable
fun ClockScreenContent(
    state: ClockUiState,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onShowSecondsChange: (Boolean) -> Unit,
    onVoiceGenderChange: (VoiceGender) -> Unit,
    onModeChange: (ClockMode) -> Unit,
    onManualDelta: (Float) -> Unit,
    onBack: () -> Unit,
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
                    onVoiceGenderChange = onVoiceGenderChange,
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
                    onVoiceGenderChange = onVoiceGenderChange,
                    onModeChange = onModeChange,
                    onManualDelta = onManualDelta,
                    isTablet = isTablet,
                )
            }

            // Top-start back IconButton (Material 3 FilledTonalIconButton).
            // System back gesture/key also pops the back stack; this is the
            // explicit visual affordance Master asked for.
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 12.dp)
                    .size(48.dp)
                    .testTag("clock_back_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = BlueyPalette.BlueySoft,
                    contentColor = BlueyPalette.BlueyDeep,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (state.language == Language.ZH) "返回游戏" else "Back to game",
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
    onVoiceGenderChange: (VoiceGender) -> Unit,
    onModeChange: (ClockMode) -> Unit,
    onManualDelta: (Float) -> Unit,
    isTablet: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 48.dp else 24.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.10f))

        AnalogClockFace(
            time = if (state.mode == ClockMode.AUTO) ClockTime.Live
                   else ClockTime.Manual(state.manualTotalSeconds),
            showSeconds = state.showSeconds,
            onManualDelta = if (state.mode == ClockMode.MANUAL) onManualDelta else null,
            modifier = Modifier
                .fillMaxWidth(if (isTablet) 0.82f else 0.84f)
                .aspectRatio(1f),
        )

        Spacer(modifier = Modifier.weight(0.18f))

        DigitalCard(state = state, big = isTablet)

        Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 18.dp))

        FormatToggle(value = state.timeFormat, onChange = onTimeFormatChange, big = isTablet)
        Spacer(modifier = Modifier.height(10.dp))
        LanguageToggle(value = state.language, onChange = onLanguageChange, big = isTablet)
        Spacer(modifier = Modifier.height(10.dp))
        ShowSecondsToggle(value = state.showSeconds, onChange = onShowSecondsChange, language = state.language, big = isTablet)
        Spacer(modifier = Modifier.height(10.dp))
        VoiceGenderToggle(value = state.voiceGender, onChange = onVoiceGenderChange, language = state.language, big = isTablet)
        Spacer(modifier = Modifier.height(10.dp))
        ModeToggle(value = state.mode, onChange = onModeChange, language = state.language, big = isTablet)

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
    onVoiceGenderChange: (VoiceGender) -> Unit,
    onModeChange: (ClockMode) -> Unit,
    onManualDelta: (Float) -> Unit,
    isTablet: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 48.dp else 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 48.dp else 20.dp),
    ) {
        Box(
            modifier = Modifier.weight(0.9f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            AnalogClockFace(
                time = if (state.mode == ClockMode.AUTO) ClockTime.Live
                       else ClockTime.Manual(state.manualTotalSeconds),
                showSeconds = state.showSeconds,
                onManualDelta = if (state.mode == ClockMode.MANUAL) onManualDelta else null,
                modifier = Modifier.fillMaxHeight(0.92f).aspectRatio(1f),
            )
        }

        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp)) {
                    FormatToggle(value = state.timeFormat, onChange = onTimeFormatChange, big = isTablet)
                    LanguageToggle(value = state.language, onChange = onLanguageChange, big = isTablet)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp)) {
                    ShowSecondsToggle(value = state.showSeconds, onChange = onShowSecondsChange, language = state.language, big = isTablet)
                    ModeToggle(value = state.mode, onChange = onModeChange, language = state.language, big = isTablet)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp)) {
                    VoiceGenderToggle(value = state.voiceGender, onChange = onVoiceGenderChange, language = state.language, big = isTablet)
                }
            }
        }
    }
}
