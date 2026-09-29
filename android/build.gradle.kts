plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

group = "com.xuexiang.flutter_xupdate"
version = "1.0-SNAPSHOT"

android {
    namespace = "com.xuexiang.flutter_xupdate"

    compileSdk = 36

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    // 内置的XUpdate更新库源码(整合自本地xupdate-lib), 仅依赖androidx
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.8.2")
    // 版本更新网络请求实现(OKHttpUpdateHttpService)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
