package com.xw00232.watchchat.wear.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchchat.app.data.local.ConversationEntity
import com.watchchat.app.ui.history.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WearHistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var confirmClearAll by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "返回",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
                Text(
                    text = "历史对话",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (conversations.isNotEmpty()) {
                    IconButton(
                        onClick = { confirmClearAll = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "清空",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFF28B82)
                        )
                    }
                }
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无历史对话", fontSize = 12.sp, color = Color(0xFF9AA0A6))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(conversations, key = { it.id }) { item ->
                        WearHistoryItem(
                            item = item,
                            onSelect = { onSelect(item.id) },
                            onDelete = { confirmDeleteId = item.id }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF232A33),
                        contentColor = Color.White
                    )
                ) { Text("返回聊天", fontSize = 11.sp) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (confirmClearAll) {
        AlertDialog(
            containerColor = Color(0xFF1F232B),
            onDismissRequest = { confirmClearAll = false },
            title = { Text("清空历史？", color = Color.White, fontSize = 13.sp) },
            text = {
                Text("将永久删除全部对话，不可恢复。", color = Color(0xFF9AA0A6), fontSize = 11.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    confirmClearAll = false
                }) {
                    Text("清空", color = Color(0xFFF28B82), fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text("取消", color = Color(0xFF9AA0A6), fontSize = 11.sp)
                }
            }
        )
    }

    confirmDeleteId?.let { deleteId ->
        AlertDialog(
            containerColor = Color(0xFF1F232B),
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("删除对话？", color = Color.White, fontSize = 13.sp) },
            text = {
                Text("该对话将永久删除。", color = Color(0xFF9AA0A6), fontSize = 11.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(deleteId)
                    confirmDeleteId = null
                }) {
                    Text("删除", color = Color(0xFFF28B82), fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) {
                    Text("取消", color = Color(0xFF9AA0A6), fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun WearHistoryItem(
    item: ConversationEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF232A33))
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { "新对话" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatTime(item.updatedAt),
                fontSize = 9.sp,
                color = Color(0xFF9AA0A6)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                modifier = Modifier.size(14.dp),
                tint = Color(0xFFF28B82)
            )
        }
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
