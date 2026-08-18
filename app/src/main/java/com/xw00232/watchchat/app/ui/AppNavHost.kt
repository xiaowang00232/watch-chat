package com.xw00232.watchchat.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.watchchat.app.AppContainer
import com.xw00232.watchchat.app.ui.chat.ChatScreen
import com.watchchat.app.ui.chat.ChatViewModel
import com.xw00232.watchchat.app.ui.history.HistoryScreen
import com.watchchat.app.ui.history.HistoryViewModel
import com.xw00232.watchchat.app.ui.settings.SettingsScreen
import com.watchchat.app.ui.settings.SettingsViewModel

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "chat") {
        composable(
            route = "chat?resume={resume}",
            arguments = listOf(navArgument("resume") { type = NavType.BoolType; defaultValue = true })
        ) { entry ->
            val resumeLast = entry.arguments?.getBoolean("resume") ?: true
            val viewModel: ChatViewModel = viewModel(factory = AppViewModelProvider.chatFactory(null, resumeLast))
            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenHistory = { navController.navigate("history") },
                onOpenSettings = { navController.navigate("settings") },
                onNewChat = {
                    navController.navigate("chat?resume=false") {
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
                viewModel(factory = AppViewModelProvider.chatFactory(conversationId))
            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenHistory = { navController.navigate("history") },
                onOpenSettings = { navController.navigate("settings") },
                onNewChat = {
                    navController.navigate("chat?resume=false") {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("history") {
            val viewModel: HistoryViewModel =
                viewModel(factory = AppViewModelProvider.historyFactory())
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenConversation = { id -> navController.navigate("chat/$id") }
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel =
                viewModel(factory = AppViewModelProvider.settingsFactory())
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
