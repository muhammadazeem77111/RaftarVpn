plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.raftar.vpn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.raftar.vpn"
        minSdk = 26
        targetSdk = 34
        // CI me har build ka versionCode unique hona chahiye, warna Android purani APK rakhta hai.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            // Release workflow secrets se aata hai (README: "Signing & Play Store upload").
            // Debug build par iska istemal nahi hota.
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            // default
        }
        release {
            // v1: R8/minify OFF taake native lib (libovpn3.so) ke sath koi issue na ho.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Kotlin 1.9.24 ke liye compose compiler version (AGP 8.5.x ke sath verified).
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // OpenVPN engine: tim06 fork (Apache-2.0), Maven Central se. AAR me libovpn3.so (4 ABIs)
    // aur apni OpenVPNService mojood hai — manifest me dobara declare NA karo.
    implementation("io.github.tim06:openvpn:1.1.3")
    // Transitive dep pin ki hui (inspect ki hui API isi version ki hai).
    implementation("io.github.tim06:basevpnprotocols:1.1.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    // Private .ovpn configs on-device encrypted storage ke liye.
    implementation("androidx.security:security-crypto:1.0.0")

    // QR code scan (apne server ki config QR se import karne ke liye).
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
