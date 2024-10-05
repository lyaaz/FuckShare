import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
}

fun String.execute(currentWorkingDir: File = file("./")): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
        standardOutput = byteOut
    }
    return byteOut.toString().trim()
}

android {
    namespace = "org.lyaaz.fuckshare"
    compileSdk = 34
    defaultConfig {
        applicationId = "org.lyaaz.fuckshare"
        minSdk = 30
        targetSdk = 34
        versionCode = "git rev-list HEAD --count".execute().toInt()
        versionName = "git describe --tag".execute()
        resourceConfigurations += setOf("en", "zh-rCN")
        vectorDrawables.useSupportLibrary = true
    }
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
    signingConfigs {
        create("release") {
            val properties = Properties().apply {
                load(File("signing.properties").reader())
            }
            storeFile = File(properties.getProperty("storeFilePath"))
            storePassword = properties.getProperty("storePassword")
            keyPassword = properties.getProperty("keyPassword")
            keyAlias = properties.getProperty("keyAlias")
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "FS debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
    }
}
composeCompiler {
    enableStrongSkippingMode = true
}
dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.arthenica:ffmpeg-kit-min:6.0-2")
    compileOnly("de.robv.android.xposed:api:82")

    // compose
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // QR code
    implementation("com.google.zxing:core:3.5.3")
}
