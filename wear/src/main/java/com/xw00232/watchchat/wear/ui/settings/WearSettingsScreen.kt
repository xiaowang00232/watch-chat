package com.xw00232.watchchat.wear.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchchat.app.ui.settings.SettingsViewModel

@Composable
fun WearSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("") }
    var models by remember { mutableStateOf(listOf<String>()) }
    var systemPrompt by remember { mutableStateOf("") }
    var streamEnabled by remember { mutableStateOf(true) }
    var resumeLastConversation by remember { mutableStateOf(true) }
    var newModel by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val settings = viewModel.loadSettings()
        apiKey = viewModel.loadApiKey().orEmpty()
        baseUrl = settings.baseUrl
        selectedModel = settings.selectedModel
        models = settings.models
        systemPrompt = settings.systemPrompt
        streamEnabled = settings.streamEnabled
        resumeLastConversation = settings.resumeLastConversation
        ready = true
    }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 6.dp)
    ) {
        if (!ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color(0xFF1A73E8),
                    modifier = Modifier.size(24.dp)
                )
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
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
                    text = "设置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            WearSectionLabel("服务配置")
            WearLabel("Base URL")
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://api.deepseek.com", fontSize = 10.sp, color = Color(0xFF6F767E)) },
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                shape = RoundedCornerShape(8.dp),
                colors = wearTfColors()
            )
            WearLabel("API Key")
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("sk-…", fontSize = 10.sp, color = Color(0xFF6F767E)) },
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                shape = RoundedCornerShape(8.dp),
                colors = wearTfColors()
            )
            WearLabel("系统提示词")
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text("留空即无预设", fontSize = 10.sp, color = Color(0xFF6F767E)) },
                textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                shape = RoundedCornerShape(8.dp),
                colors = wearTfColors()
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "流式输出", fontSize = 11.sp, color = Color.White)
                Switch(
                    checked = streamEnabled,
                    onCheckedChange = { streamEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1A73E8),
                        checkedTrackColor = Color(0xFF1A73E8).copy(alpha = 0.4f)
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "打开时继续上次对话", fontSize = 11.sp, color = Color.White)
                    Text(
                        text = "启动后自动加载最近一次对话",
                        fontSize = 9.sp,
                        color = Color(0xFF9AA0A6)
                    )
                }
                Switch(
                    checked = resumeLastConversation,
                    onCheckedChange = { resumeLastConversation = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1A73E8),
                        checkedTrackColor = Color(0xFF1A73E8).copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(Modifier.height(6.dp))
            WearSectionLabel("模型管理")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "默认: ${selectedModel.ifBlank { "未选" }}",
                    fontSize = 10.sp,
                    color = Color(0xFF9AA0A6),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { newModel = ""; showAddDialog = true },
                    modifier = Modifier.height(26.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("添加", fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            models.forEach { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = model,
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { selectedModel = model; toast("已设为默认: $model") },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "设为默认",
                            modifier = Modifier.size(13.dp),
                            tint = if (model == selectedModel) Color(0xFF1A73E8) else Color(0xFF9AA0A6)
                        )
                    }
                    if (models.size > 1) {
                        IconButton(
                            onClick = {
                                val updated = models - model
                                models = updated
                                if (selectedModel == model) selectedModel = updated.firstOrNull().orEmpty()
                                toast("已删除 $model")
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "删除",
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFFF28B82)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            baseUrl.isBlank() -> toast("请填写 Base URL")
                            selectedModel.isBlank() -> toast("请至少保留一个模型")
                            else -> viewModel.save(
                                apiKey = apiKey,
                                baseUrl = baseUrl,
                                selectedModel = selectedModel,
                                models = models,
                                systemPrompt = systemPrompt,
                                streamEnabled = streamEnabled,
                                resumeLastConversation = resumeLastConversation
                            ) { message -> toast(message) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    )
                ) { Text("保存", fontSize = 10.sp) }
                Button(
                    onClick = {
                        if (baseUrl.isBlank()) toast("请先填写 Base URL")
                        else viewModel.testConnection(apiKey, baseUrl, selectedModel) { message -> toast(message) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF232A33),
                        contentColor = Color.White
                    )
                ) { Text("测试连接", fontSize = 10.sp) }
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3C4043),
                        contentColor = Color.White
                    )
                ) { Text("返回", fontSize = 10.sp) }
            }

            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth().height(28.dp)
            ) { Text("清空所有对话", color = Color(0xFFF28B82), fontSize = 10.sp) }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showAddDialog) {
        AlertDialog(
            containerColor = Color(0xFF1F232B),
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加模型", color = Color.White, fontSize = 13.sp) },
            text = {
                OutlinedTextField(
                    value = newModel,
                    onValueChange = { newModel = it },
                    singleLine = true,
                    label = { Text("模型名", fontSize = 10.sp, color = Color(0xFF9AA0A6)) },
                    placeholder = { Text("deepseek-v4-flash", fontSize = 10.sp, color = Color(0xFF6F767E)) },
                    textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                    colors = wearTfColors()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val model = newModel.trim()
                    if (model.isNotBlank()) {
                        if (model !in models) models = models + model
                        if (selectedModel.isBlank()) selectedModel = model
                        toast("已添加 $model")
                        showAddDialog = false
                    }
                }) {
                    Text("添加", color = Color(0xFF1A73E8), fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消", color = Color(0xFF9AA0A6), fontSize = 11.sp)
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            containerColor = Color(0xFF1F232B),
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空所有对话？", color = Color.White, fontSize = 13.sp) },
            text = {
                Text("删除后将无法恢复。", color = Color(0xFF9AA0A6), fontSize = 11.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAllConversations { message -> toast(message) }
                }) {
                    Text("清空", color = Color(0xFFF28B82), fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消", color = Color(0xFF9AA0A6), fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
private fun wearTfColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF232A33),
    unfocusedContainerColor = Color(0xFF232A33),
    focusedIndicatorColor = Color(0xFF1A73E8),
    unfocusedIndicatorColor = Color(0xFF444A53)
)

@Composable
private fun WearSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1A73E8),
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun WearLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = Color(0xFF9AA0A6),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}
