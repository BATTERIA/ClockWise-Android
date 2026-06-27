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

    fun correct(language: Language): String = when (language) {
        Language.ZH -> "太棒了！答对了！"
        Language.EN -> "Great job! That's right!"
    }

    fun wrong(language: Language): String = when (language) {
        Language.ZH -> "再试一次吧！"
        Language.EN -> "Let's try again!"
    }

    /** "Next question →" button label. */
    fun nextQuestion(language: Language): String = when (language) {
        Language.ZH -> "下一题"
        Language.EN -> "Next question"
    }

    /** "Tap the clock to enter the full clock workshop" hint. */
    fun openClockHint(language: Language): String = when (language) {
        Language.ZH -> "时钟工坊"
        Language.EN -> "Clock workshop"
    }
}
