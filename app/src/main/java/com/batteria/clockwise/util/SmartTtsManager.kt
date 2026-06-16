package com.batteria.clockwise.util

import android.content.Context
import com.batteria.clockwise.presentation.clock.ClockMode
import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import com.batteria.clockwise.presentation.clock.VoiceGender

/**
 * v3.7: Smart router that prefers the high-quality pre-recorded voice pack
 * (PreRecordedTtsPlayer) and falls back to the system TextToSpeech engine
 * (TtsManager) when no clip applies (24h mode, showSeconds=true, missing
 * asset).
 *
 * One instance is created per ClockScreen composition and disposed when
 * the screen leaves composition.
 */
class SmartTtsManager(context: Context) {
    private val pre = PreRecordedTtsPlayer(context)
    private val sys = TtsManager(context)

    fun speakTime(
        hour: Int,
        minute: Int,
        second: Int,
        includeSeconds: Boolean,
        format: TimeFormat,
        isPm: Boolean,
        language: Language,
        gender: VoiceGender,
        mode: ClockMode = ClockMode.AUTO,
    ) {
        // Try pre-recorded first.
        val played = pre.playTime(
            hour = hour,
            minute = minute,
            second = second,
            includeSeconds = includeSeconds,
            format = format,
            isPm = isPm,
            language = language,
            gender = gender,
            mode = mode,
        )
        if (played) return

        // Fall back to system TTS.
        val sentence = TimeSpeech.build(
            hour = hour,
            minute = minute,
            second = second,
            includeSeconds = includeSeconds,
            format = format,
            isPm = isPm,
            language = language,
        )
        sys.speak(sentence, language.toLocale())
    }

    fun shutdown() {
        try { pre.shutdown() } catch (_: Throwable) {}
        try { sys.shutdown() } catch (_: Throwable) {}
    }
}
