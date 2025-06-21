plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.android.launcher3.icons"

    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    sourceSets {
        named("main") {
            java.setSrcDirs(listOf("src", "src_full_lib"))
            manifest.srcFile("AndroidManifest.xml")
            res.setSrcDirs(listOf("res"))
        }
    }
    kotlin {
        jvmToolchain(21)//1.8（8）
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.annotation:annotation:1.9.1")
//    api(project(":NexusLauncher:Flags"))
}
