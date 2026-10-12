@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    // 显式应用 Kotlin 插件：精确控制编译器版本为 2.4.21。
    // 必须 ≥ 2.4.20，否则读不了 Miuix 0.9.4（用 Kotlin 2.4.20 编译）的库元数据。
    // 注意：应用 KGP 后 AGP 9 会自动关闭其内置 Kotlin，不会冲突。
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.miuixledger"

    // ===== 本次修复的两个关键点 =====
    // ① 用 AGP 9 的新 DSL 指定 compileSdk（官方 example/android 的写法）：
    //    从 API 37 起 Android SDK 改用 minor version 命名，官方仓库里只有
    //    platforms;android-37.0 / 37.1 / 37.2，没有 platforms;android-37。
    // ② Compose 1.12.x（Miuix 0.9.4 与 backdrop 2.0.1 的共同依赖）按 API 37 编译。
    //    compileSdk 低于 37 时 AAR 元数据校验会报：
    //      "requires libraries and applications that depend on it to compile
    //       against version 37 or later of the Android APIs"
    //    该校验在配置之后、编译之前执行，所以表现为「构建几十秒即失败」。
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.example.miuixledger"
        // 33：液态玻璃的 AGSL 折射着色器需要 Android 13+（用户设备为 Android 17）
        minSdk = 33
        targetSdk = 37
        versionCode = 3
        versionName = "2.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += setOf(
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "kotlin/**",
                "META-INF/*.version",
                "META-INF/**/LICENSE.txt",
            )
        }
    }
    lint {
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // ===== Compose 基础：androidx 1.12.0 =====
    // Miuix 与 backdrop 的依赖链最终都落到 androidx.compose.*:1.12.0，
    // 这里声明同版本，避免版本分叉。
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.ui:ui-graphics:1.12.0")
    implementation("androidx.compose.runtime:runtime:1.12.0")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    // ===== 库 1：Miuix（HyperOS 风格组件库）=====
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4")

    // ===== 库 2：Kyant0/AndroidLiquidGlass（Backdrop 液态玻璃管线）=====
    implementation("io.github.kyant0:backdrop:2.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // 持久化使用 Android 内置 org.json，无需 kotlinx-serialization 及其编译插件
}
