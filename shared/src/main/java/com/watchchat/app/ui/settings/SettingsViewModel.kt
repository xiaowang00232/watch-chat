// 共享模块：手机端与手表端共用
package com.watchchat.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchchat.app.data.remote.ChatMessage
import com.watchchat.app.data.remote.ChatRequest
import com.watchchat.app.data.remote.OpenAiService
import com.watchchat.app.data.repo.ConversationRepository
import com.watchchat.app.data.settings.AppSettings
import com.watchchat.app.data.settings.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    suspend fun loadSettings(): AppSettings = settingsRepository.currentSettings()

    suspend fun loadApiKey(): String? = settingsRepository.currentApiKey()

    fun save(
        apiKey: String,
        baseUrl: String,
        selectedModel: String,
        models: List<String>,
        systemPrompt: String,
        streamEnabled: Boolean,
        resumeLastConversation: Boolean,
        onDone: (String) -> Unit
    ) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(apiKey)
            settingsRepository.saveAll(
                baseUrl, selectedModel, models, systemPrompt, streamEnabled, resumeLastConversation
            )
            onDone("设置已保存")
        }
    }

    fun testConnection(
        apiKey: String,
        baseUrl: String,
        selectedModel: String,
        onDone: (String) -> Unit
    ) {
        viewModelScope.launch {
            onDone("正在测试…")
            try {
                // 先保存 Key，保证测试用的是当前输入
                settingsRepository.saveApiKey(apiKey)
                val api = OpenAiService.create(baseUrl.trim().trimEnd('/'))
                val key = settingsRepository.currentApiKey().orEmpty()
                val response = api.chat(
                    "Bearer $key",
                    "application/json",
                    ChatRequest(
                        model = selectedModel,
                        messages = listOf(ChatMessage("user", "ping")),
                        stream = false
                    )
                )
                if (!response.isSuccessful) {
                    val raw = response.errorBody()?.string().orEmpty()
                    onDone("连接失败：HTTP ${response.code()} ${raw.take(200)}")
                } else {
                    val body = response.body()
                    val apiError = body?.error
                    if (apiError != null) {
                        onDone("连接失败：${apiError.message ?: apiError.type ?: "未知错误"}")
                    } else {
                        val reply = body?.choices?.firstOrNull()?.message?.content.orEmpty()
                        onDone(if (reply.isNotBlank()) "连接成功，模型已回复" else "连接成功（模型未返回内容）")
                    }
                }
            } catch (e: Exception) {
                onDone("连接失败：${e.message?.take(100) ?: "未知错误"}")
            }
        }
    }

    fun clearAllConversations(onDone: (String) -> Unit) {
        viewModelScope.launch {
            conversationRepository.clearAll()
            onDone("已清空所有对话")
        }
    }
}
