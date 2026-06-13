package com.batteria.clockwise.presentation.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val CYCLE_SECONDS = 43200f // 12h in seconds

@HiltViewModel
class ClockViewModel @Inject constructor(
    private val repo: ClockPreferencesRepository,
) : ViewModel() {

    /** Transient (non-persisted) overlay carrying mode + manual time. */
    private val transient = MutableStateFlow(TransientState())

    val uiState: StateFlow<ClockUiState> = combine(repo.state, transient) { persisted, t ->
        persisted.copy(
            mode = t.mode,
            manualTotalSeconds = t.manualTotalSeconds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ClockUiState(),
    )

    fun setTimeFormat(format: TimeFormat) = viewModelScope.launch {
        repo.setTimeFormat(format)
    }

    fun setLanguage(lang: Language) = viewModelScope.launch {
        repo.setLanguage(lang)
    }

    fun setShowSeconds(show: Boolean) = viewModelScope.launch {
        repo.setShowSeconds(show)
    }

    /** Switch between AUTO/MANUAL. Seeds manualTotalSeconds from current real time on entry. */
    fun setMode(mode: ClockMode) {
        val current = transient.value
        if (current.mode == mode) return
        if (mode == ClockMode.MANUAL) {
            val cal = Calendar.getInstance()
            val h = cal.get(Calendar.HOUR_OF_DAY) % 12
            val m = cal.get(Calendar.MINUTE)
            val s = cal.get(Calendar.SECOND)
            val seed = (h * 3600 + m * 60 + s).toFloat()
            transient.value = current.copy(mode = ClockMode.MANUAL, manualTotalSeconds = seed)
        } else {
            transient.value = current.copy(mode = ClockMode.AUTO)
        }
    }

    /** Clamp + update the source-of-truth in manual mode. */
    fun setManualTotalSeconds(value: Float) {
        if (transient.value.mode != ClockMode.MANUAL) return
        val wrapped = wrapCycle(value)
        transient.value = transient.value.copy(manualTotalSeconds = wrapped)
    }

    /** Add a delta (seconds, may be negative) to manualTotalSeconds and wrap. */
    fun addManualSeconds(delta: Float) {
        if (transient.value.mode != ClockMode.MANUAL) return
        val wrapped = wrapCycle(transient.value.manualTotalSeconds + delta)
        transient.value = transient.value.copy(manualTotalSeconds = wrapped)
    }

    private fun wrapCycle(v: Float): Float {
        var x = v % CYCLE_SECONDS
        if (x < 0f) x += CYCLE_SECONDS
        return x
    }

    private data class TransientState(
        val mode: ClockMode = ClockMode.AUTO,
        val manualTotalSeconds: Float = 0f,
    )
}
