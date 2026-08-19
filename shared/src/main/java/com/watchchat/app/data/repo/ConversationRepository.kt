// 共享模块：手机端与手表端共用
package com.watchchat.app.data.repo

import com.watchchat.app.data.export.ExportConversation
import com.watchchat.app.data.export.ExportMessage
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

    /** 导出全部对话（含消息），按时间正序。 */
    suspend fun exportConversations(): List<ExportConversation> =
        conversationDao.getAll().map { conv ->
            ExportConversation(
                title = conv.title,
                model = conv.model,
                createdAt = conv.createdAt,
                updatedAt = conv.updatedAt,
                messages = messageDao.getAllByConversation(conv.id).map { msg ->
                    ExportMessage(role = msg.role, content = msg.content, createdAt = msg.createdAt)
                }
            )
        }

    /** 批量导入对话（追加到现有数据），返回导入的对话数。 */
    suspend fun importConversations(list: List<ExportConversation>): Int {
        list.forEach { conv ->
            val id = conversationDao.insert(
                ConversationEntity(
                    title = conv.title,
                    model = conv.model,
                    createdAt = conv.createdAt,
                    updatedAt = conv.updatedAt
                )
            )
            conv.messages.forEach { msg ->
                messageDao.insert(
                    MessageEntity(
                        conversationId = id,
                        role = msg.role,
                        content = msg.content,
                        createdAt = msg.createdAt
                    )
                )
            }
        }
        return list.size
    }
}
