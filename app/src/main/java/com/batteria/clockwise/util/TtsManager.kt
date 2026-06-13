package com.batteria.clockwise.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Thin wrapper around Android's TextToSpeech engine.
 * - Lazily initialized on first speak() call.
 * - speak() switches locale per-call (so the same engine can speak ZH then EN).
 * - shutdown() should be called when the holder leaves the composition.
 */
class TtsManager(context: Context) {
    private val appCtx = context.applicationContext

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready: Boolean = false
    @Volatile private var pendingSpeak: Pair<String, Locale>? = null

    private fun ensureInit() {
        if (tts != null) return
        synchronized(this) {
            if (tts != null) return
            tts = TextToSpeech(appCtx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ready = true
                    pendingSpeak?.let { (text, locale) ->
                        pendingSpeak = null
                        doSpeak(text, locale)
                    }
                } else {
                    Log.w("TtsManager", "TTS init failed: status=$status")
                }
            }
        }
    }

    fun speak(text: String, locale: Locale) {
        ensureInit()
        if (ready) doSpeak(text, locale) else pendingSpeak = text to locale
    }

    private fun doSpeak(text: String, locale: Locale) {
        val engine = tts ?: return
        val avail = engine.setLanguage(locale)
        if (avail == TextToSpeech.LANG_MISSING_DATA || avail == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back to default locale if requested language data isn't installed.
            engine.setLanguage(Locale.getDefault())
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "clockwise-tts")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
            // ignore
        }
        tts = null
        ready = false
    }
}
