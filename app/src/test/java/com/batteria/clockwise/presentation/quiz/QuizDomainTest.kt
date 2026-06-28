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

    @Test
    fun correctMilestone_isNull_belowFirstThreshold() {
        val zh = com.batteria.clockwise.presentation.clock.Language.ZH
        // streak 0, 1, 2 → no milestone, fall back to normal Bluey copy
        assertEquals(null, QuizSpeech.correctMilestone(0, zh))
        assertEquals(null, QuizSpeech.correctMilestone(1, zh))
        assertEquals(null, QuizSpeech.correctMilestone(2, zh))
    }

    @Test
    fun correctMilestone_firesEveryThirdCorrect() {
        val zh = com.batteria.clockwise.presentation.clock.Language.ZH
        val en = com.batteria.clockwise.presentation.clock.Language.EN

        val zh3 = QuizSpeech.correctMilestone(3, zh)
        val en3 = QuizSpeech.correctMilestone(3, en)
        val zh6 = QuizSpeech.correctMilestone(6, zh)
        val en9 = QuizSpeech.correctMilestone(9, en)

        // Each milestone string should embed the current streak count so
        // the kid sees "3 in a row", "6 in a row", etc.
        assertTrue("ZH @3 should contain '3'", zh3?.contains("3") == true)
        assertTrue("EN @3 should contain '3'", en3?.contains("3") == true)
        assertTrue("ZH @6 should contain '6'", zh6?.contains("6") == true)
        assertTrue("EN @9 should contain '9'", en9?.contains("9") == true)
        // ZH and EN must differ for the same streak — we're not double-
        // localising the same string.
        assertNotEquals(zh3, en3)
    }

    @Test
    fun correctMilestone_skips_nonMultiples() {
        val zh = com.batteria.clockwise.presentation.clock.Language.ZH
        assertEquals(null, QuizSpeech.correctMilestone(4, zh))
        assertEquals(null, QuizSpeech.correctMilestone(5, zh))
        assertEquals(null, QuizSpeech.correctMilestone(7, zh))
        assertEquals(null, QuizSpeech.correctMilestone(8, zh))
    }
}
