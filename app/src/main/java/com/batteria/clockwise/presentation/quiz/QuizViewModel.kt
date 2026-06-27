package com.batteria.clockwise.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batteria.clockwise.presentation.clock.ClockPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * v4.0 — ViewModel for the quiz/game screen.
 *
 * Owns the random-question generator. Reads the persisted clock prefs once
 * so the quiz inherits the user's language/format/voice; we don't want the
 * kid to learn "三点" and then have the next question shout "3:00".
 *
 * The first quiz format is intentionally simple — "看图说时间" on quarter
 * hours — to match L1 of the published curriculum. Difficulty scales by
 * widening the minute set after a small correct streak.
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val prefs: ClockPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        // Pull language + format from saved clock prefs so the quiz speaks
        // the same way the clock screen does. We collect once here rather
        // than re-flowing so a settings change mid-quiz doesn't yank the
        // question out from under the kid.
        viewModelScope.launch {
            val p = prefs.state
            p.collect { snap ->
                _uiState.value = _uiState.value.copy(
                    language = snap.language,
                    timeFormat = snap.timeFormat,
                    voiceGender = snap.voiceGender,
                )
            }
        }
        nextQuestion()
    }

    /**
     * Generate a fresh question. Difficulty schedule:
     *   streak 0-2  → minute ∈ {0}           (整点 only, L1)
     *   streak 3-5  → minute ∈ {0, 30}       (整点 + 半点)
     *   streak 6+   → minute ∈ {0, 15, 30, 45} (quarters)
     */
    fun nextQuestion() {
        val streak = _uiState.value.correctStreak
        val allowedMinutes = when {
            streak >= 6 -> listOf(0, 15, 30, 45)
            streak >= 3 -> listOf(0, 30)
            else -> listOf(0)
        }
        val hour = Random.nextInt(1, 13)            // 1..12
        val minute = allowedMinutes.random()
        val correct = QuizChoice(hour, minute)

        // Build two distractors: one with a nearby (off-by-one) hour, one
        // with a different minute (when the level allows >1 minute slot).
        val distractors = mutableSetOf<QuizChoice>()
        while (distractors.size < 2) {
            val candidate = if (distractors.isEmpty() || allowedMinutes.size == 1) {
                // Off-by-one hour, same minute.
                val dh = ((hour - 1 + (if (Random.nextBoolean()) 1 else -1) + 12) % 12) + 1
                QuizChoice(dh, minute)
            } else {
                // Same hour, swap the minute. Falls back to off-by-two hour
                // if we keep colliding with the correct answer.
                val swappable = allowedMinutes.filter { it != minute }
                if (swappable.isNotEmpty() && Random.nextFloat() < 0.6f) {
                    QuizChoice(hour, swappable.random())
                } else {
                    val dh = ((hour - 1 + Random.nextInt(2, 4) * (if (Random.nextBoolean()) 1 else -1) + 24) % 12) + 1
                    QuizChoice(dh, minute)
                }
            }
            if (candidate != correct) distractors.add(candidate)
        }

        val choices = (listOf(correct) + distractors).shuffled()
        val correctIndex = choices.indexOf(correct)
        _uiState.value = _uiState.value.copy(
            targetHour = hour,
            targetMinute = minute,
            choices = choices,
            correctIndex = correctIndex,
            pickedIndex = null,
            phase = QuizPhase.AwaitingAnswer,
        )
    }

    fun pick(index: Int) {
        val s = _uiState.value
        if (s.phase != QuizPhase.AwaitingAnswer) return
        if (index < 0 || index >= s.choices.size) return
        val correct = index == s.correctIndex
        _uiState.value = s.copy(
            pickedIndex = index,
            phase = QuizPhase.Revealed,
            correctStreak = if (correct) s.correctStreak + 1 else 0,
            totalAnswered = s.totalAnswered + 1,
            totalCorrect = s.totalCorrect + if (correct) 1 else 0,
        )
    }
}
