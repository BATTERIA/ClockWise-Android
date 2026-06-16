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

/** v3.7: Pre-recorded voice pack persona for time announcements. */
enum class VoiceGender(val key: String) {
    GIRL("girl"),
    BOY("boy");

    companion object {
        fun fromKey(key: String?): VoiceGender =
            entries.firstOrNull { it.key == key } ?: GIRL
    }
}

/** Clock operating mode: real-time auto-tick or transient manual drag-to-set. */
enum class ClockMode { AUTO, MANUAL }

/** Immutable UI state. */
data class ClockUiState(
    val timeFormat: TimeFormat = TimeFormat.H12,
    val language: Language = Language.EN,
    /** Whether the digital clock should include seconds (default OFF for a cleaner look).
     *  When false, the analog second hand is also hidden entirely. */
    val showSeconds: Boolean = false,
    /** v3.7: which pre-recorded voice pack to use for speak-time. */
    val voiceGender: VoiceGender = VoiceGender.GIRL,
    /** AUTO = real-time tick (default). MANUAL = user drags hands; not persisted. */
    val mode: ClockMode = ClockMode.AUTO,
    /** Source-of-truth for manual mode: seconds within a 12-hour cycle, [0, 43200). */
    val manualTotalSeconds: Float = 0f,
)
