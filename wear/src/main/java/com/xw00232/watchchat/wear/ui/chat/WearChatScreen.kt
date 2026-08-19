package com.xw00232.watchchat.wear.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchchat.app.speech.SpeechRecognizerHelper
import com.watchchat.app.ui.chat.ChatViewModel
import com.watchchat.app.ui.chat.UiMessage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TYPEWRITER_TICK_MILLIS = 24L

/** 打字机效果：返回当前应显示的字数，逐字追赶完整文本。
 *  手表端采用较低刷新频率（约 40fps）减少重组开销；
 *  文本全部显示且流已结束时回调 onFinished，由 ViewModel 切换到正式消息。 */
@Composable
private fun typewriterVisibleLength(
    text: String,
    isStreaming: Boolean,
    onFinished: () -> Unit
): Int {
    var visible by remember { mutableIntStateOf(0) }
    LaunchedEffect(text, isStreaming) {
        while (visible < text.length) {
            val step = maxOf(3, (text.length - visible) / 150)
            visible = (visible + step).coerceAtMost(text.length)
            delay(TYPEWRITER_TICK_MILLIS)
        }
        if (!isStreaming) onFinished()
    }
    return visible
}

/**
 * 手表端聊天页。
 * 布局针对 372×430 这类小屏做了适配：紧凑字号、可横向滑动的模型切换、
 * 输入框唤起系统输入法（语音优先）、错误与复制用 Toast 提示。
 */
@Composable
fun WearChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewChat: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var listening by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onErrorShown()
        }
    }

    val speechHelper = remember {
        SpeechRecognizerHelper(
            context = context,
            onPartialResult = viewModel::onInputChange,
            onFinalResult = viewModel::onInputChange,
            onStateChange = { listening = it },
            onError = { message -> Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        )
    }
    DisposableEffect(Unit) {
        onDispose { speechHelper.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechHelper.start()
        } else {
            Toast.makeText(context, "需要麦克风权限", Toast.LENGTH_LONG).show()
        }
    }

    val onMicClick = {
        when {
            listening -> {
                speechHelper.cancel()
                listening = false
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> speechHelper.start()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 6.dp)
    ) {
        // 顶栏：仅功能按钮（新对话 / 历史 / 设置），不显示应用名或对话标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNewChat, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "新对话", modifier = Modifier.size(16.dp), tint = Color.White)
            }
            IconButton(onClick = onOpenHistory, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.History, contentDescription = "历史对话", modifier = Modifier.size(16.dp), tint = Color.White)
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "设置", modifier = Modifier.size(16.dp), tint = Color.White)
            }
        }

        // 模型切换：横向滑动选择（用 Button）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            uiState.models.forEach { model ->
                val selected = model == uiState.selectedModel
                Button(
                    onClick = { viewModel.onModelSelect(model) },
                    enabled = !uiState.isStreaming,
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF1A73E8) else Color(0xFF232A33),
                        contentColor = if (selected) Color.White else Color(0xFF9AA0A6)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(model, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(2.dp))

        // 消息列表
        val listState = rememberLazyListState()
        // 打字动画期间隐藏已落库的对应 AI 消息，避免与流式气泡重复显示
        val messages = uiState.messages.filterNot { it.id == uiState.streamingMessageId }
        val streamingText = uiState.streamingText
        val hasStreaming = streamingText != null

        // 就近自动滚动：仅当接近底部时用即时滚动（无动画）跟随新消息，
        // 避免流式期间持续动画滚动导致手表卡顿，也不打断向上翻阅历史
        LaunchedEffect(messages.size, streamingText) {
            val count = messages.size + if (hasStreaming) 1 else 0
            if (count == 0) return@LaunchedEffect
            delay(50)
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            if (lastVisible >= count - 3) listState.scrollToItem(count - 1)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            if (messages.isEmpty() && !hasStreaming) {
                item { WearEmptyHint(onSuggestion = viewModel::sendText) }
            }
            items(messages, key = { it.id }) { message ->
                WearMessageBubble(
                    message = message,
                    onCopy = {
                        clipboard.setText(AnnotatedString(message.content))
                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            if (hasStreaming) {
                item(key = "streaming") {
                    WearStreamingBubble(
                        text = streamingText.orEmpty(),
                        isStreaming = uiState.isStreaming,
                        onFinished = viewModel::onStreamingFinished,
                        onCopy = {
                            clipboard.setText(AnnotatedString(streamingText.orEmpty()))
                            Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // 输入行：语音 / 系统输入法输入框 / 发送
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMicClick, modifier = Modifier.size(34.dp)) {
                val micIcon = if (listening) Icons.Default.Stop else Icons.Default.Mic
                Icon(
                    micIcon,
                    contentDescription = if (listening) "停止语音" else "语音输入",
                    modifier = Modifier.size(16.dp),
                    tint = if (listening) Color(0xFFF28B82) else Color.White
                )
            }
            OutlinedTextField(
                value = uiState.input,
                onValueChange = viewModel::onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("说话或输入…", fontSize = 11.sp, color = Color(0xFF6F767E)) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF232A33),
                    unfocusedContainerColor = Color(0xFF232A33),
                    focusedIndicatorColor = Color(0xFF1A73E8),
                    unfocusedIndicatorColor = Color(0xFF444A53)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            val canSend = uiState.input.isNotBlank() && !uiState.isStreaming
            IconButton(onClick = viewModel::onSend, enabled = canSend, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    modifier = Modifier.size(16.dp),
                    tint = if (canSend) Color(0xFF1A73E8) else Color(0xFF444A53)
                )
            }
        }
    }
}

@Composable
private fun WearMessageBubble(
    message: UiMessage,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.88f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isUser) Color(0xFF2B5BA8) else Color(0xFF232A33))
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White
                )
            }
            Row(
                modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(message.createdAt),
                    fontSize = 9.sp,
                    color = Color(0xFF9AA0A6)
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF9AA0A6)
                    )
                }
            }
        }
    }
}

@Composable
private fun WearStreamingBubble(
    text: String,
    isStreaming: Boolean,
    onFinished: () -> Unit,
    onCopy: () -> Unit
) {
    val visibleLength = typewriterVisibleLength(text, isStreaming, onFinished)
    val shownText = text.substring(0, visibleLength)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(modifier = Modifier.fillMaxWidth(0.88f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF232A33))
            ) {
                Text(
                    text = "$shownText▍",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI", fontSize = 9.sp, color = Color(0xFF9AA0A6))
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF9AA0A6)
                    )
                }
            }
        }
    }
}

@Composable
private fun WearEmptyHint(onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("开始一段对话吧", fontSize = 13.sp, color = Color(0xFF9AA0A6))
        Spacer(Modifier.height(8.dp))
        listOf("介绍你自己", "推荐一部电影", "总结今天的要点").forEach { suggestion ->
            Button(
                onClick = { onSuggestion(suggestion) },
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF232A33),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(suggestion, fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
