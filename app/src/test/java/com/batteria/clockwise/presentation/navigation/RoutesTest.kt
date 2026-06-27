package com.batteria.clockwise.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v4.0 — Pure-Kotlin sanity tests for the navigation graph constants.
 * Composable nav graph integration tests live in androidTest; here we
 * just lock in the route names so we don't accidentally rename them.
 */
class RoutesTest {

    @Test
    fun routes_areStable() {
        assertEquals("quiz", Routes.QUIZ)
        assertEquals("clock", Routes.CLOCK)
        assertEquals("settings", Routes.SETTINGS)
    }
}
