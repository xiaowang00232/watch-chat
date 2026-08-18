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
    /** 流结束后落库的那条 AI 消息 id，UI 在打字动画期间用它隐藏重复消息。 */
    val streamingMessageId: Long? = null,
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
    initialConversationId: Long?,
    /** 为 true 且设置开启时，新建聊天页自动续接最近一次对话。 */
    private val resumeLast: Boolean = true
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
            attachConversation(initialConversationId)
        } else if (resumeLast) {
            // 打开 App 时续接最近一次对话；设置关闭或没有历史则保持新建对话
            viewModelScope.launch {
                val settings = settingsRepository.currentSettings()
                if (!settings.resumeLastConversation) return@launch
                val last = conversationRepository.mostRecentConversation()
                if (last != null) {
                    _uiState.update {
                        it.copy(conversationId = last.id, title = last.title, selectedModel = last.model)
                    }
                    attachConversation(last.id)
                }
            }
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

                _uiState.update { it.copy(isStreaming = true, streamingText = "", streamingMessageId = null) }
                chatRepository.streamAssistantReply(conversationId, text).collect { delta ->
                    _uiState.update {
                        it.copy(streamingText = (it.streamingText ?: "") + delta)
                    }
                }
                // 流结束：AI 回复落库，但保留 streamingText，
                // 等 UI 打字动画播完（onStreamingFinished）再切换到正式消息
                val finalText = (_uiState.value.streamingText ?: "").ifBlank { "(无回复内容)" }
                val savedId = conversationRepository.addMessage(
                    conversationId, ChatRepository.ROLE_ASSISTANT, finalText
                )
                conversationRepository.touch(conversationId)
                _uiState.update { it.copy(isStreaming = false, streamingMessageId = savedId) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        streamingText = null,
                        streamingMessageId = null,
                        error = friendlyMessage(e)
                    )
                }
            }
        }
    }

    /** UI 打字动画播完时调用，切走流式气泡、显示正式消息。 */
    fun onStreamingFinished() {
        _uiState.update { it.copy(streamingText = null, streamingMessageId = null) }
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

    private fun attachConversation(conversationId: Long) {
        viewModelScope.launch {
            conversationRepository.observeConversation(conversationId).collect { conversation ->
                if (conversation != null) {
                    _uiState.update {
                        it.copy(title = conversation.title, selectedModel = conversation.model)
                    }
                }
            }
        }
        observeMessages(conversationId)
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
