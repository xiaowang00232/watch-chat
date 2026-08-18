package com.xw00232.watchchat.wear.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.watchchat.app.ui.chat.ChatViewModel
import com.watchchat.app.ui.history.HistoryViewModel
import com.watchchat.app.ui.settings.SettingsViewModel
import com.xw00232.watchchat.wear.WearWatchChatApp

object WearViewModelProvider {

    fun chatFactory(conversationId: Long?, resumeLast: Boolean = true): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WearWatchChatApp
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
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WearWatchChatApp
            HistoryViewModel(app.container.conversationRepository)
        }
    }

    fun settingsFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WearWatchChatApp
            SettingsViewModel(
                settingsRepository = app.container.settingsRepository,
                conversationRepository = app.container.conversationRepository
            )
        }
    }
}
