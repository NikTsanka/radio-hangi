plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) // @Serializable models + JSON converter
}

android {
    namespace = "com.canka.dev.radiohangi"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.canka.dev.radiohangi"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Compose (BOM-managed) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended) // play/pause, volume, timer icons
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // --- AndroidX core / lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen) // Android 12 splash screen (backported)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)   // collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose) // viewModel() in Compose

    // --- Navigation ---
    implementation(libs.androidx.navigation.compose)

    // --- Media3 (playback core) ---
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls) // HLS (.m3u8) World Radio streams
    implementation(libs.androidx.media3.session)

    // --- Networking + JSON ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)                 // Zeno now-playing SSE stream
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // --- Images ---
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.palette) // dominant-color extraction for dynamic theming

    // --- Persistence (favorites) ---
    implementation(libs.androidx.datastore.preferences)

    // --- Home-screen widget ---
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // --- Test ---
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}