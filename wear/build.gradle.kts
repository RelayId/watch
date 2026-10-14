plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.relayid.watch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.relayid.app"
        minSdk = 30
        targetSdk = 36
        versionCode = System.getenv("RELAYID_ANDROID_VERSION_CODE")?.toIntOrNull() ?: 2
        versionName = "1.0.0"
    }

    val releaseKeystorePath = System.getenv("RELAYID_ANDROID_KEYSTORE_PATH")
    val releaseKeystorePassword = System.getenv("RELAYID_ANDROID_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("RELAYID_ANDROID_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("RELAYID_ANDROID_KEY_PASSWORD")
    val releaseSigningConfigured = listOf(
        releaseKeystorePath,
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    val debugKeystorePath = System.getenv("RELAYID_ANDROID_DEBUG_KEYSTORE_PATH")
    val debugKeystorePassword = System.getenv("RELAYID_ANDROID_DEBUG_KEYSTORE_PASSWORD")
    val debugKeyAlias = System.getenv("RELAYID_ANDROID_DEBUG_KEY_ALIAS")
    val debugKeyPassword = System.getenv("RELAYID_ANDROID_DEBUG_KEY_PASSWORD")
    val debugSigningConfigured = listOf(
        debugKeystorePath,
        debugKeystorePassword,
        debugKeyAlias,
        debugKeyPassword,
    ).all { !it.isNullOrBlank() }

    if (System.getenv("RELAYID_REQUIRE_SIGNING") == "true" && !releaseSigningConfigured) {
        error("RelayId release signing requires the Android keystore environment variables.")
    }

    signingConfigs {
        if (debugSigningConfigured) {
            create("sharedDebug") {
                storeFile = file(debugKeystorePath!!)
                storePassword = debugKeystorePassword
                keyAlias = debugKeyAlias
                keyPassword = debugKeyPassword
            }
        }
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            if (debugSigningConfigured) signingConfig = signingConfigs.getByName("sharedDebug")
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/libandroidx.graphics.path.so"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")

    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.wear.compose:compose-material3:1.6.0")
    implementation("androidx.wear.compose:compose-foundation:1.6.0")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
}
