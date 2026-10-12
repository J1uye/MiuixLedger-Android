plugins {
    // ===== 工具链：对齐两个库的官方工程 =====
    //   Miuix 官方工程          : AGP 9.4.1 + Kotlin 2.4.21 + Gradle 9.8.1
    //   AndroidLiquidGlass 官方 : AGP 9.3.2 + Kotlin 2.4.10 + Gradle 9.7.1
    //
    // ① 为什么必须 AGP 9.x
    //    从 API 37 起 Android SDK 改用 minor version 命名：官方仓库里只有
    //    platforms;android-37.0 / 37.1 / 37.2，**没有** platforms;android-37。
    //    AGP 8.x 不认识这种命名，会找不到 platform 而失败。
    //    AGP 9 还提供 compileSdk { version = release(37) { minorApiLevel } } 新 DSL。
    //
    // ② 为什么 compileSdk 必须 37
    //    Compose 1.12.x（Miuix 0.9.4 与 backdrop 2.0.1 的共同依赖）按 API 37 编译，
    //    低于 37 时 AAR 元数据校验会直接中断构建。
    //
    // ③ 为什么 Kotlin 用 2.4.21
    //    Miuix 0.9.4 用 Kotlin 2.4.20 编译、backdrop 2.0.1 用 2.4.10 编译。
    //    Kotlin 编译器读不了「比它更新」的版本编译出的库元数据，故需 ≥ 2.4.20。
    //    显式应用 KGP（而非依赖 AGP 内置 Kotlin）以便精确控制编译器版本。
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
