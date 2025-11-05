plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.android.launcher3.testing.shared"

    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    sourceSets {
        named("main") {
            java.setSrcDirs(listOf("src"))
            manifest.srcFile("AndroidManifest.xml")
            res.setSrcDirs(listOf("res"))
        }
    }
    kotlin {
        jvmToolchain(21)//1.8（8）
    }
}

dependencies {
    implementation(libs.core)
    implementation(libs.annotation)
}
