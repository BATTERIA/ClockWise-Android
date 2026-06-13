package com.batteria.clockwise.presentation.clock

/** User-selectable time format. */
enum class TimeFormat(val key: String) {
    H12("12"),
    H24("24");

    companion object {
        fun fromKey(key: String?): TimeFormat = entries.firstOrNull { it.key == key } ?: H12
    }
}

/** User-selectable language. */
enum class Language(val key: String) {
    EN("en"),
    ZH("zh");

    companion object {
        fun fromKey(key: String?): Language = entries.firstOrNull { it.key == key } ?: EN
    }
}

/** Immutable UI state. */
data class ClockUiState(
    val timeFormat: TimeFormat = TimeFormat.H12,
    val language: Language = Language.EN,
    /** Whether the digital clock should include seconds (default OFF for a cleaner look). */
    val showSeconds: Boolean = false,
)
