// 共享模块：手机端与手表端共用
package com.watchchat.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true
)

/** 流式响应中的单个数据块。 */
@Serializable
data class ChatCompletionChunk(
    val choices: List<ChunkChoice> = emptyList(),
    val error: ApiError? = null
)

@Serializable
data class ChunkChoice(
    val delta: ChunkDelta = ChunkDelta(),
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChunkDelta(
    val content: String? = null,
    val role: String? = null,
    /** deepseek-reasoner 的思维链内容，仅该模型流式返回时有值。 */
    @SerialName("reasoning_content") val reasoningContent: String? = null
)

/** 非流式响应。 */
@Serializable
data class ChatCompletion(
    val choices: List<CompletionChoice> = emptyList(),
    val error: ApiError? = null
)

@Serializable
data class CompletionChoice(
    val message: ChatMessage? = null
)

/** 统一的错误外层，兼容 OpenAI 风格的 { "error": { "message": ... } }。 */
@Serializable
data class ApiErrorResponse(
    val error: ApiError? = null
)

@Serializable
data class ApiError(
    val message: String? = null,
    val type: String? = null
)
