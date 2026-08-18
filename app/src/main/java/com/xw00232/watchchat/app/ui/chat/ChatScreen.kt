package com.xw00232.watchchat.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchchat.app.speech.SpeechRecognizerHelper
import com.watchchat.app.ui.chat.ChatViewModel
import com.watchchat.app.ui.chat.UiMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TYPEWRITER_TICK_MILLIS = 16L

/** 打字机效果：返回当前应显示的字数，逐字追赶完整文本。
 *  固定打字速度（与真实流式速度相当），超长文本温和加速避免拖太久；
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
            val step = maxOf(2, (text.length - visible) / 300)
            visible = (visible + step).coerceAtMost(text.length)
            delay(TYPEWRITER_TICK_MILLIS)
        }
        if (!isStreaming) onFinished()
    }
    return visible
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewChat: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    val showCopiedToast: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar("已复制到剪贴板") }
    }

    var listening by remember { mutableStateOf(false) }
    val speechHelper = remember {
        SpeechRecognizerHelper(
            context = context,
            onPartialResult = viewModel::onInputChange,
            onFinalResult = viewModel::onInputChange,
            onStateChange = { listening = it },
            onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
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
            scope.launch { snackbarHostState.showSnackbar("需要麦克风权限才能使用语音输入") }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        ModelSelector(
                            models = uiState.models,
                            selected = uiState.selectedModel,
                            enabled = !uiState.isStreaming,
                            onSelect = viewModel::onModelSelect
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNewChat) {
                        Icon(Icons.Default.Add, contentDescription = "新对话")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "历史对话")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(onClick = onMicClick) {
                        val micScale by animateFloatAsState(
                            targetValue = if (listening) 1.15f else 1f,
                            animationSpec = tween(200),
                            label = "micScale"
                        )
                        Icon(
                            if (listening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (listening) "停止语音" else "语音输入",
                            tint = if (listening) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.scale(micScale)
                        )
                    }
                    OutlinedTextField(
                        value = uiState.input,
                        onValueChange = viewModel::onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入或语音说话…") },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp)
                    )
                    val canSend = uiState.input.isNotBlank() && !uiState.isStreaming
                    IconButton(
                        onClick = viewModel::onSend,
                        enabled = canSend
                    ) {
                        val sendAlpha by animateFloatAsState(
                            targetValue = if (canSend) 1f else 0.4f,
                            animationSpec = tween(150),
                            label = "sendAlpha"
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(sendAlpha)
                        )
                    }
                }
            }
        }
    ) { padding ->
        val listState = rememberLazyListState()
        // 打字动画期间隐藏已落库的对应 AI 消息，避免与流式气泡重复显示
        val messages = uiState.messages.filterNot { it.id == uiState.streamingMessageId }
        val streamingText = uiState.streamingText
        val hasStreaming = streamingText != null

        LaunchedEffect(messages.size, streamingText) {
            val count = messages.size + if (hasStreaming) 1 else 0
            if (count > 0) listState.animateScrollToItem(count - 1)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (messages.isEmpty() && !hasStreaming) {
                item { EmptyChatHint(onSuggestion = viewModel::sendText) }
            }
            items(messages, key = { it.id }) { message ->
                AnimatedBubble(key = message.id) {
                    MessageBubble(
                        message = message,
                        onCopy = {
                            clipboard.setText(AnnotatedString(message.content))
                            showCopiedToast()
                        }
                    )
                }
            }
            if (hasStreaming) {
                item(key = "streaming") {
                    AssistantBubble(
                        text = streamingText.orEmpty(),
                        isStreaming = uiState.isStreaming,
                        onFinished = viewModel::onStreamingFinished,
                        onCopy = {
                            clipboard.setText(AnnotatedString(streamingText.orEmpty()))
                            showCopiedToast()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBubble(
    key: Any?,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(250)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(250)
        ),
        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = tween(150)
        )
    ) {
        content()
    }
}

@Composable
private fun ModelSelector(
    models: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            enabled = enabled && models.isNotEmpty(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                selected.ifBlank { "选择模型" },
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val arrowRotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = tween(200),
                label = "arrowRotation"
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "切换模型",
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = arrowRotation }
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model, fontSize = 13.sp) },
                    onClick = {
                        expanded = false
                        onSelect(model)
                    },
                    trailingIcon = if (model == selected) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: UiMessage,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(modifier = Modifier.widthIn(max = 320.dp)) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(
                modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantBubble(
    text: String,
    isStreaming: Boolean,
    onFinished: () -> Unit,
    onCopy: () -> Unit
) {
    val visibleLength = typewriterVisibleLength(text, isStreaming, onFinished)
    val shownText = text.substring(0, visibleLength)

    val cursorSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = 1000
            0.2f at 0
            1f at 500
            0.2f at 1000
        },
        repeatMode = RepeatMode.Reverse
    )
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = cursorSpec,
        label = "cursorAlpha"
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(modifier = Modifier.widthIn(max = 320.dp)) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                SelectionContainer {
                    Text(
                        text = if (isStreaming) "$shownText▍" else shownText,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .alpha(if (isStreaming) cursorAlpha else 1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatHint(onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "开始一段对话吧",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        listOf("帮我总结今天的要点", "推荐一部好看的科幻电影", "用三句话介绍你").forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestion(suggestion) },
                label = { Text(suggestion, fontSize = 13.sp) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
