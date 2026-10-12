# MiuixLedger Android（液态玻璃版）

Miuix 风格记账 App + Kyant0/AndroidLiquidGlass（Backdrop）液态玻璃管线。
**Android 13+（API 33）** 可看到完整的 AGSL 折射效果。

## 云端打包（无需本地环境）

1. 在 GitHub 新建仓库（Public 即可），例如 `miuix-ledger`
2. 把本文件夹里的**全部内容**上传到仓库根目录
   （包括 `.github` 隐藏文件夹——网页上传时直接把整个文件夹拖进去即可）
3. 等待约 5-8 分钟，进仓库 **Actions** 页签 → 点最新的运行 →
   底部 **Artifacts** 下载 `MiuixLedger-debug-apk`
4. 解压得到 `app-debug.apk`，传到手机安装（允许"未知来源"即可）

## 本地打包（可选，需 Android Studio）

用 Android Studio 打开本文件夹，等 Gradle 同步完，点 Run 或
Build → Build APK(s)。

## 技术栈

- UI：Jetpack Compose + Material Icons
- 玻璃管线：`io.github.kyant0:backdrop:2.0.1`
  - 底栏/FAB：`vibrancy() + blur() + lens(refractionHeight=24, refractionAmount=60)`
  - 卡片：轻量 blur 玻璃（EmptyBackdrop 降级，保证滚动性能）
- 持久化：kotlinx-serialization JSON（应用私有目录）

## 已知说明

- 本工程由 AI 生成，未经过本地编译验证（生成环境无 Android SDK）。
  如云端构建报错，把 Actions 的红色报错截图/文字发回来即可修复。
- minSdk=33：Android 12 及以下安装会提示不兼容（玻璃着色器需要 13+）。
