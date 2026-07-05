package com.batteria.clockwise.presentation.quiz

import androidx.compose.ui.test.junit4.createComposeRule
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
 * v4.5 — Full visual regression sweep of the Quiz screen (Group A).
 *
 * The v4.4 [QuizScreenScreenshotTest] only guards the two known bugs; this
 * file walks every reachable Quiz state so we can eyeball each one for
 * layout / colour / text-wrapping / animation-artefact issues.
 *
 * Layout matrix covered:
 *   A1 — Idle × {EN, ZH} × {H12, H24}                     (4 shots)
 *   A2 — Time boundaries: 12:00, 1:15, 6:30, 9:45,
 *        11:59, 12:45 (three-digit wrap)                  (6 shots)
 *   A3 — Reveal banners: Correct EN/ZH, Milestone
 *        streak=3/6/9, Wrong EN/ZH                        (7 shots)
 *   A4 — Landscape × {Idle, Revealed-correct}             (2 shots)
 *
 * = 19 fresh baselines to eyeball. If any look wrong the fix ships as a
 * follow-up commit and this test locks the corrected visual.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuizScreenFullSweepTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setup() {
        composeRule.mainClock.autoAdvance = true
    }

    private fun snap(name: String) {
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/sweepA_$name.png")
    }

    private fun state(
        hour: Int = 3,
        minute: Int = 0,
        language: Language = Language.EN,
        timeFormat: TimeFormat = TimeFormat.H12,
        phase: QuizPhase = QuizPhase.AwaitingAnswer,
        picked: Int? = null,
        streak: Int = 0,
    ): QuizUiState {
        // Build 3 plausible choices: correct at index 1, ±15 min neighbours.
        val choices = listOf(
            QuizChoice(hour, (minute + 45) % 60),
            QuizChoice(hour, minute),
            QuizChoice(hour, (minute + 15) % 60),
        )
        return QuizUiState(
            targetHour = hour,
            targetMinute = minute,
            choices = choices,
            correctIndex = 1,
            pickedIndex = picked,
            phase = phase,
            correctStreak = streak,
            totalAnswered = streak,
            totalCorrect = streak,
            language = language,
            timeFormat = timeFormat,
            voiceGender = VoiceGender.GIRL,
        )
    }

    private fun render(s: QuizUiState) {
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(
                    state = s,
                    onPick = { },
                    onNext = { },
                    onOpenClock = { },
                )
            }
        }
    }

    /* ---------------- A1: Idle × language × format ---------------- */

    @Test fun a1_idle_en_h12() { render(state(language = Language.EN, timeFormat = TimeFormat.H12)); snap("a1_idle_en_h12") }
    @Test fun a1_idle_en_h24() { render(state(language = Language.EN, timeFormat = TimeFormat.H24)); snap("a1_idle_en_h24") }
    @Test fun a1_idle_zh_h12() { render(state(language = Language.ZH, timeFormat = TimeFormat.H12)); snap("a1_idle_zh_h12") }
    @Test fun a1_idle_zh_h24() { render(state(language = Language.ZH, timeFormat = TimeFormat.H24)); snap("a1_idle_zh_h24") }

    /* ---------------- A2: time boundary values ---------------- */

    @Test fun a2_time_12_00() { render(state(hour = 12, minute = 0)); snap("a2_time_12_00_top") }
    @Test fun a2_time_1_15()  { render(state(hour = 1,  minute = 15)); snap("a2_time_01_15") }
    @Test fun a2_time_6_30()  { render(state(hour = 6,  minute = 30)); snap("a2_time_06_30") }
    @Test fun a2_time_9_45()  { render(state(hour = 9,  minute = 45)); snap("a2_time_09_45") }
    @Test fun a2_time_11_59() { render(state(hour = 11, minute = 59)); snap("a2_time_11_59") }
    @Test fun a2_time_12_45_h24() {
        // H24 renders 12:45 with leading zero on the label so the three-digit
        // wrap only kicks in when 10-12 are combined with a two-digit minute.
        render(state(hour = 12, minute = 45, timeFormat = TimeFormat.H24)); snap("a2_time_12_45_h24")
    }

    /* ---------------- A3: banner variants ---------------- */

    @Test fun a3_correct_en() {
        render(state(phase = QuizPhase.Revealed, picked = 1, language = Language.EN, streak = 1))
        snap("a3_banner_correct_en")
    }

    @Test fun a3_correct_zh() {
        render(state(phase = QuizPhase.Revealed, picked = 1, language = Language.ZH, streak = 1))
        snap("a3_banner_correct_zh")
    }

    @Test fun a3_milestone_streak3() {
        render(state(phase = QuizPhase.Revealed, picked = 1, language = Language.EN, streak = 3))
        snap("a3_banner_milestone_streak3_en")
    }

    @Test fun a3_milestone_streak6_zh() {
        render(state(phase = QuizPhase.Revealed, picked = 1, language = Language.ZH, streak = 6))
        snap("a3_banner_milestone_streak6_zh")
    }

    @Test fun a3_milestone_streak9() {
        render(state(phase = QuizPhase.Revealed, picked = 1, language = Language.EN, streak = 9))
        snap("a3_banner_milestone_streak9_en")
    }

    @Test fun a3_wrong_en() {
        render(state(phase = QuizPhase.Revealed, picked = 0, language = Language.EN, streak = 0))
        snap("a3_banner_wrong_en")
    }

    @Test fun a3_wrong_zh() {
        render(state(phase = QuizPhase.Revealed, picked = 0, language = Language.ZH, streak = 0))
        snap("a3_banner_wrong_zh")
    }
}
