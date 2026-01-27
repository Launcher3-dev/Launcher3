plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "com.android.launcher3.icons"

    compileSdk = 36

    defaultConfig {
        minSdk = 26

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
    implementation(libs.core)
    implementation(libs.annotation)
}
