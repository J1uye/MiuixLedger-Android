# MiuixLedger Android

记账 App，**同时使用你指定的两个 GitHub 库**：

| 库 | 作用 | 版本 |
| --- | --- | --- |
| [Miuix](https://github.com/compose-miuix-ui/miuix)（HyperOS 风格组件库） | 界面组件：`Scaffold`、`TopAppBar`、`Text`、`SmallTitle`、`Slider`、`Switch`、`TextField`、`Button`、`TextButton`、`OverlayDialog`、`MiuixTheme` 主题 | 0.9.4 |
| [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Backdrop） | 液态玻璃材质：底栏、FAB、卡片、放大镜的折射/磨砂/色散 | 2.0.1 |

## 工具链（对齐两个库的官方工程）

两个库都要求 **JetBrains Compose 1.12.0**，而 Compose 1.12 按 **API 37** 编译。
从 API 37 起 Google 改用 minor version 命名（只有 `android-37.0`，没有 `android-37`），
**因此必须用 AGP 9.x**：

| | 版本 | 依据 |
| --- | --- | --- |
| AGP | 9.4.1 | Miuix 官方工程 |
| Gradle | 9.8.1 | Miuix 官方工程 |
| compileSdk / targetSdk | 37 | 两个库官方工程 |
| buildToolsVersion | 37.0.0 | LiquidGlass 官方 androidApp |
| Kotlin | AGP 9 内置 | **不可再应用 `org.jetbrains.kotlin.android`** |

⚠️ **AGP 9 起内置 Kotlin**：若在 `app/build.gradle.kts` 里再应用
`org.jetbrains.kotlin.android`，会直接报
`The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.`
并中断构建。本项目只应用 `com.android.application` + `org.jetbrains.kotlin.plugin.compose`。

## 云端打包（无需本地环境）

1. 把本文件夹**里面的全部内容**上传到 GitHub 仓库根目录（含隐藏的 `.github`）
2. 等 5-8 分钟 → **Actions** 页签 → 点最新运行
3. 底部 **Artifacts** → 下载 `MiuixLedger-debug-apk`
4. 解压得到 `app-debug.apk` → 传手机安装（允许"未知来源"）

> 构建失败时，工作流会自动把 Kotlin 编译错误（`e: 文件:行号`）写进
> **job summary** 页面，方便直接复制排查。

## 玻璃管线（Backdrop API）

```kotlin
.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedCornerShape(34.dp) },
    effects = {
        vibrancy()                              // 提饱和
        blur(blurDp.toPx())                     // 磨砂
        lens(                                   // 边缘折射
            refractionHeight = hDp.toPx(),
            refractionAmount = aDp.toPx(),
            chromaticAberration = true,         // 色散
        )
    },
    highlight = { Highlight.Default },
    shadow = { Shadow(alpha = 0.14f) },
    onDrawSurface = { drawRect(tint) },
)
```

⚠️ **两个关键坑**：

1. `blur()` / `lens()` 的参数单位是**像素**，必须 `dp.toPx()` 换算，
   否则高密度屏上效果小到看不见。
2. `MiuixTheme.colorScheme` / `MiuixTheme.textStyles` 是 **`@Composable` 属性**。
   `effects{}`、`onDrawSurface{}`、`onDrawBehind{}`、`highlight{}`、`shadow{}`
   这些回调**不是 `@Composable`**，在里面直接读会报
   `@Composable invocations can only happen from the context of a @Composable function`。
   必须先在组合期取出颜色变量，再在回调里使用。

## 滑动动态效果（官方 LiquidBottomTabs 移植）

| 效果 | 实现 |
| --- | --- |
| 透镜跟随手指 | `translationX = 拖动值 × tabWidth` |
| 果冻拉伸 | `scaleX/scaleY` + 速度修正 |
| 按压折射增强 | `lens(高度 × pressProgress, 强度 × pressProgress)` |
| 拖动色散 | 拖动时 `chromaticAberration = true` |
| 松手回弹吸附 | spring 动画到最近 tab |
| 文字被扭曲 | 折射 `rememberCombinedBackdrop(背景, 底栏层)` |

> 折射靠「扭曲背后图案」体现，所以背景必须是**高对比斜条纹 + 色块**
> （`GlassBackground.kt`），平滑渐变看不出效果。

### 两条动画路径必须分开（点击 vs 拖动）

这是实测踩出来的坑，改坏了会出现「点击直接跳到结果、没有滑动过程」：

| | 弹簧参数 | 用途 |
| --- | --- | --- |
| `dragSpec` | stiffness 900 | 拖动跟手，要快要硬 |
| `glideSpec` | stiffness 190、阻尼 0.78 | 点击/吸附滑动，约 370ms，过程可见 |

三个关键点：

1. **滑动动画必须写在协程体内**，不能包一层 `launch`：
   `animateToValue()` 里 `press() → valueAnim.animateTo() → release()` 顺序执行，
   否则 `release()` 会立刻抢占并打断滑动动画，表现就是「瞬间到位」。
2. **`press()` / `release()` 各用 `Job` 持有并先 `cancel()`**，
   避免两段动画互相抢同一个 `Animatable`。
3. **点击时按位移注入速度冲量**（`glideVelAnim`）并衰减，
   让点击滑行也有果冻拉伸，手感与拖动一致。

另外 `pointerInput(Unit)` 的闭包**只创建一次**，里面读 `selected`/`onSelect`
会永远拿到初始值 → 必须用 `rememberUpdatedState` 包一层。

## 已知说明

- 本工程由 AI 生成，**未在本地编译验证**（生成环境无 Android SDK / 命令行被沙箱阻断）。
  所有 API 签名均已逐一比对官方库源码与文档站核对，但最终以云端构建结果为准。
- minSdk 33：完整折射需要 **Android 13+**（AGSL RuntimeShader）。
