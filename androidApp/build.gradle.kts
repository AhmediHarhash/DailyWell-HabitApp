plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.googleServices)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.billing)
            implementation(libs.core.splashscreen)
            implementation(libs.work.runtime)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            // Sherpa-ONNX for Piper TTS neural voice synthesis
            implementation(files("libs/sherpa-onnx-1.12.23.aar"))
        }
    }
}

// Force androidx.core to version compatible with AGP 8.7.3
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.15.0")
        force("androidx.core:core-ktx:1.15.0")
    }
}

android {
    namespace = "com.dailywell.android"
    compileSdk = 35
    assetPacks += mutableSetOf(":slmModelPack")

    defaultConfig {
        applicationId = "com.dailywell.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        // Enable baseline profile for 60fps performance
        buildConfig = true
    }

    // Exclude unnecessary TTS language files (only keep English)
    aaptOptions {
        ignoreAssetsPattern = "!en*:!gmw*:lang"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Exclude unused ONNX Runtime files
            excludes += "META-INF/native-image/**"
        }
        jniLibs {
            // Keep only ARM64 native libraries
            excludes += "**/x86/**"
            excludes += "**/x86_64/**"
            excludes += "**/armeabi-v7a/**"
        }
    }

    buildTypes {
        debug {
            // Enable for smoother animations in debug
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Known AGP/Lint + Kotlin metadata detector crashes in current toolchain.
        disable += "RememberInComposition"
        disable += "FrequentlyChangingValue"
        disable += "NullSafeMutableLiveData"
        disable += "AutoboxingStateCreation"
        // Keep dependency upgrade checks out of PR lint signal; handled separately during dependency maintenance.
        disable += "GradleDependency"
        disable += "AndroidGradlePluginVersion"
        // External custom check currently incompatible with this AGP/Kotlin API surface.
        disable += "ObsoleteLintCustomCheck"
    }

    // Split APKs by ABI for smaller downloads
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }
}

dependencies {
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
