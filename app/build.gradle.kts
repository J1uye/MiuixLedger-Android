plugins {
    // ===== 与官方 androidApp 模块完全一致 =====
    // AGP 9 内置 Kotlin 支持，因此只应用这两个插件。
    // 不应用 org.jetbrains.kotlin.android（AGP 9 下会直接报错并中断构建）。
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.miuixledger"

    // compileSdk 37：官方 androidApp 就是这么写的。
    // Compose 1.12.0（Miuix 0.9.4 与 backdrop 2.0.1 的共同依赖）按 API 37 编译，
    // 低于 37 时 AAR 元数据校验会报
    //   "requires libraries and applications that depend on it to compile
    //    against version 37 or later of the Android APIs"
    // 该校验在配置之后、编译之前执行，所以表现为「构建几十秒即失败」。
    compileSdk = 37
    // 与官方 androidApp 一致；AGP 9 需要 build-tools 37.0.0
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.example.miuixledger"
        // 33：液态玻璃的 AGSL 折射着色器需要 Android 13+（用户设备为 Android 17）
        minSdk = 33
        targetSdk = 37
        versionCode = 5
        versionName = "2.3"
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
        // 打开后可在代码里读 BuildConfig.VERSION_NAME / VERSION_CODE，
        // 用于「关于」页显示版本号（AGP 8+ 默认关闭）。
        buildConfig = true
    }
    packaging {
        resources {
            excludes += arrayOf(
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

// 注意：这里没有 kotlin { } 配置块。
// AGP 9 内置 Kotlin 会自动对齐 jvmTarget 与 compileOptions，无需手动设置；
// 手动写反而可能因扩展不存在而报错。

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    // ===== 库 1：Miuix（HyperOS 风格组件库）=====
    // 只引入 miuix-ui：本项目用到的组件（Scaffold/TopAppBar/Text/SmallTitle/
    // Slider/Switch/TextField/Button/OverlayDialog/MiuixTheme）都在这个模块。
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4")

    // ===== 库 2：Kyant0/AndroidLiquidGlass（Backdrop 液态玻璃管线）=====
    implementation("io.github.kyant0:backdrop:2.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // 持久化使用 Android 内置 org.json，无需 kotlinx-serialization 及其编译插件
    // Compose 版本不显式声明：由上面两个库的依赖链带入（均为 1.12.0）
}
