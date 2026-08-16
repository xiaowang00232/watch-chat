package com.xw00232.watchchat.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    // 使用 Wear 组件库默认配色（深色为主），后续可做跟随系统的动态取色
    MaterialTheme(content = content)
}
