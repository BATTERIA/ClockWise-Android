package com.batteria.clockwise.presentation.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.batteria.clockwise.presentation.clock.ClockMode
import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import com.batteria.clockwise.presentation.clock.VoiceGender
import com.batteria.clockwise.presentation.clock.components.AnalogClockFace
import com.batteria.clockwise.presentation.clock.components.ClockTime
import com.batteria.clockwise.presentation.theme.BlueyPalette
import com.batteria.clockwise.util.SmartTtsManager
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * v4.0 — Quiz / game screen.
 *
 * Master's spec:
 *   - First question is "what time is the clock showing?"
 *   - Random generation, three choices
 *   - Question audio playback (TTS)
 *   - Tapping the clock-icon button opens the full clock workshop screen
 *
 * The clock face is the same [AnalogClockFace] composable used by the full
 * clock screen, just driven by a [ClockTime.Static] so kids can study a
 * frozen target.
 */

private const val QUIZ_ROUTE_CLOCK = "clock"

/* -------------------- v4.2 spring-animation helpers --------------------
 *
 * All UI surfaces share a single "bouncy pop" entrance and a soft exit.
 * Centralising these keeps the screen feeling like one app instead of
 * five different easings glued together. The spring uses
 * DampingRatioMediumBouncy + StiffnessLow which gives the playful
 * overshoot Master asked for without being motion-sickness territory.
 */
private val springFloat = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
)
private val springSize = spring<androidx.compose.ui.unit.IntSize>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
)

/** Bouncy enter — scale-up from 80 % with a fade, both spring-driven. */
private val bounceEnter = scaleIn(
    animationSpec = springFloat,
    initialScale = 0.80f,
) + fadeIn(animationSpec = tween(220))

/** Soft exit — shrink back down + fade. Faster so the next thing can pop in. */
private val softExit = scaleOut(
    animationSpec = tween(durationMillis = 180),
    targetScale = 0.85f,
) + fadeOut(animationSpec = tween(160))

@Composable
fun QuizScreen(
    navController: NavController? = null,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    QuizScreenContent(
        state = state,
        onPick = viewModel::pick,
        onNext = viewModel::nextQuestion,
        onOpenClock = { navController?.navigate(QUIZ_ROUTE_CLOCK) },
        onLanguageChange = viewModel::setLanguage,
    )
}

@Composable
fun QuizScreenContent(
    state: QuizUiState,
    onPick: (Int) -> Unit,
    onNext: () -> Unit,
    onOpenClock: () -> Unit,
    onLanguageChange: (Language) -> Unit = {},
) {
    val context = LocalContext.current
    val tts = remember { SmartTtsManager(context) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    // Auto-advance to the next question ~1.4s after a correct answer.
    LaunchedEffect(state.phase, state.pickedIndex, state.totalAnswered) {
        if (state.phase == QuizPhase.Revealed && state.pickedIndex == state.correctIndex) {
            delay(1400)
            onNext()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quiz_screen"),
        color = BlueyPalette.Bg,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            val isLandscape = maxWidth > maxHeight
            val isTablet = min(maxWidth.value, maxHeight.value) >= 600f

            if (isLandscape) {
                LandscapeQuizLayout(
                    state = state,
                    onPick = onPick,
                    onNext = onNext,
                    onSpeak = { speakTime(tts, state) },
                    isTablet = isTablet,
                )
            } else {
                PortraitQuizLayout(
                    state = state,
                    onPick = onPick,
                    onNext = onNext,
                    onSpeak = { speakTime(tts, state) },
                    isTablet = isTablet,
                )
            }

            // Top-end controls: language pill + clock-workshop IconButton.
            // Two Material 3 buttons sitting in a Row, anchored TopEnd, so
            // they slide together with the system insets. v4.2 — the whole
            // row pops in on first composition for the bouncy entrance.
            AnimatedVisibility(
                visible = true,
                enter = bounceEnter,
                exit = softExit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // v4.1 — language flip. FilledTonalIconButton with a glyph
                    // showing the *other* language (tap-to-switch convention).
                    val otherLang = if (state.language == Language.ZH) Language.EN else Language.ZH
                    // v4.2 — AnimatedContent swaps the glyph with a spring scale so
                    // the user *sees* the flip rather than the letter just morphing.
                    AnimatedContent(
                        targetState = otherLang,
                        transitionSpec = {
                            (scaleIn(springFloat, initialScale = 0.6f) + fadeIn(tween(200)))
                                .togetherWith(
                                    scaleOut(tween(160), targetScale = 0.6f) + fadeOut(tween(160))
                                )
                        },
                        label = "language_glyph",
                    ) { dest ->
                        val glyph = if (dest == Language.ZH) "中" else "EN"
                        FilledTonalIconButton(
                            onClick = { onLanguageChange(dest) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quiz_language_toggle"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = BlueyPalette.BlueySoft,
                                contentColor = BlueyPalette.BlueyDeep,
                            ),
                        ) {
                            // Text glyph instead of a vector icon — '中' / 'EN'
                            // communicates the destination language more directly
                            // to a 4–7-year-old than a globe would.
                            Text(
                                text = glyph,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (glyph.length == 1) 18.sp else 14.sp,
                            )
                        }
                    }

                    // Material 3 FilledTonalIconButton, matches the back button
                    // on the clock screen for visual symmetry.
                    FilledTonalIconButton(
                        onClick = onOpenClock,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("quiz_open_clock_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = BlueyPalette.BlueySoft,
                            contentColor = BlueyPalette.BlueyDeep,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = QuizSpeech.openClockHint(state.language),
                        )
                    }
                }
            }
        }
    }
}

