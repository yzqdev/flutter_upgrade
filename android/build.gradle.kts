plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
group = "com.xuexiang.flutter_xupdate"
version = "1.0-SNAPSHOT"
android {
    namespace = "com.xuexiang.flutter_xupdate"

    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }



//    sourceSets {
//        main.java.srcDirs += "src/main/kotlin"
//        test.java.srcDirs += "src/test/kotlin"
//    }

    defaultConfig {
        minSdk = 21
    }

    dependencies {
        testImplementation(kotlin("test"))
        testImplementation("org.mockito:mockito-core:5.0.0")
    }

//    testOptions {
//        unitTests.all {
//            useJUnitPlatform()
//
//            testLogging {
//                events "passed", "skipped", "failed", "standardOut", "standardError"
//                outputs.upToDateWhen {false}
//                showStandardStreams = true
//            }
//        }
//    }
}
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") } // 确保这里添加
}
kotlin {
    jvmToolchain(17)   // 一次性把 Java/Kotlin/测试全拉到 17
}

dependencies {
    // https://mvnrepository.com/artifact/com.github.xuexiangjys/XUpdate
    implementation("com.github.xuexiangjys:XUpdate:2.1.5")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.github.getActivity:EasyHttp:13.0")
    implementation("com.squareup.okhttp3:okhttp:4.+")

}
