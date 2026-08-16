// 共享模块：手机端与手表端共用
package com.watchchat.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchchat.app.data.local.ConversationEntity
import com.watchchat.app.data.repo.ConversationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: ConversationRepository
) : ViewModel() {

    val conversations: StateFlow<List<ConversationEntity>> =
        repository.observeConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteConversation(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
