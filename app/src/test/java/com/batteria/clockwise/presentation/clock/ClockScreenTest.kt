package com.batteria.clockwise.presentation.clock

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.batteria.clockwise.presentation.theme.ClockWiseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * v4.0 — Compose UI tests for the clock screen.
 *
 * Verifies that the back button is present, is clickable, and invokes the
 * provided onBack callback so the screen returns to the quiz.
 *
 * NOTE: the screen runs the clock in [ClockMode.MANUAL] for these tests
 * because [ClockMode.AUTO] drives an infinite `withFrameNanos` loop which
 * keeps Compose busy and trips Espresso's idling watchdog under
 * Robolectric. MANUAL mode shows the exact same back button + the same
 * 5 toggles, so the test still covers the navigation affordance.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ClockScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val staticState = ClockUiState(mode = ClockMode.MANUAL, manualTotalSeconds = 3f * 3600f)

    @Test
    fun clockScreen_rendersBackButton() {
        composeRule.setContent {
            ClockWiseTheme {
                ClockScreenContent(
                    state = staticState,
                    onTimeFormatChange = {},
                    onLanguageChange = {},
                    onShowSecondsChange = {},
                    onVoiceGenderChange = {},
                    onModeChange = {},
                    onManualDelta = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("clock_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("clock_back_button").assertHasClickAction()
    }

    @Test
    fun backButton_invokesOnBackCallback() {
        var backCalled = false
        composeRule.setContent {
            ClockWiseTheme {
                ClockScreenContent(
                    state = staticState,
                    onTimeFormatChange = {},
                    onLanguageChange = {},
                    onShowSecondsChange = {},
                    onVoiceGenderChange = {},
                    onModeChange = {},
                    onManualDelta = {},
                    onBack = { backCalled = true },
                )
            }
        }

        composeRule.onNodeWithTag("clock_back_button").performClick()
        composeRule.waitForIdle()
        assertTrue("Back button should invoke onBack callback", backCalled)
    }
}
