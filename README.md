# MiuixLedger Android

记账 App，**同时使用你指定的两个 GitHub 库**：

| 库 | 作用 | 版本 |
| --- | --- | --- |
| [Miuix](https://github.com/compose-miuix-ui/miuix)（HyperOS 风格组件库） | 界面组件：`Scaffold`、`TopAppBar`、`Text`、`SmallTitle`、`Slider`、`Switch`、`TextField`、`Button`、`OverlayDialog`、`MiuixTheme` 主题 | 0.9.4 |
| [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Backdrop） | 液态玻璃材质：底栏、FAB、卡片、放大镜的折射/磨砂/色散 | 2.0.1 |

## 工具链（对齐两个库的官方工程）

两个库都要求 **JetBrains Compose 1.12.x**，而 Compose 1.12 按 **API 37** 编译。
从 API 37 起 Google 改用 minor version 命名（只有 `android-37.0`，没有 `android-37`），
**因此必须用 AGP 9.x**：

| | 版本 | 依据 |
| --- | --- | --- |
| AGP | 9.4.1 | Miuix 官方工程 |
| Gradle | 9.8.1 | Miuix 官方工程 |
| Kotlin | 2.4.21 | Miuix 0.9.4 用 2.4.20、backdrop 2.0.1 用 2.4.10，取 ≥ 两者 |
| compileSdk | 37（`release(37){ minorApiLevel = 0 }`） | Compose 1.12 要求 |

## 云端打包（无需本地环境）

1. GitHub 新建仓库（Public）
2. 把本文件夹**里面的全部内容**上传到仓库根目录（含隐藏的 `.github`）
3. 等 5-8 分钟 → **Actions** 页签 → 点最新运行 → 底部 **Artifacts** 下载
4. 解压得到 `app-debug.apk` → 传手机安装（允许"未知来源"）

## 玻璃管线（Backdrop API）

```kotlin
.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedCornerShape(34.dp) },
    effects = {
        vibrancy()                              // 提饱和
        blur(blurDp.toPx())                     // 磨砂
        lens(heightDp.toPx(), amountDp.toPx())  // 边缘折射
    },
    highlight = { Highlight.Default },
    shadow = { Shadow(alpha = 0.14f) },
)
```

⚠️ **关键**：`blur()` / `lens()` 的参数单位是**像素**，必须 `dp.toPx()` 换算，
否则高密度屏上效果小到看不见。

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

## 已知说明

- 本工程由 AI 生成，未本地编译验证（生成环境无 Android SDK）。
  若云端构建报错，把 Actions 的报错文字发回即可修复。
- minSdk 33：完整折射需要 **Android 13+**（AGSL RuntimeShader）。
