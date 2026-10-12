plugins {
    // 版本组合对齐两个库的共同要求：
    //  - Miuix 0.9.4      → Kotlin 2.4.20 + JetBrains Compose 1.12.0
    //  - backdrop 2.0.1   → Kotlin 2.4.10 + JetBrains Compose 1.12.0
    // 取 Kotlin 2.4.20（较高者）即可同时满足
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
}
