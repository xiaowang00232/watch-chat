package com.xw00232.watchchat.wear.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.watchchat.app.AppContainer
import com.watchchat.app.ui.chat.ChatViewModel
import com.watchchat.app.ui.history.HistoryViewModel
import com.watchchat.app.ui.settings.SettingsViewModel
import com.xw00232.watchchat.wear.ui.chat.WearChatScreen
import com.xw00232.watchchat.wear.ui.history.WearHistoryScreen
import com.xw00232.watchchat.wear.ui.settings.WearSettingsScreen

@Composable
fun WearNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            val viewModel: ChatViewModel =
                viewModel(factory = WearViewModelProvider.chatFactory(null))
            WearChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenHistory = { navController.navigate("history") },
                onOpenSettings = { navController.navigate("settings") },
                onNewChat = {
                    navController.navigate("chat") {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "chat/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { entry ->
            val conversationId = entry.arguments?.getLong("conversationId")
            val viewModel: ChatViewModel =
                viewModel(factory = WearViewModelProvider.chatFactory(conversationId))
            WearChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenHistory = { navController.navigate("history") },
                onOpenSettings = { navController.navigate("settings") },
                onNewChat = {
                    navController.navigate("chat") {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("history") {
            val viewModel: HistoryViewModel =
                viewModel(factory = WearViewModelProvider.historyFactory())
            WearHistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSelect = { id -> navController.navigate("chat/$id") }
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel =
                viewModel(factory = WearViewModelProvider.settingsFactory())
            WearSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
