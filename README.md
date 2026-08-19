# WatchChat —— 手表 & 手机 AI 对话助手

面向智能手表场景设计的 AI 对话 App，同时提供手机端版本方便日常调试与快速体验。工程分为三个模块：

- **shared**：数据层与业务逻辑（数据库、API 调用、设置、语音识别、导入导出），手机端与手表端共用；
- **app**：Android 手机端（包名 `com.xw00232.watchchat.app`，方便日常调试与快速体验）；
- **wear**：Wear OS 手表端（包名 `com.xw00232.watchchat.wear`，适配 372×430 / 326ppi 小屏）。

当前版本：**0.1.3**（versionCode 3）

## 功能特性

- **自带 API Key**：在设置页粘贴任意 OpenAI 兼容接口的 Key，经 Android Keystore 加密后本地存储，不上传任何服务器。
- **多服务商并存**：内置 DeepSeek / OpenAI / 通义 / GLM / Kimi / 小米 MiMo 等服务商地址，每个模型可**单独配置 Base URL 与 API Key**——切换模型时自动使用对应服务商的配置，无需手动改地址。
- **多模型切换**：内置常用模型列表，默认 `deepseek-v4-flash`；聊天页顶部可即时切换，每个对话记住当时使用的模型。
- **流式输出 + 打字机效果**：AI 回复默认流式逐字输出，并以"打字机"效果逐字显示；网络读取在 IO 线程，不阻塞界面。
- **续接上次对话**：默认打开 App 自动加载最近一次对话，可在设置中关闭，每次启动新建对话。
- **记忆对话**：
  - 本地存储：所有对话保存在本地 Room 数据库，历史页可随时回看、继续、删除。
  - 上下文记忆：每次请求会把最近 20 条消息作为上下文一起发给模型，AI 记得你们之前聊过什么。
- **导入 / 导出对话**：设置页「数据管理」可导出备份为 JSON 文件（**仅对话** 或 **对话 + 设置**），也可导入恢复；手机端与手表端均支持。
- **语音输入**：点击麦克风直接说话，识别结果实时回填输入框（调用系统语音识别，无需额外服务）。
- **复制**：每条消息下方有复制按钮，一键复制到剪贴板；手机端长按消息文本也可系统选中复制。
- **思维链支持**：正确处理 DeepSeek `deepseek-reasoner` 模型的 `reasoning_content` 字段，推理过程与正文分开展示。
- **测试连接**：设置页一键验证 Key、地址、模型是否可用。
- **手表直接联网**：手表端直接向 API 发起请求，不依赖手机转发（需手表连接 Wi-Fi / LTE）。
- **Key 只存在本地**：手机端与手表端各自的 Key 均通过系统 Keystore 加密落盘，不上传、不云端中转。
- **372×430 小屏适配**：手表端紧凑字号、横向滑动模型切换、安全区适配；针对弱机型优化（即时滚动、低刷新打字机、精简动画），流畅度更好。
- **右滑退出**：手表端通过主题配置 `windowSwipeToDismiss`，支持从左向右滑动退出 App。
- **统一图标**：手机端与手表端使用相同的应用图标（蓝色背景 + 白色对话气泡）。
- 浅色 / 深色主题自适应（Android 12+ 跟随系统动态取色）。

## 多服务商配置说明

每个模型可以拥有独立的服务配置，切换模型时自动生效：

```
模型管理（设置页）
 └─ 点 ✎ 打开「配置模型」弹窗
     ├─ Base URL：该模型的服务地址（留空自动用内置地址或默认配置）
     └─ API Key：该模型的 Key（留空使用默认 Key）
```

**服务地址解析顺序**：每模型配置 → 内置服务商地址 → 默认服务配置（设置页顶部的 Base URL / API Key）。

内置服务商地址：

| 模型 | 自动匹配的 Base URL |
| --- | --- |
| `deepseek-*` | `https://api.deepseek.com` |
| `gpt-*` | `https://api.openai.com/v1` |
| `qwen-plus` | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| `glm-4-flash` | `https://open.bigmodel.cn/api/paas/v4` |
| `moonshot-v1-8k` | `https://api.moonshot.cn/v1` |
| `mimo-v2.5-pro` | `https://api.xiaomimimo.com/v1` |

> 例：选择 `deepseek-chat` 自动用 DeepSeek 地址和你在该模型下保存的 Key；切到 `gpt-4o` 自动切到 OpenAI 地址和 Key。

## 导入 / 导出说明

- 位置：设置页 → **数据管理** → 「导出对话」（二级菜单：仅对话 / 对话和设置）与「导入对话」。
- 格式：JSON 备份文件（通过系统文件选择器保存/选取，可放网盘、任意目录）。
- 导入为**追加合并**，不会清空现有数据。
- ⚠️ 「对话和设置」中的 API Key 是**设备绑定**的加密值：同设备导入可正常解密；换设备导入后需重新填写 Key，对话内容不受影响。

## 默认配置

开箱即用的默认值，适配 DeepSeek 用户：

