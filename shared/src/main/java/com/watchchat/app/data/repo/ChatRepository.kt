// 共享模块：手机端与手表端共用
package com.watchchat.app.data.repo

import com.watchchat.app.data.remote.ApiErrorResponse
import com.watchchat.app.data.remote.ChatCompletionChunk
import com.watchchat.app.data.remote.ChatMessage
import com.watchchat.app.data.remote.ChatRequest
import com.watchchat.app.data.remote.OpenAiApi
import com.watchchat.app.data.remote.OpenAiService
import com.watchchat.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.io.IOException

/**
 * 对话业务核心：
 * 1. 把用户消息写入本地数据库（记忆）
 * 2. 取最近 CONTEXT_MESSAGE_LIMIT 条消息作为上下文发给模型
 * 3. 流式接收回复并逐字下发，结束后写入本地数据库
 */
class ChatRepository(
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository
) {

    fun streamAssistantReply(conversationId: Long, userText: String): Flow<String> = flow {
        conversationRepository.addMessage(conversationId, ROLE_USER, userText)
        conversationRepository.touch(conversationId)

        val settings = settingsRepository.currentSettings()
        val history = conversationRepository.recentMessages(conversationId, CONTEXT_MESSAGE_LIMIT)
        val messages = buildList {
            if (settings.systemPrompt.isNotBlank()) {
                add(ChatMessage(ROLE_SYSTEM, settings.systemPrompt))
            }
            addAll(history.map { ChatMessage(it.first, it.second) })
        }
        val request = ChatRequest(
            model = settings.selectedModel,
            messages = messages,
            stream = settings.streamEnabled
        )

        val api = OpenAiService.create(settings.baseUrl)
        val auth = "Bearer ${settingsRepository.currentApiKey().orEmpty()}"

        val reply = if (settings.streamEnabled) {
            val streamed = streamFromApi(api, auth, request) { chunk -> emit(chunk) }
            // 流式未返回任何正文内容时，回退到非流式请求，避免"无回复"
            if (streamed.isBlank()) {
                val text = requestFromApi(api, auth, request.copy(stream = false))
                emit(text)
                text
            } else {
                streamed
            }
        } else {
            val text = requestFromApi(api, auth, request)
            emit(text)
            text
        }

        conversationRepository.addMessage(
            conversationId,
            ROLE_ASSISTANT,
            reply.ifBlank { "(无回复内容)" }
        )
        conversationRepository.touch(conversationId)
    }

    private suspend fun streamFromApi(
        api: OpenAiApi,
        auth: String,
        request: ChatRequest,
        onChunk: suspend (String) -> Unit
    ): String {
        val response = api.streamChat(auth, request)
        if (!response.isSuccessful) throw httpException(response)
        val body = response.body() ?: throw IOException("响应为空")
        val source = body.source()
        val builder = StringBuilder()
        val reasoningBuilder = StringBuilder()
        var contentStarted = false
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                val data = line.trim().removePrefix("data:").trim()
                if (data.isEmpty()) continue
                if (data == "[DONE]") break
                val chunk = try {
                    OpenAiService.json.decodeFromString(ChatCompletionChunk.serializer(), data)
                } catch (e: Exception) {
                    continue
                }
                // 处理流式过程中的错误事件（HTTP 200 但 body 内含 error）
                chunk.error?.let { throw IOException("服务返回错误：${it.message ?: it.type}") }

                val delta = chunk.choices.firstOrNull()?.delta ?: continue
                // deepseek-reasoner 先输出 reasoning_content（思维链），再输出 content（正文）
                if (!delta.reasoningContent.isNullOrEmpty()) {
                    reasoningBuilder.append(delta.reasoningContent)
                    onChunk(delta.reasoningContent)
                }
                if (!delta.content.isNullOrEmpty()) {
                    // 从思维链切换到正文时插入分隔符
                    if (!contentStarted && reasoningBuilder.isNotEmpty()) {
                        onChunk("\n\n")
                        contentStarted = true
                    }
                    builder.append(delta.content)
                    onChunk(delta.content)
                }
            }
        } finally {
            body.close()
        }
        return builder.toString()
    }

    private suspend fun requestFromApi(
        api: OpenAiApi,
        auth: String,
        request: ChatRequest
    ): String {
        val response = api.chat(auth, request)
        if (!response.isSuccessful) throw httpException(response)
        val body = response.body()
        body?.error?.let { throw IOException("服务返回错误：${it.message ?: it.type}") }
        return body?.choices?.firstOrNull()?.message?.content.orEmpty()
    }

    private suspend fun httpException(response: Response<*>): IOException {
        val raw = response.errorBody()?.string().orEmpty()
        val message = try {
            OpenAiService.json.decodeFromString(ApiErrorResponse.serializer(), raw).error?.message
        } catch (e: Exception) {
            null
        } ?: raw.take(300)
        return IOException("HTTP ${response.code()}：$message")
    }

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"
        const val CONTEXT_MESSAGE_LIMIT = 20
    }
}
