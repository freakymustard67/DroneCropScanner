// app/build.gradle.kts
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.cropdrone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cropdrone"
        minSdk = 31          // Android 12+
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Inject API keys from local.properties (do NOT hardcode in source)
        val localProps = rootProject.file("local.properties")
        val props = Properties()
        if (localProps.exists()) props.load(localProps.inputStream())

        buildConfigField("String", "GOOGLE_MAPS_API_KEY",
            "\"${props.getProperty("GOOGLE_MAPS_API_KEY", "YOUR_API_KEY_HERE")}\"")
        buildConfigField("String", "GEMINI_API_KEY",
            "\"${props.getProperty("GEMINI_API_KEY", "YOUR_API_KEY_HERE")}\"")
        buildConfigField("String", "SH_CLIENT_ID",
            "\"${props.getProperty("SH_CLIENT_ID", "YOUR_CLIENT_ID_HERE")}\"")
        buildConfigField("String", "SH_CLIENT_SECRET",
            "\"${props.getProperty("SH_CLIENT_SECRET", "YOUR_CLIENT_SECRET_HERE")}\"")

        // Inject Google Maps key into AndroidManifest placeholder
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] =
            props.getProperty("GOOGLE_MAPS_API_KEY", "YOUR_API_KEY_HERE")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit / OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Kotlinx
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Maps
    implementation(libs.google.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // TensorFlow Lite
    implementation(libs.tflite)
    implementation(libs.tflite.support)
    implementation(libs.tflite.gpu)

    // Coil
    implementation(libs.coil.compose)

    // WorkManager (queued Pass 2 cloud analysis)
    implementation(libs.work.runtime)

    // DataStore (settings / API keys)
    implementation(libs.datastore.preferences)
}
