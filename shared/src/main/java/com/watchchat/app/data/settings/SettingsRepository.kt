// 共享模块：手机端与手表端共用
package com.watchchat.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.watchchat.app.data.export.ExportSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watchchat_settings")

/** 全局默认服务地址（自定义/未列出的模型回退用）。 */
private val DEFAULT_BASE_URL = "https://api.deepseek.com"

val DEFAULT_MODELS = listOf(
    "deepseek-v4-flash",
    "deepseek-chat",
    "deepseek-reasoner",
    "gpt-4o-mini",
    "gpt-4o",
    "gpt-3.5-turbo",
    "qwen-plus",
    "glm-4-flash",
    "moonshot-v1-8k",
    "mimo-v2.5-pro"
)

/** 内置服务商地址：模型名 → 默认 Base URL。选择该模型时自动使用，可被每模型配置或全局默认覆盖。 */
val BUILT_IN_BASE_URLS = mapOf(
    "deepseek-v4-flash" to "https://api.deepseek.com",
    "deepseek-chat" to "https://api.deepseek.com",
    "deepseek-reasoner" to "https://api.deepseek.com",
    "gpt-4o-mini" to "https://api.openai.com/v1",
    "gpt-4o" to "https://api.openai.com/v1",
    "gpt-3.5-turbo" to "https://api.openai.com/v1",
    "qwen-plus" to "https://dashscope.aliyuncs.com/compatible-mode/v1",
    "glm-4-flash" to "https://open.bigmodel.cn/api/paas/v4",
    "moonshot-v1-8k" to "https://api.moonshot.cn/v1",
    "mimo-v2.5-pro" to "https://api.xiaomimimo.com/v1"
)

private const val DEFAULT_SYSTEM_PROMPT = ""

/** 单个模型的独立服务配置；apiKey 在内存中为明文，落盘/导出时加密。 */
@Serializable
data class ProviderConfig(
    val baseUrl: String = "",
    val apiKey: String = ""
)

data class AppSettings(
    val baseUrl: String = DEFAULT_BASE_URL,
    val selectedModel: String = DEFAULT_MODELS.first(),
    val models: List<String> = DEFAULT_MODELS,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val streamEnabled: Boolean = true,
    /** 启动 App 时自动加载最近一次对话，而不是新建对话。 */
    val resumeLastConversation: Boolean = true,
    /** 每个模型独立的服务配置（baseUrl + apiKey，明文）；未配置的模型回退内置地址/全局默认。 */
    val providers: Map<String, ProviderConfig> = emptyMap()
)

/** 非敏感设置存 DataStore；API Key 经 Keystore 加密后再存 DataStore。 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key_encrypted")
        val BASE_URL = stringPreferencesKey("base_url")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val MODELS = stringPreferencesKey("models")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val STREAM_ENABLED = booleanPreferencesKey("stream_enabled")
        val RESUME_LAST_CONVERSATION = booleanPreferencesKey("resume_last_conversation")
        val PROVIDERS = stringPreferencesKey("providers")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            baseUrl = prefs[Keys.BASE_URL] ?: DEFAULT_BASE_URL,
            selectedModel = prefs[Keys.SELECTED_MODEL] ?: DEFAULT_MODELS.first(),
            models = prefs[Keys.MODELS]
                ?.split("\n")
                ?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_MODELS,
            systemPrompt = prefs[Keys.SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT,
            streamEnabled = prefs[Keys.STREAM_ENABLED] ?: true,
            resumeLastConversation = prefs[Keys.RESUME_LAST_CONVERSATION] ?: true,
            providers = decodeProviders(prefs[Keys.PROVIDERS])
        )
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.API_KEY]?.let { ApiKeyCipher.decrypt(it) }
    }

    suspend fun currentSettings(): AppSettings = settingsFlow.first()

    suspend fun currentApiKey(): String? = apiKeyFlow.first()

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { prefs ->
            if (key.isBlank()) {
                prefs.remove(Keys.API_KEY)
            } else {
                prefs[Keys.API_KEY] = ApiKeyCipher.encrypt(key.trim())
            }
        }
    }

    suspend fun saveAll(
        baseUrl: String,
        selectedModel: String,
        models: List<String>,
        systemPrompt: String,
        streamEnabled: Boolean,
        resumeLastConversation: Boolean,
        providers: Map<String, ProviderConfig>
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = baseUrl.trim().trimEnd('/')
            prefs[Keys.SELECTED_MODEL] = selectedModel
            prefs[Keys.MODELS] = models.filter { it.isNotBlank() }.distinct().joinToString("\n")
            prefs[Keys.SYSTEM_PROMPT] = systemPrompt.trim()
            prefs[Keys.STREAM_ENABLED] = streamEnabled
            prefs[Keys.RESUME_LAST_CONVERSATION] = resumeLastConversation
            prefs[Keys.PROVIDERS] = encodeProviders(providers)
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { prefs -> prefs[Keys.SELECTED_MODEL] = model }
    }

    /** 导出设置（含加密后的 Key，设备绑定）。 */
    suspend fun exportSettings(): ExportSettings {
        val s = currentSettings()
        return ExportSettings(
            baseUrl = s.baseUrl,
            selectedModel = s.selectedModel,
            models = s.models,
            systemPrompt = s.systemPrompt,
            streamEnabled = s.streamEnabled,
            resumeLastConversation = s.resumeLastConversation,
            providers = s.providers.mapValues { (_, c) ->
                c.copy(apiKey = if (c.apiKey.isBlank()) "" else ApiKeyCipher.encrypt(c.apiKey.trim()))
            },
            apiKeyEncrypted = context.dataStore.data.first()[Keys.API_KEY]
        )
    }

    /** 导入设置；文件中的 Key 已是加密值，原样写回（仅同设备可解密）。 */
    suspend fun importSettings(s: ExportSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = s.baseUrl.trim().trimEnd('/')
            prefs[Keys.SELECTED_MODEL] = s.selectedModel
            prefs[Keys.MODELS] = s.models.filter { it.isNotBlank() }.distinct().joinToString("\n")
            prefs[Keys.SYSTEM_PROMPT] = s.systemPrompt.trim()
            prefs[Keys.STREAM_ENABLED] = s.streamEnabled
            prefs[Keys.RESUME_LAST_CONVERSATION] = s.resumeLastConversation
            s.apiKeyEncrypted?.let { prefs[Keys.API_KEY] = it }
            prefs[Keys.PROVIDERS] = json.encodeToString(s.providers)
        }
    }

    private fun decodeProviders(raw: String?): Map<String, ProviderConfig> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, ProviderConfig>>(raw) }
            .getOrNull()
            ?.mapValues { (_, c) ->
                c.copy(apiKey = c.apiKey.ifBlank { "" }.let { k -> ApiKeyCipher.decrypt(k) ?: "" })
            }
            ?: emptyMap()
    }

    private fun encodeProviders(providers: Map<String, ProviderConfig>): String = json.encodeToString(
        providers.mapValues { (_, c) ->
            c.copy(apiKey = if (c.apiKey.isBlank()) "" else ApiKeyCipher.encrypt(c.apiKey.trim()))
        }
    )
}
