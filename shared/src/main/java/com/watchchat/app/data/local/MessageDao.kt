// 共享模块：手机端与手表端共用
package com.watchchat.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeByConversation(conversationId: Long): Flow<List<MessageEntity>>

    /** 返回最近 limit 条（倒序），由调用方再反转。 */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC, id DESC LIMIT :limit")
    suspend fun recentDesc(conversationId: Long, limit: Int): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long
}
