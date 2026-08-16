// 共享模块：手机端与手表端共用
package com.watchchat.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一段对话（会话）。MVP 只记录标题、使用的模型与时间。 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val model: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
