plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.miuixledger"
    // 官方 1.0.0 示例用的就是 compileSdk 36 + AGP 8.13.0
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.miuixledger"
        // 31：Android 12 可装（有模糊）；33+ 才有完整折射（AGSL 着色器）
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // 与官方 1.0.0 完全相同的依赖组合（androidx Compose 1.9.4）
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.foundation:foundation:1.9.4")
    implementation("androidx.compose.ui:ui:1.9.4")
    implementation("androidx.compose.ui:ui-graphics:1.9.4")

    // Kyant0/AndroidLiquidGlass —— Backdrop 液态玻璃管线（Android 版）
    implementation("io.github.kyant0:backdrop:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // 说明：UI 只用 foundation + ui，不引入 material3 / 图标库，避免版本冲突
}
