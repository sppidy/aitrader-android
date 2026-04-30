plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val apiHost = providers.gradleProperty("apiHost").orElse("BACKEND_HOST")
val apiPort = providers.gradleProperty("apiPort").orElse("8443")
val apiScheme = providers.gradleProperty("apiScheme").orElse("https")
val wsScheme = providers.gradleProperty("wsScheme").orElse("wss")
val apiKey = providers.gradleProperty("apiKey").orElse("change-me")
val forexUrl = providers.gradleProperty("forexUrl").orElse("https://forex.example.com")

android {
    namespace = "com.aitrader.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aitrader.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Centralized server config from gradle.properties
        buildConfigField("String", "DEFAULT_BASE_URL", "\"${apiScheme.get()}://${apiHost.get()}:${apiPort.get()}\"")
        buildConfigField("String", "DEFAULT_WS_URL", "\"${wsScheme.get()}://${apiHost.get()}:${apiPort.get()}\"")
        buildConfigField("String", "DEFAULT_API_KEY", "\"${apiKey.get()}\"")
        // Forex defaults
        buildConfigField("String", "DEFAULT_FOREX_URL", "\"${forexUrl.get()}\"")
    }

    signingConfigs {
        create("release") {
            val storeFileProp = providers.gradleProperty("RELEASE_STORE_FILE").orNull
            val storePasswordProp = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
            val keyAliasProp = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
            val keyPasswordProp = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull

            if (!storeFileProp.isNullOrBlank()) {
                storeFile = file(storeFileProp)
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val hasReleaseSigning = !providers.gradleProperty("RELEASE_STORE_FILE").orNull.isNullOrBlank()
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(kotlin("test"))

    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Markdown rendering for agent chat replies
    implementation(libs.markdown.renderer.m3)
}
