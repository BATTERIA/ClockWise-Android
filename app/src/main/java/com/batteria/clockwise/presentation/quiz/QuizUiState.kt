package com.batteria.clockwise.presentation.quiz

import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import com.batteria.clockwise.presentation.clock.VoiceGender

/**
 * v4.0 — Immutable state for the quiz/game screen.
 *
 * The first quiz mode is "what time is the clock showing?": a static analog
 * dial at [targetHour]:[targetMinute] and three [choices] (one correct, two
 * plausible distractors). After the kid taps a choice we move to a brief
 * [QuizPhase.Result] phase showing the verdict, then auto-advance to the
 * next random question.
 */
data class QuizUiState(
    val targetHour: Int = 3,          // 1..12 (12-hour clock)
    val targetMinute: Int = 0,        // 0, 15, 30, or 45 (Level 1: only on the quarters)
    val choices: List<QuizChoice> = emptyList(),
    val correctIndex: Int = 0,
    val pickedIndex: Int? = null,
    val phase: QuizPhase = QuizPhase.AwaitingAnswer,
    val correctStreak: Int = 0,
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    // Inherited from the clock prefs so the dial/choices speak the
    // language and respect the format Master configured.
    val language: Language = Language.EN,
    val timeFormat: TimeFormat = TimeFormat.H12,
    val voiceGender: VoiceGender = VoiceGender.GIRL,
)

/** A single answer choice rendered as a Material 3 elevated button. */
data class QuizChoice(
    val hour: Int,    // 1..12
    val minute: Int,  // 0..59
) {
    /** Display label honoring [TimeFormat] + [Language]. */
    fun display(format: TimeFormat): String = when (format) {
        TimeFormat.H12 -> "%d:%02d".format(hour, minute)
        TimeFormat.H24 -> "%02d:%02d".format(hour, minute)
    }
}

enum class QuizPhase {
    /** Waiting for the kid to pick an answer. */
    AwaitingAnswer,

    /** A choice was picked and we're showing the verdict (correct / try again). */
    Revealed,
}
