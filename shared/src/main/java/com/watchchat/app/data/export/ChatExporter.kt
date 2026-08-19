// 共享模块：手机端与手表端共用
package com.watchchat.app.data.export

import com.watchchat.app.data.settings.ProviderConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 导出的单条消息。 */
@Serializable
data class ExportMessage(
    val role: String,
    val content: String,
    val createdAt: Long = 0
)

/** 导出的单个对话（含全部消息）。 */
@Serializable
data class ExportConversation(
    val title: String,
    val model: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val messages: List<ExportMessage> = emptyList()
)

/** 导出的设置（Key 为加密值，设备绑定）。 */
@Serializable
data class ExportSettings(
    val baseUrl: String = "",
    val selectedModel: String = "",
    val models: List<String> = emptyList(),
    val systemPrompt: String = "",
    val streamEnabled: Boolean = true,
    val resumeLastConversation: Boolean = true,
    val providers: Map<String, ProviderConfig> = emptyMap(),
    val apiKeyEncrypted: String? = null
)

/** 备份文件根结构：conversations 必有，settings 仅在"导出对话和设置"时存在。 */
@Serializable
data class ExportData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val conversations: List<ExportConversation> = emptyList(),
    val settings: ExportSettings? = null
)

object ChatExporter {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun encode(data: ExportData): String = json.encodeToString(ExportData.serializer(), data)

    fun decode(text: String): ExportData {
        val data = json.decodeFromString(ExportData.serializer(), text)
        require(data.version <= 1) { "不支持的备份版本：${data.version}" }
        return data
    }
}
