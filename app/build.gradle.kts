import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
}

val releasePropertiesFile = rootProject.file("keystore.properties")
val releaseProperties = Properties().apply {
    if (releasePropertiesFile.exists()) releasePropertiesFile.inputStream().use(::load)
}
val hasReleaseSigning = releasePropertiesFile.exists()

android {
    namespace = "com.musicplayer.offline"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.musicplayer.offline"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        if (hasReleaseSigning) create("release") {
            storeFile = rootProject.file(requireNotNull(releaseProperties.getProperty("storeFile")))
            storePassword = requireNotNull(releaseProperties.getProperty("storePassword"))
            keyAlias = requireNotNull(releaseProperties.getProperty("keyAlias"))
            keyPassword = requireNotNull(releaseProperties.getProperty("keyPassword"))
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation("com.github.axet:liblame:3.100")
    implementation("net.java.dev.jna:jna:5.19.1@aar")
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit4)
}
