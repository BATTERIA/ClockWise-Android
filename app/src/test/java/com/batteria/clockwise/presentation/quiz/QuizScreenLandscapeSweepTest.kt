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
 * v4.5 — Group A4 landscape screenshots. Split into a separate class so
 * we can set `qualifiers` (Robolectric device config) once at class level
 * to a landscape-shaped viewport (891×411 dp, phone flipped).
 *
 * QuizScreenContent uses BoxWithConstraints to pick portrait vs landscape,
 * so the layout switch is driven by the container dimensions Roborazzi
 * gives us via the qualifiers string.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w891dp-h411dp-land")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuizScreenLandscapeSweepTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setup() { composeRule.mainClock.autoAdvance = true }

    private fun snap(name: String) {
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/sweepA_$name.png")
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

    private fun state(
        phase: QuizPhase = QuizPhase.AwaitingAnswer,
        picked: Int? = null,
        streak: Int = 0,
        language: Language = Language.EN,
    ) = QuizUiState(
        targetHour = 6,
        targetMinute = 30,
        choices = listOf(QuizChoice(6, 15), QuizChoice(6, 30), QuizChoice(6, 45)),
        correctIndex = 1,
        pickedIndex = picked,
        phase = phase,
        correctStreak = streak,
        totalAnswered = streak,
        totalCorrect = streak,
        language = language,
        timeFormat = TimeFormat.H12,
        voiceGender = VoiceGender.GIRL,
    )

    @Test fun a4_landscape_idle_en() { render(state()); snap("a4_landscape_idle_en") }
    @Test fun a4_landscape_correct_en() {
        render(state(phase = QuizPhase.Revealed, picked = 1, streak = 1))
        snap("a4_landscape_correct_en")
    }
    @Test fun a4_landscape_milestone_zh() {
        render(state(phase = QuizPhase.Revealed, picked = 1, streak = 3, language = Language.ZH))
        snap("a4_landscape_milestone_zh")
    }
}
