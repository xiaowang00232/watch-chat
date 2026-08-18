package com.xw00232.watchchat.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xw00232.watchchat.app.WatchChatApp
import com.watchchat.app.ui.chat.ChatViewModel
import com.watchchat.app.ui.history.HistoryViewModel
import com.watchchat.app.ui.settings.SettingsViewModel

object AppViewModelProvider {

    fun chatFactory(conversationId: Long?, resumeLast: Boolean = true): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WatchChatApp
            ChatViewModel(
                settingsRepository = app.container.settingsRepository,
                chatRepository = app.container.chatRepository,
                conversationRepository = app.container.conversationRepository,
                initialConversationId = conversationId,
                resumeLast = resumeLast
            )
        }
    }

    fun historyFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WatchChatApp
            HistoryViewModel(app.container.conversationRepository)
        }
    }

    fun settingsFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WatchChatApp
            SettingsViewModel(
                settingsRepository = app.container.settingsRepository,
                conversationRepository = app.container.conversationRepository
            )
        }
    }
}
