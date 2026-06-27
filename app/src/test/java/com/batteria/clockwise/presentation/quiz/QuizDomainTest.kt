package com.batteria.clockwise.presentation.quiz

import com.batteria.clockwise.presentation.clock.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v4.0 — Pure-Kotlin unit tests for the quiz domain primitives.
 *
 * These don't need Compose / Robolectric so they're fast and trivial to
 * run in CI.
 */
class QuizDomainTest {

    @Test
    fun choice_displayUses12hFormat() {
        assertEquals("3:05", QuizChoice(3, 5).display(TimeFormat.H12))
        assertEquals("12:00", QuizChoice(12, 0).display(TimeFormat.H12))
    }

    @Test
    fun choice_displayUses24hFormat() {
        assertEquals("03:05", QuizChoice(3, 5).display(TimeFormat.H24))
        assertEquals("12:00", QuizChoice(12, 0).display(TimeFormat.H24))
    }

    @Test
    fun initialState_hasNoPick_andAwaitingPhase() {
        val s = QuizUiState()
        assertEquals(null, s.pickedIndex)
        assertEquals(QuizPhase.AwaitingAnswer, s.phase)
        assertEquals(0, s.correctStreak)
    }

    @Test
    fun choicesAreOrderedDeterministically_inGivenState() {
        val choices = listOf(QuizChoice(3, 0), QuizChoice(2, 0), QuizChoice(4, 0))
        val s = QuizUiState(choices = choices, correctIndex = 0)
        assertEquals(3, s.choices[0].hour)
        assertEquals(2, s.choices[1].hour)
    }

    @Test
    fun choicesAreDistinct_whenManuallyConstructed() {
        val choices = listOf(QuizChoice(3, 0), QuizChoice(2, 0), QuizChoice(4, 0))
        val distinctSize = choices.toSet().size
        assertEquals(3, distinctSize)
    }

    @Test
    fun choiceEquality_isStructural() {
        assertEquals(QuizChoice(3, 0), QuizChoice(3, 0))
        assertNotEquals(QuizChoice(3, 0), QuizChoice(3, 15))
    }

    @Test
    fun quizSpeech_returnsLocalizedStrings() {
        assertTrue(QuizSpeech.question(com.batteria.clockwise.presentation.clock.Language.ZH).isNotEmpty())
        assertTrue(QuizSpeech.question(com.batteria.clockwise.presentation.clock.Language.EN).isNotEmpty())
        assertNotEquals(
            QuizSpeech.question(com.batteria.clockwise.presentation.clock.Language.ZH),
            QuizSpeech.question(com.batteria.clockwise.presentation.clock.Language.EN),
        )
    }
}
