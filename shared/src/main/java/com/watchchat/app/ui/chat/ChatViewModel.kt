// 共享模块：手机端与手表端共用
package com.watchchat.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchchat.app.data.local.MessageEntity
import com.watchchat.app.data.repo.ChatRepository
import com.watchchat.app.data.repo.ConversationRepository
import com.watchchat.app.data.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(
    val id: Long,
    val role: String,
    val content: String,
    val createdAt: Long
)

data class ChatUiState(
    val conversationId: Long? = null,
    val title: String = "新对话",
    val messages: List<UiMessage> = emptyList(),
    val streamingText: String? = null,
    val isStreaming: Boolean = false,
    val input: String = "",
    val error: String? = null,
    val models: List<String> = emptyList(),
    val selectedModel: String = ""
)

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val conversationRepository: ConversationRepository,
    initialConversationId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(conversationId = initialConversationId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(models = settings.models, selectedModel = settings.selectedModel)
                }
            }
        }

        if (initialConversationId != null) {
            viewModelScope.launch {
                conversationRepository.observeConversation(initialConversationId).collect { conversation ->
                    if (conversation != null) {
                        _uiState.update {
                            it.copy(title = conversation.title, selectedModel = conversation.model)
                        }
                    }
                }
            }
            observeMessages(initialConversationId)
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun onSend() {
        val text = _uiState.value.input.trim()
        if (text.isNotEmpty()) sendText(text)
    }

    fun sendText(text: String) {
        val state = _uiState.value
        if (text.isBlank() || state.isStreaming) return
        val model = state.selectedModel.ifBlank { state.models.firstOrNull().orEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(input = "", error = null) }
            try {
                var conversationId = _uiState.value.conversationId
                if (conversationId == null) {
                    val title = text.take(24)
                    conversationId = conversationRepository.createConversation(title = title, model = model)
                    _uiState.update { it.copy(conversationId = conversationId, title = title) }
                    observeMessages(conversationId)
                }

                _uiState.update { it.copy(isStreaming = true, streamingText = "") }
                chatRepository.streamAssistantReply(conversationId, text).collect { delta ->
                    _uiState.update {
                        it.copy(streamingText = (it.streamingText ?: "") + delta)
                    }
                }
                _uiState.update { it.copy(isStreaming = false, streamingText = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isStreaming = false, streamingText = null, error = friendlyMessage(e))
                }
            }
        }
    }

    fun onModelSelect(model: String) {
        viewModelScope.launch {
            settingsRepository.setSelectedModel(model)
            _uiState.value.conversationId?.let { conversationRepository.updateModel(it, model) }
            _uiState.update { it.copy(selectedModel = model) }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeMessages(conversationId: Long) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            conversationRepository.observeMessages(conversationId).collect { list ->
                _uiState.update { it.copy(messages = list.map { message -> message.toUiMessage() }) }
            }
        }
    }

    private fun friendlyMessage(e: Exception): String {
        val message = e.message?.lowercase() ?: "未知错误"
        return when {
            message.contains("401") -> "API Key 无效或已过期，请到设置中检查"
            message.contains("403") -> "没有权限访问该模型，请检查 Key 的模型权限"
            message.contains("404") -> "接口地址错误，请检查 Base URL（例如 https://api.openai.com/v1）"
            message.contains("429") -> "请求过于频繁或额度不足"
            message.contains("timeout") -> "请求超时，请检查网络"
            message.contains("failed to connect") || message.contains("unable to resolve host") ->
                "网络连接失败，请检查网络"
            message.contains("400") -> "请求参数有误：${e.message}"
            else -> "请求失败：${e.message?.take(200)}"
        }
    }

    private fun MessageEntity.toUiMessage() = UiMessage(id, role, content, createdAt)
}
