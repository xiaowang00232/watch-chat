# WatchChat —— 手表 & 手机 AI 对话助手

面向智能手表场景设计的 AI 对话 App，同时提供手机端版本方便日常调试与快速体验。工程分为三个模块：

- **shared**：数据层与业务逻辑（数据库、API 调用、设置、语音识别），手机端与手表端共用；
- **app**：Android 手机端（包名 `com.xw00232.watchchat.app`，方便日常调试与快速体验）；
- **wear**：Wear OS 手表端（包名 `com.xw00232.watchchat.wear`，适配 372×430 / 326ppi 小屏）。

## 功能特性

- **自带 API Key**：在设置页粘贴任意 OpenAI 兼容接口的 Key，经 Android Keystore 加密后本地存储，不上传任何服务器。
- **多模型切换**：内置常用模型列表，默认 `deepseek-v4-flash`；聊天页顶部可即时切换，每个对话记住当时使用的模型。
- **复制**：每条消息下方有复制按钮，一键复制到剪贴板；长按消息文本也可系统选中复制。
- **记忆对话**：
  - 本地存储：所有对话保存在本地 Room 数据库，历史页可随时回看、继续、删除。
  - 上下文记忆：每次请求会把最近 20 条消息作为上下文一起发给模型，AI 记得你们之前聊过什么。
- **语音输入**：点击麦克风直接说话，识别结果实时回填输入框（调用系统语音识别，无需额外服务）。
- **流式输出**：AI 回复逐字显示，等待更直观；默认关闭，可在设置里开启以兼容特殊接口。
- **思维链支持**：正确处理 DeepSeek `deepseek-reasoner` 模型的 `reasoning_content` 字段，推理过程与正文分开展示。
- **测试连接**：设置页一键验证 Key、地址、模型是否可用。
- **调用系统输入法**：手表端点输入框即唤起系统输入法（Wear OS 默认语音输入，支持键盘的手表则弹出键盘）。
- **手表直接联网**：手表端直接向 API 发起请求，不依赖手机转发（需手表连接 Wi-Fi / LTE）。
- **Key 只存在本地**：手机端与手表端各自的 Key 均通过系统 Keystore 加密落盘，不上传、不云端中转。
- **372×430 小屏适配**：手表端采用紧凑字号、横向滑动的模型切换、可滚动列表与安全区适配，兼顾圆形 / 方形表盘。
- **动画优化**：消息气泡渐入 + 流式打字光标呼吸效果，交互更顺滑。
- **极简顶栏**：聊天页顶栏无返回图标，通过底部导航 / 系统手势切换页面。
- **右滑退出**：手表端通过主题配置 `windowSwipeToDismiss`，支持从左向右滑动退出 App。
- **统一图标**：手机端与手表端使用相同的应用图标（蓝色背景 + 白色对话气泡）。
- 浅色 / 深色主题自适应（Android 12+ 跟随系统动态取色）。

## 默认配置

开箱即用的默认值，适配 DeepSeek 用户：

| 配置项 | 默认值 |
| --- | --- |
| Base URL | `https://api.deepseek.com` |
| 默认模型 | `deepseek-v4-flash` |
| 流式输出 | 关闭 |
| 系统提示词 | 空（不预设） |

内置模型列表：`deepseek-v4-flash`、`deepseek-chat`、`deepseek-reasoner`、`gpt-4o-mini`、`gpt-4o`、`gpt-3.5-turbo`、`qwen-plus`、`glm-4-flash`、`moonshot-v1-8k`。可在设置页自由增删。

## 快速开始

### 方式一：直接安装预构建 APK

已为手机端和手表端分别生成 debug APK：

| 版本 | 路径 | 包名 | 适用设备 |
| --- | --- | --- | --- |
| 手机端 | `app/build/outputs/apk/debug/app-debug.apk` | `com.xw00232.watchchat.app` | Android 8.0+ 手机，方便调试测试 |
| 手表端 | `wear/build/outputs/apk/debug/wear-debug.apk` | `com.xw00232.watchchat.wear` | Wear OS 手表（372×430 / 326ppi） |

