plugins {
    // AGP 9 ships built-in Kotlin — the org.jetbrains.kotlin.android plugin
    // is no longer needed (and is incompatible with AGP 9's new DSL).
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
