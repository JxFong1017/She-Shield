// Top-level build file
plugins {
    // Reference the catalog using the alias defined in [plugins] section of toml
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false

    // Firebase Google Services
    alias(libs.plugins.google.gms.google.services) apply false
}