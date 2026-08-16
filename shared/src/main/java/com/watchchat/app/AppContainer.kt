package com.watchchat.app

import android.content.Context
import com.watchchat.app.data.local.AppDatabase
import com.watchchat.app.data.repo.ChatRepository
import com.watchchat.app.data.repo.ConversationRepository
import com.watchchat.app.data.settings.SettingsRepository

/** 极简手动依赖容器，手机端与手表端共用同一套数据层。 */
class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)

    val conversationRepository = ConversationRepository(
        database.conversationDao(),
        database.messageDao()
    )
    val settingsRepository = SettingsRepository(context)
    val chatRepository = ChatRepository(conversationRepository, settingsRepository)
}
