package com.xw00232.watchchat.app.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchchat.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var ready by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("") }
    var models by remember { mutableStateOf(listOf<String>()) }
    var systemPrompt by remember { mutableStateOf("") }
    var streamEnabled by remember { mutableStateOf(true) }
    var showKey by remember { mutableStateOf(false) }
    var newModel by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val settings = viewModel.loadSettings()
        apiKey = viewModel.loadApiKey().orEmpty()
        baseUrl = settings.baseUrl
        selectedModel = settings.selectedModel
        models = settings.models
        systemPrompt = settings.systemPrompt
        streamEnabled = settings.streamEnabled
        ready = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (!ready) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionTitle("服务配置")

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                singleLine = true,
                visualTransformation = if (showKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "隐藏 Key" else "显示 Key"
                        )
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                singleLine = true
            )
            Text(
                "支持任何 OpenAI 兼容接口，如 DeepSeek（https://api.deepseek.com/v1）、Ollama 本地等",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("流式输出", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "如果接口不支持流式，请关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(checked = streamEnabled, onCheckedChange = { streamEnabled = it })
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("模型管理")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("默认模型", modifier = Modifier.weight(1f))
                ModelDropdown(
                    models = models,
                    selected = selectedModel,
                    onSelect = { selectedModel = it }
                )
            }
            Spacer(Modifier.height(8.dp))

            models.forEach { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        model,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = {
                            val updated = models - model
                            models = updated
                            if (selectedModel == model) {
                                selectedModel = updated.firstOrNull().orEmpty()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "删除 $model",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newModel,
                    onValueChange = { newModel = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("添加模型") },
                    placeholder = { Text("如 gpt-4o-mini") },
                    singleLine = true
                )
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = {
                        val model = newModel.trim()
                        if (model.isNotEmpty() && model !in models) {
                            models = models + model
                            if (selectedModel.isBlank()) selectedModel = model
                        }
                        newModel = ""
                    },
                    enabled = newModel.isNotBlank()
                ) { Text("添加") }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("高级")
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("系统提示词（可选）") },
                minLines = 3,
                maxLines = 6
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    when {
                        baseUrl.isBlank() -> scope.launch {
                            snackbarHostState.showSnackbar("请填写 Base URL")
                        }
                        selectedModel.isBlank() -> scope.launch {
                            snackbarHostState.showSnackbar("请至少保留一个模型")
                        }
                        else -> viewModel.save(
                            apiKey = apiKey,
                            baseUrl = baseUrl,
                            selectedModel = selectedModel,
                            models = models,
                            systemPrompt = systemPrompt,
                            streamEnabled = streamEnabled
                        ) { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存设置") }

            TextButton(
                onClick = {
                    if (baseUrl.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("请先填写 Base URL") }
                    } else {
                        viewModel.testConnection(
                            apiKey = apiKey,
                            baseUrl = baseUrl,
                            selectedModel = selectedModel
                        ) { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("测试连接") }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            TextButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("清空所有对话") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空所有对话？") },
            text = { Text("删除后将无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllConversations { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    }
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ModelDropdown(
    models: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                selected.ifBlank { "选择模型" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        expanded = false
                        onSelect(model)
                    },
                    trailingIcon = if (model == selected) {
                        {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else null
                )
            }
        }
    }
}
