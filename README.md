# MiuixLedger Android（液态玻璃版）

记账 App + [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Backdrop）液态玻璃管线。
**Android 13+（API 33）** 可见完整折射效果。

## 云端打包（无需本地环境）

1. GitHub 新建仓库（Public）
2. 把本文件夹**里面的全部内容**上传到仓库根目录
   （含隐藏的 `.github` 文件夹——它负责自动打包）
3. 等 5-8 分钟 → 进 **Actions** 页签 → 点最新运行 → 底部 **Artifacts** 下载
4. 解压得到 `app-debug.apk`，传到手机安装（允许"未知来源"）

## 技术栈（版本对齐官方 1.0.0 标签）

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| AGP | 8.13.0 | 与官方示例一致 |
| Kotlin | 2.2.21 | 与官方示例一致 |
| Compose | androidx 1.9.4 | 与官方示例一致 |
| backdrop | 1.0.0 | **Android 版**（2.x 是 Compose Multiplatform 版，不兼容） |

## 玻璃管线（对应官方 API）

- 底栏 / FAB：`drawBackdrop(backdrop, shape, effects { vibrancy(); blur(); lens(refractionHeight, refractionAmount) }, highlight, shadow)`
- 卡片：轻量 `blur()` 磨砂（列表滚动性能考虑）
- 背景层：`Modifier.layerBackdrop(backdrop)` 采集为折射源

## 已知说明

- 本工程由 AI 生成，未本地编译验证（生成环境无 Android SDK）。
  若云端构建报错，把 Actions 的报错文字发回即可修复。
- minSdk 31：Android 12 可安装（有模糊）；**Android 13+ 才有完整折射**。
