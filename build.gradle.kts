plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.kotlin.android) apply false

    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.plugin.compose)
}
