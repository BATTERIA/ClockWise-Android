package com.batteria.clockwise.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.batteria.clockwise.R

/**
 * ClockWise v3.7 — Friendlier, chunkier typography.
 *
 * Uses Fredoka (variable font) bundled in res/font/fredoka.ttf for all display +
 * UI text. Fredoka has bouncy rounded shapes that feel friendly to kids learning
 * to read numbers. Falls through to system sans-serif if the resource is missing.
 *
 * Master asked for everything to feel bolder, matching the HTML prototype's
 * chunky look. We push every weight up one step: Normal→SemiBold,
 * SemiBold→Bold, Bold→ExtraBold (via the variable weight axis at 800).
 * Still one TTF file, no APK bloat.
 */
@OptIn(ExperimentalTextApi::class)
private val Fredoka: FontFamily = FontFamily(
    Font(
        resId = R.font.fredoka,
        weight = FontWeight.Normal,
        // v3.7: "Normal" body text is rendered at 600 so reading copy is chunkier.
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.fredoka,
        weight = FontWeight.SemiBold,
        // v3.7: "SemiBold" is now full Bold.
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        resId = R.font.fredoka,
        weight = FontWeight.Bold,
        // v3.7: "Bold" is pushed to ExtraBold so the big digital readout feels
        // as chunky as the HTML prototype.
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
    Font(
        resId = R.font.fredoka,
        weight = FontWeight.ExtraBold,
        // Explicit ExtraBold for any callers that ask for it directly.
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

private val DisplayFamily = Fredoka
private val UiFamily = Fredoka

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily,
        // v3.7: push the giant clock readout up one notch.
        fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        // v3.6: slightly POSITIVE letter-spacing — Fredoka likes a little air to feel chunky
        letterSpacing = 0.5.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = 0.3.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = UiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    ),
)