```bash
# 手机（启用 ADB 调试后）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 手表（通过 USB 或 adb pair 连接到手表）
adb install -r wear/build/outputs/apk/debug/wear-debug.apk
```

安装后进入设置页面填入 API Key 即可直接使用（默认 Base URL、模型、流式开关均已配好）。

### 方式二：从源码构建

1. 安装 [Android Studio](https://developer.android.com/studio)（Ladybug 或更新版本均可），或自行配置 JDK 17 + Android SDK + Gradle 8.9。
2. 打开本项目目录（`File → Open`，选择 `WatchChat` 文件夹），等待 Gradle 同步完成（首次需联网下载依赖）。
3. 连接 Android 手机（Android 8.0+）或启动模拟器，选择 **app** 模块运行。
4. 打开 App 进入 **设置**：
   - 填入你的 API Key；
   - 确认 Base URL 与默认模型（DeepSeek 用户通常无需修改）；
   - 点 **保存设置**，再点 **测试连接** 验证。
5. 回到聊天页，说话或打字，开始对话。

**在手表上运行（wear 模块）**

1. 手表开启「开发者选项 → ADB 调试」，并通过 Wi-Fi 或 USB 连接电脑（`adb devices` 能看到设备）。
2. 在 Android Studio 中把运行目标切换为手表设备，选择 **wear** 模块直接运行；
   或命令行构建：`gradlew :wear:assembleDebug`，再 `adb install -r wear/build/outputs/apk/debug/wear-debug.apk`。
3. 手表需连接 Wi-Fi / LTE 才能直连 API；首次使用在手表端设置里填入 Key 与 Base URL。

> 手机端与手表端数据互不相通（各自本地存储），分别在两端配置即可。

### 命令行构建

```bash
# 同时构建手机端与手表端 debug APK
gradlew :app:assembleDebug :wear:assembleDebug

# 构建产物位置
# app/build/outputs/apk/debug/app-debug.apk
# wear/build/outputs/apk/debug/wear-debug.apk
```

环境要求：JDK 17、Android SDK（compileSdk 35，buildToolsVersion 36.0.0）。

## 支持的服务商（任选其一）

| 服务商 | Base URL | 说明 |
| --- | --- | --- |
| DeepSeek | `https://api.deepseek.com` | **默认值**；也可用 `https://api.deepseek.com/v1` |
| OpenAI | `https://api.openai.com/v1` | |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 需开通 DashScope |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | |
| Kimi | `https://api.moonshot.cn/v1` | |
| Ollama（本地） | `http://<电脑IP>:11434/v1` | 模型填本地模型名，Key 可随便填 |

> 原理上任何 OpenAI 兼容的 `POST /chat/completions` 接口都可以用。

## 技术栈

| 分类 | 技术 / 版本 |
| --- | --- |
| 语言 | Kotlin 2.0.21 |
| 构建 | Android Gradle Plugin 8.7.3、Gradle 8.9、JDK 17 |
| UI | Jetpack Compose（BOM 2024.10.01）、Material3 |
| Wear OS | Wear Compose 1.4.0（material + foundation） |
| 导航 | Navigation Compose 2.8.4 |
| 数据库 | Room 2.6.1 |
| 偏好存储 | DataStore Preferences 1.1.1 |
| 网络 | Retrofit 2.11.0 + OkHttp 4.12.0 |
| 序列化 | kotlinx.serialization 1.7.3 |
| 协程 | kotlinx.coroutines 1.9.0 |
| 最低 SDK | 26（Android 8.0） |
| 目标 SDK | 35（Android 15） |

## 项目结构

```
WatchChat/
├── shared/                          数据层 + 业务逻辑（两端共用，包名 com.watchchat.app）
│   └── src/main/java/com/watchchat/app/
│       ├── data/
│       │   ├── local/               Room 数据库（会话 + 消息，记忆功能）
│       │   │   ├── AppDatabase.kt
│       │   │   ├── ConversationDao.kt / ConversationEntity.kt
│       │   │   └── MessageDao.kt / MessageEntity.kt
│       │   ├── remote/              OpenAI 兼容接口（流式 / 非流式）
│       │   │   ├── Dtos.kt          含 reasoning_content 字段处理
│       │   │   ├── OpenAiApi.kt
│       │   │   └── OpenAiService.kt
│       │   ├── repo/                对话业务逻辑（拼接上下文、读写本地）
│       │   │   ├── ChatRepository.kt
│       │   │   └── ConversationRepository.kt
│       │   └── settings/            设置存储 + API Key 加密
│       │       ├── ApiKeyCipher.kt
│       │       └── SettingsRepository.kt
│       ├── speech/                  语音识别封装
│       │   └── SpeechRecognizerHelper.kt
│       ├── ui/                      三个 ViewModel（聊天 / 历史 / 设置）
│       │   ├── chat/ChatViewModel.kt
│       │   ├── history/HistoryViewModel.kt
│       │   └── settings/SettingsViewModel.kt
│       └── AppContainer.kt          依赖注入容器
│
├── app/                             手机端（包名 com.xw00232.watchchat.app）
│   └── src/main/java/com/xw00232/watchchat/app/
│       ├── ui/
│       │   ├── chat/ChatScreen.kt        聊天页（无返回图标 + 动画优化）
│       │   ├── history/HistoryScreen.kt  历史对话页
│       │   ├── settings/SettingsScreen.kt 设置页
│       │   ├── theme/                     主题（Color / Theme / Type）
│       │   ├── AppNavHost.kt              导航
│       │   └── AppViewModelProvider.kt    ViewModel 工厂
│       ├── MainActivity.kt
│       └── WatchChatApp.kt
│
├── wear/                            手表端（包名 com.xw00232.watchchat.wear）
│   └── src/main/java/com/xw00232/watchchat/wear/
│       ├── ui/
│       │   ├── chat/WearChatScreen.kt        手表聊天页
│       │   ├── history/WearHistoryScreen.kt  手表历史页
│       │   ├── settings/WearSettingsScreen.kt 手表设置页
│       │   ├── theme/WearTheme.kt
│       │   ├── WearNavHost.kt
│       │   └── WearViewModelProvider.kt
│       ├── MainActivity.kt
│       └── WearWatchChatApp.kt
│
├── gradle/libs.versions.toml        版本目录（统一依赖管理）
├── build.gradle.kts                 项目级构建脚本
├── settings.gradle.kts
└── README.md
```

## 模块包名

| 模块 | 包名 / applicationId | 说明 |
| --- | --- | --- |
| shared | `com.watchchat.app` | 共享库，不独立安装 |
| app | `com.xw00232.watchchat.app` | 手机端 APK |
| wear | `com.xw00232.watchchat.wear` | 手表端 APK |

> shared 模块包名保持 `com.watchchat.app`，app 和 wear 模块通过 import 引用其内部的 ViewModel、数据层等类。

## 常见问题

- **401**：API Key 无效或已过期。
- **403**：Key 没有该模型的访问权限，换个模型试试。
- **404**：Base URL 填错了，检查是否需要 `/v1` 后缀。
- **429**：请求太频繁或额度不足。
- **超时 / 连接失败**：检查网络；使用 Ollama 时手机和电脑需在同一局域网。
- **手表上网络不通**：确认手表已连接 Wi-Fi / LTE；部分手表默认走蓝牙代理，需要在手表设置中关闭「通过手机连接」或连接 Wi-Fi。
- **语音按钮无反应**：确认已授予麦克风权限；部分模拟器不支持语音识别。
- **流式无输出**：部分接口对流式支持不完整，可在设置中关闭流式输出改为整包返回。

## 后续规划

- 手表与手机数据同步（手机端配置 Key，一键推送到手表）；
- Markdown 渲染、TTS 语音朗读回复；
- 上下文长度可视化配置；
- 多账号 / 多服务商并存；
- 手表端蓝牙代理模式（无 Wi-Fi 时经手机转发请求）。

---

> 本项目部分使用 AI 辅助。