/* -------------------- portrait layout -------------------- */

@Composable
private fun PortraitQuizLayout(
    state: QuizUiState,
    onPick: (Int) -> Unit,
    onNext: () -> Unit,
    onSpeak: () -> Unit,
    isTablet: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 48.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.12f))
        AnimatedVisibility(visible = true, enter = bounceEnter, exit = softExit) {
            QuestionHeader(state = state, onSpeak = onSpeak, big = isTablet)
        }
        Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))

        // v4.2 — swap the clock face with a spring scale when the question
        // changes so the new time pops in instead of jump-cutting.
        // v4.4 — the AnalogClockFace shadow uses CircleShape now, so the
        // spring scale no longer produces a square-edge cut around the dial.
        AnimatedContent(
            targetState = state.targetHour to state.targetMinute,
            transitionSpec = {
                (scaleIn(springFloat, initialScale = 0.85f) + fadeIn(tween(240)))
                    .togetherWith(
                        scaleOut(tween(180), targetScale = 0.92f) + fadeOut(tween(160))
                    )
            },
            label = "quiz_clock_swap",
            modifier = Modifier
                .fillMaxWidth(if (isTablet) 0.66f else 0.78f)
                .aspectRatio(1f),
        ) { (h, m) ->
            AnalogClockFace(
                time = ClockTime.Static(hour = h, minute = m),
                showSeconds = false,
                onManualDelta = null,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("quiz_clock_face"),
            )
        }

        Spacer(modifier = Modifier.weight(0.06f))

        ChoicesGrid(
            state = state,
            onPick = onPick,
            big = isTablet,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))
        ResultBanner(state = state, onNext = onNext, big = isTablet)
        Spacer(modifier = Modifier.weight(0.10f))
    }
}

/* -------------------- landscape layout -------------------- */

@Composable
private fun LandscapeQuizLayout(
    state: QuizUiState,
    onPick: (Int) -> Unit,
    onNext: () -> Unit,
    onSpeak: () -> Unit,
    isTablet: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isTablet) 48.dp else 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 40.dp else 20.dp),
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = state.targetHour to state.targetMinute,
                transitionSpec = {
                    (scaleIn(springFloat, initialScale = 0.85f) + fadeIn(tween(240)))
                        .togetherWith(
                            scaleOut(tween(180), targetScale = 0.92f) + fadeOut(tween(160))
                        )
                },
                label = "quiz_clock_swap_land",
                modifier = Modifier
                    .fillMaxHeight(0.86f)
                    .aspectRatio(1f),
            ) { (h, m) ->
                AnalogClockFace(
                    time = ClockTime.Static(hour = h, minute = m),
                    showSeconds = false,
                    onManualDelta = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("quiz_clock_face"),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1.1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(visible = true, enter = bounceEnter, exit = softExit) {
                QuestionHeader(state = state, onSpeak = onSpeak, big = isTablet)
            }
            Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 14.dp))
            ChoicesGrid(
                state = state,
                onPick = onPick,
                big = isTablet,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(if (isTablet) 16.dp else 12.dp))
            ResultBanner(state = state, onNext = onNext, big = isTablet)
        }
    }
}

