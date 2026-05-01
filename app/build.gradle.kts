plugins {
    alias(libs.plugins.android.application)
    // AGP 9 ships built-in Kotlin — kotlin-android plugin alias removed.
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
    // 37 is required because androidx.biometric:1.4.0-alpha07 depends on
    // SDK extension level 36.1+. Bumping to 37 satisfies that.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aitrader.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        // BuildConfig fields are wired via the new androidComponents.onVariants
        // API below — defaultConfig.buildConfigField() is no longer the
        // canonical place under the AGP 9 DSL.
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

    // kotlinOptions {} removed — built-in Kotlin defaults
    // kotlin.compilerOptions.jvmTarget to compileOptions.targetCompatibility
    // (which is JVM_11 above), so an explicit block isn't needed.

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// New AGP 9 DSL: BuildConfig fields are added per-variant rather than via
// defaultConfig.buildConfigField(). The values are computed at configuration
// time from gradle.properties (apiHost, apiPort, apiScheme, wsScheme, apiKey,
// forexUrl) so the same APK build can be re-pointed by overriding properties.
androidComponents {
    onVariants { variant ->
        val baseUrl = "${apiScheme.get()}://${apiHost.get()}:${apiPort.get()}"
        val wsUrl = "${wsScheme.get()}://${apiHost.get()}:${apiPort.get()}"
        variant.buildConfigFields?.put(
            "DEFAULT_BASE_URL",
            com.android.build.api.variant.BuildConfigField(
                type = "String",
                value = "\"$baseUrl\"",
                comment = null,
            )
        )
        variant.buildConfigFields?.put(
            "DEFAULT_WS_URL",
            com.android.build.api.variant.BuildConfigField(
                type = "String",
                value = "\"$wsUrl\"",
                comment = null,
            )
        )
        variant.buildConfigFields?.put(
            "DEFAULT_API_KEY",
            com.android.build.api.variant.BuildConfigField(
                type = "String",
                value = "\"${apiKey.get()}\"",
                comment = null,
            )
        )
        variant.buildConfigFields?.put(
            "DEFAULT_FOREX_URL",
            com.android.build.api.variant.BuildConfigField(
                type = "String",
                value = "\"${forexUrl.get()}\"",
                comment = null,
            )
        )
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

    // Explicit JUnit 4 binding for kotlin.test — `kotlin("test")` alone doesn't
    // resolve a framework under AGP 9 + built-in Kotlin, leaving `kotlin.test.Test`
    // unresolved at compile time.
    testImplementation(kotlin("test-junit"))

    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Markdown rendering for agent chat replies
    implementation(libs.markdown.renderer.m3)
}
