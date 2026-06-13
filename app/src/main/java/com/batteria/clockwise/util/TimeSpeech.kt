package com.batteria.clockwise.util

import com.batteria.clockwise.presentation.clock.Language
import com.batteria.clockwise.presentation.clock.TimeFormat
import java.util.Locale

object TimeSpeech {
    /**
     * Build the sentence to speak.
     * @param hour      0..23 (the displayed hour — in 12h mode this is the 12h hour we should
     *                  speak; for manual 24h mode it's 0..11 because that's what the dial shows;
     *                  pass whatever the digital readout actually displays).
     * @param minute    0..59
     * @param second    0..59
     * @param includeSeconds  whether the displayed readout includes seconds
     * @param format    H12 or H24 — affects whether we say "AM/PM" / "上午/下午"
     * @param isPm      relevant only in H12; if format=H24 ignored
     * @param language  EN → English sentence; ZH → Chinese sentence
     */
    fun build(
        hour: Int,
        minute: Int,
        second: Int,
        includeSeconds: Boolean,
        format: TimeFormat,
        isPm: Boolean,
        language: Language,
    ): String = when (language) {
        Language.EN -> buildEn(hour, minute, second, includeSeconds, format, isPm)
        Language.ZH -> buildZh(hour, minute, second, includeSeconds, format, isPm)
    }

    private fun buildEn(h: Int, m: Int, s: Int, withS: Boolean, fmt: TimeFormat, pm: Boolean): String {
        val hourWord = when {
            fmt == TimeFormat.H24 -> h.toString()
            h == 0 -> "12"
            else -> h.toString()
        }
        val sb = StringBuilder()
        // "It's six thirty-two AM" / "Six o'clock"
        sb.append("It's ").append(hourWord)
        when {
            m == 0 && !withS -> sb.append(" o'clock")
            else -> {
                sb.append(' ')
                sb.append(twoDigitWords(m))
                if (withS) {
                    sb.append(" and ").append(twoDigitWords(s)).append(" seconds")
                }
            }
        }
        if (fmt == TimeFormat.H12) sb.append(if (pm) " PM" else " AM")
        return sb.toString()
    }

    private fun buildZh(h: Int, m: Int, s: Int, withS: Boolean, fmt: TimeFormat, pm: Boolean): String {
        val prefix = if (fmt == TimeFormat.H12) (if (pm) "下午" else "上午") else ""
        val hourWord = when (fmt) {
            TimeFormat.H24 -> "${h}点"
            TimeFormat.H12 -> {
                val h12 = if (h == 0) 12 else h
                "${h12}点"
            }
        }
        val rest = when {
            m == 0 && !withS -> "整"
            !withS -> "${m}分"
            else -> "${m}分${s}秒"
        }
        return "现在是${prefix}${hourWord}${rest}"
    }

    // Simple English number-to-words for 0..59
    private fun twoDigitWords(n: Int): String {
        if (n < 20) return ones(n)
        val tens = n / 10
        val rest = n % 10
        val tensWord = listOf("twenty", "thirty", "forty", "fifty")[tens - 2]
        return if (rest == 0) tensWord else "$tensWord-${ones(rest)}"
    }

    private fun ones(n: Int) = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
    )[n]
}

fun Language.toLocale(): Locale = when (this) {
    Language.EN -> Locale.ENGLISH
    Language.ZH -> Locale.SIMPLIFIED_CHINESE
}
