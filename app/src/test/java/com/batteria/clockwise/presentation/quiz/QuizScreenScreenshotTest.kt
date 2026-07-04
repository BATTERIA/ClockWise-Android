package com.batteria.clockwise.presentation.quiz

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import com.batteria.clockwise.presentation.clock.VoiceGender
import com.batteria.clockwise.presentation.theme.ClockWiseTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * v4.4 — Visual regression screenshot tests for the QuizScreen.
 *
 * These run on the JVM via Robolectric Native Graphics (no emulator
 * needed), so we can self-verify the two bugs Master reported in
 * 2026-06-28:
 *
 *   1. A square "background block" visible behind the time / behind the
 *      result banner text when picking an option.
 *   2. A "cut" edge artefact around the clock when switching to the
 *      next question (shadow being scaled with the swap animation).
 *
 * Screenshots are written under
 *     app/build/outputs/roborazzi/<TestName>__<displayName>.png
 * Capture *during* the AnimatedContent transition so we actually see
 * the artefacts rather than the final settled state.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuizScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun pauseClock() {
        // v4.4 — autoAdvance ON so AnimatedContent enter/exit transitions
        // actually progress. We still use mainClock.advanceTimeBy(..) to
        // park at a specific point inside a transition.
        composeRule.mainClock.autoAdvance = true
    }

    /** Inflate one composition frame and let pending snapshot writes settle. */
    private fun frame(times: Int = 1) {
        repeat(times) {
            composeRule.waitForIdle()
        }
    }

    private fun baseState(
        phase: QuizPhase = QuizPhase.AwaitingAnswer,
        picked: Int? = null,
        correctIndex: Int = 1,
        choices: List<QuizChoice> = listOf(
            QuizChoice(10, 0),
            QuizChoice(10, 15),
            QuizChoice(10, 30),
        ),
        targetHour: Int = 10,
        targetMinute: Int = 15,
        correctStreak: Int = 0,
    ) = QuizUiState(
        phase = phase,
        choices = choices,
        correctIndex = correctIndex,
        pickedIndex = picked,
        targetHour = targetHour,
        targetMinute = targetMinute,
        language = Language.EN,
        timeFormat = TimeFormat.H12,
        voiceGender = VoiceGender.GIRL,
        correctStreak = correctStreak,
        totalAnswered = correctStreak,
        totalCorrect = correctStreak,
    )

    /** Bug 1 — square block behind result banner + clock during pick reveal. */
    @Test
    fun bug1_optionRevealed_shouldNotShowSquareBackgroundBlock() {
        val state = mutableStateOf(baseState(phase = QuizPhase.AwaitingAnswer))
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(
                    state = state.value,
                    onPick = { },
                    onNext = { },
                    onOpenClock = { },
                )
            }
        }
        // Settle initial composition.
        frame(5)
        // Now reveal correct pick — banner + colour swap should pop in.
        state.value = state.value.copy(phase = QuizPhase.Revealed, pickedIndex = 1)
        // Capture mid-pop (~100 ms into the spring) so the banner is visibly
        // mid-scale, where the background-block artefact is at its worst.
        composeRule.mainClock.advanceTimeBy(100L)
        frame(2)
        composeRule.onRoot().captureRoboImage(
            "build/outputs/roborazzi/bug1_correct_pick_reveal.png"
        )
        // Also capture wrong pick — sees the FAB banner background block.
        state.value = state.value.copy(phase = QuizPhase.Revealed, pickedIndex = 0)
        composeRule.mainClock.advanceTimeBy(100L)
        frame(2)
        composeRule.onRoot().captureRoboImage(
            "build/outputs/roborazzi/bug1_wrong_pick_reveal.png"
        )
    }

    /** Bug 2 — shadow "cut" edge during clock-swap AnimatedContent transition. */
    @Test
    fun bug2_clockSwap_shouldNotShowShadowCutEdge() {
        val state = mutableStateOf(baseState(targetHour = 10, targetMinute = 15))
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(
                    state = state.value,
                    onPick = { },
                    onNext = { },
                    onOpenClock = { },
                )
            }
        }
        frame(5)
        // Trigger clock swap — new (hour, minute) drives AnimatedContent.
        state.value = state.value.copy(targetHour = 3, targetMinute = 45)
        // Capture mid-transition (~120 ms into the 240 ms enter/exit window).
        composeRule.mainClock.advanceTimeBy(120L)
        frame(2)
        composeRule.onRoot().captureRoboImage(
            "build/outputs/roborazzi/bug2_clock_swap_midtransition.png"
        )
        // And captured a settled frame too for comparison.
        composeRule.mainClock.advanceTimeBy(2000L)
        frame(3)
        composeRule.onRoot().captureRoboImage(
            "build/outputs/roborazzi/bug2_clock_swap_settled.png"
        )
    }

    /** Idle baseline so the diff between bugs is obvious. */
    @Test
    fun baseline_idleQuiz() {
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(
                    state = baseState(),
                    onPick = { },
                    onNext = { },
                    onOpenClock = { },
                )
            }
        }
        frame(5)
        composeRule.onRoot().captureRoboImage(
            "build/outputs/roborazzi/baseline_idle.png"
        )
    }
}
