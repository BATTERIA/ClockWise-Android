package com.batteria.clockwise.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.batteria.clockwise.presentation.clock.ClockScreen
import com.batteria.clockwise.presentation.quiz.QuizScreen
import com.batteria.clockwise.presentation.settings.SettingsScreen

/**
 * v4.0 — App navigation.
 *
 * Start destination is the quiz/game screen ("看图说时间") so kids land
 * straight into an interactive question. From there a top-end clock-icon
 * button navigates to the full clock workshop; the workshop's top-start
 * back button + the system back gesture both pop back to the quiz.
 *
 * Navigation Compose enables Android's predictive-back gesture
 * automatically on Android 14+; no manual interception needed.
 */
object Routes {
    const val QUIZ = "quiz"
    const val CLOCK = "clock"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.QUIZ) {
        composable(Routes.QUIZ) { QuizScreen(navController) }
        composable(Routes.CLOCK) { ClockScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
    }
}
