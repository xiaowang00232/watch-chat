// 共享模块：手机端与手表端共用
package com.watchchat.app.data.repo

import com.watchchat.app.data.local.ConversationDao
import com.watchchat.app.data.local.ConversationEntity
import com.watchchat.app.data.local.MessageDao
import com.watchchat.app.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    fun observeConversation(id: Long): Flow<ConversationEntity?> = conversationDao.observeById(id)

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.observeByConversation(conversationId)

    suspend fun createConversation(title: String, model: String): Long =
        conversationDao.insert(ConversationEntity(title = title, model = model))

    /** 最近更新的一条对话；没有对话时返回 null。 */
    suspend fun mostRecentConversation(): ConversationEntity? = conversationDao.mostRecent()

    /** 插入一条消息，返回新消息 id。 */
    suspend fun addMessage(conversationId: Long, role: String, content: String): Long =
        messageDao.insert(MessageEntity(conversationId = conversationId, role = role, content = content))

    /** 返回按时间正序的最近消息（role, content）。 */
    suspend fun recentMessages(conversationId: Long, limit: Int): List<Pair<String, String>> =
        messageDao.recentDesc(conversationId, limit).reversed().map { it.role to it.content }

    suspend fun touch(conversationId: Long) {
        conversationDao.getById(conversationId)?.let {
            conversationDao.update(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updateModel(conversationId: Long, model: String) {
        conversationDao.getById(conversationId)?.let {
            conversationDao.update(it.copy(model = model))
        }
    }

    suspend fun deleteConversation(id: Long) = conversationDao.deleteById(id)

    suspend fun clearAll() = conversationDao.deleteAll()
}
