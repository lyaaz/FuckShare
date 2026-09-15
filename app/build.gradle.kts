plugins {
    id("fuck.android.application")
    id("fuck.compose")
}

android {
    namespace = "org.lyaaz.fuckshare"
    defaultConfig {
        minSdk = 30
    }
    androidResources {
        generateLocaleConfig = true
        localeFilters.add("zh-rCN")
    }
    buildTypes {
        debug {
            resValue("string", "app_name", "FS debug")
        }
    }
}

dependencies {
    implementation(project(":ui"))
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.timber)
    implementation(libs.material)
    implementation(libs.glide.gifencoder)
    implementation(libs.zxing.core)
}
