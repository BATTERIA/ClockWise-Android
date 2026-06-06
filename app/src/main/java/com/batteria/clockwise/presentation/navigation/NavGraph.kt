package com.batteria.clockwise.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.batteria.clockwise.presentation.clock.ClockScreen
import com.batteria.clockwise.presentation.quiz.QuizScreen
import com.batteria.clockwise.presentation.settings.SettingsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "clock") {
        composable("clock") { ClockScreen(navController) }
        composable("quiz") { QuizScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}
