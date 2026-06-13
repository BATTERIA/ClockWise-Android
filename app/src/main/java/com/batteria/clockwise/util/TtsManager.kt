package com.batteria.clockwise.util

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import java.util.Locale

/**
 * Thin wrapper around Android's TextToSpeech engine.
 * - Lazily initialized on first speak() call.
 * - speak() switches locale per-call (so the same engine can speak ZH then EN).
 * - shutdown() should be called when the holder leaves the composition.
 *
 * v3.6.6: surface failures via Toast so the user actually knows why nothing
 * came out (silent failure on devices without a TTS engine / language pack
 * is the most common "I tapped and nothing happened" cause — esp. MIUI, EMUI,
 * etc., where Google TTS isn't pre-installed).
 */
class TtsManager(context: Context) {
    private val appCtx = context.applicationContext

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready: Boolean = false
    @Volatile private var initFailed: Boolean = false
    @Volatile private var pendingSpeak: Pair<String, Locale>? = null

    private fun ensureInit() {
        if (tts != null || initFailed) return
        synchronized(this) {
            if (tts != null || initFailed) return
            tts = TextToSpeech(appCtx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ready = true
                    Log.i(TAG, "TTS engine ready, default engine=${tts?.defaultEngine}")
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "TTS onStart id=$utteranceId")
                        }
                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "TTS onDone id=$utteranceId")
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            Log.w(TAG, "TTS onError id=$utteranceId")
                            toast("TTS playback failed — check device TTS settings")
                        }
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            Log.w(TAG, "TTS onError id=$utteranceId code=$errorCode")
                            toast("TTS playback failed (code=$errorCode)")
                        }
                    })
                    pendingSpeak?.let { (text, locale) ->
                        pendingSpeak = null
                        doSpeak(text, locale)
                    }
                } else {
                    initFailed = true
                    Log.w(TAG, "TTS init failed: status=$status")
                    toast(
                        "No TTS engine installed. " +
                                "Install \"Google Text-to-speech\" from Play Store."
                    )
                }
            }
        }
    }

    fun speak(text: String, locale: Locale) {
        ensureInit()
        if (initFailed) {
            toast("No TTS engine available on this device.")
            return
        }
        if (ready) doSpeak(text, locale) else pendingSpeak = text to locale
    }

    private fun doSpeak(text: String, locale: Locale) {
        val engine = tts ?: return
        val avail = engine.setLanguage(locale)
        when (avail) {
            TextToSpeech.LANG_MISSING_DATA -> {
                Log.w(TAG, "Language data MISSING for $locale — prompting install")
                toast("Voice data for $locale not installed. Opening installer…")
                promptInstallVoiceData()
                // Try fallback to default so the tap isn't a complete silent miss.
                val fb = engine.setLanguage(Locale.getDefault())
                if (fb == TextToSpeech.LANG_MISSING_DATA || fb == TextToSpeech.LANG_NOT_SUPPORTED) {
                    return
                }
            }
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                Log.w(TAG, "Language NOT supported for $locale")
                toast("Language $locale not supported by current TTS engine.")
                val fb = engine.setLanguage(Locale.getDefault())
                if (fb == TextToSpeech.LANG_MISSING_DATA || fb == TextToSpeech.LANG_NOT_SUPPORTED) {
                    return
                }
            }
        }
        // Force MUSIC stream + max-effective volume params so devices that route
        // accessibility audio to a different stream still produce audible output.
        val params = Bundle().apply {
            putInt(Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val rc = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
        if (rc != TextToSpeech.SUCCESS) {
            Log.w(TAG, "speak() returned $rc")
            toast("TTS speak() failed (rc=$rc).")
        }
    }

    private fun promptInstallVoiceData() {
        try {
            val intent = Intent(Engine.ACTION_INSTALL_TTS_DATA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appCtx.startActivity(intent)
        } catch (t: Throwable) {
            Log.w(TAG, "Couldn't launch INSTALL_TTS_DATA: $t")
        }
    }

    private fun toast(msg: String) {
        // Toast must run on main thread; TextToSpeech callbacks are on a binder
        // thread. Use the app context's main looper via Handler? Simpler: just
        // post via Toast.makeText on main looper using a Handler.
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(appCtx, msg, Toast.LENGTH_SHORT).show()
        }
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
        initFailed = false
    }

    companion object {
        private const val TAG = "TtsManager"
        private const val UTTERANCE_ID = "clockwise-tts"
    }
}
