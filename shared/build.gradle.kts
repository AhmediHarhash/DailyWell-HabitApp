import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

// Load API keys from local.properties (not committed to git)
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // Disable iOS targets for now - Android only build
    // Uncomment when ready for iOS
    /*
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    */

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // GitLive Firebase (KMP multiplatform Firebase)
            implementation(libs.firebase.gitlive.firestore)
            implementation(libs.firebase.gitlive.auth)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.datastore.preferences)
            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.room.runtime)
            implementation(libs.room.ktx)
            implementation(libs.billing)
            implementation(libs.work.runtime)
            implementation(libs.health.connect)
            implementation("com.google.android.gms:play-services-location:21.3.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
            implementation(libs.glance.appwidget)
            implementation(libs.glance.material3)

            // Sherpa-ONNX for Piper TTS neural voice synthesis
            // compileOnly so shared lib compiles against it; androidApp provides the runtime AAR
            compileOnly(files("libs/sherpa-onnx-1.12.23.aar"))

            // Firebase
            implementation(platform(libs.firebase.bom.get()))
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.analytics)

            // Credential Manager (Google Sign-In 2026)
            implementation(libs.credentials)
            implementation(libs.credentials.play.services)
            implementation(libs.googleid)

            // Ktor (for Claude/Cartesia API calls)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.android)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)

            // Media3 (for TTS audio playback)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.ui)

            // CameraX (for food scanning)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)

            // ML Kit barcode scanning (FREE, on-device)
            implementation(libs.mlkit.barcode)
            implementation(libs.mlkit.text)

            // Coil for image loading
            implementation(libs.coil.compose)

            // Accompanist permissions (camera access)
            implementation(libs.accompanist.permissions)

            // Llamatik: KMP llama.cpp wrapper (supports Gemma 3N + Qwen2.5)
            // Provides SLM fallback when cloud API budget exceeded
            implementation("com.llamatik:library:0.12.0")

            // SECURITY: Encrypted storage (CVE-DW-003 FIX)
            // EncryptedSharedPreferences for sensitive data at rest
            implementation("androidx.security:security-crypto:1.1.0-alpha06")
        }
    }
}

// Force androidx.core to version compatible with AGP 8.7.3
// (glance-appwidget 1.1.1 transitively pulls 1.17.0 which needs AGP 8.9.1+)
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.15.0")
        force("androidx.core:core-ktx:1.15.0")
    }
}

android {
    namespace = "com.dailywell.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        // Inject API keys securely from local.properties
        buildConfigField("String", "CLAUDE_API_KEY", "\"${localProperties.getProperty("CLAUDE_API_KEY", "")}\"")

        // Calendar OAuth credentials (Google Cloud Console & Azure Portal)
        buildConfigField("String", "GOOGLE_OAUTH_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_OAUTH_CLIENT_ID", "")}\"")
        buildConfigField("String", "OUTLOOK_OAUTH_CLIENT_ID", "\"${localProperties.getProperty("OUTLOOK_OAUTH_CLIENT_ID", "")}\"")

        // Google Sign-In Web Client ID (from Firebase Console > Authentication > Google)
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Known AGP/Lint + Kotlin metadata incompatibility in Compose detector.
        // Keep lint enabled while disabling only the unstable rule.
        disable += "RememberInComposition"
        disable += "FrequentlyChangingValue"
        // Known crash with lifecycle lint detector under current AGP/Kotlin combo.
        disable += "NullSafeMutableLiveData"
        // Known crash with compose lint detector under current AGP/Kotlin combo.
        disable += "AutoboxingStateCreation"
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
}

// Compose Compiler optimizations for 60fps performance
composeCompiler {
    // Enable strong skipping for better recomposition
    enableStrongSkippingMode = true

    // Use stability configuration file
    stabilityConfigurationFile = rootProject.file("compose-stability.conf")

    // Enable compiler metrics in release for debugging performance
    if (project.findProperty("composeCompilerReports") == "true") {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}