| 配置项 | 默认值 |
| --- | --- |
| Base URL | `https://api.deepseek.com` |
| 默认模型 | `deepseek-v4-flash` |
| 流式输出 | 开启（AI 逐字输出） |
| 续接上次对话 | 开启（启动自动加载最近对话） |
| 系统提示词 | 空（不预设） |

内置模型列表：`deepseek-v4-flash`、`deepseek-chat`、`deepseek-reasoner`、`gpt-4o-mini`、`gpt-4o`、`gpt-3.5-turbo`、`qwen-plus`、`glm-4-flash`、`moonshot-v1-8k`、`mimo-v2.5-pro`。可在设置页自由增删，每个模型可单独配置服务地址与 Key。

## 快速开始

### 方式一：下载正式版 APK

正式版（keystore 签名）发布在 GitHub Releases，请前往下载：

👉 **https://github.com/xiaowang00232/watch-chat/releases/**

| 安装包 | 包名 | 适用设备 |
| --- | --- | --- |
| `watchchat-app-v0.1.3.apk` | `com.xw00232.watchchat.app` | Android 8.0+ 手机 |
| `watchchat-wear-v0.1.3.apk` | `com.xw00232.watchchat.wear` | Wear OS 手表（372×430 / 326ppi） |

安装后进入设置页填入 API Key 即可直接使用（默认 Base URL、模型、流式开关均已配好）。

### 方式二：从源码构建

1. 安装 [Android Studio](https://developer.android.com/studio)（Ladybug 或更新版本均可），或自行配置 JDK 17 + Android SDK + Gradle 8.9。
2. 打开本项目目录（`File → Open`，选择 `WatchChat` 文件夹），等待 Gradle 同步完成（首次需联网下载依赖）。
3. 连接 Android 手机（Android 8.0+）或启动模拟器，选择 **app** 模块运行。
4. 打开 App 进入 **设置**：
   - 填入你的 API Key；
   - 确认默认模型（DeepSeek 用户通常无需修改 Base URL）；
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

# 构建正式版（使用 keystore 签名，需 key.properties 配置）
gradlew :app:assembleRelease :wear:assembleRelease

# 构建产物位置
# app/build/outputs/apk/debug/app-debug.apk
# wear/build/outputs/apk/debug/wear-debug.apk
# app/build/outputs/apk/release/app-release.apk
# wear/build/outputs/apk/release/wear-release.apk
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
| 小米 MiMo | `https://api.xiaomimimo.com/v1` | 小米官方开放平台，模型如 `mimo-v2.5-pro` |
| Ollama（本地） | `http://<电脑IP>:11434/v1` | 模型填本地模型名，Key 可随便填 |

> 原理上任何 OpenAI 兼容的 `POST /chat/completions` 接口都可以用。
> 切换模型时自动使用该模型对应服务商的 Base URL 与 API Key（设置页可逐模型配置）。

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
│       │   ├── settings/            设置存储 + API Key 加密 + 多服务商配置
│       │   │   ├── ApiKeyCipher.kt
│       │   │   └── SettingsRepository.kt
│       │   └── export/              导入/导出 JSON 格式（ChatExporter.kt）
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
│       │   ├── settings/SettingsScreen.kt 设置页（多服务商配置 / 数据管理）
│       │   ├── theme/                     主题（Color / Theme / Type）
│       │   ├── AppNavHost.kt              导航
│       │   └── AppViewModelProvider.kt    ViewModel 工厂
│       ├── MainActivity.kt
│       └── WatchChatApp.kt
│
├── wear/                            手表端（包名 com.xw00232.watchchat.wear）
│   └── src/main/java/com/xw00232/watchchat/wear/
│       ├── ui/
│       │   ├── chat/WearChatScreen.kt        手表聊天页（性能优化）
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

- **401**：API Key 无效或已过期，检查对应模型（服务商）下保存的 Key。
- **403**：Key 没有该模型的访问权限，换个模型试试。
- **404**：Base URL 填错了，检查是否需要 `/v1` 后缀。
- **429**：请求太频繁或额度不足。
- **超时 / 连接失败**：检查网络；使用 Ollama 时手机和电脑需在同一局域网。
- **切换模型后连不上**：确认该模型的 Base URL / API Key 已配置（点 ✎ 检查），或留空让它回退内置地址。
- **导入备份提示格式错误**：文件不是本 App 导出的 JSON（或版本过旧）。
- **导入后 Key 失效**：备份里的 Key 是设备绑定的加密值，换设备需重新填写。
- **手表上网络不通**：确认手表已连接 Wi-Fi / LTE；部分手表默认走蓝牙代理，需要在手表设置中关闭「通过手机连接」或连接 Wi-Fi。
- **语音按钮无反应**：确认已授予麦克风权限；部分模拟器不支持语音识别。
- **流式无输出**：部分接口对流式支持不完整，可在设置中关闭流式输出改为整包返回。

## 后续规划

- 手表与手机数据同步（手机端配置 Key，一键推送到手表）；
- Markdown 渲染、TTS 语音朗读回复；
- 上下文长度可视化配置；
- 手表端蓝牙代理模式（无 Wi-Fi 时经手机转发请求）。

---

> 本项目部分使用 AI 辅助。
