plugins {
    // ===== 工具链：严格对齐官方 Kyant0/AndroidLiquidGlass（kmp 分支）=====
    //   AGP    9.3.2   （官方 libs.versions.toml: agp = "9.3.2"）
    //   Kotlin 由 AGP 内置提供（AGP 9 起不再需要、且不允许应用 kotlin.android）
    //
    // 为什么必须 AGP 9.x：
    //   从 API 37 起 Android SDK 改用 minor version 命名，官方仓库里只有
    //   platforms;android-37.0 / 37.1 / 37.2，**没有** platforms;android-37。
    //   AGP 8.x 不认识这种命名，会找不到 platform 而构建失败。
    //
    // 为什么 compileSdk 必须 37：
    //   Compose 1.12.0（Miuix 0.9.4 与 backdrop 2.0.1 的共同依赖）按 API 37 编译，
    //   低于 37 时 AAR 元数据校验会直接中断构建。
    id("com.android.application") version "9.4.1" apply false

    // 只应用 Compose 编译器插件。
    // ⚠️ 绝对不要再应用 org.jetbrains.kotlin.android：
    //    AGP 9 内置 Kotlin，重复应用会直接报
    //    "The 'org.jetbrains.kotlin.android' plugin is no longer required
    //     for Kotlin support since AGP 9.0." 并中断构建。
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
