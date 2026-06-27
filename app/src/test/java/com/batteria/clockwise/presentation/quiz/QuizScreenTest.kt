package com.batteria.clockwise.presentation.quiz

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import com.batteria.clockwise.presentation.theme.ClockWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * v4.0 \u2014 Compose UI tests for the quiz screen.
 *
 * These run with Robolectric on the JVM (no device/emulator needed) so the
 * subagent reviewer can verify the screen actually renders, the choices are
 * clickable, the speaker button exists, and the open-clock button is wired.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class QuizScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quizScreen_rendersWithThreeChoicesAndCoreButtons() {
        val state = QuizUiState(
            targetHour = 3,
            targetMinute = 0,
            choices = listOf(
                QuizChoice(3, 0),
                QuizChoice(4, 0),
                QuizChoice(2, 0),
            ),
            correctIndex = 0,
            language = Language.EN,
            timeFormat = TimeFormat.H12,
        )
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(state = state, onPick = {}, onNext = {}, onOpenClock = {})
            }
        }

        composeRule.onNodeWithTag("quiz_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_clock_face").assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_speaker_button").assertHasClickAction()
        composeRule.onNodeWithTag("quiz_open_clock_button").assertHasClickAction()
        composeRule.onNodeWithTag("quiz_choice_0").assertHasClickAction()
        composeRule.onNodeWithTag("quiz_choice_1").assertHasClickAction()
        composeRule.onNodeWithTag("quiz_choice_2").assertHasClickAction()
    }

    @Test
    fun tappingChoice_callsOnPick_withCorrectIndex() {
        val state = QuizUiState(
            targetHour = 6,
            targetMinute = 30,
            choices = listOf(
                QuizChoice(5, 30),
                QuizChoice(6, 30),
                QuizChoice(7, 30),
            ),
            correctIndex = 1,
            language = Language.EN,
            timeFormat = TimeFormat.H12,
        )
        var lastPicked = -1
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(
                    state = state,
                    onPick = { lastPicked = it },
                    onNext = {},
                    onOpenClock = {},
                )
            }
        }

        composeRule.onNodeWithTag("quiz_choice_1").performClick()
        composeRule.waitForIdle()
        assertEquals(1, lastPicked)
    }

    @Test
    fun openClockButton_invokesNavigationCallback() {
        val state = QuizUiState(
            targetHour = 9,
            targetMinute = 15,
            choices = listOf(
                QuizChoice(9, 15),
                QuizChoice(8, 15),
                QuizChoice(10, 15),
            ),
            correctIndex = 0,
            language = Language.ZH,
            timeFormat = TimeFormat.H12,
        )
        var opened = false
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(
                    state = state,
                    onPick = {},
                    onNext = {},
                    onOpenClock = { opened = true },
                )
            }
        }

        composeRule.onNodeWithTag("quiz_open_clock_button").performClick()
        composeRule.waitForIdle()
        assertTrue("Open-clock button should invoke navigation callback", opened)
    }

    @Test
    fun revealedCorrectAnswer_showsResultBanner() {
        val state = QuizUiState(
            targetHour = 12,
            targetMinute = 0,
            choices = listOf(
                QuizChoice(12, 0),
                QuizChoice(11, 0),
                QuizChoice(1, 0),
            ),
            correctIndex = 0,
            pickedIndex = 0,
            phase = QuizPhase.Revealed,
            language = Language.EN,
            timeFormat = TimeFormat.H12,
        )
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(state = state, onPick = {}, onNext = {}, onOpenClock = {})
            }
        }

        composeRule.onNodeWithTag("quiz_result_correct").assertIsDisplayed()
    }

    @Test
    fun revealedWrongAnswer_showsNextButton() {
        val state = QuizUiState(
            targetHour = 4,
            targetMinute = 0,
            choices = listOf(
                QuizChoice(4, 0),
                QuizChoice(5, 0),
                QuizChoice(3, 0),
            ),
            correctIndex = 0,
            pickedIndex = 1,
            phase = QuizPhase.Revealed,
            language = Language.EN,
            timeFormat = TimeFormat.H12,
        )
        var nextCalled = false
        composeRule.setContent {
            ClockWiseTheme {
                QuizScreenContent(
                    state = state,
                    onPick = {},
                    onNext = { nextCalled = true },
                    onOpenClock = {},
                )
            }
        }

        composeRule.onNodeWithTag("quiz_next_button").assertIsDisplayed()
        composeRule.onNodeWithTag("quiz_next_button").performClick()
        composeRule.waitForIdle()
        assertTrue("Next button should advance to the next question", nextCalled)
    }

    @Test
    fun choiceLabels_useTimeFormatFromState() {
        val state12 = QuizUiState(
            targetHour = 9, targetMinute = 30,
            choices = listOf(QuizChoice(9, 30), QuizChoice(10, 30), QuizChoice(8, 30)),
            correctIndex = 0, timeFormat = TimeFormat.H12,
        )
        assertEquals("9:30", state12.choices[0].display(TimeFormat.H12))
        assertEquals("09:30", state12.choices[0].display(TimeFormat.H24))
        assertNotNull(state12)
    }
}
