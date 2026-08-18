// 共享模块：手机端与手表端共用
package com.watchchat.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watchchat_settings")

private val DEFAULT_BASE_URL = "https://api.deepseek.com"

private val DEFAULT_MODELS = listOf(
    "deepseek-v4-flash",
    "deepseek-chat",
    "deepseek-reasoner",
    "gpt-4o-mini",
    "gpt-4o",
    "gpt-3.5-turbo",
    "qwen-plus",
    "glm-4-flash",
    "moonshot-v1-8k"
)

private const val DEFAULT_SYSTEM_PROMPT = ""

data class AppSettings(
    val baseUrl: String = DEFAULT_BASE_URL,
    val selectedModel: String = DEFAULT_MODELS.first(),
    val models: List<String> = DEFAULT_MODELS,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val streamEnabled: Boolean = true,
    /** 启动 App 时自动加载最近一次对话，而不是新建对话。 */
    val resumeLastConversation: Boolean = true
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
    }

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
            resumeLastConversation = prefs[Keys.RESUME_LAST_CONVERSATION] ?: true
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
        resumeLastConversation: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = baseUrl.trim().trimEnd('/')
            prefs[Keys.SELECTED_MODEL] = selectedModel
            prefs[Keys.MODELS] = models.filter { it.isNotBlank() }.distinct().joinToString("\n")
            prefs[Keys.SYSTEM_PROMPT] = systemPrompt.trim()
            prefs[Keys.STREAM_ENABLED] = streamEnabled
            prefs[Keys.RESUME_LAST_CONVERSATION] = resumeLastConversation
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { prefs -> prefs[Keys.SELECTED_MODEL] = model }
    }
}
