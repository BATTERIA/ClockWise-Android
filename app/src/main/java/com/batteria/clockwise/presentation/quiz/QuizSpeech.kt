package com.batteria.clockwise.presentation.quiz

import com.batteria.clockwise.presentation.clock.Language

/**
 * v4.0 — Tiny helper that builds the question/result sentences spoken by
 * the quiz screen. Kept here (not in util/) because the wording is quiz-
 * specific and we don't want it leaking into other features.
 *
 * For the actual time announcement we always defer to [SmartTtsManager]
 * which already routes through the high-quality voice pack.
 */
object QuizSpeech {
    /** "What time does this clock show?" prompt. */
    fun question(language: Language): String = when (language) {
        Language.ZH -> "请问当前时钟是几点？"
        Language.EN -> "What time does this clock show?"
    }

    /**
     * Default correct-answer copy — "Bluey-style" wording per Master's
     * v4.3 pick (option B in the review page). Warmer than the v4.0/4.2
     * "太棒了！答对了！" / "Great job!" line.
     */
    fun correct(language: Language): String = when (language) {
        Language.ZH -> "哇噢，就是这个！"
        Language.EN -> "Wackadoo! You got it!"
    }

    fun wrong(language: Language): String = when (language) {
        Language.ZH -> "差一点点～"
        Language.EN -> "Sooo close!"
    }

    /**
     * Milestone copy (option C in the review). Returned only when
     * [correctStreak] is a multiple of [MILESTONE_EVERY] (3, 6, 9, ...).
     *
     * `null` means "no milestone this round, use [correct] instead".
     *
     * The wording is intentionally past-tense + ✨ so the streak count is
     * the news, not the latest answer.
     */
    fun correctMilestone(correctStreak: Int, language: Language): String? {
        if (correctStreak <= 0 || correctStreak % MILESTONE_EVERY != 0) return null
        return when (language) {
            Language.ZH -> "连对 $correctStreak 次！要升级啦 ✨"
            Language.EN -> "$correctStreak in a row! Leveling up ✨"
        }
    }

    /** "Next question" button label. */
    fun nextQuestion(language: Language): String = when (language) {
        Language.ZH -> "下一题"
        Language.EN -> "Next question"
    }

    /** "Tap the clock to enter the full clock workshop" hint. */
    fun openClockHint(language: Language): String = when (language) {
        Language.ZH -> "时钟工坊"
        Language.EN -> "Clock workshop"
    }

    /** Every Nth correct answer triggers the milestone banner. */
    const val MILESTONE_EVERY = 3
}
