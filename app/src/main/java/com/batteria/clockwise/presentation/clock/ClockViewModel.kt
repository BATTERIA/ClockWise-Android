package com.batteria.clockwise.presentation.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ClockViewModel @Inject constructor(
    private val repo: ClockPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<ClockUiState> = repo.state.stateIn(
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
}
