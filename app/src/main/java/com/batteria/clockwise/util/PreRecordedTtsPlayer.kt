package com.batteria.clockwise.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.batteria.clockwise.presentation.clock.ClockMode
import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import com.batteria.clockwise.presentation.clock.VoiceGender
import java.io.IOException

/**
 * v3.7.2: Pre-recorded voice pack player.
 *
 * Plays time announcements from full-sentence OGG/Opus clips that ship inside
 * the APK assets. This sidesteps the device's TTS engine entirely (no more
 * Xiaomi/MIUI failures, no "attributionTag" warnings, no language packs to
 * install, identical voice every time).
 *
 * Asset layout under  app/src/main/assets:
 *
 *   voice/<gender>/<lang>/<period>/HHMM.ogg
 *
 * where:
 *   gender ∈ { girl, boy }
 *   lang   ∈ { zh, en }
 *   period ∈ { am, pm, manual, h24 }
 *   HH     = 01..12 for am/pm/manual buckets, 00..23 for the h24 bucket
 *   MM     = minute 00..59
 *
 * The "manual" bucket drops the "现在是" / "It's" lead-in (because in manual
 * mode the user is dragging the dial, not asking what time it actually is)
 * and also drops AM/PM (the dial doesn't carry that information).
 *
 * Coverage: 12h-mode (am/pm/manual) AND 24h-mode (h24). The legacy
 * [TtsManager] system fallback only kicks in for genuinely missing assets;
 * v3.7.1+ never speaks seconds, so includeSeconds is ignored.
 *
 * Example:  voice/girl/zh/am/0925.ogg  →  "现在是上午九点二十五分"
 *           voice/boy/en/pm/0700.ogg   →  "It's 7 o'clock PM."
 *           voice/girl/zh/h24/1330.ogg →  "现在是13点30分"
 *           voice/boy/en/h24/0005.ogg  →  "It's 0 oh five."
 */
class PreRecordedTtsPlayer(context: Context) {
    private val appCtx = context.applicationContext

    @Volatile
    private var current: MediaPlayer? = null

    /**
     * Try to play the time-announcement clip for the given moment.
     *
     * @return true if a clip was found and playback started; false if no clip
     *         applies (e.g. 24h mode, showSeconds, missing asset). Caller
     *         should fall back to [TtsManager] in that case.
     */
    fun playTime(
        hour: Int,
        minute: Int,
        @Suppress("UNUSED_PARAMETER") second: Int,
        @Suppress("UNUSED_PARAMETER") includeSeconds: Boolean,
        format: TimeFormat,
        isPm: Boolean,
        language: Language,
        gender: VoiceGender,
        mode: ClockMode = ClockMode.AUTO,
    ): Boolean {
        // v3.7.2: voice pack covers BOTH 12h (am/pm/manual) and 24h (h24).
        // includeSeconds is ignored — Master's rule: seconds are never spoken,
        // so we always route to the corresponding minute-precision clip.
        val (period, hh) = when {
            format == TimeFormat.H24 -> {
                // 24h dial uses the bare 00..23 hour.
                val h24 = ((hour % 24) + 24) % 24
                "h24" to h24
            }
            mode == ClockMode.MANUAL -> {
                // Manual 12h drops AM/PM and the "现在是" lead-in.
                val h12 = ((hour - 1) % 12 + 12) % 12 + 1
                "manual" to h12
            }
            isPm -> {
                val h12 = ((hour - 1) % 12 + 12) % 12 + 1
                "pm" to h12
            }
            else -> {
                val h12 = ((hour - 1) % 12 + 12) % 12 + 1
                "am" to h12
            }
        }
        val lang = when (language) {
            Language.ZH -> "zh"
            Language.EN -> "en"
        }
        val assetPath = "voice/${gender.key}/$lang/$period/%02d%02d.ogg".format(hh, minute)
        return playAsset(assetPath)
    }

    private fun playAsset(assetPath: String): Boolean {
        // Stop / release previous playback so taps don't stack.
        stop()
        return try {
            val afd = appCtx.assets.openFd(assetPath)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                setOnCompletionListener { mp2 ->
                    try { mp2.release() } catch (_: Throwable) {}
                    if (current === mp2) current = null
                }
                setOnErrorListener { mp2, what, extra ->
                    Log.w(TAG, "MediaPlayer error what=$what extra=$extra path=$assetPath")
                    try { mp2.release() } catch (_: Throwable) {}
                    if (current === mp2) current = null
                    true
                }
                start()
            }
            current = mp
            Log.d(TAG, "Playing $assetPath")
            true
        } catch (io: IOException) {
            // No such asset → caller falls back to system TTS.
            Log.d(TAG, "Asset miss: $assetPath")
            false
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to play $assetPath: $t")
            false
        }
    }

    fun stop() {
        val mp = current ?: return
        current = null
        try { mp.stop() } catch (_: Throwable) {}
        try { mp.release() } catch (_: Throwable) {}
    }

    fun shutdown() {
        stop()
    }

    companion object {
        private const val TAG = "PreRecTts"
    }
}