/* -------------------- question header (text + speaker) -------------------- */

@Composable
private fun QuestionHeader(state: QuizUiState, onSpeak: () -> Unit, big: Boolean) {
    Card(
        shape = RoundedCornerShape(percent = 50),
        colors = CardDefaults.cardColors(
            containerColor = BlueyPalette.BgElevated,
            contentColor = BlueyPalette.Ink,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = if (big) 22.dp else 16.dp, vertical = if (big) 14.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (big) 14.dp else 10.dp),
        ) {
            Text(
                text = QuizSpeech.question(state.language),
                fontSize = if (big) 26.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = BlueyPalette.Ink,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            FilledIconButton(
                onClick = onSpeak,
                modifier = Modifier
                    .size(if (big) 44.dp else 36.dp)
                    .testTag("quiz_speaker_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = BlueyPalette.Bluey.copy(alpha = 0.18f),
                    contentColor = BlueyPalette.Bluey,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (state.language == Language.ZH) "播报题目" else "Read question",
                    modifier = Modifier.size(if (big) 24.dp else 20.dp),
                )
            }
        }
    }
}

/* -------------------- 3-choice answer grid -------------------- */

@Composable
private fun ChoicesGrid(
    state: QuizUiState,
    onPick: (Int) -> Unit,
    big: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (big) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.choices.forEachIndexed { idx, choice ->
            val isPicked = state.pickedIndex == idx
            val isCorrect = idx == state.correctIndex
            val revealed = state.phase == QuizPhase.Revealed
            val tag = "quiz_choice_${idx}"

            // Pop the picked button briefly using a bouncy spring.
            val scale by animateFloatAsState(
                targetValue = if (isPicked && revealed) 1.05f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "choice_scale_$idx",
            )
            val container: Color
            val content: Color
            when {
                revealed && isCorrect -> {
                    container = BlueyPalette.Mint.copy(alpha = 0.85f)
                    content = BlueyPalette.Ink
                }
                revealed && isPicked && !isCorrect -> {
                    container = BlueyPalette.Chilli.copy(alpha = 0.18f)
                    content = BlueyPalette.Chilli
                }
                else -> {
                    container = BlueyPalette.BlueySoft
                    content = BlueyPalette.BlueyDeep
                }
            }

            // v4.2 — spring-pop each choice into view, staggered ~70 ms apart
            // so the three options cascade in rather than slamming together.
            // Using the RowScope overload directly with .weight(1f) so the
            // animated container itself participates in the Row layout.
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                    initialScale = 0.5f,
                ) + fadeIn(animationSpec = tween(durationMillis = 240, delayMillis = idx * 70)),
                exit = softExit,
                modifier = Modifier.weight(1f),
            ) {
                ElevatedButton(
                    onClick = { if (state.phase == QuizPhase.AwaitingAnswer) onPick(idx) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (big) 76.dp else 60.dp)
                        .scale(scale)
                        .testTag(tag),
                    // v4.5 — fixed 22.dp corners; the previous percent-based
                    // shape recomputed radius from the live scaled bounds, so
                    // the pop-up spring briefly flattened the pill mid-frame.
                    shape = RoundedCornerShape(22.dp),
                    colors = androidx.compose.material3.ButtonDefaults.elevatedButtonColors(
                        containerColor = container,
                        contentColor = content,
                    ),
                ) {
                    Text(
                        text = choice.display(state.timeFormat),
                        fontSize = if (big) 26.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/* -------------------- result + "next" CTA -------------------- */

private enum class ResultVariant { Idle, Correct, Milestone, Wrong }

@Composable
private fun ResultBanner(state: QuizUiState, onNext: () -> Unit, big: Boolean) {
    // v4.3 — three distinct revealed variants:
    //  · Correct       → Bluey-style "Wackadoo!" banner (mint card)
    //  · Milestone     → every Nth correct streak "连对 N 次！要升级啦 ✨"
    //                    banner painted in sun/yellow so it pops
    //  · Wrong         → Bluey "差一点点～" / "Sooo close!" FAB
    // AnimatedContent’s targetState now carries the chosen variant so the
    // spring-scale transition fires when we hop between them too.
    val milestone = QuizSpeech.correctMilestone(state.correctStreak, state.language)
    val variant: ResultVariant = when {
        state.phase != QuizPhase.Revealed -> ResultVariant.Idle
        state.pickedIndex != state.correctIndex -> ResultVariant.Wrong
        milestone != null -> ResultVariant.Milestone
        else -> ResultVariant.Correct
    }

    // v4.2 — swap with a springy scale-in instead of a flat fade so the
    // banner / FAB *pops* the moment the kid commits. Exit shrinks softly.
    AnimatedContent(
        targetState = variant,
        transitionSpec = {
            (scaleIn(springFloat, initialScale = 0.6f) + fadeIn(tween(220)))
                .togetherWith(
                    scaleOut(tween(180), targetScale = 0.85f) + fadeOut(tween(160))
                )
        },
        label = "result_banner",
    ) { v ->
        when (v) {
            ResultVariant.Correct -> {
                // v4.4 — ditched ElevatedCard. M3 cards paint a tonal-elevation
                // overlay across the *whole* card shape, which read as a square
                // background block behind the banner text on Master's device.
                // A plain Box with a translucent mint fill + rounded corners
                // sits cleanly on the cream background with no rectangle halo.
                Box(
                    modifier = Modifier
                        // v4.5 — CircleShape keeps the pill perfectly rounded
                        // through the AnimatedContent scale spring (0.6 → 1).
                        // percent-based shapes recalc radius from the live
                        // bounds and briefly show a squared-off edge.
                        .clip(CircleShape)
                        .background(BlueyPalette.Mint.copy(alpha = 0.18f))
                        .padding(
                            horizontal = if (big) 24.dp else 18.dp,
                            vertical = if (big) 16.dp else 12.dp,
                        )
                        .testTag("quiz_result_correct"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (big) 12.dp else 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = BlueyPalette.Mint,
                            modifier = Modifier.size(if (big) 32.dp else 24.dp),
                        )
                        Text(
                            text = QuizSpeech.correct(state.language),
                            fontSize = if (big) 22.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueyPalette.Ink,
                        )
                    }
                }
            }
            ResultVariant.Milestone -> {
                // “连对 N 次！要升级啦” — sun-yellow card to set it apart from the
                // mint everyday card. Same flat-Box treatment as Correct so the
                // sun-yellow halo doesn't read as a rectangle.
                Box(
                    modifier = Modifier
                        // v4.5 — same reasoning as the Correct banner above.
                        .clip(CircleShape)
                        .background(BlueyPalette.Sun.copy(alpha = 0.28f))
                        .padding(
                            horizontal = if (big) 28.dp else 22.dp,
                            vertical = if (big) 18.dp else 14.dp,
                        )
                        .testTag("quiz_result_milestone"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (big) 12.dp else 10.dp),
                    ) {
                        Text(
                            text = "🄯",
                            fontSize = if (big) 32.sp else 26.sp,
                        )
                        // milestone is non-null in this branch by construction.
                        Text(
                            text = milestone
                                ?: QuizSpeech.correct(state.language),
                            fontSize = if (big) 22.sp else 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BlueyPalette.Ink,
                        )
                    }
                }
            }
            ResultVariant.Wrong -> {
                // v4.2 — dropped the leading "→" arrow icon + arrow glyph per
                // Master's request. The FAB itself is the affordance; the
                // wording "差一点点～ · 下一题" / "Sooo close! · Next question"
                // reads cleaner.
                ExtendedFloatingActionButton(
                    onClick = onNext,
                    containerColor = BlueyPalette.Bluey,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("quiz_next_button"),
                ) {
                    Text(
                        text = "${QuizSpeech.wrong(state.language)}  ·  ${QuizSpeech.nextQuestion(state.language)}",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            ResultVariant.Idle -> {
                Spacer(modifier = Modifier.height(if (big) 56.dp else 44.dp))
            }
        }
    }
}

/* -------------------- helpers -------------------- */

private fun speakTime(tts: SmartTtsManager, state: QuizUiState) {
    // Speak the current question's target time using the smart pre-recorded
    // pack (girl/boy voice, ZH/EN, 12h/24h aware). MANUAL mode is used so we
    // skip the "现在是" / "It's" lead-in — the kid is asking the clock, not
    // being told what time it is.
    val h = state.targetHour
    val m = state.targetMinute
    tts.speakTime(
        hour = h,
        minute = m,
        second = 0,
        includeSeconds = false,
        format = state.timeFormat,
        isPm = false,
        language = state.language,
        gender = state.voiceGender,
        mode = ClockMode.MANUAL,
    )
}
